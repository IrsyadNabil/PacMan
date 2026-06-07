package pacman.model.ghost;

import pacman.model.Entity;
import pacman.model.GameMap;
import pacman.model.PacMan;
import pacman.util.Direction;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Abstract base class for all ghosts.
 * Demonstrates: Abstraction (abstract class), Inheritance (extends Entity)
 * Each ghost subclass overrides getTargetTile() for unique AI behavior.
 */
public abstract class Ghost extends Entity {

    public enum GhostMode {
        CHASE,      // Hunting pac-man
        SCATTER,    // Retreating to corner
        FRIGHTENED, // Running away (blue)
        EATEN       // Being eaten, returning to house
    }

    private GhostMode mode;
    private GhostMode previousMode;
    private Color normalColor;
    private boolean inHouse;
    private int houseTimer;
    private int frightenedTimer;
    private boolean flashingWhite;

    protected GameMap gameMap;
    protected Random random;

    private static final int FRIGHTENED_DURATION = 200;
    private static final int FLASH_START = 60; // start flashing when 60 frames left

    // Scatter corner for each ghost (overridden per ghost)
    protected int scatterRow;
    protected int scatterCol;

    public Ghost(int x, int y, int tileSize, GameMap gameMap, Color color, int houseDelay) {
        super(x, y, 2, tileSize);
        this.gameMap = gameMap;
        this.normalColor = color;
        this.mode = GhostMode.SCATTER;
        this.previousMode = GhostMode.SCATTER;
        this.inHouse = true;
        this.houseTimer = houseDelay;
        this.frightenedTimer = 0;
        this.flashingWhite = false;
        this.random = new Random();
        setDirection(Direction.UP);
    }

    /**
     * Abstract method: each ghost subclass defines its own target tile (AI behavior).
     * Polymorphism: different ghosts override this for different behavior.
     */
    public abstract int[] getTargetTile(PacMan pacman);

    /**
     * Polymorphism: overrides move() from Entity
     */
    @Override
    public void move() {
        if (inHouse) {
            houseTimer--;
            if (houseTimer <= 0) {
                inHouse = false;
                setX(13 * tileSize); // exit position
                setY(11 * tileSize);
            }
            return;
        }

        if (frightenedTimer > 0) {
            frightenedTimer--;
            flashingWhite = frightenedTimer < FLASH_START && (frightenedTimer / 10) % 2 == 0;
            if (frightenedTimer <= 0) {
                mode = previousMode;
                flashingWhite = false;
            }
        }

        int row = getTileRow();
        int col = getTileCol();

        // Move ghost by speed
        int newX = getX() + getDirection().getDx() * getSpeed();
        int newY = getY() + getDirection().getDy() * getSpeed();

        // Check boundary
        int newCol = (newX + tileSize / 2) / tileSize;
        int newRow = (newY + tileSize / 2) / tileSize;

        if (!gameMap.isWalkable(newRow, newCol)) {
            // Hit a wall: choose new direction
            chooseNewDirection(row, col, null);
        } else {
            setX(newX);
            setY(newY);

            // At each tile center, choose next direction
            if (isAtTileCenter()) {
                snapToGrid();
                chooseNewDirection(getTileRow(), getTileCol(), null);
            }
        }

        // Tunnel wrap
        int mapWidth = gameMap.getCols() * tileSize;
        if (getX() < -tileSize) setX(mapWidth - tileSize);
        if (getX() >= mapWidth) setX(0);
    }

    private boolean isAtTileCenter() {
        return getX() % tileSize == 0 && getY() % tileSize == 0;
    }

    protected void chooseNewDirection(int row, int col, PacMan pacman) {
        List<Direction> validDirs = new ArrayList<>();
        Direction current = getDirection();

        for (Direction dir : Direction.values()) {
            if (dir == Direction.NONE) continue;
            if (dir == current.opposite()) continue; // No 180-turns

            int nextRow = row + dir.getDy();
            int nextCol = col + dir.getDx();
            if (gameMap.isWalkable(nextRow, nextCol)) {
                validDirs.add(dir);
            }
        }

        if (validDirs.isEmpty()) {
            setDirection(current.opposite()); // dead end
            return;
        }

        if (mode == GhostMode.FRIGHTENED) {
            // Random movement when frightened
            setDirection(validDirs.get(random.nextInt(validDirs.size())));
            return;
        }

        if (mode == GhostMode.EATEN) {
            // Head back to ghost house
            moveToward(row, col, 13, 11, validDirs);
            return;
        }

        if (mode == GhostMode.SCATTER) {
            moveToward(row, col, scatterRow, scatterCol, validDirs);
            return;
        }

        // CHASE - move toward target tile
        if (pacman != null) {
            int[] target = getTargetTile(pacman);
            moveToward(row, col, target[0], target[1], validDirs);
        } else {
            setDirection(validDirs.get(0));
        }
    }

