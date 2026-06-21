package com.tractor.game;

import com.corundumstudio.socketio.SocketIOClient;
import com.tractor.model.*;
import lombok.Data;

import java.util.*;

@Data
public class GameRoom {

    private static GameRoom instance;

    private List<Player> players = new ArrayList<>();
    private GamePhase phase = GamePhase.WAITING;
    private List<Card> bottomCards = new ArrayList<>();
    private Suit trumpSuit;
    private Rank levelRank = Rank.TWO;
    private int bankerPosition = -1;
    private int currentPlayerPosition = -1;
    private int declareTurnPosition = -1;
    private int declareStartPosition = -1;
    private int declareRoundCount = 0;
    private List<PlayedCards> currentRound = new ArrayList<>();
    private List<List<PlayedCards>> roundHistory = new ArrayList<>();
    private int roundNumber = 0;

    private GameRoom() {
    }

    public static synchronized GameRoom getInstance() {
        if (instance == null) {
            instance = new GameRoom();
        }
        return instance;
    }

    public synchronized boolean addPlayer(String playerId, String nickname, SocketIOClient client) {
        if (players.size() >= 4) {
            return false;
        }
        Player player = new Player(playerId, nickname, client);
        player.setPosition(players.size());
        players.add(player);
        if (players.size() == 4) {
            startGame();
        }
        return true;
    }

    public synchronized void removePlayer(String playerId) {
        Player player = getPlayerById(playerId);
        if (player != null) {
            players.remove(player);
            for (int i = 0; i < players.size(); i++) {
                players.get(i).setPosition(i);
            }
            resetGame();
        }
    }

    public Player getPlayerById(String playerId) {
        for (Player player : players) {
            if (player.getId().equals(playerId)) {
                return player;
            }
        }
        return null;
    }

    public int getPlayerCount() {
        return players.size();
    }

    private void startGame() {
        phase = GamePhase.DECLARE_TRUMP;
        levelRank = Rank.TWO;
        roundNumber = 0;
        currentRound.clear();
        roundHistory.clear();
        bottomCards.clear();
        bankerPosition = -1;
        trumpSuit = null;
        List<Card> deck = DeckUtil.createDoubleDeck();
        DeckUtil.shuffle(deck);
        for (Player player : players) {
            player.getHandCards().clear();
            player.setBanker(false);
            player.setTeamA(player.getPosition() % 2 == 0);
        }
        for (int i = 0; i < 25; i++) {
            for (Player player : players) {
                player.addCard(deck.remove(0));
            }
        }
        bottomCards.addAll(deck);
        for (Player player : players) {
            sortHandCards(player);
        }
        Random random = new Random();
        declareTurnPosition = random.nextInt(4);
        declareStartPosition = declareTurnPosition;
        declareRoundCount = 0;
        currentPlayerPosition = declareTurnPosition;
    }

    private void sortHandCards(Player player) {
        List<Card> cards = player.getHandCards();
        cards.sort((c1, c2) -> {
            boolean t1 = DeckUtil.isTrump(c1, trumpSuit != null ? trumpSuit : Suit.SPADE, levelRank);
            boolean t2 = DeckUtil.isTrump(c2, trumpSuit != null ? trumpSuit : Suit.SPADE, levelRank);
            if (t1 && !t2) return -1;
            if (!t1 && t2) return 1;
            if (c1.getSuit() != c2.getSuit()) {
                return c1.getSuit().getPriority() - c2.getSuit().getPriority();
            }
            return c2.getRank().getValue() - c1.getRank().getValue();
        });
    }

    public synchronized boolean declareTrump(String playerId, Suit suit) {
        if (phase != GamePhase.DECLARE_TRUMP) {
            return false;
        }
        Player player = getPlayerById(playerId);
        if (player == null || player.getPosition() != declareTurnPosition) {
            return false;
        }
        boolean hasCard = false;
        for (Card card : player.getHandCards()) {
            if (card.getRank() == levelRank && card.getSuit() == suit) {
                hasCard = true;
                break;
            }
        }
        if (!hasCard) {
            return false;
        }
        trumpSuit = suit;
        bankerPosition = player.getPosition();
        player.setBanker(true);
        phase = GamePhase.BOTTOM_CARDS;
        currentPlayerPosition = bankerPosition;
        player.getHandCards().addAll(bottomCards);
        bottomCards.clear();
        sortHandCards(player);
        return true;
    }

