package pacman.view;

import pacman.controller.GameController;
import pacman.model.GameState;
import pacman.model.ghost.Ghost;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Main game rendering panel.
 * MVC: View layer.
 * Uses double buffering for smooth rendering.
 */
public class GamePanel extends JPanel {

    private static final Color BG_COLOR = Color.BLACK;

    private GameController controller;
    private MapRenderer mapRenderer;
    private HUDRenderer hudRenderer;

    private final int mapWidth;
    private final int mapHeight;
    private final int hudHeight = 70;

    public GamePanel() {
        this.mapWidth  = 28 * 20; // 28 cols * 20px
        this.mapHeight = 31 * 20; // 31 rows * 20px

        setPreferredSize(new Dimension(mapWidth, mapHeight + hudHeight));
        setBackground(BG_COLOR);
        setFocusable(true);

        mapRenderer = new MapRenderer();
        hudRenderer = new HUDRenderer();

        // Create controller (passes 'this' so controller can call repaint)
        controller = new GameController(this);

        // Key listener
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                controller.handleKeyPress(e.getKeyCode());
            }
        });

        // Show menu on start
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Black background
        g2d.setColor(BG_COLOR);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        GameState state = controller.getGameState();

        if (state.isMenu()) {
            hudRenderer.renderMenu(g2d, getWidth(), getHeight());
            return;
        }

        // Draw map
        mapRenderer.render(g2d, controller.getGameMap());

        // Draw Pac-Man
        if (controller.getPacman().isAlive() || state.isDeadAnimation()) {
            controller.getPacman().render(g2d);
        }

        // Draw Ghosts
        for (Ghost ghost : controller.getGhosts()) {
            ghost.render(g2d);
        }

        // Draw HUD
        hudRenderer.renderHUD(g2d, controller.getPacman(), state, getWidth(), mapHeight);

        // Overlay screens
        if (state.isPaused()) {
            hudRenderer.renderPause(g2d, getWidth(), getHeight());
        } else if (state.isGameOver()) {
            hudRenderer.renderGameOver(g2d, getWidth(), getHeight(), controller.getPacman().getScore());
        } else if (state.isLevelComplete()) {
            hudRenderer.renderLevelComplete(g2d, getWidth(), getHeight(), state.getLevel());
        }
    }

    public GameController getController() { return controller; }
}
