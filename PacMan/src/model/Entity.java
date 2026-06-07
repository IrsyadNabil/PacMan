package pacman.model;

import pacman.interfaces.Collidable;
import pacman.interfaces.Movable;
import pacman.interfaces.Renderable;
import pacman.util.Direction;

import java.awt.*;

/**
 * Abstract base class for all game entities (Pac-Man, Ghosts).
 * Demonstrates: Abstraction, Encapsulation
 */
public abstract class Entity implements Movable, Renderable, Collidable {

    // Encapsulation: private fields with getters/setters
    private int x;
    private int y;
    private int speed;
    private Direction direction;
    private Direction nextDirection;
    protected int tileSize;

    public Entity(int x, int y, int speed, int tileSize) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.tileSize = tileSize;
        this.direction = Direction.LEFT;
        this.nextDirection = Direction.LEFT;
    }

    // Abstract method - subclasses must implement their own movement logic
    @Override
    public abstract void move();

    // Abstract method - subclasses must implement their own rendering
    @Override
    public abstract void render(Graphics2D g2d);

    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, tileSize - 2, tileSize - 2);
    }

    @Override
    public boolean collidesWith(Collidable other) {
        return getBounds().intersects(other.getBounds());
    }

    // Getters and Setters (Encapsulation)
    @Override
    public int getX() { return x; }

    @Override
    public void setX(int x) { this.x = x; }

    @Override
    public int getY() { return y; }

    @Override
    public void setY(int y) { this.y = y; }

    @Override
    public int getSpeed() { return speed; }

    public void setSpeed(int speed) { this.speed = speed; }

    public Direction getDirection() { return direction; }

    public void setDirection(Direction direction) { this.direction = direction; }

    public Direction getNextDirection() { return nextDirection; }

    public void setNextDirection(Direction nextDirection) { this.nextDirection = nextDirection; }

    public int getTileSize() { return tileSize; }

    /**
     * Get tile column from pixel X
     */
    public int getTileCol() {
        return (x + tileSize / 2) / tileSize;
    }

    /**
     * Get tile row from pixel Y
     */
    public int getTileRow() {
        return (y + tileSize / 2) / tileSize;
    }

    /**
     * Snap entity to tile grid
     */
    public void snapToGrid() {
        x = getTileCol() * tileSize;
        y = getTileRow() * tileSize;
    }
}
