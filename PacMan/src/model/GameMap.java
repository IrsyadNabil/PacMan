package pacman.model;

import pacman.util.Direction;

/**
 * Represents the game map/board.
 * Manages tiles: walls, pellets, power pellets, empty spaces.
 * Encapsulation: all map data is private.
 */
public class GameMap {

    public static final int EMPTY       = 0;
    public static final int WALL        = 1;
    public static final int PELLET      = 2;
    public static final int POWER_PELLET = 3;
    public static final int GHOST_HOUSE = 4;

    private int[][] map;
    private final int rows;
    private final int cols;
    private final int tileSize;
    private int totalPellets;
    private int pelletsEaten;

    // Classic Pac-Man map (28x31 tiles)
    private static final int[][] LEVEL_MAP = {
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        {1,2,2,2,2,2,2,2,2,2,2,2,2,1,1,2,2,2,2,2,2,2,2,2,2,2,2,1},
        {1,2,1,1,1,1,2,1,1,1,1,1,2,1,1,2,1,1,1,1,1,2,1,1,1,1,2,1},
        {1,3,1,1,1,1,2,1,1,1,1,1,2,1,1,2,1,1,1,1,1,2,1,1,1,1,3,1},
        {1,2,1,1,1,1,2,1,1,1,1,1,2,1,1,2,1,1,1,1,1,2,1,1,1,1,2,1},
        {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1},
        {1,2,1,1,1,1,2,1,1,2,1,1,1,1,1,1,1,1,2,1,1,2,1,1,1,1,2,1},
        {1,2,1,1,1,1,2,1,1,2,1,1,1,1,1,1,1,1,2,1,1,2,1,1,1,1,2,1},
        {1,2,2,2,2,2,2,1,1,2,2,2,2,1,1,2,2,2,2,1,1,2,2,2,2,2,2,1},
        {1,1,1,1,1,1,2,1,1,1,1,1,0,1,1,0,1,1,1,1,1,2,1,1,1,1,1,1},
        {1,1,1,1,1,1,2,1,1,1,1,1,0,1,1,0,1,1,1,1,1,2,1,1,1,1,1,1},
        {1,1,1,1,1,1,2,1,1,0,0,0,0,0,0,0,0,0,0,1,1,2,1,1,1,1,1,1},
        {1,1,1,1,1,1,2,1,1,0,1,1,1,4,4,1,1,1,0,1,1,2,1,1,1,1,1,1},
        {1,1,1,1,1,1,2,1,1,0,1,4,4,4,4,4,4,1,0,1,1,2,1,1,1,1,1,1},
        {0,0,0,0,0,0,2,0,0,0,1,4,4,4,4,4,4,1,0,0,0,2,0,0,0,0,0,0},
        {1,1,1,1,1,1,2,1,1,0,1,4,4,4,4,4,4,1,0,1,1,2,1,1,1,1,1,1},
        {1,1,1,1,1,1,2,1,1,0,1,1,1,1,1,1,1,1,0,1,1,2,1,1,1,1,1,1},
        {1,1,1,1,1,1,2,1,1,0,0,0,0,0,0,0,0,0,0,1,1,2,1,1,1,1,1,1},
        {1,1,1,1,1,1,2,1,1,0,1,1,1,1,1,1,1,1,0,1,1,2,1,1,1,1,1,1},
        {1,1,1,1,1,1,2,1,1,0,1,1,1,1,1,1,1,1,0,1,1,2,1,1,1,1,1,1},
        {1,2,2,2,2,2,2,2,2,2,2,2,2,1,1,2,2,2,2,2,2,2,2,2,2,2,2,1},
        {1,2,1,1,1,1,2,1,1,1,1,1,2,1,1,2,1,1,1,1,1,2,1,1,1,1,2,1},
        {1,2,1,1,1,1,2,1,1,1,1,1,2,1,1,2,1,1,1,1,1,2,1,1,1,1,2,1},
        {1,3,2,2,1,1,2,2,2,2,2,2,2,0,0,2,2,2,2,2,2,2,1,1,2,2,3,1},
        {1,1,1,2,1,1,2,1,1,2,1,1,1,1,1,1,1,1,2,1,1,2,1,1,2,1,1,1},
        {1,1,1,2,1,1,2,1,1,2,1,1,1,1,1,1,1,1,2,1,1,2,1,1,2,1,1,1},
        {1,2,2,2,2,2,2,1,1,2,2,2,2,1,1,2,2,2,2,1,1,2,2,2,2,2,2,1},
        {1,2,1,1,1,1,1,1,1,1,1,1,2,1,1,2,1,1,1,1,1,1,1,1,1,1,2,1},
        {1,2,1,1,1,1,1,1,1,1,1,1,2,1,1,2,1,1,1,1,1,1,1,1,1,1,2,1},
        {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1},
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
    };

    public GameMap(int tileSize) {
        this.tileSize = tileSize;
        this.rows = LEVEL_MAP.length;
        this.cols = LEVEL_MAP[0].length;
        this.map = new int[rows][cols];
        this.totalPellets = 0;
        this.pelletsEaten = 0;
        loadMap();
    }

    private void loadMap() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                map[r][c] = LEVEL_MAP[r][c];
                if (map[r][c] == PELLET || map[r][c] == POWER_PELLET) {
                    totalPellets++;
                }
            }
        }
    }

    public void reset() {
        pelletsEaten = 0;
        loadMap();
    }

    public int getTile(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return WALL;
        return map[row][col];
    }

    public void setTile(int row, int col, int value) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            map[row][col] = value;
        }
    }

    public boolean isWall(int row, int col) {
        return getTile(row, col) == WALL;
    }

    public boolean isWalkable(int row, int col) {
        int tile = getTile(row, col);
        return tile != WALL;
    }

    /**
     * Check if movement in given direction from (row, col) is possible
     */
    public boolean canMove(int row, int col, Direction dir) {
        int nextRow = row + dir.getDy();
        int nextCol = col + dir.getDx();
        return isWalkable(nextRow, nextCol);
    }

    /**
     * Eat pellet at pixel position, return score gained
     */
    public int eatPellet(int pixelX, int pixelY) {
        int col = pixelX / tileSize;
        int row = pixelY / tileSize;
        int tile = getTile(row, col);
        if (tile == PELLET) {
            map[row][col] = EMPTY;
            pelletsEaten++;
            return 10;
        } else if (tile == POWER_PELLET) {
            map[row][col] = EMPTY;
            pelletsEaten++;
            return 50;
        }
        return 0;
    }

    public boolean eatsPowerPellet(int pixelX, int pixelY) {
        int col = pixelX / tileSize;
        int row = pixelY / tileSize;
        return getTile(row, col) == POWER_PELLET;
    }

    public boolean allPelletsEaten() {
        return pelletsEaten >= totalPellets;
    }

    // Getters
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getTileSize() { return tileSize; }
    public int getTotalPellets() { return totalPellets; }
    public int getPelletsEaten() { return pelletsEaten; }
    public int[][] getMap() { return map; }
}