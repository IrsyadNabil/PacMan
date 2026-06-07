package pacman.model.ghost;

import pacman.model.GameMap;
import pacman.model.PacMan;

import java.awt.*;

/**
 * Clyde (Orange Ghost) - chases when far from Pac-Man, scatters when close.
 * Demonstrates: Inheritance, Polymorphism
 * Personality: POKEY - shy, keeps distance
 */
public class Clyde extends Ghost {

    private static final double CHASE_THRESHOLD = 8.0; // tiles

    public Clyde(int x, int y, int tileSize, GameMap gameMap) {
        super(x, y, tileSize, gameMap, new Color(255, 165, 0), 90);
        this.scatterRow = gameMap.getRows() - 1;
        this.scatterCol = 0; // Bottom-left corner
    }

    /**
     * Polymorphism: Clyde chases when > 8 tiles away, otherwise retreats to corner.
     */
    @Override
    public int[] getTargetTile(PacMan pacman) {
        double dist = Math.hypot(
            getTileRow() - pacman.getTileRow(),
            getTileCol() - pacman.getTileCol()
        );

        if (dist > CHASE_THRESHOLD) {
            // Chase pac-man directly
            return new int[]{pacman.getTileRow(), pacman.getTileCol()};
        } else {
            // Retreat to scatter corner
            return new int[]{scatterRow, scatterCol};
        }
    }
}

