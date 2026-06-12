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
 * Abstract base class untuk semua ghost.
 * Abstraction: abstract class + abstract method getTargetTile()
 * Inheritance: extends Entity
 *
 * Siklus mode sesuai spesifikasi:
 *   SCATTER/CHASE → (power pellet) → FRIGHTENED
 *   FRIGHTENED    → (dimakan)      → EATEN
 *   EATEN         → (sampai spawn) → RESPAWNING
 *   RESPAWNING    → (timer habis)  → SCATTER/CHASE
 */
public abstract class Ghost extends Entity {

    // ── Mode ─────────────────────────────────────────────────────────────
    public enum GhostMode {
        CHASE,       // Mengejar Pac-Man
        SCATTER,     // Mundur ke sudut
        FRIGHTENED,  // Biru – bisa dimakan Pac-Man
        EATEN,       // Kembali ke spawn (tampil hanya mata)
        RESPAWNING   // Diam di spawn, hitung mundur sebelum aktif kembali
    }

    // ── Konstanta ─────────────────────────────────────────────────────────
    private static final int FRIGHTENED_DURATION = 300; // ~5 detik @ 60fps
    private static final int FLASH_START         = 80;  // kedip 80 frame sebelum habis
    private static final int RESPAWN_DURATION    = 180; // ~3 detik diam di spawn
    private static final int SPEED_NORMAL        = 2;
    private static final int SPEED_FRIGHTENED    = 1;
    private static final int SPEED_EATEN         = 4;   // cepat balik ke rumah

    // ── State ─────────────────────────────────────────────────────────────
    private GhostMode mode;
    private GhostMode previousMode;
    private final Color normalColor;

    private boolean inHouse;   // belum keluar pertama kali dari rumah
    private int houseTimer;    // delay sebelum keluar pertama kali

    private int frightenedTimer;
    private boolean flashingWhite;
    private int respawnTimer;

    // Titik spawn permanen (pixel) — ghost kembali ke sini setelah dimakan
    private final int spawnX;
    private final int spawnY;

    protected final GameMap gameMap;
    protected final Random random;

    protected int scatterRow;
    protected int scatterCol;

    // ── Constructor ───────────────────────────────────────────────────────
    public Ghost(int x, int y, int tileSize, GameMap gameMap,
                 Color color, int houseDelay) {
        super(x, y, SPEED_NORMAL, tileSize);
        this.spawnX       = x;
        this.spawnY       = y;
        this.gameMap      = gameMap;
        this.normalColor  = color;
        this.mode         = GhostMode.SCATTER;
        this.previousMode = GhostMode.SCATTER;
        this.inHouse      = true;
        this.houseTimer   = houseDelay;
        this.random       = new Random();
        setDirection(Direction.UP);
    }

    // ── Abstract ──────────────────────────────────────────────────────────
    /**
     * Setiap subclass menentukan target tile-nya sendiri.
     * Polimorfisme: Blinky, Pinky, Inky, Clyde implementasi berbeda.
     */
    public abstract int[] getTargetTile(PacMan pacman);

    // ── Move ──────────────────────────────────────────────────────────────
    @Override
    public void move() {

        // RESPAWNING: diam di spawn, hitung mundur
        if (mode == GhostMode.RESPAWNING) {
            respawnTimer--;
            if (respawnTimer <= 0) {
                // Keluar dari rumah ke koridor utama
                setX(13 * tileSize);
                setY(11 * tileSize);
                mode = previousMode;
                setSpeed(SPEED_NORMAL);
                setDirection(Direction.LEFT);
            }
            return;
        }

        // INHOUSE: belum keluar untuk pertama kali
        if (inHouse) {
            houseTimer--;
            if (houseTimer <= 0) {
                inHouse = false;
                setX(13 * tileSize);
                setY(11 * tileSize);
                setDirection(Direction.LEFT);
            }
            return;
        }

        // Hitung mundur frightened
        if (mode == GhostMode.FRIGHTENED) {
            frightenedTimer--;
            flashingWhite = frightenedTimer < FLASH_START && (frightenedTimer / 10) % 2 == 0;
            if (frightenedTimer <= 0) {
                // Efek power pellet habis → kembali ke mode sebelumnya
                mode          = previousMode;
                flashingWhite = false;
                setSpeed(SPEED_NORMAL);
            }
        }

        // Gerak fisik
        int row  = getTileRow();
        int col  = getTileCol();
        int newX = getX() + getDirection().getDx() * getSpeed();
        int newY = getY() + getDirection().getDy() * getSpeed();
        int newCol = (newX + tileSize / 2) / tileSize;
        int newRow = (newY + tileSize / 2) / tileSize;

        if (!gameMap.isWalkable(newRow, newCol)) {
            chooseNewDirection(row, col, null);
        } else {
            setX(newX);
            setY(newY);
            if (isAtTileCenter()) {
                snapToGrid();
                chooseNewDirection(getTileRow(), getTileCol(), null);
            }
        }

        // Tunnel wrap kiri-kanan
        int mapWidth = gameMap.getCols() * tileSize;
        if (getX() < -tileSize)  setX(mapWidth - tileSize);
        if (getX() >= mapWidth)  setX(0);

        // Cek apakah ghost EATEN sudah sampai di area spawn
        if (mode == GhostMode.EATEN) {
            int targetRow = spawnY / tileSize;
            int targetCol = spawnX / tileSize;
            if (Math.abs(getTileRow() - targetRow) <= 1 &&
                Math.abs(getTileCol() - targetCol) <= 1) {
                arriveAtSpawn();
            }
        }
    }

