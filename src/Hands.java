import java.util.ArrayList;

public class Hands {
    boolean player;
    int handValue;
    ArrayList<Card> hand;
    int money;
    int bet;
    int insuranceBet;
    boolean firstHandPlayed;
    boolean secondHandPlayed;

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



}
