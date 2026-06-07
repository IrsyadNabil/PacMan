package pacman.model.ghost;

import pacman.model.GameMap;
import pacman.model.PacMan;

import java.awt.*;

/**
 * Blinky (Red Ghost) - directly chases Pac-Man's current tile.
 * Demonstrates: Inheritance (extends Ghost), Polymorphism (overrides getTargetTile)
 * Personality: SHADOW - aggressive direct chaser
 */
public class Blinky extends Ghost {

    public Blinky(int x, int y, int tileSize, GameMap gameMap) {
        super(x, y, tileSize, gameMap, Color.RED, 0); // No house delay, exits first
        this.scatterRow = 0;
        this.scatterCol = gameMap.getCols() - 1; // Top-right corner
    }

    /**
     * Polymorphism: Blinky targets Pac-Man's exact current tile.
     */
    @Override
    public int[] getTargetTile(PacMan pacman) {
        return new int[]{pacman.getTileRow(), pacman.getTileCol()};
    }
}

