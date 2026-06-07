package pacman.interfaces;

import java.awt.*;

/**
 * Interface for collision detection between game entities.
 */
public interface Collidable {
    Rectangle getBounds();
    boolean collidesWith(Collidable other);
}
