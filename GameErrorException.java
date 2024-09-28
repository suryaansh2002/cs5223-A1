public class GameErrorException extends Exception {

    private String message;

    GameErrorException(String message) {
        this.message = message;
    }
}
