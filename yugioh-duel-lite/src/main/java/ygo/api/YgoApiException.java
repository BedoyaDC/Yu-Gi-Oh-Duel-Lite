package ygo.api;

/** Error al consultar YGOProDeck. El mensaje es apto para mostrarlo al usuario. */
public class YgoApiException extends Exception {
    public YgoApiException(String message) { super(message); }
    public YgoApiException(String message, Throwable cause) { super(message, cause); }
}
