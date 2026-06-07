package pacman.model.ghost;

import pacman.model.GameMap;
import pacman.model.PacMan;

import java.awt.*;

/**
 * Inky (Cyan Ghost) - complex AI using Blinky's position as reference.
 * Demonstrates: Inheritance, Polymorphism
 * Personality: BASHFUL - unpredictable
 */
public class Inky extends Ghost {

    private Blinky blinky;

    public Inky(int x, int y, int tileSize, GameMap gameMap, Blinky blinky) {
        super(x, y, tileSize, gameMap, Color.CYAN, 60);
        this.blinky = blinky;
        this.scatterRow = gameMap.getRows() - 1;
        this.scatterCol = gameMap.getCols() - 1; // Bottom-right corner
    }

    /**
     * Polymorphism: Inky uses Blinky's position to calculate target.
     * Target = 2 tiles ahead of Pac-Man, then double vector from Blinky to that point.
     */
    @Override
    public int[] getTargetTile(PacMan pacman) {
        // 2 tiles ahead of Pac-Man
        int pivotRow = pacman.getTileRow() + pacman.getDirection().getDy() * 2;
        int pivotCol = pacman.getTileCol() + pacman.getDirection().getDx() * 2;

        // Double the vector from Blinky to pivot
        int bRow = blinky.getTileRow();
        int bCol = blinky.getTileCol();

        int targetRow = pivotRow + (pivotRow - bRow);
        int targetCol = pivotCol + (pivotCol - bCol);

        return new int[]{targetRow, targetCol};
    }
}
