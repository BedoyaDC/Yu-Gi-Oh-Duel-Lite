package ygo.duel;

import ygo.model.Card;
import ygo.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Duel {
    public static final int CARDS_PER_PLAYER = 3;
    public static final int ROUNDS_TO_WIN = 2;
    public static final String PLAYER_NAME = "Jugador";
    public static final String AI_NAME = "Máquina";

    private final List<Card> playerHand;
    private final List<Card> aiHand;
    private final boolean[] playerUsed = new boolean[CARDS_PER_PLAYER];
    private final boolean[] aiUsed = new boolean[CARDS_PER_PLAYER];
    private final BattleListener listener;
    private final Random random = new Random();

    private int playerScore;
    private int aiScore;
    private int roundsPlayed;
    private boolean playerInitiative;
    private boolean active;

    public Duel(List<Card> playerHand, List<Card> aiHand, BattleListener listener) {
        if (playerHand.size() != CARDS_PER_PLAYER || aiHand.size() != CARDS_PER_PLAYER) {
            throw new IllegalArgumentException("Cada bando necesita exactamente " + CARDS_PER_PLAYER + " cartas.");
        }
        for (Card c : playerHand) if (c == null) throw new IllegalArgumentException("Faltan cartas del jugador.");
        for (Card c : aiHand) if (c == null) throw new IllegalArgumentException("Faltan cartas de la máquina.");
        this.playerHand = new ArrayList<>(playerHand);
        this.aiHand = new ArrayList<>(aiHand);
        this.listener = listener;
    }

    /** Inicia (o reinicia) el duelo: marcador en 0, cartas disponibles y turno inicial aleatorio. */
    public void start() {
        playerScore = 0;
        aiScore = 0;
        roundsPlayed = 0;
        java.util.Arrays.fill(playerUsed, false);
        java.util.Arrays.fill(aiUsed, false);
        playerInitiative = random.nextBoolean();
        active = true;

        listener.onDuelStarted(playerInitiative);
        listener.onScoreChanged(0, 0);
        listener.onRoundStarted(1, playerInitiative);
    }

    /**
     * Juega una ronda: el jugador usa la carta indicada en la posición indicada y la máquina responde al azar.
     */
    public void playRound(int playerIndex, Position playerPosition) {
        if (!active) throw new IllegalStateException("El duelo no está en curso.");
        if (playerIndex < 0 || playerIndex >= CARDS_PER_PLAYER || playerUsed[playerIndex]) {
            throw new IllegalArgumentException("Carta no disponible: " + playerIndex);
        }

        Card playerCard = playerHand.get(playerIndex);
        if (playerCard.isLink()) playerPosition = Position.ATTACK;

        // La máquina elige al azar entre sus cartas disponibles
        List<Integer> available = new ArrayList<>();
        for (int i = 0; i < CARDS_PER_PLAYER; i++) if (!aiUsed[i]) available.add(i);
        int aiIndex = available.get(random.nextInt(available.size()));
        Card aiCard = aiHand.get(aiIndex);
        Position aiPosition = aiCard.isLink() || random.nextBoolean() ? Position.ATTACK : Position.DEFENSE;

        playerUsed[playerIndex] = true;
        aiUsed[aiIndex] = true;
        roundsPlayed++;

        RoundResult result = resolve(roundsPlayed, playerIndex, playerCard, playerPosition,
                aiIndex, aiCard, aiPosition, playerInitiative);
        if (result.isPlayerWon()) playerScore++; else aiScore++;

        String winner = result.isPlayerWon() ? PLAYER_NAME : AI_NAME;
        listener.onRoundPlayed(result);
        listener.onTurn(describe(playerCard, playerPosition), describe(aiCard, aiPosition), winner);
        listener.onScoreChanged(playerScore, aiScore);

        if (playerScore >= ROUNDS_TO_WIN || aiScore >= ROUNDS_TO_WIN || roundsPlayed >= CARDS_PER_PLAYER) {
            active = false;
            listener.onDuelEnded(playerScore > aiScore ? PLAYER_NAME : AI_NAME);
        } else {
            playerInitiative = !playerInitiative; // la iniciativa alterna cada ronda
            listener.onRoundStarted(roundsPlayed + 1, playerInitiative);
        }
    }

    /** Aplica las reglas de comparación ATK vs DEF y devuelve quién gana y por qué. */
    static RoundResult resolve(int round, int playerIndex, Card pc, Position pp,
                               int aiIndex, Card ac, Position ap, boolean playerInitiative) {
        boolean playerWins;
        String why;

        if (pp == Position.ATTACK && ap == Position.ATTACK) {
            int a = pc.getAtk(), b = ac.getAtk();
            if (a != b) {
                playerWins = a > b;
                why = "Ambas en ATAQUE: ATK " + a + " vs ATK " + b + " → gana el mayor ATK.";
            } else {
                playerWins = playerInitiative;
                why = "Ambas en ATAQUE con ATK " + a + " (empate) → gana quien tiene la iniciativa.";
            }
        } else if (pp == Position.ATTACK) {            // jugador ataca, máquina defiende
            int atk = pc.getAtk(), def = ac.getDef();
            playerWins = atk > def;
            why = "ATAQUE del Jugador (ATK " + atk + ") contra DEFENSA de la Máquina (DEF " + def + ") → "
                    + (playerWins ? "el ataque supera la defensa." : "la defensa resiste.");
        } else if (ap == Position.ATTACK) {            // máquina ataca, jugador defiende
            int atk = ac.getAtk(), def = pc.getDef();
            boolean aiWins = atk > def;
            playerWins = !aiWins;
            why = "ATAQUE de la Máquina (ATK " + atk + ") contra DEFENSA del Jugador (DEF " + def + ") → "
                    + (aiWins ? "el ataque supera la defensa." : "la defensa resiste.");
        } else {                                       // ambas en defensa
            int a = pc.getDef(), b = ac.getDef();
            if (a != b) {
                playerWins = a > b;
                why = "Ambas en DEFENSA: DEF " + a + " vs DEF " + b + " → gana la mayor DEF.";
            } else {
                playerWins = playerInitiative;
                why = "Ambas en DEFENSA con DEF " + a + " (empate) → gana quien tiene la iniciativa.";
            }
        }
        return new RoundResult(round, playerIndex, pc, pp, aiIndex, ac, ap, playerWins, why);
    }

    /** Texto para el log: nombre [POSICIÓN valor]. */
    private static String describe(Card card, Position pos) {
        int value = pos == Position.ATTACK ? card.getAtk() : card.getDef();
        String stat = pos == Position.ATTACK ? "ATK " : "DEF ";
        return card.getName() + " [" + pos.getLabel() + " " + stat + value + "]";
    }

    public boolean isActive() { return active; }
    public boolean isPlayerInitiative() { return playerInitiative; }
    public int getRoundsPlayed() { return roundsPlayed; }
    public boolean isPlayerCardUsed(int index) { return playerUsed[index]; }
}
