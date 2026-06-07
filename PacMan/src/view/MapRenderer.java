package pacman.view;

import pacman.model.GameMap;

import java.awt.*;

/**
 * Handles rendering of the game map (walls, pellets, power pellets).
 * Single Responsibility Principle (SOLID): only responsible for map rendering.
 */
public class MapRenderer {

    private static final Color WALL_COLOR         = new Color(33, 33, 255);
    private static final Color WALL_INNER_COLOR    = new Color(0, 0, 0);
    private static final Color PELLET_COLOR        = new Color(255, 184, 174);
    private static final Color POWER_PELLET_COLOR  = new Color(255, 184, 174);

    private int pelletPulse = 0;
    private int pulseDir = 1;

    public void render(Graphics2D g2d, GameMap map) {
        int tileSize = map.getTileSize();
        int[][] grid = map.getMap();

        pelletPulse += pulseDir * 2;
        if (pelletPulse >= 60 || pelletPulse <= 0) pulseDir = -pulseDir;

        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                int tile = grid[row][col];
                int x = col * tileSize;
                int y = row * tileSize;

                switch (tile) {
                    case GameMap.WALL:
                        drawWall(g2d, x, y, tileSize, row, col, map);
                        break;
                    case GameMap.PELLET:
                        drawPellet(g2d, x, y, tileSize);
                        break;
                    case GameMap.POWER_PELLET:
                        drawPowerPellet(g2d, x, y, tileSize);
                        break;
                    case GameMap.GHOST_HOUSE:
                        g2d.setColor(new Color(40, 40, 60));
                        g2d.fillRect(x, y, tileSize, tileSize);
                        break;
                    default:
                        // empty
                        break;
                }
            }
        }
    }

    private void drawWall(Graphics2D g2d, int x, int y, int ts, int row, int col, GameMap map) {
        g2d.setColor(WALL_COLOR);
        g2d.fillRect(x, y, ts, ts);

        // Inner wall detail
        g2d.setColor(WALL_INNER_COLOR);
        int margin = 2;
        g2d.fillRect(x + margin, y + margin, ts - margin * 2, ts - margin * 2);

        // Rounded edges on wall borders
        g2d.setColor(WALL_COLOR);
        // Check neighbors and fill corners
        boolean up    = map.isWall(row - 1, col);
        boolean down  = map.isWall(row + 1, col);
        boolean left  = map.isWall(row, col - 1);
        boolean right = map.isWall(row, col + 1);

        int inner = margin;
        if (up)    g2d.fillRect(x + inner, y, ts - inner * 2, inner);
        if (down)  g2d.fillRect(x + inner, y + ts - inner, ts - inner * 2, inner);
        if (left)  g2d.fillRect(x, y + inner, inner, ts - inner * 2);
        if (right) g2d.fillRect(x + ts - inner, y + inner, inner, ts - inner * 2);
    }

    private void drawPellet(Graphics2D g2d, int x, int y, int ts) {
        g2d.setColor(PELLET_COLOR);
        int dotSize = 3;
        int offset = (ts - dotSize) / 2;
        g2d.fillOval(x + offset, y + offset, dotSize, dotSize);
    }

    private void drawPowerPellet(Graphics2D g2d, int x, int y, int ts) {
        // Pulsing power pellet
        float alpha = 0.5f + (pelletPulse / 120f);
        g2d.setColor(new Color(
            POWER_PELLET_COLOR.getRed(),
            POWER_PELLET_COLOR.getGreen(),
            POWER_PELLET_COLOR.getBlue(),
            (int)(alpha * 255)
        ));
        int dotSize = 8 + (pelletPulse / 20);
        int offset = (ts - dotSize) / 2;
        g2d.fillOval(x + offset, y + offset, dotSize, dotSize);
    }
}
