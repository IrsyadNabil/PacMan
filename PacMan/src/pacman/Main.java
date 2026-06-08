package pacman;

import pacman.view.GameWindow;

import javax.swing.*;

/**
 * Main entry point for Pac-Man game.
 * Launches the GUI on the Event Dispatch Thread.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.setVisible(true);
        });
    }
}