import java.util.ArrayList;

public class Hands {
    private boolean player;
    private int handValue;
    private ArrayList<Card> hand;
    private int money;
    private int bet;
    private int insuranceBet;
    private boolean firstHandPlayed;
    private boolean secondHandPlayed;

    public Hands (boolean player){
        this.player = player;
        hand = new ArrayList<>();
    }
    public Hands (boolean player, int money){
        this.player = player;
        this.money = money;
        hand = new ArrayList<>();
    }

    public void clearHand(){
        hand.clear();
        handValue =0;
    }

    public boolean isFirstHandPlayed() {
        return firstHandPlayed;
    }

    public boolean isSecondHandPlayed() {
        return secondHandPlayed;
    }

    public boolean isPlayer() {
        return player;
    }

    public int getBet() {
        return bet;
    }

    public int getHandValue() {
        return handValue;
    }

    public int getInsuranceBet() {
        return insuranceBet;
    }

    public int getMoney() {
        return money;
    }

    public void setBet(int bet) {
        this.bet = bet;
    }

    public void addBet(int increase){
        bet+=increase;
    }

    public void multiplyBet (double factor){
        bet*=factor;
    }

    public void setHand(ArrayList<Card> hand) {
        this.hand = hand;
    }

    public void setFirstHandPlayed(boolean firstHandPlayed) {
        this.firstHandPlayed = firstHandPlayed;
    }

    public void setHandValue(int handValue) {
        this.handValue = handValue;
    }

    public void addHandValue(int increase){
        handValue+=increase;
    }

    public void setInsuranceBet(int insuranceBet) {
        this.insuranceBet = insuranceBet;
    }

    public void addInsuranceBet(int increase){
        insuranceBet+=increase;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public void addMoney(int increase){
        money+=increase;
    }

    public void setPlayer(boolean player) {
        this.player = player;
    }

    public void setSecondHandPlayed(boolean secondHandPlayed) {
        this.secondHandPlayed = secondHandPlayed;
    }

    public ArrayList<Card> getHand() {
        return hand;
    }


}