    public synchronized boolean skipDeclare(String playerId) {
        if (phase != GamePhase.DECLARE_TRUMP) {
            return false;
        }
        Player player = getPlayerById(playerId);
        if (player == null || player.getPosition() != declareTurnPosition) {
            return false;
        }
        declareTurnPosition = (declareTurnPosition + 1) % 4;
        currentPlayerPosition = declareTurnPosition;
        if (declareTurnPosition == declareStartPosition) {
            declareRoundCount++;
            if (declareRoundCount >= 1 && trumpSuit == null) {
                randomTrump();
            }
        }
        return true;
    }

    private void randomTrump() {
        Suit[] suits = {Suit.SPADE, Suit.HEART, Suit.CLUB, Suit.DIAMOND};
        Random random = new Random();
        trumpSuit = suits[random.nextInt(4)];
        bankerPosition = declareTurnPosition;
        players.get(bankerPosition).setBanker(true);
        phase = GamePhase.BOTTOM_CARDS;
        currentPlayerPosition = bankerPosition;
        players.get(bankerPosition).getHandCards().addAll(bottomCards);
        bottomCards.clear();
        sortHandCards(players.get(bankerPosition));
    }

    public synchronized boolean discardBottomCards(String playerId, List<Integer> cardIds) {
        if (phase != GamePhase.BOTTOM_CARDS) {
            return false;
        }
        Player player = getPlayerById(playerId);
        if (player == null || !player.isBanker()) {
            return false;
        }
        if (cardIds.size() != 8) {
            return false;
        }
        List<Card> toBottom = new ArrayList<>();
        for (Integer cardId : cardIds) {
            Card card = findCardById(player.getHandCards(), cardId);
            if (card == null) {
                return false;
            }
            toBottom.add(card);
        }
        player.getHandCards().removeAll(toBottom);
        bottomCards.addAll(toBottom);
        sortHandCards(player);
        phase = GamePhase.PLAYING;
        currentPlayerPosition = bankerPosition;
        currentRound.clear();
        roundNumber = 0;
        return true;
    }

    private Card findCardById(List<Card> cards, int cardId) {
        for (Card card : cards) {
            if (card.getId() == cardId) {
                return card;
            }
        }
        return null;
    }

