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
 * Siklus mode:
 *   SCATTER/CHASE  → (power pellet) → FRIGHTENED
 *   FRIGHTENED     → (dimakan)      → EATEN
 *   EATEN          → (sampai spawn) → RESPAWNING
 *   RESPAWNING     → (timer habis)  → SCATTER/CHASE
 *
 * Fase spawn awal:
 *   IN_HOUSE → (houseTimer habis) → EXITING → (sampai exit tile) → SCATTER/CHASE
 */
public abstract class Ghost extends Entity {

    // ── Mode ─────────────────────────────────────────────────────────────────
    public enum GhostMode {
        CHASE,
        SCATTER,
        FRIGHTENED,
        EATEN,       // kembali ke spawn (tampil hanya mata)
        RESPAWNING,  // diam di spawn, tunggu timer
        IN_HOUSE,    // belum keluar — menunggu di dalam rumah
        EXITING      // sedang berjalan keluar dari rumah ke koridor
    }

    // ── Konstanta ─────────────────────────────────────────────────────────────
    private static final int FRIGHTENED_DURATION = 300; // ~5 detik @ 60fps
    private static final int FLASH_START         = 80;
    private static final int RESPAWN_DURATION    = 180; // ~3 detik
    protected static final int SPEED_NORMAL      = 2;
    private static final int SPEED_FRIGHTENED    = 1;
    private static final int SPEED_EATEN         = 4;
    private static final int SPEED_HOUSE         = 1;   // lambat saat di dalam/keluar rumah

    // Tile exit dari ghost house (tepat di atas pintu rumah)
    private static final int EXIT_ROW = 11;
    private static final int EXIT_COL = 13;

    // ── State ─────────────────────────────────────────────────────────────────
    private GhostMode mode;
    private GhostMode previousMode;
    private final Color normalColor;

    private int houseTimer;       // frame tunggu sebelum mulai keluar
    private int frightenedTimer;
    private boolean flashingWhite;
    private int respawnTimer;

    // Titik spawn permanen (pixel) — ghost kembali ke sini setelah dimakan
    private final int spawnX;
    private final int spawnY;

    // PacMan reference untuk AI chase
    private PacMan trackedPacman;

    protected final GameMap gameMap;
    protected final Random  random;

    protected int scatterRow;
    protected int scatterCol;

    // ── Constructor ───────────────────────────────────────────────────────────
    public Ghost(int x, int y, int tileSize, GameMap gameMap,
                 Color color, int houseDelay) {
        super(x, y, SPEED_HOUSE, tileSize);
        this.spawnX       = x;
        this.spawnY       = y;
        this.gameMap      = gameMap;
        this.normalColor  = color;
        this.mode         = GhostMode.IN_HOUSE;
        this.previousMode = GhostMode.SCATTER;
        this.houseTimer   = houseDelay;
        this.random       = new Random();
        setDirection(Direction.UP);
    }

    // ── Abstract ──────────────────────────────────────────────────────────────
    public abstract int[] getTargetTile(PacMan pacman);

    public void setTrackedPacman(PacMan pacman) { this.trackedPacman = pacman; }

