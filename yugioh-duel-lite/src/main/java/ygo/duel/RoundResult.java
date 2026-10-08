package ygo.duel;

import ygo.model.Card;
import ygo.model.Position;

/** Resultado inmutable de una ronda. */
public class RoundResult {
    private final int round;
    private final int playerIndex;
    private final Card playerCard;
    private final Position playerPosition;
    private final int aiIndex;
    private final Card aiCard;
    private final Position aiPosition;
    private final boolean playerWon;
    private final String explanation;

    public RoundResult(int round, int playerIndex, Card playerCard, Position playerPosition,
                       int aiIndex, Card aiCard, Position aiPosition,
                       boolean playerWon, String explanation) {
        this.round = round;
        this.playerIndex = playerIndex;
        this.playerCard = playerCard;
        this.playerPosition = playerPosition;
        this.aiIndex = aiIndex;
        this.aiCard = aiCard;
        this.aiPosition = aiPosition;
        this.playerWon = playerWon;
        this.explanation = explanation;
    }

    public int getRound() { return round; }
    public int getPlayerIndex() { return playerIndex; }
    public Card getPlayerCard() { return playerCard; }
    public Position getPlayerPosition() { return playerPosition; }
    public int getAiIndex() { return aiIndex; }
    public Card getAiCard() { return aiCard; }
    public Position getAiPosition() { return aiPosition; }
    public boolean isPlayerWon() { return playerWon; }
    public String getExplanation() { return explanation; }
}