    public synchronized boolean playCards(String playerId, List<Integer> cardIds) {
        // #region debug-point H1-H6:play-cards-entry
        System.out.println("[DEBUG] playCards called - playerId=" + playerId + ", cardIds=" + cardIds + ", phase=" + phase + ", currentPlayerPos=" + currentPlayerPosition + ", currentRoundSize=" + currentRound.size());
        // #endregion
        if (phase != GamePhase.PLAYING) {
            // #region debug-point H1:play-wrong-phase
            System.out.println("[DEBUG] playCards rejected - wrong phase: " + phase);
            // #endregion
            return false;
        }
        // BUG 2 FIX: 如果上一轮已经结束（有4张牌），先清空再开始新轮
        if (!currentRound.isEmpty() && currentRound.size() == 4) {
            // #region debug-point H2:play-clear-last-round
            System.out.println("[DEBUG] playCards - clearing last round (size=4) before starting new round");
            // #endregion
            currentRound.clear();
        }
        Player player = getPlayerById(playerId);
        if (player == null || player.getPosition() != currentPlayerPosition) {
            // #region debug-point H1:play-wrong-player
            System.out.println("[DEBUG] playCards rejected - wrong player, expectedPos=" + currentPlayerPosition + ", playerPos=" + (player!=null?player.getPosition():"null"));
            // #endregion
            return false;
        }
        List<Card> cards = new ArrayList<>();
        for (Integer cardId : cardIds) {
            Card card = findCardById(player.getHandCards(), cardId);
            if (card == null) {
                return false;
            }
            cards.add(card);
        }
        if (cards.isEmpty()) {
            return false;
        }
        if (!currentRound.isEmpty()) {
            PlayedCards lead = currentRound.get(0);
            String leadType = CardTypeUtil.getCardType(lead.getCards(), trumpSuit, levelRank);
            String playType = CardTypeUtil.getCardType(cards, trumpSuit, levelRank);
            // #region debug-point H1,H6:play-follow-check
            System.out.println("[DEBUG] playCards follow check - leadType=" + leadType + ", playType=" + playType + ", leadSize=" + lead.getCards().size() + ", playSize=" + cards.size());
            // #endregion
            if (cards.size() != lead.getCards().size()) {
                // #region debug-point H1:play-wrong-size
                System.out.println("[DEBUG] playCards rejected - wrong size");
                // #endregion
                return false;
            }
            List<Card> handCardsBeforePlay = new ArrayList<>(player.getHandCards());
            boolean validFollow = CardTypeUtil.isValidFollow(cards, lead.getCards(), handCardsBeforePlay, trumpSuit, levelRank);
            // #region debug-point H1:play-valid-follow
            System.out.println("[DEBUG] playCards isValidFollow=" + validFollow + ", player=" + player.getNickname());
            // #endregion
            if (!validFollow) {
                // #region debug-point H1:play-invalid-follow
                System.out.println("[DEBUG] playCards rejected - invalid follow rule");
                // #endregion
                return false;
            }
        }
        String cardType = CardTypeUtil.getCardType(cards, trumpSuit, levelRank);
        PlayedCards playedCards = new PlayedCards(player.getId(), player.getNickname(), player.getPosition(), cards);
        playedCards.setCardType(cardType);
        
        if (currentRound.isEmpty()) {
            roundNumber++;
        }
        
        currentRound.add(playedCards);
        player.getHandCards().removeAll(cards);
        // #region debug-point H2:play-added
        System.out.println("[DEBUG] playCards success - player=" + player.getNickname() + ", cards=" + cards + ", type=" + cardType + ", roundSize=" + currentRound.size());
        // #endregion
        if (currentRound.size() == 4) {
            // #region debug-point H2:finish-round-start
            System.out.println("[DEBUG] playCards - 4 cards played, calling finishRound, currentRound=" + currentRound);
            // #endregion
            finishRound();
            // #region debug-point H2:finish-round-end
            System.out.println("[DEBUG] playCards - finishRound done, currentRound size=" + currentRound.size() + ", nextPlayer=" + currentPlayerPosition);
            // #endregion
        } else {
            currentPlayerPosition = (currentPlayerPosition + 1) % 4;
        }
        return true;
    }

    private void finishRound() {
        PlayedCards lead = currentRound.get(0);
        int winnerPos = lead.getPosition();
        int maxPower = calculateRoundPower(lead);
        // #region debug-point H2,H6:finish-round-lead
        System.out.println("[DEBUG] finishRound - lead: " + lead.getNickname() + ", power=" + maxPower + ", cards=" + lead.getCards());
        // #endregion
        for (PlayedCards pc : currentRound) {
            int power = calculateRoundPower(pc);
            // #region debug-point H6:finish-round-power
            System.out.println("[DEBUG] finishRound - player=" + pc.getNickname() + ", power=" + power + ", cards=" + pc.getCards() + ", canWin=" + (power >= 0));
            // #endregion
            if (power > maxPower) {
                maxPower = power;
                winnerPos = pc.getPosition();
            }
        }
        for (PlayedCards pc : currentRound) {
            pc.setWinning(pc.getPosition() == winnerPos);
        }
        roundHistory.add(new ArrayList<>(currentRound));
        currentPlayerPosition = winnerPos;
        boolean allEmpty = true;
        for (Player p : players) {
            if (!p.getHandCards().isEmpty()) {
                allEmpty = false;
                break;
            }
        }
        if (allEmpty) {
            // #region debug-point H5:game-over
            System.out.println("[DEBUG] finishRound - all cards played, GAME OVER! Resetting to WAITING...");
            // #endregion
            phase = GamePhase.GAME_OVER;
            // BUG 5 FIX: 对局结束后自动重置房间，恢复4人等待状态
            resetGame();
            // #region debug-point H5:game-over-reset
            System.out.println("[DEBUG] finishRound - reset complete, phase now: " + phase + ", playerCount: " + players.size());
            // #endregion
            return;
        }
        // #region debug-point H2:finish-round-clear
        System.out.println("[DEBUG] finishRound - NOT clearing currentRound here, will clear at next round start, winnerPos=" + winnerPos + ", allEmpty=" + allEmpty + ", currentRound size=" + currentRound.size());
        // #endregion
        // BUG 2 FIX: 移到下一轮出牌时clear，确保广播时前端能看到本轮出牌
        // currentRound.clear();
    }

