import java.io.Serializable;
import java.util.*;



// Object implementing State of the Game
public class GameState implements Serializable {

    // Grid Size N Obtained from tracker
    private int gridDimensions;

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
    private Random random = new Random();


    public GameState(int n, int k) {
        
        this.gridDimensions = n;
        this.totalTreasures = k;
        this.playerPositionGrid = new String[n][n];
        this.treasurePositionGrid = new int[n][n];
        this.playerPositionMap = new HashMap<>();
        this.playerLastMoveMap = new HashMap<>();
        this.playerTreasureMap = new HashMap<>();

        // Initialize the treasure positions
        for (int i = 0; i < k; i++) {
            placeNewTreasure();
        }

    }

    // Function to check if a particular cell in grid is vacant
    private boolean isCellEmpty(int x, int y) {
        return isPlayerVacantCell(x, y) && isEmptyTreasureCell(x, y);
    }

    // Function to get a random position in the grid
    private Position getRandomPosition() {
        int x = random.nextInt(gridDimensions);
        int y = random.nextInt(gridDimensions);
        return new Position(x, y);
    }
    
     // Check if a particular cell in grid contains a treasure
     private boolean isEmptyTreasureCell(int x, int y) {
        return treasurePositionGrid[x][y] != 1;
    }

    // Check if a particular cell in grid contains a player
    private boolean isPlayerVacantCell(int x, int y) {
        return playerPositionGrid[x][y] == null;
    }

    // Function to find a random vacant position on the grid
    private Position findVacantPos() {
        Position pos;
        do {
            pos = getRandomPosition();  // Generate a random position
        } while (!isCellEmpty(pos.getX(), pos.getY()));  // Check if the cell is vacant
        return pos;  // Return the vacant position
    }

    // Function to be used by primary server to add a new player to game.
    public synchronized void registerPlayer(String playerName) {
        // Find an empty position for the new player
        Position newPosition = findVacantPos();
        // Assign the player to that position on the grid
        playerPositionGrid[newPosition.getX()][newPosition.getY()] = playerName;
        
        // Add player details to the relevant maps
        playerPositionMap.put(playerName, newPosition);  // Store player's position
        playerTreasureMap.put(playerName, 0);            // Initialize player's treasure count
        playerLastMoveMap.put(playerName, -1);           // Initialize player's last move
    }

    // Function to check whether a cell is present within boundary of grid
    private boolean isCellValid(Integer x, Integer y) {
        return x >= 0 && x < gridDimensions && y >= 0 && y < gridDimensions;
    }


    // Function to move a player in a given direction
    public synchronized void movePlayer(String playerName, Direction direction, int steps) {
        Position currentPosition = playerPositionMap.get(playerName);
        
        // Check if player exists
        if (currentPosition == null) {
            return;
        }

        // Verify if the move has already been performed
        if (playerLastMoveMap.get(playerName) >= steps) {
            Logger.error("Move already executed for player: " + playerName);
            return;
        }

        // Calculate the new position after the move
        Position nextPosition = calculateNewPlayerPosition(currentPosition, direction);
        
        // Validate the new position
        if (!isMoveLegal(nextPosition)) {
            Logger.error("Invalid move attempt by player: " + playerName);
            nextPosition = currentPosition;  // Revert to original position if move is invalid
        }

        // Update player's position on the grid
        playerPositionGrid[currentPosition.getX()][currentPosition.getY()] = null; // Clear old position
        playerPositionGrid[nextPosition.getX()][nextPosition.getY()] = playerName; // Set new position

        // Update player's position and steps in the maps
        playerPositionMap.put(playerName, nextPosition);
        playerLastMoveMap.put(playerName, steps);

        // Check for treasure at the new position
        int treasure = treasurePositionGrid[nextPosition.getX()][nextPosition.getY()];
        if (treasure > 0) {
            // Update player's score and remove treasure from the grid
            playerTreasureMap.put(playerName, playerTreasureMap.get(playerName) + treasure);
            treasurePositionGrid[nextPosition.getX()][nextPosition.getY()] = 0;
            Logger.info("Treasure collected by player: " + playerName + " at (" + nextPosition.getX() + ", " + nextPosition.getY() + ")");

            // Spawn new treasure
            placeNewTreasure();
        }
    }