    // ── Move ──────────────────────────────────────────────────────────────────
    @Override
    public void move() {

        switch (mode) {

            // ── IN_HOUSE: diam di dalam rumah, hitung mundur delay ──────────
            case IN_HOUSE:
                houseTimer--;
                if (houseTimer <= 0) {
                    // Mulai bergerak menuju pintu keluar
                    mode = GhostMode.EXITING;
                    setSpeed(SPEED_HOUSE);
                    setDirection(Direction.UP);  // arah keluar = naik
                }
                return;

            // ── EXITING: berjalan keluar dari rumah ke exit tile ────────────
            case EXITING: {
                // Gerakkan ghost menuju tile exit (row=11, col=13) secara smooth
                int exitX = EXIT_COL * tileSize;
                int exitY = EXIT_ROW * tileSize;

                // Sesuaikan X dulu ke col 13 (gerakan horizontal di dalam rumah)
                if (getX() != exitX) {
                    int dx = exitX - getX();
                    int step = Math.min(Math.abs(dx), SPEED_HOUSE) * (dx > 0 ? 1 : -1);
                    setX(getX() + step);
                } else if (getY() > exitY) {
                    // Sudah di kolom yang benar, gerak naik menuju exit row
                    int step = Math.min(getY() - exitY, SPEED_HOUSE);
                    setY(getY() - step);
                } else {
                    // Sudah sampai di exit tile → mulai normal
                    setX(exitX);
                    setY(exitY);
                    mode = GhostMode.SCATTER;
                    setSpeed(SPEED_NORMAL);
                    setDirection(Direction.LEFT);
                    snapToGrid();
                }
                return;
            }

            // ── RESPAWNING: diam di spawn, hitung mundur ────────────────────
            case RESPAWNING:
                respawnTimer--;
                if (respawnTimer <= 0) {
                    // Keluar lagi melalui jalur yang sama
                    mode = GhostMode.EXITING;
                    setX(spawnX);
                    setY(spawnY);
                    setSpeed(SPEED_HOUSE);
                    setDirection(Direction.UP);
                }
                return;

            default:
                break;
        }

        // ── Mode FRIGHTENED: hitung mundur timer ────────────────────────────
        if (mode == GhostMode.FRIGHTENED) {
            frightenedTimer--;
            flashingWhite = frightenedTimer < FLASH_START && (frightenedTimer / 10) % 2 == 0;
            if (frightenedTimer <= 0) {
                mode          = previousMode;
                flashingWhite = false;
                setSpeed(SPEED_NORMAL);
            }
        }

        // ── Gerak fisik tile-by-tile ─────────────────────────────────────────
        // Ghost hanya mengambil keputusan arah saat tepat di center tile,
        // lalu bergerak lurus sampai tile berikutnya.
        if (isAtTileCenter()) {
            snapToGrid();
            chooseNewDirection(getTileRow(), getTileCol());
        }

        int newX = getX() + getDirection().getDx() * getSpeed();
        int newY = getY() + getDirection().getDy() * getSpeed();
        int newRow = (newY + tileSize / 2) / tileSize;
        int newCol = (newX + tileSize / 2) / tileSize;

        if (!gameMap.isWalkable(newRow, newCol)) {
            // Tabrak tembok (seharusnya tidak terjadi jika chooseNewDirection benar)
            // Paksa pilih arah baru dari posisi saat ini
            chooseNewDirection(getTileRow(), getTileCol());
        } else {
            setX(newX);
            setY(newY);
        }

        // ── Tunnel wrap kiri-kanan ───────────────────────────────────────────
        int mapWidth = gameMap.getCols() * tileSize;
        if (getX() < 0)        setX(mapWidth - tileSize);
        if (getX() >= mapWidth) setX(0);

        // ── EATEN: cek apakah sudah sampai di spawn ──────────────────────────
        if (mode == GhostMode.EATEN) {
            if (getTileRow() == spawnY / tileSize && getTileCol() == spawnX / tileSize) {
                arriveAtSpawn();
            }
        }
    }

    /** True jika posisi ghost tepat di tengah tile */
    private boolean isAtTileCenter() {
        return getX() % tileSize == 0 && getY() % tileSize == 0;
    }

    // ── Pilih arah berikutnya ─────────────────────────────────────────────────
    protected void chooseNewDirection(int row, int col) {
        List<Direction> valid = getValidDirections(row, col);

        if (valid.isEmpty()) {
            // Dead end: terpaksa balik arah
            setDirection(getDirection().opposite());
            return;
        }

        switch (mode) {

            case FRIGHTENED:
                // Gerakan ACAK saat frightened — tidak mengikuti Pac-Man
                setDirection(valid.get(random.nextInt(valid.size())));
                break;

            case EATEN:
                // Kembali ke spawn point secepat mungkin
                moveToward(row, col, spawnY / tileSize, spawnX / tileSize, valid);
                break;

            case SCATTER:
                moveToward(row, col, scatterRow, scatterCol, valid);
                break;

            case CHASE:
                if (trackedPacman != null) {
                    int[] t = getTargetTile(trackedPacman);
                    moveToward(row, col, t[0], t[1], valid);
                } else {
                    setDirection(valid.get(0));
                }
                break;

            default:
                setDirection(valid.get(0));
        }
    }

    /**
     * Daftar arah valid dari tile (row, col).
     * Ghost tidak boleh balik 180° kecuali di dead end.
     * Ghost tidak boleh masuk ke tile WALL.
     */
    private List<Direction> getValidDirections(int row, int col) {
        List<Direction> list = new ArrayList<>();
        Direction current    = getDirection();
        for (Direction dir : Direction.values()) {
            if (dir == Direction.NONE)     continue;
            if (dir == current.opposite()) continue; // dilarang balik 180°
            int nr = row + dir.getDy();
            int nc = col + dir.getDx();
            if (gameMap.isWalkable(nr, nc)) list.add(dir);
        }
        return list;
    }

    /** Pilih arah yang paling dekat ke target tile */
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

    // ── Update AI mode (CHASE/SCATTER cycling) ────────────────────────────────
    public void updateAI(PacMan pacman, int globalTimer) {
        if (mode == GhostMode.IN_HOUSE
                || mode == GhostMode.EXITING
                || mode == GhostMode.FRIGHTENED
                || mode == GhostMode.EATEN
                || mode == GhostMode.RESPAWNING) return;

        // Pola klasik SCATTER/CHASE (frame @ 60fps)
        int t = globalTimer % 3360;
        GhostMode target;
        if      (t < 420)  target = GhostMode.SCATTER;
        else if (t < 1260) target = GhostMode.CHASE;
        else if (t < 1680) target = GhostMode.SCATTER;
        else               target = GhostMode.CHASE;

        if (target != mode) {
            setDirection(getDirection().opposite()); // balik arah saat ganti mode
            mode = target;
        }
    }

