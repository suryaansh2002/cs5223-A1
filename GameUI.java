public class GameUI {
    public static void printGameSummary(GameState gameState, String playerID, String primaryPlayerID, String backupPlayerID) {
        System.out.println("Game Summary");
        System.out.println("Player ID: " + playerID);
        System.out.println("Primary Player ID: " + primaryPlayerID);
        System.out.println("Backup Player ID: " + backupPlayerID);
        System.out.println("Game State: " + gameState);
        GameUI.printMaze(gameState, primaryPlayerID, backupPlayerID);
    }

    public static void printMaze(GameState gameState, String primaryPlayerID, String backupPlayerID) {
        System.out.println("Maze:");
    }
}
