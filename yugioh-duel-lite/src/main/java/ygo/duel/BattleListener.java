package ygo.duel;

/**
 * Eventos del duelo. Desacopla la lógica (Duel) de la interfaz.
 * Los tres primeros métodos son los obligatorios del laboratorio; los demás son
 * opcionales (default) y ayudan a la UI a mostrar el estado sin consultar al Duel.
 * Todos se invocan en el mismo hilo que llama a Duel (en esta app, el EDT de Swing).
 */
public interface BattleListener {
    /** Se jugó un turno. Los textos incluyen nombre, posición y valor usado; winner es "Jugador" o "Máquina". */
    void onTurn(String playerCard, String aiCard, String winner);

    /** El marcador cambió (rondas ganadas). */
    void onScoreChanged(int playerScore, int aiScore);

    /** El duelo terminó: alguien ganó 2 rondas. */
    void onDuelEnded(String winner);

    /** El duelo comenzó; playerStarts indica quién tiene el turno inicial (definido al azar). */
    default void onDuelStarted(boolean playerStarts) { }

    /** Comienza una ronda: el jugador ya puede elegir carta. */
    default void onRoundStarted(int round, boolean playerHasInitiative) { }

    /** Detalle de la ronda jugada (índices de cartas, posiciones, ganador y explicación). */
    default void onRoundPlayed(RoundResult result) { }
}
