import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;
    private int gridSize;
    private int numTreasures;
    private Map<String, Integer> playerScores;
    private Map<String, Position> playerPositionMap;
    private List<Position> treasurePositionMap;
    private HashSet<Position> occupiedPositions;
    private Random random = new Random();

    public GameState(int gridSize, int numTreasures) {
        // Initialize the game state
        this.gridSize = gridSize;
        this.numTreasures = numTreasures;
        this.playerScores = new HashMap<>();
        this.playerPositionMap = new HashMap<>();
        this.treasurePositionMap = new ArrayList<>();
        this.occupiedPositions = new HashSet<>();

        // Initialize the treasure positions
        randomizeTreasurePositions();
        updateOccupiedPositions();
    }

    public synchronized void addPlayer(String playerID) {
        // Add a player to the game
        playerScores.put(playerID, 0);
        playerPositionMap.put(playerID, getAvailableRandomPosition());
        updateOccupiedPositions();
    }

    public void updatePlayersList(List<String> playerIDs) {
        // Update the list of players by removing players that are not in the list
        for (String playerID : playerPositionMap.keySet()) {
            if (!playerIDs.contains(playerID)) {
                quitGame(playerID);
            }
        }
    }

    private void randomizeTreasurePositions() {
        // Randomize the treasure positions
        Position position = getAvailableRandomPosition();
        for (int i = 0; i < numTreasures; i++) {
            treasurePositionMap.add(position);
            position = getAvailableRandomPosition();
        }
    }

    private void updateOccupiedPositions() {
        // Update the occupied positions. This is used to check if a player can move to a certain position
        occupiedPositions.clear();
        occupiedPositions.addAll(playerPositionMap.values());
        occupiedPositions.addAll(treasurePositionMap);    
    }

    private Position getAvailableRandomPosition() {
        // Get a random position that is not already occupied
        Position position = getRandomPosition();
        while (occupiedPositions.contains(position)) {
            position = getRandomPosition();
        }
        return position;
    }

    private Position getRandomPosition() {
        // Get a random position
        int x = random.nextInt(gridSize);
        int y = random.nextInt(gridSize);
        return new Position(x, y);
    }

    public int getGridSize() {
        return gridSize;
    }

    public int getNumTreasures() {
        return numTreasures;
    }

    public Map<String, Integer> getPlayerScores() {
        return playerScores;
    }

    public Map<String, Position> getPlayerPositionMap() {
        return playerPositionMap;
    }

    public Position getPlayerPosition(String playerID) {
        return playerPositionMap.get(playerID);
    }

    public Position computeNewPosition(String playerID, Direction direction) {
        // Compute the new position based on the current position and the direction
        Position currentPosition = playerPositionMap.get(playerID);
        int x = currentPosition.getX();
        int y = currentPosition.getY();
        switch (direction) {
            case NORTH:
                y = Math.max(0, y - 1);
                break;
            case SOUTH:
                y = Math.min(gridSize - 1, y + 1);
                break;
            case WEST:
                x = Math.max(0, x - 1);
                break;
            case EAST:
                x = Math.min(gridSize - 1, x + 1);
                break;
            default:
                break;  // Do nothing
        }
        // check if the new position is occupied, if so, return the current position
        if (occupiedPositions.contains(new Position(x, y))) {
            return currentPosition;
        }

        return new Position(x, y);
    }

    public synchronized void movePlayerPosition(String playerID, Position newPosition) {
        // Move a player to a new position
        playerPositionMap.put(playerID, newPosition);
        // Check if the player has found a treasure
        if (treasurePositionMap.contains(newPosition)) {
            playerScores.put(playerID, playerScores.get(playerID) + 1);
            treasurePositionMap.remove(newPosition);
            treasurePositionMap.add(getAvailableRandomPosition());
        }
        
        updateOccupiedPositions();
    }



    public synchronized void quitGame(String playerID) {
        // Remove a player from the game
        playerScores.remove(playerID);
        playerPositionMap.remove(playerID);
        updateOccupiedPositions();
    }
}