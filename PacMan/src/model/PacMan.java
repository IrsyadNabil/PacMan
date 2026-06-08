package pacman.model;

import model.Entity;
import pacman.util.Direction;

import java.awt.*;
import java.awt.geom.Arc2D;

/**
 * Pac-Man player character.
 * Demonstrates: Inheritance (extends Entity), Encapsulation, Polymorphism (overrides move/render)
 */
public class PacMan extends Entity {

    private int lives;
    private int score;
    private boolean powered;       // True when eaten power pellet
    private int powerTimer;
    private double mouthAngle;
    private int mouthDir;          // 1 = opening, -1 = closing
    private boolean alive;
    private int deathTimer;

    private static final int MAX_LIVES = 3;
    private static final int POWER_DURATION = 200; // frames
    private static final double MOUTH_MAX = 45.0;
    private static final double MOUTH_MIN = 5.0;
    private static final double MOUTH_SPEED = 5.0;

    private GameMap gameMap;

    public PacMan(int startX, int startY, int tileSize, GameMap gameMap) {
        super(startX, startY, 2, tileSize);
        this.lives = MAX_LIVES;
        this.score = 0;
        this.powered = false;
        this.powerTimer = 0;
        this.mouthAngle = MOUTH_MAX;
        this.mouthDir = -1;
        this.alive = true;
        this.deathTimer = 0;
        this.gameMap = gameMap;
        setDirection(Direction.LEFT);
        setNextDirection(Direction.LEFT);
    }
    
    private boolean canMoveInDirection(Direction dir) {
    int nextX = getX() + dir.getDx() * getSpeed();
    int nextY = getY() + dir.getDy() * getSpeed();

    int leftCol   = nextX / tileSize;
    int rightCol  = (nextX + tileSize - 2) / tileSize;
    int topRow    = nextY / tileSize;
    int bottomRow = (nextY + tileSize - 2) / tileSize;

    return gameMap.isWalkable(topRow, leftCol)
        && gameMap.isWalkable(topRow, rightCol)
        && gameMap.isWalkable(bottomRow, leftCol)
        && gameMap.isWalkable(bottomRow, rightCol);
    }
    
    /**
     * Polymorphism: overrides abstract move() from Entity
     */
    @Override
    public void move() {
        if (!alive) {
            deathTimer++;
            return;
        }

        // Try to turn in the queued direction first
        Direction next = getNextDirection();

        if (isCenteredOnTile() && canMoveInDirection(next)) {
            setDirection(next);
        }

        Direction dir = getDirection();

        if (!canMoveInDirection(dir)) {
            return;
        }

        int newX = getX() + dir.getDx() * getSpeed();
        int newY = getY() + dir.getDy() * getSpeed();

        // Tunnel wrapping (left/right)
        int mapWidth = gameMap.getCols() * tileSize;
        if (newX < -tileSize) newX = mapWidth - tileSize;
        if (newX >= mapWidth) newX = 0;

        setX(newX);
        setY(newY);
        
        if (getX() % tileSize == 0 &&
            getY() % tileSize == 0) {
            snapToGrid();
        }

        // Animate mouth
        animateMouth();

        // Eat pellet
        boolean powerPellet = gameMap.eatsPowerPellet(getX() + tileSize / 2, getY() + tileSize / 2);
        int gained = gameMap.eatPellet(getX() + tileSize / 2, getY() + tileSize / 2);
        if (gained > 0) {
            score += gained;
            if (powerPellet) {
                powered = true;
                powerTimer = POWER_DURATION;
            }
        }

        // Power timer countdown
        if (powered) {
            powerTimer--;
            if (powerTimer <= 0) {
                powered = false;
            }
        }
    }

    private void animateMouth() {
        mouthAngle += mouthDir * MOUTH_SPEED;
        if (mouthAngle <= MOUTH_MIN) {
            mouthAngle = MOUTH_MIN;
            mouthDir = 1;
        } else if (mouthAngle >= MOUTH_MAX) {
            mouthAngle = MOUTH_MAX;
            mouthDir = -1;
        }
    }

    /**
     * Polymorphism: overrides abstract render() from Entity
     */
    @Override
    public void render(Graphics2D g2d) {
        int x = getX();
        int y = getY();
        int size = tileSize - 2;

        // Determine rotation based on direction
        double rotation = 0;
        switch (getDirection()) {
            case RIGHT: rotation = 0;    break;
            case DOWN:  rotation = 90;   break;
            case LEFT:  rotation = 180;  break;
            case UP:    rotation = 270;  break;
            default:    rotation = 180;  break;
        }

        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Translate & rotate to center
        g.translate(x + size / 2, y + size / 2);
        g.rotate(Math.toRadians(rotation));
        g.translate(-size / 2, -size / 2);

        // Body color
        g.setColor(alive ? Color.YELLOW : new Color(255, 200, 0));

        // Draw pac-man as arc (Pac shape)
        double startAngle = mouthAngle;
        double arcAngle   = 360 - mouthAngle * 2;

        Shape pacShape = new Arc2D.Double(0, 0, size, size, startAngle, arcAngle, Arc2D.PIE);
        g.fill(pacShape);

        // Eye
        if (alive) {
            g.setColor(Color.BLACK);
            g.fillOval(size / 2 - 2, size / 6 - 1, 4, 4);
        }

        g.dispose();
    }

    public void die() {
        if (alive) {
            alive = false;
            deathTimer = 0;
            lives--;
        }
    }

    public void respawn(int startX, int startY) {
        setX(startX);
        setY(startY);
        alive = true;
        deathTimer = 0;
        powered = false;
        powerTimer = 0;
        mouthAngle = MOUTH_MAX;
        mouthDir = -1;
        setDirection(Direction.LEFT);
        setNextDirection(Direction.LEFT);
    }

    public void addScore(int points) {
        this.score += points;
    }
    
    private boolean isCenteredOnTile() {
    return getX() % tileSize == 0 &&
           getY() % tileSize == 0;
    }
    
    // Getters and Setters (Encapsulation)
    public int getLives() { return lives; }
    public int getScore() { return score; }
    public boolean isPowered() { return powered; }
    public int getPowerTimer() { return powerTimer; }
    public boolean isAlive() { return alive; }
    public int getDeathTimer() { return deathTimer; }
    public void setLives(int lives) { this.lives = lives; }
    public void setScore(int score) { this.score = score; }
}
