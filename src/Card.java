public class Card {
    private int value;
    private int suit;

    public Card(int value, int suit){
        this.value = value;
        this.suit = suit;
    }

    public int getValue() {
        return value;
    }

    public int getSuit() {
        return suit;
    }

    public void setSuit(int suitSet) {
        suit = suitSet;
    }

    public void setValue(int valueSet) {
        value = valueSet;
    }
}