    private boolean isAtTileCenter() {
        return getX() % tileSize == 0 && getY() % tileSize == 0;
    }

    // ── Pilih arah baru ───────────────────────────────────────────────────
    protected void chooseNewDirection(int row, int col, PacMan pacman) {
        List<Direction> valid = getValidDirections(row, col);
        if (valid.isEmpty()) {
            setDirection(getDirection().opposite());
            return;
        }

        switch (mode) {
            case FRIGHTENED:
                // Bergerak ACAK saat frightened
                setDirection(valid.get(random.nextInt(valid.size())));
                break;

            case EATEN:
                // Kembali ke titik spawn secepat mungkin
                moveToward(row, col, spawnY / tileSize, spawnX / tileSize, valid);
                break;

            case SCATTER:
                moveToward(row, col, scatterRow, scatterCol, valid);
                break;

            case CHASE:
                if (pacman != null) {
                    int[] t = getTargetTile(pacman);
                    moveToward(row, col, t[0], t[1], valid);
                } else {
                    setDirection(valid.get(0));
                }
                break;

            default:
                setDirection(valid.get(0));
        }
    }

    private List<Direction> getValidDirections(int row, int col) {
        List<Direction> list = new ArrayList<>();
        Direction current    = getDirection();
        for (Direction dir : Direction.values()) {
            if (dir == Direction.NONE)         continue;
            if (dir == current.opposite())     continue; // dilarang balik arah
            if (gameMap.isWalkable(row + dir.getDy(), col + dir.getDx()))
                list.add(dir);
        }
        return list;
    }

    protected void moveToward(int fromRow, int fromCol, int targetRow, int targetCol,
                               List<Direction> validDirs) {
        Direction best     = validDirs.get(0);
        double    bestDist = Double.MAX_VALUE;
        for (Direction dir : validDirs) {
            double dist = Math.hypot(
                (fromRow + dir.getDy()) - targetRow,
                (fromCol + dir.getDx()) - targetCol);
            if (dist < bestDist) { bestDist = dist; best = dir; }
        }
        setDirection(best);
    }

    // ── Update AI (dipanggil controller tiap frame) ───────────────────────
    public void updateAI(PacMan pacman) {
        if (inHouse || mode == GhostMode.FRIGHTENED
                    || mode == GhostMode.EATEN
                    || mode == GhostMode.RESPAWNING) return;
        chooseNewDirection(getTileRow(), getTileCol(), pacman);
    }

    // ── Event: Power Pellet dimakan → semua ghost frightened ──────────────
    public void setFrightened() {
        if (mode == GhostMode.EATEN || mode == GhostMode.RESPAWNING) return;
        previousMode    = (mode == GhostMode.FRIGHTENED) ? previousMode : mode;
        mode            = GhostMode.FRIGHTENED;
        frightenedTimer = FRIGHTENED_DURATION;
        flashingWhite   = false;
        setSpeed(SPEED_FRIGHTENED);
        setDirection(getDirection().opposite()); // balik arah seketika
    }

    // ── Event: Ghost dimakan Pac-Man → mode EATEN ─────────────────────────
    public void setEaten() {
        mode            = GhostMode.EATEN;
        frightenedTimer = 0;
        flashingWhite   = false;
        setSpeed(SPEED_EATEN);
        // Ghost langsung menuju spawn point
    }

