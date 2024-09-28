import java.io.Serializable;
import java.util.*;



// Object implementing State of the Game
public class GameState implements Serializable {

    // Grid Size N Obtained from tracker
    private int gridSize;

    // No. of Treasures K obtained from tracker
    private int totalTreasures;

    // NxN Map tracking position of players in the game.
    private String[][] playerPositionGrid;

    // NxN Map tracking position of treasures in the game.
    public int[][] treasurePositionGrid;

    // Map
    private Map<String, Position> playerPositionMap;

    // Map of player name to number of moves made
    public Map<String, Integer> playerLastMoveMap;


    // Map of player to number of treasures collected
    private Map<String, Integer> playerTreasureMap;

    public GameState(int n, int k) {
        
        this.gridSize = n;
        this.totalTreasures = k;
        
    }

    // Function to be used by primary server to add a new player to game.
    public synchronized void addNewPlayerToGame(String playerName) {
    }


    // Function to get a random position in the grid
    // private Position getRandomPosition() {
    // }

    // Function to get get a random available position on the grid
    // private Position getAvailablePosition() {
    // }

    // Check if a particular cell in grid contains a player
    // private boolean isPlayerVacantCell(int x, int y) {
    // }

    // Check if a particular cell in grid contains a treasure
    // private boolean isTreasureVacantCell(int x, int y) {
    // }


    // Function to make a move my a player in a particular direction
    public synchronized void makeMove(String playerName, Direction direction, int numOfStep) {
    }

        // Function to check whether new position of player after making a move is valid or not
        // private boolean checkPositionIsValid(Position newPlayerPosition) {
        // }
    
        // Function to check whether a cell is present within boundary of grid
        // private boolean isValidCell(Integer x, Integer y) {
        // }
    

    // Function to get available position in grid and spawn an new treasure there
    private void spawnNewTreasure() {
    }


    // Function to make a move by player and get its new position
    // private Position getNewPlayerPosition(Position position, Direction direction) {
    // }


    // Function to handle player leaving a game
    // public void playerQuit(String playerName) {
    // }


    // Remove player from playerPositionGrid 
    private void removePlayerFromGrid(String playerName) {
    }

    // Get size of grid: N
    // public int getGridSize() {
    // }

    // Get total no. of treasures K
    // public int getTotalTreasures() {
    // }

    // Get the map of player to score
    // public Map<String, Integer> getPlayerTreasureMap() {
    // }

    // Get map of player to Position coordinated
    // public Map<String, Position> getPlayerPositionMap() {
    // }

    //  Function to check whether a player is present in a particular location in the grid
    // public String getPlayerAt(int j, int i) {
    // }

    //  Function to check whether a treasure is present in a particular location in the grid
    // public int getTreasureAt(int j, int i) {
    // }


    // Function to update Player list 
    // public void updatePlayerList(List<String> existPlayerList) {
    // }

}
