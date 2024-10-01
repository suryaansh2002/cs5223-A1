import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class GameGUI extends JFrame {

    private GameState gameState;
    private String playerName;
    private String primaryId;
    private String backupId;

    private JPanel mazePanel;  // Make mazePanel a class variable to update it
    private JPanel infoPanel;  // Make infoPanel a class variable to update it

    public GameGUI(GameState gameState, String playerName, String primaryId, String backupId) {
        this.gameState = gameState;
        this.playerName = playerName;
        this.primaryId = primaryId;
        this.backupId = backupId;

        // set up main frame
        setTitle("Maze Game - " + playerName);
        setSize(800, 600);  // window size
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // add panels for the game
        mazePanel = createMazePanel();  // Initialize mazePanel
        add(mazePanel, BorderLayout.CENTER);  // maze in the center

        infoPanel = createPlayerInfoPanel();  // Initialize infoPanel
        add(infoPanel, BorderLayout.EAST);  // player info on the right

        // Display the window
        setVisible(true);
    }

    // create maze panel
    private JPanel createMazePanel() {
        JPanel mazePanel = new JPanel();
        mazePanel.setLayout(new GridLayout(gameState.getGridSize(), gameState.getGridSize()));  // grid layout

        // grid cells based on game state
        for (int row = 0; row < gameState.getGridSize(); row++) {
            for (int col = 0; col < gameState.getGridSize(); col++) {
                JPanel cell = new JPanel();
                cell.setBorder(BorderFactory.createLineBorder(Color.BLACK));  // borders

                // add players / treasure markers
                if (gameState.getPlayerAt(row, col) != null) {
                    cell.add(new JLabel(gameState.getPlayerAt(row, col)));
                } else if (gameState.getTreasureAt(row, col) == 1) {
                    cell.add(new JLabel("*"));
                }
                mazePanel.add(cell);  // add cell to panel
            }
        }

        return mazePanel;
    }

    // player info panel
    private JPanel createPlayerInfoPanel() {
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));  // vertical box layout

        // header for player info
        infoPanel.add(new JLabel("Player Info"));
        infoPanel.add(new JLabel("ID    |  Score"));

        // add info primary player
        if (primaryId != null && gameState.getPlayerTreasureMap().containsKey(primaryId)) {
            Integer primaryScore = gameState.getPlayerTreasureMap().get(primaryId);
            infoPanel.add(new JLabel(primaryId + " (M) | " + primaryScore));
        }

        // add info backup player if there
        if (backupId != null && !backupId.isEmpty() && gameState.getPlayerTreasureMap().containsKey(backupId)) {
            Integer backupScore = gameState.getPlayerTreasureMap().get(backupId);
            infoPanel.add(new JLabel(backupId + " (S) | " + backupScore));
        }

        // rest of the players
        for (Map.Entry<String, Integer> entry : gameState.getPlayerTreasureMap().entrySet()) {
            String playerName = entry.getKey();
            Integer score = entry.getValue();
            if (!playerName.equals(primaryId) && !playerName.equals(backupId)) {
                infoPanel.add(new JLabel(playerName + "      | " + score));
            }
        }

        return infoPanel;
    }

    // Update the game state and refresh the GUI
    public void updateGameState(GameState newGameState, String primaryId, String backupId) {
        this.gameState = newGameState;
        this.primaryId = primaryId;
        this.backupId = backupId;
        // Update the maze panel
        remove(mazePanel);  // Remove the old panel
        mazePanel = createMazePanel();  // Recreate the maze panel with the updated state
        add(mazePanel, BorderLayout.CENTER);  // Add the new panel

        // Update the player info panel
        remove(infoPanel);  // Remove the old panel
        infoPanel = createPlayerInfoPanel();  // Recreate the info panel with updated state
        add(infoPanel, BorderLayout.EAST);  // Add the new panel

        // Revalidate and repaint to refresh the frame
        revalidate();
        repaint();
    }
}
