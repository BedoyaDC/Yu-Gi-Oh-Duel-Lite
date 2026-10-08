package ygo.model;

/** Posición en la que se juega una carta durante un turno. */
public enum Position {
    ATTACK("ATAQUE"),
    DEFENSE("DEFENSA");

    private final String label;

    Position(String label) { this.label = label; }

    public String getLabel() { return label; }
}
