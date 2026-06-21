package com.tractor.model;

public enum GamePhase {
    WAITING("等待中"),
    DECLARE_TRUMP("亮主阶段"),
    BOTTOM_CARDS("扣底阶段"),
    PLAYING("出牌阶段"),
    GAME_OVER("对局结束");

    private final String description;

    GamePhase(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
