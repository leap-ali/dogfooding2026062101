package com.tractor.game;

import com.tractor.model.Card;
import com.tractor.model.Rank;
import com.tractor.model.Suit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeckUtil {

    public static List<Card> createDoubleDeck() {
        List<Card> deck = new ArrayList<>();
        int cardId = 0;
        for (int deckNum = 0; deckNum < 2; deckNum++) {
            for (Suit suit : Suit.values()) {
                if (suit == Suit.JOKER) {
                    continue;
                }
                for (Rank rank : Rank.values()) {
                    if (rank == Rank.SMALL_JOKER || rank == Rank.BIG_JOKER) {
                        continue;
                    }
                    deck.add(new Card(suit, rank, cardId++));
                }
            }
            deck.add(new Card(Suit.JOKER, Rank.SMALL_JOKER, cardId++));
            deck.add(new Card(Suit.JOKER, Rank.BIG_JOKER, cardId++));
        }
        return deck;
    }

    public static void shuffle(List<Card> deck) {
        Collections.shuffle(deck);
    }

    public static boolean isTrump(Card card, Suit trumpSuit, Rank levelRank) {
        if (card.isJoker()) {
            return true;
        }
        if (card.getRank() == levelRank) {
            return true;
        }
        return card.getSuit() == trumpSuit;
    }

    public static int getCardPower(Card card, Suit trumpSuit, Rank levelRank) {
        if (card.isBigJoker()) {
            return 100;
        }
        if (card.isSmallJoker()) {
            return 90;
        }
        if (card.getRank() == levelRank) {
            if (card.getSuit() == trumpSuit) {
                return 80;
            }
            return 70;
        }
        if (card.getSuit() == trumpSuit) {
            return 50 + card.getRank().getValue();
        }
        return card.getRank().getValue();
    }

    public static int compareCards(Card c1, Card c2, Suit trumpSuit, Rank levelRank) {
        int p1 = getCardPower(c1, trumpSuit, levelRank);
        int p2 = getCardPower(c2, trumpSuit, levelRank);
        return Integer.compare(p1, p2);
    }
}
