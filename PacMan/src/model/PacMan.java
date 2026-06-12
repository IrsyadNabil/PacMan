package pacman.model;

import pacman.util.Direction;

import java.awt.*;
import java.awt.geom.Arc2D;

/**
 * Pac-Man player character.
 * Inheritance: extends Entity | Polymorphism: overrides move(), render()
 */
public class PacMan extends Entity {

    private int lives;
    private int score;
    private boolean powered;
    private int powerTimer;
    private double mouthAngle;
    private int mouthDir;
    private boolean alive;
    private int deathTimer;

    // Flag agar frightened hanya dipicu sekali per power pellet
    private boolean powerPelletJustEaten;

    private static final int MAX_LIVES     = 3;
    private static final int POWER_DURATION = 300; // ~5 detik @ 60fps
    private static final double MOUTH_MAX  = 45.0;
    private static final double MOUTH_MIN  = 5.0;
    private static final double MOUTH_SPEED = 5.0;

    private final GameMap gameMap;

    public PacMan(int startX, int startY, int tileSize, GameMap gameMap) {
        super(startX, startY, 2, tileSize);
        this.lives    = MAX_LIVES;
        this.score    = 0;
        this.powered  = false;
        this.powerTimer = 0;
        this.mouthAngle = MOUTH_MAX;
        this.mouthDir   = -1;
        this.alive      = true;
        this.deathTimer = 0;
        this.powerPelletJustEaten = false;
        this.gameMap    = gameMap;
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

    private boolean isCenteredOnTile() {
        return getX() % tileSize == 0 && getY() % tileSize == 0;
    }

    @Override
    public void move() {
        if (!alive) { deathTimer++; return; }

        // Reset flag tiap frame sebelum cek
        powerPelletJustEaten = false;

        // Coba belok sesuai antrian jika berada di tengah tile
        if (isCenteredOnTile() && canMoveInDirection(getNextDirection())) {
            setDirection(getNextDirection());
        }

        if (!canMoveInDirection(getDirection())) return;

        int newX = getX() + getDirection().getDx() * getSpeed();
        int newY = getY() + getDirection().getDy() * getSpeed();

        // Tunnel wrap kiri-kanan
        int mapWidth = gameMap.getCols() * tileSize;
        if (newX < -tileSize)  newX = mapWidth - tileSize;
        if (newX >= mapWidth)  newX = 0;

        setX(newX);
        setY(newY);

        if (isCenteredOnTile()) snapToGrid();

        animateMouth();

        // Makan pellet/power pellet
        boolean isPP = gameMap.eatsPowerPellet(getX() + tileSize / 2, getY() + tileSize / 2);
        int gained   = gameMap.eatPellet(getX() + tileSize / 2, getY() + tileSize / 2);
        if (gained > 0) {
            score += gained;
            if (isPP) {
                // Aktifkan efek power pellet
                powered  = true;
                powerTimer = POWER_DURATION;
                powerPelletJustEaten = true; // sinyal ke controller untuk frighten semua ghost
            }
        }

        // Hitung mundur durasi power pellet
        if (powered) {
            powerTimer--;
            if (powerTimer <= 0) {
                powered = false;
                powerTimer = 0;
            }
        }
    }

    private void animateMouth() {
        mouthAngle += mouthDir * MOUTH_SPEED;
        if (mouthAngle <= MOUTH_MIN) { mouthAngle = MOUTH_MIN; mouthDir =  1; }
        else if (mouthAngle >= MOUTH_MAX) { mouthAngle = MOUTH_MAX; mouthDir = -1; }
    }

    @Override
    public void render(Graphics2D g2d) {
        int x = getX(), y = getY(), size = tileSize - 2;

        double rotation = 0;
        switch (getDirection()) {
            case RIGHT: rotation =   0; break;
            case DOWN:  rotation =  90; break;
            case LEFT:  rotation = 180; break;
            case UP:    rotation = 270; break;
            default:    rotation = 180; break;
        }

        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(x + size / 2, y + size / 2);
        g.rotate(Math.toRadians(rotation));
        g.translate(-size / 2, -size / 2);

        g.setColor(Color.YELLOW);
        Shape pacShape = new Arc2D.Double(0, 0, size, size,
                mouthAngle, 360 - mouthAngle * 2, Arc2D.PIE);
        g.fill(pacShape);

        if (alive) {
            g.setColor(Color.BLACK);
            g.fillOval(size / 2 - 2, size / 6 - 1, 4, 4);
        }
        g.dispose();
    }

    public void die() {
        if (alive) { alive = false; deathTimer = 0; lives--; }
    }

    public void respawn(int startX, int startY) {
        setX(startX); setY(startY);
        alive  = true; deathTimer = 0;
        powered = false; powerTimer = 0;
        powerPelletJustEaten = false;
        mouthAngle = MOUTH_MAX; mouthDir = -1;
        setDirection(Direction.LEFT);
        setNextDirection(Direction.LEFT);
    }

    public void addScore(int points) { score += points; }

    // Getters & Setters
    public int getLives()     { return lives; }
    public int getScore()     { return score; }
    public boolean isPowered()   { return powered; }
    public int getPowerTimer()   { return powerTimer; }
    public boolean isAlive()     { return alive; }
    public int getDeathTimer()   { return deathTimer; }
    public boolean hasPowerPelletJustEaten() { return powerPelletJustEaten; }
    public void setLives(int lives)  { this.lives = lives; }
    public void setScore(int score)  { this.score = score; }
}