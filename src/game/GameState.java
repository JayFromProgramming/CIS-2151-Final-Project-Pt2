package game;

import cards.Deck;
import cards.dealer.Dealer;
import exceptions.player.NotEnoughMoneyException;
import player.Player;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameState extends Thread implements Serializable {
    //Fields
    public int potValue;
    public final List<Player> players;
    public final Deck deck;
    public final Dealer dealer;
    public boolean running;
    public int turn;
    public int initialBet;

    //Constructor
    public GameState(String... playerNames) {
        //Setting up the deck
        deck = new Deck(6);
        deck.genAndShuffle();

        //Dealer
        this.dealer = new Dealer();

        //Setting up players
        players = new ArrayList<>();
        int numberOfPlayers = playerNames.length;
        int iteration = 0;
        for (int player = 0; player < numberOfPlayers; player++) {
            players.add(new Player(playerNames[iteration]));
            iteration++;
        }
        running = true;
    }

    public void startNewRound() {
        if (deck.getCards().size() <= 30) {
            deck.genAndShuffle();
        }

        turn = -1;
        potValue = 0;
        initialBet = 1;

        for (Player player : players) {
            player.hand.drawCard(deck);
            player.hand.drawCard(deck);
            player.hand.sort();
        }
        //Making dealer draw
        while (dealer.value() < 17) {
            dealer.drawCard(deck);
        }
        dealer.sort();
    }

    public void checkForLastDoubleUp() {
        //Checking if this is the last double up
        boolean allPlayersSelected = true;
        for (Player player : players) {
            if (player.bankrupt) continue;
            if (player.busted) continue;
            if (player.doubleUp == 0) {
                allPlayersSelected = false;
                break;
            }
        }

        //If it is, make all players pay the initial bet according the the double up values
        if (allPlayersSelected) {
            for (Player player : players) {
                try {
                    player.bet(this, initialBet);
                } catch (NotEnoughMoneyException e) {
                    player.bankrupt = true;
                }
            }

            advanceTurn();
        }
    }

    public void advanceTurn() {
        boolean validPlayer = false;
        int player = turn + 1;
        while (!validPlayer) {
            if (player >= players.size()) player = 0;
            if (!players.get(player).busted && !players.get(player).standing && !players.get(player).bankrupt) {
                validPlayer = true;
            } else {
                player++;
            }
            if (player == turn) {
                endRound();
                return;
            }
        }
        turn = player;
    }

    public void endRound() {
        //Divvying up the pot to people who are higher than the dealer
        List<Player> winnningPlayers = new ArrayList<>();
        for (Player player : players) {
            if (player.hand.value() > dealer.value()) {
                winnningPlayers.add(player);
            }
        }
        for (Player player : winnningPlayers) {
            player.money += potValue / winnningPlayers.size();
        }


        //Clearing player hands
        for (Player player : players) {
            player.hand.clear();
            player.doubleUp = 0;
            player.busted = false;
            player.standing = false;
        }
        dealer.clear();

        //Start new round
        startNewRound();
    }

    public void hitPressed(int playerNumber) {
        Player player = players.get(playerNumber);
        if (turn >= 0 && turn % players.size() == playerNumber) {
            player.hand.drawCard(deck);
            player.hand.sort();
            if (player.hand.value() > 21) {
                player.busted = true;
            }
            advanceTurn();
        }
    }

    public void standPressed(int playerNumber) {
        Player player = players.get(playerNumber);
        if (turn >= 0 && turn % players.size() == playerNumber) {
            player.standing = true;
            advanceTurn();
        }
    }

    public void doubleUpPressed(int playerNumber) {

        //Setting the player to be double up
        if (turn == -1 && players.get(playerNumber).doubleUp == 0) {
            //Forcing them to draw a card and checking if they busted
            players.get(playerNumber).hand.drawCard(deck);
            players.get(playerNumber).hand.sort();
            if (players.get(playerNumber).hand.value() > 21) {
                players.get(playerNumber).busted = true;
            }

            players.get(playerNumber).doubleUp = 1;
            initialBet += initialBet;
        }

        checkForLastDoubleUp();
    }

    public void dontDoubleUpPressed(int playerNumber) {
        //Setting the player to be a not double up
        if (turn == -1 && players.get(playerNumber).doubleUp == 0) {
            players.get(playerNumber).doubleUp = -1;
        }

        checkForLastDoubleUp();
    }

    //Getters and setters
    public List<Player> getPlayers() {
        return players;
    }

    public boolean isRunning() {
        return running;
    }
}
