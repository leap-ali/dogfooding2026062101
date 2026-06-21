package com.tractor.model;

import lombok.Data;

@Data
public class Card implements Comparable<Card> {
    private Suit suit;
    private Rank rank;
    private int id;

    public Card() {
    }

    public Card(Suit suit, Rank rank, int id) {
        this.suit = suit;
        this.rank = rank;
        this.id = id;
    }

    public boolean isJoker() {
        return suit == Suit.JOKER;
    }

    public boolean isBigJoker() {
        return suit == Suit.JOKER && rank == Rank.BIG_JOKER;
    }

    public boolean isSmallJoker() {
        return suit == Suit.JOKER && rank == Rank.SMALL_JOKER;
    }

    public String getDisplayText() {
        if (isJoker()) {
            return rank.getDisplayName();
        }
        return suit.getSymbol() + rank.getDisplayName();
    }

    @Override
    public int compareTo(Card other) {
        if (this.suit == Suit.JOKER && other.suit != Suit.JOKER) {
            return 1;
        }
        if (this.suit != Suit.JOKER && other.suit == Suit.JOKER) {
            return -1;
        }
        if (this.suit == Suit.JOKER && other.suit == Suit.JOKER) {
            return Integer.compare(this.rank.getValue(), other.rank.getValue());
        }
        int suitCompare = Integer.compare(this.suit.getPriority(), other.suit.getPriority());
        if (suitCompare != 0) {
            return suitCompare;
        }
        return Integer.compare(this.rank.getValue(), other.rank.getValue());
    }
}
