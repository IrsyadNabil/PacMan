package pacman.interfaces;

/**
 * Interface for all entities that can move on the game board.
 * Part of abstraction principle in OOP.
 */
public interface Movable {
    void move();
    int getX();
    int getY();
    void setX(int x);
    void setY(int y);
    int getSpeed();
}
