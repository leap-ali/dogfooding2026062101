package com.tractor.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PlayedCards {
    private String playerId;
    private String nickname;
    private int position;
    private List<Card> cards = new ArrayList<>();
    private String cardType;
    private boolean isWinning;

    public PlayedCards() {
    }

    public PlayedCards(String playerId, String nickname, int position, List<Card> cards) {
        this.playerId = playerId;
        this.nickname = nickname;
        this.position = position;
        this.cards = cards;
    }
}