    // ── Tiba di spawn point (dari mode EATEN) → RESPAWNING ───────────────
    private void arriveAtSpawn() {
        setX(spawnX);
        setY(spawnY);
        mode         = GhostMode.RESPAWNING;
        respawnTimer = RESPAWN_DURATION;
        setSpeed(0);
        setDirection(Direction.UP);
    }

    // ── Reset penuh (level baru / game restart) ───────────────────────────
    public void respawn(int x, int y, int houseDelay) {
        setX(x); setY(y);
        mode          = GhostMode.SCATTER;
        previousMode  = GhostMode.SCATTER;
        inHouse       = true;
        houseTimer    = houseDelay;
        frightenedTimer = 0;
        respawnTimer  = 0;
        flashingWhite = false;
        setSpeed(SPEED_NORMAL);
        setDirection(Direction.UP);
    }

    // ── Render ────────────────────────────────────────────────────────────
    @Override
    public void render(Graphics2D g2d) {
        // Ghost tidak terlihat saat sedang respawning di dalam rumah
        if (mode == GhostMode.RESPAWNING) return;

        int x = getX(), y = getY(), w = tileSize - 2, h = tileSize - 2;
        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (mode == GhostMode.EATEN) {
            // Hanya tampilkan mata — ghost sedang kembali ke spawn
            drawEyes(g, x, y, w, h);
            g.dispose();
            return;
        }

        // Tentukan warna tubuh
        Color bodyColor;
        if (mode == GhostMode.FRIGHTENED) {
            bodyColor = flashingWhite ? Color.WHITE : new Color(20, 20, 200);
        } else {
            bodyColor = normalColor;
        }

        // Gambar tubuh
        g.setColor(bodyColor);
        g.fillArc(x, y, w, h, 0, 180);
        g.fillRect(x, y + h / 2, w, h / 2);

        // Bawah bergelombang
        int waveW = w / 3;
        for (int i = 0; i < 3; i++) {
            g.setColor(Color.BLACK);
            g.fillArc(x + i * waveW, y + h - waveW / 2, waveW, waveW, 0, -180);
            g.setColor(bodyColor);
        }

        // Gambar mata
        if (mode == GhostMode.FRIGHTENED) {
            // Ekspresi ketakutan
            g.setColor(Color.WHITE);
            g.fillOval(x + w / 4 - 2, y + h / 4, 6, 6);
            g.fillOval(x + 3 * w / 4 - 4, y + h / 4, 6, 6);
            g.setColor(new Color(200, 0, 0));
            g.fillOval(x + w / 4,       y + h / 4 + 1, 3, 3);
            g.fillOval(x + 3 * w / 4 - 2, y + h / 4 + 1, 3, 3);
            // Mulut zigzag
            g.setColor(new Color(200, 0, 0));
            int mx = x + 3, my = y + h * 2 / 3;
            for (int i = 0; i < 4; i++) {
                int zx = mx + i * (w - 6) / 4;
                int zy = my + (i % 2 == 0 ? 3 : -3);
                g.fillRect(zx, zy, (w - 6) / 4, 2);
            }
        } else {
            drawEyes(g, x, y, w, h);
        }

        g.dispose();
    }

    private void drawEyes(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(Color.WHITE);
        g.fillOval(x + w / 4 - 3, y + h / 4, 8, 8);
        g.fillOval(x + 3 * w / 4 - 5, y + h / 4, 8, 8);
        // Pupil mengikuti arah gerak
        int px = 0, py = 0;
        switch (getDirection()) {
            case UP:    px =  0; py = -2; break;
            case DOWN:  px =  0; py =  2; break;
            case LEFT:  px = -2; py =  0; break;
            case RIGHT: px =  2; py =  0; break;
            default: break;
        }
        g.setColor(new Color(0, 0, 200));
        g.fillOval(x + w / 4 - 1 + px, y + h / 4 + 2 + py, 4, 4);
        g.fillOval(x + 3 * w / 4 - 3 + px, y + h / 4 + 2 + py, 4, 4);
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public GhostMode getMode()              { return mode; }
    public void setMode(GhostMode mode)     { this.mode = mode; }
    public Color getNormalColor()           { return normalColor; }
    public boolean isInHouse()              { return inHouse; }
    public boolean isFrightened()           { return mode == GhostMode.FRIGHTENED; }
    public boolean isEaten()                { return mode == GhostMode.EATEN; }
    public boolean isRespawning()           { return mode == GhostMode.RESPAWNING; }
    public int getFrightenedTimer()         { return frightenedTimer; }
    public int getSpawnX()                  { return spawnX; }
    public int getSpawnY()                  { return spawnY; }
}