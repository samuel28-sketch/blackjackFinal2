// Graphics Libraries
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.Random;

public class BlackJackGame implements Runnable, KeyListener {

    final int WIDTH = 1000;
    final int HEIGHT = 700;
    static final int DEALER_DELAY_MS = 1000;
    static final int ROUND_END_DELAY_MS = 1500;
    static final int DECK_X = 820;
    static final int DECK_Y = 80;

    Hands player;
    Hands dealer;
    ArrayList<Card> deck = new ArrayList<>();
    ArrayList<Card> discard = new ArrayList<>();
    ArrayList<FlyingCard> flyingCards = new ArrayList<>();
    ArrayList<Hands> playerHands = new ArrayList<>();
    int handIndex;
    Random random = new Random();
    boolean blackjack;

    int aceInHand;
    int highScore;
    boolean insurance;
    boolean insuranceBetting;
    String betStr;

    boolean firstHandPlayed;
    boolean secondHandPlayed;
    long dealerDrawTime = 0;
    boolean dealerThinking = false;
    boolean roundEnding = false;
    long roundEndTime = 0;

    ArrayList<Confetti> confettiList = new ArrayList<>();
    boolean showingBlackjack = false;
    long blackjackStartTime = 0;
    static final int BLACKJACK_DURATION_MS = 3000;
    float blackjackPulse = 0f;

    class FlyingCard {
        Card card;
        Hands target;
        float x, y;
        float startX, startY;
        float endX, endY;
        float progress;
        float rotation;
        boolean done;

        FlyingCard(Card card, Hands target, float startX, float startY, float endX, float endY) {
            this.card = card;
            this.target = target;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.x = startX;
            this.y = startY;
            this.progress = 0;
            this.rotation = 0;
            this.done = false;
        }
    }

    class Confetti {
        float x, y;
        float vx, vy;
        float rotation;
        float rotSpeed;
        Color color;
        int w, h;

        Confetti(float x, float y, float vx, float vy, float rotSpeed, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.rotation = 0;
            this.rotSpeed = rotSpeed;
            this.color = color;
            this.w = 10 + (int)(Math.random() * 8);
            this.h = 5 + (int)(Math.random() * 5);
        }
    }

    public BlackJackGame() {
        setUpGraphics();
        player = new Hands(true, 2500);
        playerHands.add(player);
        dealer = new Hands(false);
        for (int y = 1; y < 14; y++) {
            for (int x = 1; x < 5; x++) {
                Card newCard = new Card(y, x);
                deck.add(newCard);
            }
        }
    }

    public void startBlackjackCelebration() {
        showingBlackjack = true;
        blackjackStartTime = System.currentTimeMillis();
        blackjackPulse = 0f;
        confettiList.clear();
        Color[] colors = {
                new Color(255, 215, 0),
                new Color(255, 50, 50),
                new Color(50, 200, 255),
                new Color(255, 255, 255),
                new Color(180, 0, 255),
                new Color(50, 255, 100)
        };
        for (int i = 0; i < 120; i++) {
            float x = (float)(Math.random() * 1000);
            float y = (float)(Math.random() * -200);
            float vx = (float)(Math.random() * 4 - 2);
            float vy = (float)(Math.random() * 3 + 1);
            float rotSpeed = (float)(Math.random() * 8 - 4);
            Color color = colors[(int)(Math.random() * colors.length)];
            confettiList.add(new Confetti(x, y, vx, vy, rotSpeed, color));
        }
    }

