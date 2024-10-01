// add numbers to graph
// player score panel
// treasure image



import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.io.File;
import java.util.Map;



public class GameGUI extends JFrame {

    // Pastel colors
    private final Color pastelGreen = new Color(204, 255, 204);
    private final Color pastelBlue = new Color(204, 229, 255);
    private final Color pastelYellow = new Color(255, 255, 204);
    private final Color pastelPink = new Color(255, 204, 229);
    private final Color pastelPurple = new Color(229, 204, 255);
    

    // Define the treasure icon
    private final ImageIcon treasureIcon = new ImageIcon(new ImageIcon("treasure.png").getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH));


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
        setTitle("Maze Game - Player:" + playerName);
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

        // Create maze panel with row and column numbers
    private JPanel createMazePanel() {
        // Create a panel with one extra row and column for numbering
        JPanel mazePanel = new JPanel();
        mazePanel.setLayout(new GridLayout(gameState.getGridSize() + 1, gameState.getGridSize() + 1));

        // Add empty label at the top-left corner (0,0 position)
        mazePanel.add(new JLabel("INDEX")); 

        // Add column numbers at the top
        for (int col = 0; col < gameState.getGridSize(); col++) {
            // mazePanel.add(new JLabel(String.valueOf(col), SwingConstants.CENTER));
            JLabel colLabel = new JLabel(String.valueOf(col), SwingConstants.CENTER);
            colLabel.setOpaque(true);  // Make sure the background is painted
            colLabel.setBackground(pastelPurple);  // Use pastel purple for column labels
            mazePanel.add(colLabel);
        }

        // Add row numbers and the maze grid
        for (int row = 0; row < gameState.getGridSize(); row++) {
            // Add the row number at the start of each row
            JLabel rowLabel = new JLabel(String.valueOf(row), SwingConstants.CENTER);
            rowLabel.setOpaque(true);
            rowLabel.setBackground(pastelPink);  // Use pastel pink for row labels
            mazePanel.add(rowLabel);

            // Add the actual maze cells
            for (int col = 0; col < gameState.getGridSize(); col++) {
                JPanel cell = new JPanel();
                cell.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                    // Apply a pastel background color to each cell
                if (gameState.getPlayerAt(row, col) != null) {
                    cell.setBackground(pastelBlue);  // Pastel blue for player cells
                    cell.add(new JLabel(gameState.getPlayerAt(row, col)));
                } else if (gameState.getTreasureAt(row, col) == 1) {
                    cell.setBackground(pastelYellow);  // Pastel yellow for treasure cells
                    cell.add(new JLabel(treasureIcon));  // Add the treasure icon
                } else {
                    cell.setBackground(pastelGreen);  // Pastel green for empty cells
                }

                mazePanel.add(cell);  // Add cell to the grid
            }
        }

        return mazePanel;
    }

    /// Create the player info panel with a JTable using pastel colors
    private JPanel createPlayerInfoPanel() {
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BorderLayout());  // Use BorderLayout to handle table layout

        // Table columns and data
        String[] columnNames = {"ID", "SCORE"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);  // Create an empty table model

        // Add primary player info
        if (primaryId != null && gameState.getPlayerTreasureMap().containsKey(primaryId)) {
            Integer primaryScore = gameState.getPlayerTreasureMap().get(primaryId);
            tableModel.addRow(new Object[]{primaryId + " (P)", primaryScore});
        }

        // Add backup player info
        if (backupId != null && gameState.getPlayerTreasureMap().containsKey(backupId)) {
            Integer backupScore = gameState.getPlayerTreasureMap().get(backupId);
            tableModel.addRow(new Object[]{backupId + " (B)", backupScore});
        }

        // Add rest of the players
        for (Map.Entry<String, Integer> entry : gameState.getPlayerTreasureMap().entrySet()) {
            String id = entry.getKey();
            Integer score = entry.getValue();
            if (!id.equals(primaryId) && !id.equals(backupId)) {
                tableModel.addRow(new Object[]{id, score});
            }
        }

        // Create JTable with the model
        JTable scoreTable = new JTable(tableModel);

        // Set up custom cell renderer to apply pastel colors
        scoreTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                // Alternate row colors
                if (row == 0) {
                    c.setBackground(pastelBlue);  // Primary player (row 0)
                } else if (row == 1) {
                    c.setBackground(pastelPurple);  // Backup player (row 1)
                } else {
                    // Alternate the rest of the rows
                    if (row % 2 == 0) {
                        c.setBackground(pastelGreen);  // Even rows
                    } else {
                        c.setBackground(pastelYellow);  // Odd rows
                    }
                }
                return c;
            }
        });

        // Add the table to a scroll pane and set its preferred size
        JScrollPane scrollPane = new JScrollPane(scoreTable);
        scoreTable.setFillsViewportHeight(true);  // Make the table fill its container
        scrollPane.setPreferredSize(new Dimension(100, getHeight()));  // Set preferred size for the scoreboard

        infoPanel.add(scrollPane, BorderLayout.CENTER);  // Add the table to the info panel

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
