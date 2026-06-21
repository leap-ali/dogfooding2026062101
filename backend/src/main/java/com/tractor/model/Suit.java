package com.tractor.model;

public enum Suit {
    SPADE("♠", 4),
    HEART("♥", 3),
    CLUB("♣", 2),
    DIAMOND("♦", 1),
    JOKER("★", 5);

    private final String symbol;
    private final int priority;

    Suit(String symbol, int priority) {
        this.symbol = symbol;
        this.priority = priority;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getPriority() {
        return priority;
    }
}