    protected void moveToward(int fromRow, int fromCol, int targetRow, int targetCol,
                               List<Direction> validDirs) {
        Direction best = validDirs.get(0);
        double bestDist = Double.MAX_VALUE;

        for (Direction dir : validDirs) {
            int nextRow = fromRow + dir.getDy();
            int nextCol = fromCol + dir.getDx();
            double dist = Math.hypot(nextRow - targetRow, nextCol - targetCol);
            if (dist < bestDist) {
                bestDist = dist;
                best = dir;
            }
        }
        setDirection(best);
    }

    public void updateAI(PacMan pacman) {
        if (inHouse || mode == GhostMode.FRIGHTENED || mode == GhostMode.EATEN) return;

        int row = getTileRow();
        int col = getTileCol();
        chooseNewDirection(row, col, pacman);
    }

    public void setFrightened() {
        if (mode != GhostMode.EATEN) {
            previousMode = mode;
            mode = GhostMode.FRIGHTENED;
            frightenedTimer = FRIGHTENED_DURATION;
            setDirection(getDirection().opposite()); // reverse
        }
    }

    public void setEaten() {
        mode = GhostMode.EATEN;
        frightenedTimer = 0;
        flashingWhite = false;
    }

    public void respawn(int x, int y, int houseDelay) {
        setX(x);
        setY(y);
        mode = GhostMode.SCATTER;
        previousMode = GhostMode.SCATTER;
        inHouse = true;
        houseTimer = houseDelay;
        frightenedTimer = 0;
        flashingWhite = false;
        setDirection(Direction.UP);
    }

    /**
     * Polymorphism: overrides abstract render() from Entity
     * All ghosts share the same render but different colors
     */
    @Override
    public void render(Graphics2D g2d) {
        int x = getX();
        int y = getY();
        int w = tileSize - 2;
        int h = tileSize - 2;

        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bodyColor;
        if (mode == GhostMode.FRIGHTENED) {
            bodyColor = flashingWhite ? Color.WHITE : new Color(0, 0, 200);
        } else if (mode == GhostMode.EATEN) {
            bodyColor = new Color(100, 100, 100, 120); // semi-transparent grey
        } else {
            bodyColor = normalColor;
        }

        g.setColor(bodyColor);

        // Ghost body: half circle on top
        g.fillArc(x, y, w, h, 0, 180);

        // Ghost body rectangle bottom
        g.fillRect(x, y + h / 2, w, h / 2);

        // Wavy bottom
        int waveCount = 3;
        int waveW = w / waveCount;
        for (int i = 0; i < waveCount; i++) {
            g.setColor(new Color(0, 0, 0)); // bg color
            g.fillArc(x + i * waveW, y + h - waveW / 2, waveW, waveW, 0, -180);
            g.setColor(bodyColor);
        }

        // Eyes (not when eaten)
        if (mode != GhostMode.EATEN) {
            g.setColor(Color.WHITE);
            g.fillOval(x + w / 4 - 3, y + h / 4, 8, 8);
            g.fillOval(x + 3 * w / 4 - 5, y + h / 4, 8, 8);

            g.setColor(new Color(0, 0, 180));
            g.fillOval(x + w / 4 - 1, y + h / 4 + 2, 4, 4);
            g.fillOval(x + 3 * w / 4 - 3, y + h / 4 + 2, 4, 4);
        }

        g.dispose();
    }

    // Getters
    public GhostMode getMode() { return mode; }
    public void setMode(GhostMode mode) { this.mode = mode; }
    public Color getNormalColor() { return normalColor; }
    public boolean isInHouse() { return inHouse; }
    public boolean isFrightened() { return mode == GhostMode.FRIGHTENED; }
    public boolean isEaten() { return mode == GhostMode.EATEN; }
}
