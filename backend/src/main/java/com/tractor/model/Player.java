package com.tractor.model;

import com.corundumstudio.socketio.SocketIOClient;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Player {
    private String id;
    private String nickname;
    private int position;
    private List<Card> handCards = new ArrayList<>();
    private boolean isBanker;
    private boolean isTeamA;
    private transient SocketIOClient client;

    public Player() {
    }

    public Player(String id, String nickname, SocketIOClient client) {
        this.id = id;
        this.nickname = nickname;
        this.client = client;
    }

    public int getHandCardCount() {
        return handCards.size();
    }

    public void addCard(Card card) {
        handCards.add(card);
    }

    public void removeCards(List<Card> cards) {
        handCards.removeAll(cards);
    }
}
