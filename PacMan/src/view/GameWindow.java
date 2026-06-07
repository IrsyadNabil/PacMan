package pacman.view;

import javax.swing.*;
import java.awt.*;

/**
 * Main application window.
 * Wraps GamePanel in a JFrame.
 */
public class GameWindow extends JFrame {

    public GameWindow() {
        setTitle("PAC-MAN");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel panel = new GamePanel();
        add(panel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null); // Center on screen
        setBackground(Color.BLACK);

        // Request focus so keyboard input works immediately
        panel.requestFocusInWindow();
    }
}
