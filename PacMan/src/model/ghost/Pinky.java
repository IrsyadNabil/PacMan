package pacman.model.ghost;

import pacman.model.GameMap;
import pacman.model.PacMan;

import java.awt.*;

/**
 * Pinky (Pink Ghost) - targets 4 tiles ahead of Pac-Man's direction.
 * Demonstrates: Inheritance (extends Ghost), Polymorphism (overrides getTargetTile)
 * Personality: SPEEDY - ambushes Pac-Man
 */
public class Pinky extends Ghost {

    public Pinky(int x, int y, int tileSize, GameMap gameMap) {
        super(x, y, tileSize, gameMap, new Color(255, 184, 255), 30);
        this.scatterRow = 0;
        this.scatterCol = 0; // Top-left corner
    }

    /**
     * Polymorphism: Pinky targets 4 tiles ahead of Pac-Man.
     */
    @Override
    public int[] getTargetTile(PacMan pacman) {
        int targetRow = pacman.getTileRow() + pacman.getDirection().getDy() * 4;
        int targetCol = pacman.getTileCol() + pacman.getDirection().getDx() * 4;
        return new int[]{targetRow, targetCol};
    }
}