    public void moveThings() {
        // Blackjack celebration
        if (showingBlackjack) {
            blackjackPulse += 0.08f;
            for (Confetti c : confettiList) {
                c.x += c.vx;
                c.y += c.vy;
                c.vy += 0.05f;
                c.rotation += c.rotSpeed;
            }
            confettiList.removeIf(c -> c.y > HEIGHT + 20);
            if (System.currentTimeMillis() - blackjackStartTime >= BLACKJACK_DURATION_MS) {
                showingBlackjack = false;
                confettiList.clear();
                if (handIndex>0){handIndex--;} //HUMAN CHANGE MADE TO BLACKJACK CELEBRATION
                else {
                    startEndRound();
                }
            }
            return;
        }

        for (FlyingCard fc : flyingCards) {
            fc.progress += 0.05f;
            fc.rotation += 15;
            fc.x = fc.startX + (fc.endX - fc.startX) * fc.progress;
            fc.y = fc.startY + (fc.endY - fc.startY) * fc.progress;

            if (fc.progress >= 1.0f) {
                fc.done = true;
                fc.target.getHand().add(fc.card);
                printHand(fc.target);
                if (fc.target.isPlayer()) {
                    if (fc.target.getHandValue() > 21) {

                        if (playerHands.size()>1) {
                            if (handIndex > 0) {
                                handIndex--;
                            }
                            else {
                                startDealerAI();
                            }
                        }
                        else {
                            startEndRound();
                        }

                    } else if (fc.target.getHandValue() == 21 && fc.target.getHand().size() == 2) {
                        blackjack = true;
                        startBlackjackCelebration();
                    } else if (fc.target.getHandValue() == 21) {
                        if (handIndex > 0) {
                            handIndex--;
                        } else {
                            startDealerAI();
                        }
                    }
                }
            }
        }
        flyingCards.removeIf(fc -> fc.done);

        if (dealerThinking && flyingCards.isEmpty()) {
            if (System.currentTimeMillis() - dealerDrawTime >= DEALER_DELAY_MS) {
                if (!playerHands.get(handIndex).isSecondHandPlayed()) {
                    draw(dealer);
                    playerHands.get(handIndex).setSecondHandPlayed(true);
                    dealerDrawTime = System.currentTimeMillis();
                } else if (dealer.getHandValue() < player.getHandValue() && dealer.getHandValue() <= 21) {
                    draw(dealer);
                    dealerDrawTime = System.currentTimeMillis();
                } else {
                    dealerThinking = false;
                    startEndRound();
                }
            }
        }

        if (roundEnding) {
            if (System.currentTimeMillis() - roundEndTime >= ROUND_END_DELAY_MS) {
                roundEnding = false;
                endRound();
            }
        }
    }

    private void render() {
        Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();
        g.clearRect(0, 0, WIDTH, HEIGHT);

        drawBackground(g);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g.drawString("DEALER", WIDTH / 2 - 35, 65);
        drawHand(g, dealer, 80, false);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g.drawString("PLAYER", WIDTH / 2 - 35, 385);

        for (int i = 0; i < playerHands.size(); i++) {
            Hands hand = playerHands.get(i);
            int slotX = getHandSlotX(i);
            drawHandAt(g, hand, slotX, 400);

            if (i == handIndex) {
                drawActiveHandBorder(g, hand, slotX, 400);
            }
        }

        drawDeck(g);

        for (FlyingCard fc : flyingCards) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.rotate(Math.toRadians(fc.rotation), fc.x + 40, fc.y + 60);
            drawCardBack(g2, (int) fc.x, (int) fc.y);
            g2.dispose();
        }

        drawHUD(g);

        if (showingBlackjack) {
            drawBlackjackCelebration(g);
        }