    // ── Events ────────────────────────────────────────────────────────────────
    /** Power pellet dimakan → ghost masuk FRIGHTENED */
    public void setFrightened() {
        if (mode == GhostMode.EATEN || mode == GhostMode.RESPAWNING
                || mode == GhostMode.IN_HOUSE || mode == GhostMode.EXITING) return;
        previousMode    = (mode == GhostMode.FRIGHTENED) ? previousMode : mode;
        mode            = GhostMode.FRIGHTENED;
        frightenedTimer = FRIGHTENED_DURATION;
        flashingWhite   = false;
        setSpeed(SPEED_FRIGHTENED);
        if (isAtTileCenter()) setDirection(getDirection().opposite());
    }

    /** Ghost dimakan Pac-Man → mode EATEN, kembali ke spawn */
    public void setEaten() {
        mode            = GhostMode.EATEN;
        frightenedTimer = 0;
        flashingWhite   = false;
        setSpeed(SPEED_EATEN);
    }

    /** Ghost tiba di spawn setelah dimakan → RESPAWNING */
    private void arriveAtSpawn() {
        setX(spawnX);
        setY(spawnY);
        mode          = GhostMode.RESPAWNING;
        respawnTimer  = RESPAWN_DURATION;
        setSpeed(0);
        setDirection(Direction.UP);
        previousMode  = GhostMode.SCATTER;
    }

    /** Reset penuh (level baru / Pac-Man mati / restart) */
    public void respawn(int x, int y, int houseDelay) {
        setX(x); setY(y);
        mode          = GhostMode.IN_HOUSE;
        previousMode  = GhostMode.SCATTER;
        houseTimer    = houseDelay;
        frightenedTimer = 0;
        respawnTimer  = 0;
        flashingWhite = false;
        setSpeed(SPEED_HOUSE);
        setDirection(Direction.UP);
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    public void render(Graphics2D g2d) {
        // Tidak terlihat saat diam respawning di dalam rumah
        if (mode == GhostMode.RESPAWNING || mode == GhostMode.IN_HOUSE) return;

        int x = getX(), y = getY(), w = tileSize - 2, h = tileSize - 2;
        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (mode == GhostMode.EATEN) {
            // Hanya mata saja — ghost "terbang" kembali ke rumah
            drawEyes(g, x, y, w, h);
            g.dispose();
            return;
        }

        // Tentukan warna
        Color bodyColor;
        if (mode == GhostMode.FRIGHTENED) {
            bodyColor = flashingWhite ? Color.WHITE : new Color(20, 20, 200);
        } else {
            bodyColor = normalColor;
        }

        // Badan
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

        // Mata
        if (mode == GhostMode.FRIGHTENED) {
            drawFrightenedFace(g, x, y, w, h);
        } else {
            drawEyes(g, x, y, w, h);
        }

        g.dispose();
    }

    private void drawEyes(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(Color.WHITE);
        g.fillOval(x + w / 4 - 3, y + h / 4, 8, 8);
        g.fillOval(x + 3 * w / 4 - 5, y + h / 4, 8, 8);
        int px = getDirection().getDx() * 2;
        int py = getDirection().getDy() * 2;
        g.setColor(new Color(0, 0, 180));
        g.fillOval(x + w / 4 - 1 + px, y + h / 4 + 2 + py, 4, 4);
        g.fillOval(x + 3 * w / 4 - 3 + px, y + h / 4 + 2 + py, 4, 4);
    }

    private void drawFrightenedFace(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(Color.WHITE);
        g.fillOval(x + w / 4 - 2, y + h / 4, 6, 6);
        g.fillOval(x + 3 * w / 4 - 4, y + h / 4, 6, 6);
        g.setColor(new Color(200, 0, 0));
        g.fillOval(x + w / 4,         y + h / 4 + 1, 3, 3);
        g.fillOval(x + 3 * w / 4 - 2, y + h / 4 + 1, 3, 3);
        // Mulut zigzag
        int mx = x + 3, my = y + h * 2 / 3, seg = (w - 6) / 4;
        for (int i = 0; i < 4; i++) {
            g.fillRect(mx + i * seg, my + (i % 2 == 0 ? 3 : -3), seg, 2);
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public GhostMode getMode()       { return mode; }
    public void setMode(GhostMode m) { this.mode = m; }
    public Color getNormalColor()    { return normalColor; }
    public boolean isFrightened()    { return mode == GhostMode.FRIGHTENED; }
    public boolean isEaten()         { return mode == GhostMode.EATEN; }
    public boolean isRespawning()    { return mode == GhostMode.RESPAWNING; }
    public int getFrightenedTimer()  { return frightenedTimer; }
    public int getSpawnX()           { return spawnX; }
    public int getSpawnY()           { return spawnY; }
}