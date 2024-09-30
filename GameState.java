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
    private Random random = new Random();


    public GameState(int n, int k) {
        
        this.gridSize = n;
        this.totalTreasures = k;
        this.playerPositionGrid = new String[n][n];
        this.treasurePositionGrid = new int[n][n];
        this.playerPositionMap = new HashMap<>();
        this.playerLastMoveMap = new HashMap<>();
        this.playerTreasureMap = new HashMap<>();

        // Initialize the treasure positions
        for (int i = 0; i < k; i++) {
            spawnNewTreasure();
        }

    }

    // Function to be used by primary server to add a new player to game.
    public synchronized void addNewPlayerToGame(String playerName) {
        Position position = getAvailablePosition();
        playerPositionGrid[position.getX()][position.getY()] = playerName; // Assign Random position to new player
        // Add player to necessary maps
        playerPositionMap.put(playerName, position);
        playerTreasureMap.put(playerName, 0);
        playerLastMoveMap.put(playerName, -1);
    }


    // Function to get a random position in the grid
    private Position getRandomPosition() {
        int x = random.nextInt(gridSize);
        int y = random.nextInt(gridSize);
        return new Position(x, y);
    }

    // Function to get get a random available position on the grid
    private Position getAvailablePosition() {
        Position position = getRandomPosition();
        while (!isVacantCell(position.getX(), position.getY())) {
            position = getRandomPosition();
        }
        return position;
    }

    // Function to check if a particular cell in grid is vacant
    private boolean isVacantCell(int x, int y) {
        return isPlayerVacantCell(x, y) && isTreasureVacantCell(x, y);
    }

    // Check if a particular cell in grid contains a player
    private boolean isPlayerVacantCell(int x, int y) {
        return playerPositionGrid[x][y] == null;
    }

    // Check if a particular cell in grid contains a treasure
    private boolean isTreasureVacantCell(int x, int y) {
        return treasurePositionGrid[x][y] != 1;
    }


    // Function to make a move my a player in a particular direction
    public synchronized void makeMove(String playerName, Direction direction, int numOfStep) {
        Position position = playerPositionMap.get(playerName);
        if (position == null) {
            return;
        }
        if (playerLastMoveMap.get(playerName) >= numOfStep) {
            Logger.error("This move has already been made for player: " + playerName);
            return;
        }
        Position newPlayerPosition = getNewPlayerPosition(position, direction); // Get New position of the player after making move
        if (!checkPositionIsValid(newPlayerPosition)) { // Check if that position is valid
            Logger.error("Invalid move by player: " + playerName);
            newPlayerPosition = position; // If invalid move keep player at original position only
        }
                // Updating map and grid with new player position and increase numOfStep

        playerPositionGrid[position.getX()][position.getY()] = null; // Set Previous poisition of player to null
        
        // Updating map and grid with new player position and increase numOfStep
        playerPositionGrid[newPlayerPosition.getX()][newPlayerPosition.getY()] = playerName;
        playerPositionMap.put(playerName, newPlayerPosition);
        playerLastMoveMap.put(playerName, numOfStep);

        // If treasure present in new position, to collect it and update player score
        int treasure = treasurePositionGrid[newPlayerPosition.getX()][newPlayerPosition.getY()];
        if (treasure >0) {
            this.playerTreasureMap.put(playerName, playerTreasureMap.get(playerName) +  treasure);
            this.treasurePositionGrid[newPlayerPosition.getX()][newPlayerPosition.getY()] = 0;
            Logger.info("Teasure collected by player " + playerName + " Location of treasure collected- " + newPlayerPosition.getX() + " , " + newPlayerPosition.getY());

            spawnNewTreasure();
        }
    }

    // Function to check whether new position of player after making a move is valid or not
    private boolean checkPositionIsValid(Position newPlayerPosition) {
        return isValidCell(newPlayerPosition.getX(), newPlayerPosition.getY()) 
            && isPlayerVacantCell(newPlayerPosition.getX(), newPlayerPosition.getY());
    }

    // Function to check whether a cell is present within boundary of grid
    private boolean isValidCell(Integer x, Integer y) {
        return x >= 0 && x < gridSize && y >= 0 && y < gridSize;
    }
    

    // Function to get available position in grid and spawn an new treasure there
    private void spawnNewTreasure() {
        Position position = getAvailablePosition();
        while (!isTreasureVacantCell(position.getX(), position.getY())) {
            position = getAvailablePosition();
        }
        treasurePositionGrid[position.getX()][position.getY()] = 1;
    }


    // Function to make a move by player and get its new position
    private Position getNewPlayerPosition(Position position, Direction direction) {
        int x = position.getX();
        int y = position.getY();
        switch (direction) {
            case N:
                x = Math.max(0, x - 1);
                break;
            case S:
                x = Math.min(gridSize - 1, x + 1);
                break;
            case W:
                y = Math.max(0, y - 1);
                break;
            case E:
                y = Math.min(gridSize - 1, y + 1);
                break;
            default:
                break;
        }
        return new Position(x, y);
    }


    // Function to handle player leaving a game
    public void playerQuit(String playerName) {
        // Removing player from all maps and grids needed
        Position position = playerPositionMap.get(playerName);
        if (position != null) {
            playerPositionGrid[position.getX()][position.getY()] = null;
        }
        removePlayerFromGrid(playerName);
        playerLastMoveMap.remove(playerName);
        playerTreasureMap.remove(playerName);
        playerPositionMap.remove(playerName);
    }


    // Remove player from playerPositionGrid 
    private void removePlayerFromGrid(String playerName) {
        Position position = playerPositionMap.get(playerName);
        playerPositionGrid[position.getX()][position.getY()] = null;
    }

    // Get size of grid: N
    public int getGridSize() {
        return gridSize;
    }

    // Get total no. of treasures K
    public int getTotalTreasures() {
        return totalTreasures;
    }

    // Get the map of player to score
    public Map<String, Integer> getPlayerTreasureMap() {
        return playerTreasureMap;
    }

    // Get map of player to Position coordinated
    public Map<String, Position> getPlayerPositionMap() {
        return playerPositionMap;
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
