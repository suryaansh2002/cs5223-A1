import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class GameGUI extends JFrame {

    private GameState gameState;
    private String playerName;
    private String primaryId;
    private String backupId;

    public GameGUI(GameState gameState, String playerName, String primaryId, String backupId) {
        this.gameState = gameState;
        this.playerName = playerName;
        this.primaryId = primaryId;
        this.backupId = backupId;

        // Set up the main frame
        setTitle("Maze Game");
        setSize(800, 600);  // You can adjust the size as needed
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create and add panels for the game
        add(createMazePanel(), BorderLayout.CENTER);  // Maze in the center
        add(createPlayerInfoPanel(), BorderLayout.EAST);  // Player info on the right

        // Display the window
        setVisible(true);
    }

    // Method to create the maze panel
    private JPanel createMazePanel() {
        JPanel mazePanel = new JPanel();
        mazePanel.setLayout(new GridLayout(gameState.getGridSize(), gameState.getGridSize()));  // Grid layout

        // Fill the grid with cells based on the game state
        for (int row = 0; row < gameState.getGridSize(); row++) {
            for (int col = 0; col < gameState.getGridSize(); col++) {
                JPanel cell = new JPanel();
                cell.setBorder(BorderFactory.createLineBorder(Color.BLACK));  // Add borders to the cells

                // Add players or treasure markers
                if (gameState.getPlayerAt(row, col) != null) {
                    cell.add(new JLabel(gameState.getPlayerAt(row, col)));
                } else if (gameState.getTreasureAt(row, col) == 1) {
                    cell.add(new JLabel("*"));
                }
                mazePanel.add(cell);  // Add the cell to the panel
            }
        }

        return mazePanel;
    }

    // Method to create player info panel
   // Method to create player info panel without relying on GameView
private JPanel createPlayerInfoPanel() {
    JPanel infoPanel = new JPanel();
    infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));  // Vertical box layout

    // Add a header for player info
    infoPanel.add(new JLabel("Player Info"));
    infoPanel.add(new JLabel("ID    |  Score"));

    // Add information for primary player
    if (primaryId != null && gameState.getPlayerTreasureMap().containsKey(primaryId)) {
        Integer primaryScore = gameState.getPlayerTreasureMap().get(primaryId);
        infoPanel.add(new JLabel(primaryId + " (M) | " + primaryScore));
    }

    // Add information for backup player if applicable
    if (backupId != null && !backupId.isEmpty() && gameState.getPlayerTreasureMap().containsKey(backupId)) {
        Integer backupScore = gameState.getPlayerTreasureMap().get(backupId);
        infoPanel.add(new JLabel(backupId + " (S) | " + backupScore));
    }

    // Add the rest of the players (excluding primary and backup)
    for (Map.Entry<String, Integer> entry : gameState.getPlayerTreasureMap().entrySet()) {
        String playerName = entry.getKey();
        Integer score = entry.getValue();
        if (!playerName.equals(primaryId) && !playerName.equals(backupId)) {
            infoPanel.add(new JLabel(playerName + "      | " + score));
        }
    }

    return infoPanel;
}

}