    // Method to locate a vacant position on the grid and place a new treasure there
    private void placeNewTreasure() {
        Position emptySpot = findVacantPos(); // Get a random vacant position
        while (!isEmptyTreasureCell(emptySpot.getX(), emptySpot.getY())) { // Ensure the cell is free of treasure
            emptySpot = findVacantPos(); // Keep searching for another vacant position
        }
        treasurePositionGrid[emptySpot.getX()][emptySpot.getY()] = 1; // Place treasure at the found position
    }

    // Function to check whether new position of player after making a move is valid or not
    private boolean isMoveLegal(Position newPlayerPosition) {
        return isCellValid(newPlayerPosition.getX(), newPlayerPosition.getY()) 
            && isPlayerVacantCell(newPlayerPosition.getX(), newPlayerPosition.getY());
    }

    // Method to calculate a player's new position after moving in a specific direction
    private Position calculateNewPlayerPosition(Position currentPos, Direction direction) {
        int xCoord = currentPos.getX();
        int yCoord = currentPos.getY();

        // Adjust coordinates based on the direction of movement
        switch (direction) {
            case N:
                xCoord = Math.max(0, xCoord - 1); // Move north, decrease x within bounds
                break;
            case S:
                xCoord = Math.min(gridDimensions - 1, xCoord + 1); // Move south, increase x within bounds
                break;
            case W:
                yCoord = Math.max(0, yCoord - 1); // Move west, decrease y within bounds
                break;
            case E:
                yCoord = Math.min(gridDimensions - 1, yCoord + 1); // Move east, increase y within bounds
                break;
            default:
                break;
        }

        // Return the updated position after movement
        return new Position(xCoord, yCoord);
    }

    // Get size of grid: N
    public int getgridDimensions() {
        return gridDimensions;
    }

    // Function to handle player leaving a game
    public void playerQuit(String playerName) {
        // Removing player from all maps and grids needed
        Position position = playerPositionMap.get(playerName);
        if (position != null) {
            playerPositionGrid[position.getX()][position.getY()] = null;
        }
        evictPlayerFromGrid(playerName);
        playerLastMoveMap.remove(playerName);
        playerTreasureMap.remove(playerName);
        playerPositionMap.remove(playerName);
    }

    // Get map of player to Position coordinated
    public Map<String, Position> getPlayerPositionMap() {
        return playerPositionMap;
    }

    // Get total no. of treasures K
    public int getTotalTreasures() {
        return totalTreasures;
    }

    // Remove player from playerPositionGrid 
    private void evictPlayerFromGrid(String playerName) {
        // Remove player from playerPositionGrid
        for (int i = 0; i < gridDimensions; i++) {
            for (int j = 0; j < gridDimensions; j++) {
                if (playerPositionGrid[i][j] != null && playerPositionGrid[i][j].equals(playerName)) {
                    playerPositionGrid[i][j] = null;
                }
            }
        }
    }


    // Get the map of player to score
    public Map<String, Integer> getPlayerTreasureMap() {
        return playerTreasureMap;
    }


    //  Function to check whether a player is present in a particular location in the grid
    public String getPlayerAt(int j, int i) {
        String playerName =  playerPositionGrid[j][i];
        if (playerName == null || playerName.isEmpty()) return null;

        Position position = playerPositionMap.get(playerName);
        if (position == null) {
            playerPositionGrid[j][i] = null;
            return null;
        }

        if (position.getX() == j && position.getY() == i) {
            return playerName;
        } else {
            //wrong player position
            playerPositionGrid[j][i] = null;
            return null;
        }
    }

    //  Function to check whether a treasure is present in a particular location in the grid
    public int getTreasureAt(int j, int i) {
        return treasurePositionGrid[j][i];
    }


    // Function to update Player list 
    public void updatePlayerList(List<String> existPlayerList) {
        Set<String> allPlayers = new HashSet<>(playerPositionMap.keySet());
        allPlayers.removeAll(existPlayerList);
        allPlayers.parallelStream().forEach(playerName -> playerQuit(playerName));
    }

}
