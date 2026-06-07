package pacman.interfaces;

import java.awt.*;

/**
 * Interface for all entities that can be drawn on screen.
 * Part of abstraction principle in OOP.
 */
public interface Renderable {
    void render(Graphics2D g2d);
}
