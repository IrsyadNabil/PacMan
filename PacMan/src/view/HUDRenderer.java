package pacman.view;

import pacman.model.GameState;
import pacman.model.PacMan;

import java.awt.*;
import java.awt.geom.Arc2D;

/**
 * Renders the HUD: score, high score, lives, level.
 * Single Responsibility Principle (SOLID).
 */
public class HUDRenderer {

    private static final Font SCORE_FONT  = new Font("Press Start 2P", Font.PLAIN, 10);
    private static final Font TITLE_FONT  = new Font("Press Start 2P", Font.PLAIN, 18);
    private static final Font NORMAL_FONT = new Font("Courier New", Font.BOLD, 13);

    public void renderHUD(Graphics2D g2d, PacMan pacman, GameState state, int panelWidth, int mapHeight) {
        int hudY = mapHeight + 5;

        // Score
        drawLabel(g2d, "SCORE", 10, hudY);
        drawValue(g2d, String.valueOf(pacman.getScore()), 10, hudY + 18);

        // High Score
        drawLabel(g2d, "HIGH SCORE", panelWidth / 2 - 50, hudY);
        drawValue(g2d, String.valueOf(state.getHighScore()), panelWidth / 2 - 50, hudY + 18);

        // Level
        drawLabel(g2d, "LVL " + state.getLevel(), panelWidth - 70, hudY);

        // Lives (draw pac-man icons)
        drawLives(g2d, pacman.getLives(), 10, hudY + 38);
    }

    private void drawLabel(Graphics2D g2d, String text, int x, int y) {
        g2d.setFont(NORMAL_FONT);
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, x, y);
    }

    private void drawValue(Graphics2D g2d, String text, int x, int y) {
        g2d.setFont(new Font("Courier New", Font.BOLD, 15));
        g2d.setColor(Color.YELLOW);
        g2d.drawString(text, x, y);
    }

    private void drawLives(Graphics2D g2d, int lives, int startX, int y) {
        g2d.setColor(Color.YELLOW);
        for (int i = 0; i < lives; i++) {
            int x = startX + i * 22;
            // Mini Pac-Man icon
            Shape icon = new Arc2D.Double(x, y, 16, 16, 30, 300, Arc2D.PIE);
            g2d.fill(icon);
        }
    }

    public void renderMenu(Graphics2D g2d, int w, int h) {
        // Translucent overlay
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, w, h);

        // Title
        g2d.setFont(new Font("Courier New", Font.BOLD, 36));
        g2d.setColor(Color.YELLOW);
        drawCentered(g2d, "PAC-MAN", w, h / 2 - 60);

        g2d.setFont(new Font("Courier New", Font.BOLD, 14));
        g2d.setColor(Color.WHITE);
        drawCentered(g2d, "PRESS ENTER TO START", w, h / 2 + 10);

        g2d.setFont(new Font("Courier New", Font.PLAIN, 11));
        g2d.setColor(new Color(180, 180, 180));
        drawCentered(g2d, "ARROW KEYS / WASD to move", w, h / 2 + 40);
        drawCentered(g2d, "P or ESC to pause", w, h / 2 + 58);
    }

    public void renderPause(Graphics2D g2d, int w, int h) {
        g2d.setColor(new Color(0, 0, 0, 170));
        g2d.fillRect(0, 0, w, h);

        g2d.setFont(new Font("Courier New", Font.BOLD, 28));
        g2d.setColor(Color.CYAN);
        drawCentered(g2d, "PAUSED", w, h / 2);

        g2d.setFont(new Font("Courier New", Font.PLAIN, 12));
        g2d.setColor(Color.WHITE);
        drawCentered(g2d, "Press P to resume", w, h / 2 + 30);
    }

    public void renderGameOver(Graphics2D g2d, int w, int h, int score) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, w, h);

        g2d.setFont(new Font("Courier New", Font.BOLD, 30));
        g2d.setColor(Color.RED);
        drawCentered(g2d, "GAME OVER", w, h / 2 - 40);

        g2d.setFont(new Font("Courier New", Font.BOLD, 16));
        g2d.setColor(Color.YELLOW);
        drawCentered(g2d, "SCORE: " + score, w, h / 2 + 5);

        g2d.setFont(new Font("Courier New", Font.PLAIN, 12));
        g2d.setColor(Color.WHITE);
        drawCentered(g2d, "Press ENTER to play again", w, h / 2 + 35);
    }

    public void renderLevelComplete(Graphics2D g2d, int w, int h, int level) {
        g2d.setColor(new Color(0, 0, 0, 160));
        g2d.fillRect(0, 0, w, h);

        g2d.setFont(new Font("Courier New", Font.BOLD, 22));
        g2d.setColor(Color.GREEN);
        drawCentered(g2d, "LEVEL " + level + " COMPLETE!", w, h / 2);
    }

    private void drawCentered(Graphics2D g2d, String text, int w, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        int x = (w - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, y);
    }
}