        g.dispose();
        bufferStrategy.show();
    }

    private int getHandSlotX(int index) {
        int numHands = playerHands.size();
        int slotWidth = (WIDTH - 40) / numHands;
        int slotCenterX = 20 + index * slotWidth + slotWidth / 2;

        Hands hand = playerHands.get(index);
        int n = hand.getHand().size();
        if (n == 0) {
            return slotCenterX;
        }
        int totalWidth = n * 80 + (n - 1) * 15;
        return slotCenterX - totalWidth / 2;
    }

    private void drawHandAt(Graphics2D g, Hands hand, int startX, int yPos) {
        int n = hand.getHand().size();
        if (n == 0) return;
        for (int i = 0; i < n; i++) {
            int cx = startX + i * 95;
            drawCard(g, hand.getHand().get(i), cx, yPos);
        }
    }

    private void drawActiveHandBorder(Graphics2D g, Hands hand, int startX, int yPos) {
        int n = hand.getHand().size();
        if (n == 0) return;
        int totalWidth = n * 80 + (n - 1) * 15;
        g.setColor(new Color(255, 215, 0));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(startX - 5, yPos - 5, totalWidth + 10, 130, 10, 10);
    }

    private void drawBlackjackCelebration(Graphics2D g) {
        for (Confetti c : confettiList) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.rotate(Math.toRadians(c.rotation), c.x + c.w / 2f, c.y + c.h / 2f);
            g2.setColor(c.color);
            g2.fillRect((int)c.x, (int)c.y, c.w, c.h);
            g2.dispose();
        }

        float alpha = 0.6f + 0.1f * (float)Math.sin(blackjackPulse * 2);
        g.setColor(new Color(0, 0, 0, (int)(alpha * 180)));
        g.fillRoundRect(WIDTH / 2 - 280, HEIGHT / 2 - 80, 560, 160, 30, 30);

        float borderPulse = (float)(0.5 + 0.5 * Math.sin(blackjackPulse * 3));
        int borderAlpha = (int)(150 + 105 * borderPulse);
        g.setColor(new Color(255, 215, 0, borderAlpha));
        g.setStroke(new BasicStroke(4));
        g.drawRoundRect(WIDTH / 2 - 280, HEIGHT / 2 - 80, 560, 160, 30, 30);

        float scale = 1.0f + 0.08f * (float)Math.sin(blackjackPulse * 2.5f);
        int fontSize = (int)(72 * scale);
        g.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        FontMetrics fm = g.getFontMetrics();
        String text = "BLACKJACK!";

        g.setColor(new Color(0, 0, 0, 180));
        g.drawString(text, WIDTH / 2 - fm.stringWidth(text) / 2 + 3, HEIGHT / 2 + fm.getAscent() / 2 - 10 + 3);

        GradientPaint goldGrad = new GradientPaint(
                WIDTH / 2 - 200, HEIGHT / 2 - 40, new Color(255, 240, 100),
                WIDTH / 2 + 200, HEIGHT / 2 + 40, new Color(200, 140, 0)
        );
        g.setPaint(goldGrad);
        g.drawString(text, WIDTH / 2 - fm.stringWidth(text) / 2, HEIGHT / 2 + fm.getAscent() / 2 - 10);

        g.setFont(new Font("Segoe UI", Font.ITALIC, 22));
        g.setColor(new Color(255, 255, 200));
        String sub = "+$" + (int)(playerHands.get(handIndex).getBet() * 1.5) + "  Natural 21!";
        fm = g.getFontMetrics();
        g.drawString(sub, WIDTH / 2 - fm.stringWidth(sub) / 2, HEIGHT / 2 + 55);
    }

    private void drawBackground(Graphics2D g) {
        GradientPaint gp = new GradientPaint(0, 0, new Color(30, 100, 35), 0, HEIGHT, new Color(15, 60, 20));
        g.setPaint(gp);
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void drawDeck(Graphics2D g) {
        int layers = Math.min(deck.size(), 10);
        for (int i = 0; i < layers; i++) {
            int offsetX = DECK_X + (10 - i);
            int offsetY = DECK_Y + (10 - i);
            g.setColor(new Color(20, 40, 120));
            g.fillRoundRect(offsetX, offsetY, 80, 120, 10, 10);
            g.setColor(new Color(40, 70, 180));
            g.setStroke(new BasicStroke(1));
            g.drawRoundRect(offsetX, offsetY, 80, 120, 10, 10);
        }
        drawCardBack(g, DECK_X, DECK_Y);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g.drawString(deck.size() + "", DECK_X + 25, DECK_Y + 145);
    }

    private void drawHand(Graphics2D g, Hands hand, int yPos, boolean hideFirst) {
        int n = hand.getHand().size();
        if (n == 0) return;
        int totalWidth = n * 80 + (n - 1) * 15;
        int startX = (WIDTH - totalWidth) / 2;
        for (int i = 0; i < n; i++) {
            int cx = startX + i * 95;
            if (i == 0 && hideFirst) {
                drawCardBack(g, cx, yPos);
            } else {
                drawCard(g, hand.getHand().get(i), cx, yPos);
            }
        }
    }

    private void drawCard(Graphics2D g, Card card, int x, int y) {
        int W = 80, H = 120, ARC = 10;
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, W, H, ARC, ARC);
        g.setColor(new Color(180, 180, 180));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x, y, W, H, ARC, ARC);
        boolean red = (card.getSuit() == 1 || card.getSuit() == 3);
        g.setColor(red ? new Color(200, 20, 20) : Color.BLACK);
        String rank = toRank(card.getValue());
        String sym = toSuit(card.getSuit());
        g.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g.drawString(rank, x + 6, y + 18);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g.drawString(sym, x + 6, y + 32);
        g.setFont(new Font("Segoe UI", Font.BOLD, 36));
        FontMetrics fm = g.getFontMetrics();
        int sx = x + (W - fm.stringWidth(sym)) / 2;
        int sy = y + H / 2 + fm.getAscent() / 2 - 4;
        g.drawString(sym, sx, sy);
    }

    private void drawCardBack(Graphics2D g, int x, int y) {
        int W = 80, H = 120, ARC = 10;
        g.setColor(new Color(20, 40, 120));
        g.fillRoundRect(x, y, W, H, ARC, ARC);
        Shape clip = new java.awt.geom.RoundRectangle2D.Float(x, y, W, H, ARC, ARC);
        Shape oldClip = g.getClip();
        g.setClip(clip);
        g.setColor(new Color(40, 70, 180));
        g.setStroke(new BasicStroke(1));
        for (int i = -H; i < W + H; i += 10) {
            g.drawLine(x + i, y, x + i + H, y + H);
            g.drawLine(x + i + H, y, x + i, y + H);
        }
        g.setClip(oldClip);
        g.setColor(new Color(60, 100, 200));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(x + 5, y + 5, W - 10, H - 10, ARC, ARC);
    }

    private void drawHUD(Graphics2D g) {
        int hudY = HEIGHT - 75;
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, hudY, WIDTH, 75);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 16));
        g.drawString("Money: $" + player.getMoney(), 20, hudY + 25);

        String dealerStr = "Dealer: " + dealer.getHandValue();
        String playerStr = "Player: " + player.getHandValue();
        String center = dealerStr + "   |   " + playerStr;
        FontMetrics fm = g.getFontMetrics();
        g.drawString(center, (WIDTH - fm.stringWidth(center)) / 2, hudY + 25);

        String instr;
        if (!firstHandPlayed) {
            instr = "E = Deal   R = +Bet   F = -Bet";
        } else if (firstHandPlayed && !playerHands.get(handIndex).isSecondHandPlayed()) {
            instr = "E = Hit   R = Stand";
        } else {
            instr = "Press E to Deal";
        }
        g.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        fm = g.getFontMetrics();
        g.drawString(instr, WIDTH - fm.stringWidth(instr) - 20, hudY + 25);

        g.setFont(new Font("Segoe UI", Font.BOLD, 18));
        fm = g.getFontMetrics();
        if (!insuranceBetting) {
            betStr = "Bet: $" + player.getBet();
        }
        else {
            betStr = "Insurance Bet: $" + player.getInsuranceBet();
        }
        g.setColor(new Color(255, 215, 0));
        g.drawString(betStr, (WIDTH - fm.stringWidth(betStr)) / 2, hudY + 55);

        String highSCore = "High Score: $" + highScore;
        g.drawString(highSCore, 20, hudY + 55);

    }

    private String toRank(int v) {
        if (v == 1) return "A";
        if (v == 11) return "Q";
        if (v == 12) return "K";
        if (v == 13) return "J";
        return String.valueOf(v);
    }

    private String toSuit(int s) {
        if (s == 1) return "\u2666";
        if (s == 2) return "\u2660";
        if (s == 3) return "\u2665";
        if (s == 4) return "\u2663";
        return "?";
    }

    public JFrame frame;
    public Canvas canvas;
    public JPanel panel;
    public BufferStrategy bufferStrategy;

    public static void main(String[] args) {
        BlackJackGame ex = new BlackJackGame();
        new Thread(ex).start();
    }

    public void run() {
        while (true) {
            moveThings();
            render();
            pause(10);
            if (player.getMoney() <=0){
                player.setMoney(2500);
                deck.addAll(discard);
                discard.clear();
            }
        }
    }

    public void pause(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }

    private Image getImage(String filename) {
        return Toolkit.getDefaultToolkit().getImage(filename);
    }

    private void setUpGraphics() {
        frame = new JFrame("Application Template");
        panel = (JPanel) frame.getContentPane();
        panel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        panel.setLayout(null);
        canvas = new Canvas();
        canvas.setBounds(0, 0, WIDTH, HEIGHT);
        canvas.setIgnoreRepaint(true);
        canvas.addKeyListener(this);
        panel.add(canvas);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);
        canvas.createBufferStrategy(2);
        bufferStrategy = canvas.getBufferStrategy();
        canvas.requestFocus();
        System.out.println("DONE graphic setup");
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (showingBlackjack) {
            return;
        }

        int keycode = e.getKeyCode();

        if (keycode == KeyEvent.VK_E) {
            if (firstHandPlayed && !dealerThinking && !roundEnding && flyingCards.isEmpty()&&!insuranceBetting) {
                draw(playerHands.get(handIndex));
                if (handIndex>0) {
                    if (playerHands.get(handIndex).isFirstHandPlayed()) {
                        playerHands.get(handIndex).setSecondHandPlayed(true);
                    }
                    playerHands.get(handIndex).setFirstHandPlayed(true);
                }
                else {
                    playerHands.get(handIndex).setSecondHandPlayed(true);}
               // else if (){}
            } else if (!firstHandPlayed && player.getBet() > 0) {
                draw(dealer);
                draw(player);
                draw(player);
                firstHandPlayed = true;
            }
        }
