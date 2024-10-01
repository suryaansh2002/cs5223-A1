import javax.swing.*;
import java.awt.*;

public class ImageLoaderTest extends JFrame {
    // Define the treasure icon
    private final ImageIcon treasureIcon;

    public ImageLoaderTest() {
        // Load the treasure icon
        treasureIcon = new ImageIcon("treasure.png");

        // Check if the image has loaded successfully
        if (treasureIcon.getImageLoadStatus() == MediaTracker.COMPLETE) {
            System.out.println("Image loaded successfully!");
        } else {
            System.out.println("Image failed to load.");
        }

        // Set up the JFrame
        setTitle("Image Loader Test");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create a label to display the image
        JLabel label = new JLabel(treasureIcon);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);

        // Show the JFrame
        setVisible(true);
    }

    public static void main(String[] args) {
        // Create the ImageLoaderTest instance
        SwingUtilities.invokeLater(ImageLoaderTest::new);
    }
}