    private int calculateRoundPower(PlayedCards playedCards) {
        List<Card> cards = playedCards.getCards();
        PlayedCards lead = currentRound.get(0);
        Suit leadSuit = CardTypeUtil.getLeadSuit(lead.getCards(), trumpSuit);
        boolean isTrumpLead = leadSuit == trumpSuit || lead.getCards().get(0).isJoker();
        boolean allTrump = cards.stream().allMatch(c -> DeckUtil.isTrump(c, trumpSuit, levelRank));
        if (!isTrumpLead && !allTrump) {
            boolean hasLeadSuit = cards.stream().anyMatch(c -> c.getSuit() == leadSuit);
            if (!hasLeadSuit) {
                return -1;
            }
        }
        if (isTrumpLead && !allTrump) {
            return -1;
        }
        int maxPower = 0;
        for (Card card : cards) {
            int power = DeckUtil.getCardPower(card, trumpSuit, levelRank);
            if (isTrumpLead || card.getSuit() == leadSuit || DeckUtil.isTrump(card, trumpSuit, levelRank)) {
                if (power > maxPower) {
                    maxPower = power;
                }
            }
        }
        return maxPower;
    }

    public synchronized void resetGame() {
        phase = GamePhase.WAITING;
        bottomCards.clear();
        trumpSuit = null;
        levelRank = Rank.TWO;
        bankerPosition = -1;
        currentPlayerPosition = -1;
        declareTurnPosition = -1;
        declareStartPosition = -1;
        declareRoundCount = 0;
        currentRound.clear();
        roundHistory.clear();
        roundNumber = 0;
        for (Player player : players) {
            player.getHandCards().clear();
            player.setBanker(false);
        }
    }

    public Map<String, Object> getPublicState() {
        Map<String, Object> state = new HashMap<>();
        state.put("phase", phase.name());
        state.put("phaseDesc", phase.getDescription());
        state.put("trumpSuit", trumpSuit != null ? trumpSuit.name() : null);
        state.put("trumpSuitSymbol", trumpSuit != null ? trumpSuit.getSymbol() : "");
        state.put("levelRank", levelRank.getDisplayName());
        state.put("bankerPosition", bankerPosition);
        state.put("currentPlayerPosition", currentPlayerPosition);
        state.put("roundNumber", roundNumber);
        state.put("playerCount", players.size());
        List<Map<String, Object>> playerInfos = new ArrayList<>();
        for (Player player : players) {
            Map<String, Object> pInfo = new HashMap<>();
            pInfo.put("id", player.getId());
            pInfo.put("nickname", player.getNickname());
            pInfo.put("position", player.getPosition());
            pInfo.put("handCardCount", player.getHandCardCount());
            pInfo.put("isBanker", player.isBanker());
            pInfo.put("isTeamA", player.isTeamA());
            playerInfos.add(pInfo);
        }
        state.put("players", playerInfos);
        state.put("currentRound", currentRound);
        state.put("bottomCardCount", bottomCards.size());
        return state;
    }

    public Map<String, Object> getPlayerState(String playerId) {
        Map<String, Object> state = getPublicState();
        Player player = getPlayerById(playerId);
        if (player != null) {
            state.put("myCards", player.getHandCards());
            state.put("myPosition", player.getPosition());
        }
        return state;
    }
}