//started adding hand.second hand boolean below
        if (keycode == KeyEvent.VK_Q &&firstHandPlayed&&!playerHands.get(handIndex).isSecondHandPlayed() && player.getBet() <=(player.getMoney() /2)){
            player.multiplyBet(2);
            draw(playerHands.get(handIndex));
            if (playerHands.get(handIndex).getHandValue() >21){
                startEndRound(); //STILL DIDNT FIX IT
            }
            else if (handIndex==0) {
                startDealerAI();
            }
        }
        if (keycode == KeyEvent.VK_A&&firstHandPlayed&&!playerHands.get(handIndex).isSecondHandPlayed() && player.getBet() <(player.getMoney() /2)&&((playerHands.get(handIndex).getHand().getLast().getValue()== playerHands.get(handIndex).getHand().getFirst().getValue())||(playerHands.get(handIndex).getHand().getLast().getValue()>=10&& playerHands.get(handIndex).getHand().getFirst().getValue()>=10))){
            Hands splitHand = new Hands(true);
            splitHand.getHand().add(playerHands.get(handIndex).getHand().getLast());
            playerHands.get(handIndex).getHand().remove(player.getHand().getLast());
            playerHands.add(splitHand);
            handIndex++;
        }
        if (keycode == KeyEvent.VK_C){
            System.out.println(playerHands.get(handIndex).isSecondHandPlayed());
            System.out.println(playerHands.get(handIndex).isFirstHandPlayed());
            //System.out.println(playerHands.get(handIndex).hand.getLast());
            //System.out.println(playerHands.get(handIndex).hand.getFirst());

        }

        if (keycode==KeyEvent.VK_T&&firstHandPlayed&&!secondHandPlayed&& dealer.getHand().getFirst().getValue()==1){
            if (insuranceBetting){
                insuranceBetting=false;
                player.addMoney(-player.getInsuranceBet());
            }
            else{
                insuranceBetting = true;
            }
            insurance=true;
            //System.out.println("INSURANCE ACTIVE");
        }

        if (keycode == KeyEvent.VK_R && player.getBet() < player.getMoney() && (!firstHandPlayed||(insuranceBetting&& player.getInsuranceBet() <(player.getBet() /2)))) {
            player.addBet(100);
            if (insurance){player.addInsuranceBet(100);}
        }

        if (keycode == KeyEvent.VK_F && player.getBet() > 100 && (!firstHandPlayed||insuranceBetting)) {
            player.addBet(-100);
            if (insurance){
                player.addInsuranceBet(-100);
            }
        }

        if (keycode == KeyEvent.VK_R && firstHandPlayed && !dealerThinking && !roundEnding && flyingCards.isEmpty() && !insuranceBetting) {
            if (handIndex>0){handIndex--;}
            else {
                startDealerAI();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    public void draw(Hands hand) {
        int deckSize = deck.size();
        if (deckSize == 0) {
            deck.addAll(discard);
            discard.clear();
        }

        Card card = deck.get(random.nextInt(deck.size()));
        deck.remove(card);
        discard.add(card);

        int futureN = hand.getHand().size() + 1;
        int totalWidth = futureN * 80 + (futureN - 1) * 15;

        int startXPos;
        if (hand.isPlayer()) {
            int handIndexInList = playerHands.indexOf(hand);
            startXPos = getHandSlotX(handIndexInList);
        } else {
            startXPos = (WIDTH - totalWidth) / 2;
        }

        int endX = startXPos + hand.getHand().size() * 95;
        int endY = hand.isPlayer() ? 400 : 80;

        FlyingCard fc = new FlyingCard(card, hand, DECK_X, DECK_Y, endX, endY);
        flyingCards.add(fc);
    }

    public void updateHand(Hands hand) {
        hand.setHandValue(0);
        for (Card card : hand.getHand()) {
            if (card.getValue() > 1 && card.getValue() <= 10) {
                hand.addHandValue(card.getValue());
            } else if (card.getValue() >= 11 && card.getValue() <= 13) {
                hand.addHandValue(10);
            } else if (card.getValue() == 1) {
                if (hand.getHandValue() < 12) {
                    hand.addHandValue(11);
                } else {
                    hand.addHandValue(1);
                }
            }
        }
    }

    public void printHand(Hands hand) {
        String value = "";
        hand.setHandValue(0);
        System.out.println();
        if (hand.isPlayer()) {
            System.out.println("player's hand:");
        } else {
            System.out.println("dealer's hand:");
        }
        for (Card  card : hand.getHand()) {
            if (card.getValue() > 1 && card.getValue() <= 10) {
                value += card.getValue() + " of";
                hand.addHandValue(card.getValue());
            } else if (card.getValue() == 11) {
                value += "queen of";
                hand.addHandValue(10);
            } else if (card.getValue() == 12) {
                value += "king of";
                hand.addHandValue(10);
            } else if (card.getValue() == 13) {
                value += "jack of";
                hand.addHandValue(10);
            } else if (card.getValue() == 1) {
                value += "ace of";
                aceInHand++;
            }

            if (card.getSuit() == 1) {
                value += " diamonds";
            } else if (card.getSuit() == 2) {
                value += " spades";
            } else if (card.getSuit() == 3) {
                value += " hearts";
            } else if (card.getSuit() == 4) {
                value += " clubs";
            }
            System.out.println(value);
            value = "";
        }
for (int i=0; i<aceInHand;i++) {
    if (hand.getHandValue() < 12) {
        hand.addHandValue(11);
    } else if (hand.getHandValue() >= 10) {
        hand.addHandValue(1);
    }
}
aceInHand =0;

        System.out.println(hand.getHandValue());
    }

    public void startDealerAI() {
        dealerThinking = true;
        dealerDrawTime = System.currentTimeMillis();
    }

    public void startEndRound() {
        roundEnding = true;
        roundEndTime = System.currentTimeMillis();
    }

    public void endRound() {

        for (Hands player : playerHands) {
            if (player.getHandValue() > 21) {
                this.player.addMoney( -player.getBet());
            } else if (dealer.getHandValue() > 21) {
                this.player.addMoney(player.getBet());
            } else if (player.getHandValue() == 21) {
                if (player.getHand().size() == 2 && !(dealer.getHand().size() == 2)) {
                    this.player.addMoney( (int)(player.getBet() * 1.5) );
                } else if (player.getHandValue() != dealer.getHandValue()) {
                    this.player.addMoney(player.getBet());
                }
            } else if (player.getHandValue() < dealer.getHandValue()) {
                this.player.addMoney(-player.getBet());
            } else if (player.getHandValue() > dealer.getHandValue()) {
                this.player.addMoney(player.getBet());
            }

            if (insurance){
                if (dealer.getHand().size()==2&& dealer.getHandValue() ==21){
                    player.addMoney(player.getInsuranceBet());
                }
                insurance = false;
            }
        }

        if (player.getMoney() >highScore){
            highScore= player.getMoney();
        }

        player.setBet(0);
        for (Hands player:playerHands) {
            player.clearHand();
        }
//need to fix splits, need to add split cards to discard, need to fix it so that you cant split after drawing, and double prob has the same bug
        dealer.clearHand();
        System.out.println(player.getMoney());
        playerHands.clear();
        playerHands.add(player);
        handIndex = 0;
        firstHandPlayed = false;
        playerHands.get(handIndex).setSecondHandPlayed(false);
        blackjack = false;
    }
}

//NOTES: