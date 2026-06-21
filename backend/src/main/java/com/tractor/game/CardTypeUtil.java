package com.tractor.game;

import com.tractor.model.Card;
import com.tractor.model.Rank;
import com.tractor.model.Suit;

import java.util.*;

public class CardTypeUtil {

    public static final String TYPE_SINGLE = "单张";
    public static final String TYPE_PAIR = "对子";
    public static final String TYPE_TRIPLE = "三张";
    public static final String TYPE_TRACTOR = "拖拉机";
    public static final String TYPE_THROW = "甩牌";

    public static String getCardType(List<Card> cards, Suit trumpSuit, Rank levelRank) {
        if (cards == null || cards.isEmpty()) {
            return null;
        }
        if (cards.size() == 1) {
            return TYPE_SINGLE;
        }
        Suit suit = cards.get(0).getSuit();
        boolean allSameSuit = cards.stream().allMatch(c -> c.getSuit() == suit);
        if (!allSameSuit) {
            return TYPE_THROW;
        }
        Map<Rank, Integer> rankCount = new HashMap<>();
        for (Card card : cards) {
            rankCount.put(card.getRank(), rankCount.getOrDefault(card.getRank(), 0) + 1);
        }
        boolean allPairs = rankCount.values().stream().allMatch(c -> c == 2);
        boolean allTriples = rankCount.values().stream().allMatch(c -> c == 3);
        if (allPairs && rankCount.size() == 1) {
            return TYPE_PAIR;
        }
        if (allTriples && rankCount.size() == 1) {
            return TYPE_TRIPLE;
        }
        if (allPairs && rankCount.size() >= 2 && isConsecutiveRanks(new ArrayList<>(rankCount.keySet()), trumpSuit, levelRank)) {
            return TYPE_TRACTOR;
        }
        return TYPE_THROW;
    }

    private static boolean isConsecutiveRanks(List<Rank> ranks, Suit trumpSuit, Rank levelRank) {
        if (ranks.size() < 2) {
            return false;
        }
        List<Integer> values = new ArrayList<>();
        for (Rank rank : ranks) {
            values.add(rank.getValue());
        }
        Collections.sort(values);
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) - values.get(i - 1) != 1) {
                return false;
            }
        }
        return true;
    }

    public static boolean canFollow(List<Card> handCards, List<Card> leadCards, Suit trumpSuit, Rank levelRank) {
        if (handCards.size() < leadCards.size()) {
            return false;
        }
        String leadType = getCardType(leadCards, trumpSuit, levelRank);
        Suit leadSuit = getLeadSuit(leadCards, trumpSuit);
        boolean isTrumpLead = isTrumpSuit(leadSuit, trumpSuit, levelRank);
        if (TYPE_SINGLE.equals(leadType)) {
            if (isTrumpLead) {
                return hasTrump(handCards, trumpSuit, levelRank);
            } else {
                return hasSuit(handCards, leadSuit);
            }
        }
        if (TYPE_PAIR.equals(leadType)) {
            if (isTrumpLead) {
                return hasTrumpPair(handCards, trumpSuit, levelRank);
            } else {
                return hasSuitPair(handCards, leadSuit);
            }
        }
        if (TYPE_TRIPLE.equals(leadType)) {
            if (isTrumpLead) {
                return hasTrumpTriple(handCards, trumpSuit, levelRank);
            } else {
                return hasSuitTriple(handCards, leadSuit);
            }
        }
        return true;
    }

    public static Suit getLeadSuit(List<Card> leadCards, Suit trumpSuit) {
        for (Card card : leadCards) {
            if (!card.isJoker()) {
                return card.getSuit();
            }
        }
        return trumpSuit;
    }

    private static boolean isTrumpSuit(Suit suit, Suit trumpSuit, Rank levelRank) {
        return suit == trumpSuit;
    }

    private static boolean hasSuit(List<Card> handCards, Suit suit) {
        return handCards.stream().anyMatch(c -> c.getSuit() == suit);
    }

    private static boolean hasTrump(List<Card> handCards, Suit trumpSuit, Rank levelRank) {
        return handCards.stream().anyMatch(c -> DeckUtil.isTrump(c, trumpSuit, levelRank));
    }

    private static boolean hasSuitPair(List<Card> handCards, Suit suit) {
        Map<Rank, Integer> count = new HashMap<>();
        for (Card card : handCards) {
            if (card.getSuit() == suit) {
                count.put(card.getRank(), count.getOrDefault(card.getRank(), 0) + 1);
            }
        }
        return count.values().stream().anyMatch(c -> c >= 2);
    }

    private static boolean hasTrumpPair(List<Card> handCards, Suit trumpSuit, Rank levelRank) {
        Map<Rank, Integer> count = new HashMap<>();
        for (Card card : handCards) {
            if (DeckUtil.isTrump(card, trumpSuit, levelRank)) {
                count.put(card.getRank(), count.getOrDefault(card.getRank(), 0) + 1);
            }
        }
        return count.values().stream().anyMatch(c -> c >= 2);
    }

    private static boolean hasSuitTriple(List<Card> handCards, Suit suit) {
        Map<Rank, Integer> count = new HashMap<>();
        for (Card card : handCards) {
            if (card.getSuit() == suit) {
                count.put(card.getRank(), count.getOrDefault(card.getRank(), 0) + 1);
            }
        }
        return count.values().stream().anyMatch(c -> c >= 3);
    }

    private static boolean hasTrumpTriple(List<Card> handCards, Suit trumpSuit, Rank levelRank) {
        Map<Rank, Integer> count = new HashMap<>();
        for (Card card : handCards) {
            if (DeckUtil.isTrump(card, trumpSuit, levelRank)) {
                count.put(card.getRank(), count.getOrDefault(card.getRank(), 0) + 1);
            }
        }
        return count.values().stream().anyMatch(c -> c >= 3);
    }
}
