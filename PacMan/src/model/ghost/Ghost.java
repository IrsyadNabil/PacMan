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
 *   SCATTER/CHASE  → (power pellet dimakan) → FRIGHTENED
 *   FRIGHTENED     → (dimakan Pac-Man)       → EATEN
 *   EATEN          → (sampai spawn point)    → RESPAWNING
 *   RESPAWNING     → (timer habis)           → SCATTER/CHASE
 */
public abstract class Ghost extends Entity {

    // ── Mode ─────────────────────────────────────────────────────────────────
    public enum GhostMode {
        CHASE,       // Mengejar Pac-Man
        SCATTER,     // Mundur ke sudut peta
        FRIGHTENED,  // Biru – bisa dimakan Pac-Man
        EATEN,       // Kembali ke spawn (tampil hanya mata)
        RESPAWNING   // Diam di spawn, hitung mundur sebelum aktif kembali
    }

    // ── Konstanta ─────────────────────────────────────────────────────────────
    private static final int FRIGHTENED_DURATION = 300; // ~5 detik @ 60fps
    private static final int FLASH_START         = 80;  // mulai berkedip 80 frame sebelum habis
    private static final int RESPAWN_DURATION    = 180; // ~3 detik diam di dalam rumah
    private static final int SPEED_NORMAL        = 2;
    private static final int SPEED_FRIGHTENED    = 1;
    private static final int SPEED_EATEN         = 4;   // cepat kembali ke rumah

    // ── State ─────────────────────────────────────────────────────────────────
    private GhostMode mode;
    private GhostMode previousMode;
    private final Color normalColor;

    private boolean inHouse;  // belum keluar pertama kali
    private int houseTimer;   // delay awal sebelum keluar

    private int frightenedTimer;
    private boolean flashingWhite;
    private int respawnTimer;

    // Titik spawn permanen (pixel) — ghost kembali ke sini setelah dimakan
    private final int spawnX;
    private final int spawnY;

    // Referensi PacMan untuk AI chase — di-set oleh GameController
    private PacMan trackedPacman;

    protected final GameMap gameMap;
    protected final Random  random;

    protected int scatterRow;
    protected int scatterCol;

    // ── Constructor ───────────────────────────────────────────────────────────
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

    // ── Abstract ──────────────────────────────────────────────────────────────
    /**
     * Tiap subclass menentukan target tile-nya sendiri (AI behavior).
     * Polymorphism: Blinky, Pinky, Inky, Clyde punya implementasi berbeda.
     */
    public abstract int[] getTargetTile(PacMan pacman);

    // ── Setter PacMan (dipanggil GameController setelah initGame) ─────────────
    public void setTrackedPacman(PacMan pacman) {
        this.trackedPacman = pacman;
    }

    // ── Move ──────────────────────────────────────────────────────────────────
    @Override
    public void move() {

        // RESPAWNING: diam di spawn, tunggu timer
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

        // INHOUSE (awal): belum keluar pertama kali
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

        // Hitung mundur frightened timer
        if (mode == GhostMode.FRIGHTENED) {
            frightenedTimer--;
            flashingWhite = frightenedTimer < FLASH_START && (frightenedTimer / 10) % 2 == 0;
            if (frightenedTimer <= 0) {
                // Power Pellet habis → kembali ke mode sebelumnya
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
        int newRow = (newY + tileSize / 2) / tileSize;
        int newCol = (newX + tileSize / 2) / tileSize;

        if (!gameMap.isWalkable(newRow, newCol)) {
            // Tabrak tembok → pilih arah baru
            chooseNewDirection(row, col);
        } else {
            setX(newX);
            setY(newY);
            // Di tiap tile center → tentukan arah berikutnya
            if (isAtTileCenter()) {
                snapToGrid();
                chooseNewDirection(getTileRow(), getTileCol());
            }
        }

        // Tunnel wrap kiri-kanan
        int mapWidth = gameMap.getCols() * tileSize;
        if (getX() < -tileSize) setX(mapWidth - tileSize);
        if (getX() >= mapWidth) setX(0);

        // EATEN: cek apakah sudah cukup dekat dengan spawn point
        if (mode == GhostMode.EATEN) {
            int spawnTileRow = spawnY / tileSize;
            int spawnTileCol = spawnX / tileSize;
            if (getTileRow() == spawnTileRow && getTileCol() == spawnTileCol) {
                arriveAtSpawn();
            }
        }
    }

    private boolean isAtTileCenter() {
        return getX() % tileSize == 0 && getY() % tileSize == 0;
    }

    // ── Pilih arah baru ───────────────────────────────────────────────────────
    protected void chooseNewDirection(int row, int col) {
        List<Direction> valid = getValidDirections(row, col);

        if (valid.isEmpty()) {
            // Dead end: paksa balik arah
            setDirection(getDirection().opposite());
            return;
        }

        switch (mode) {
            case FRIGHTENED:
                // ACAK saat frightened — tidak mengejar Pac-Man
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
                // FIX UTAMA: gunakan trackedPacman (bukan parameter null)
                if (trackedPacman != null) {
                    int[] target = getTargetTile(trackedPacman);
                    moveToward(row, col, target[0], target[1], valid);
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
            if (dir == Direction.NONE)     continue;
            if (dir == current.opposite()) continue; // dilarang balik 180°
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

    // ── Update AI (dipanggil controller tiap frame) ───────────────────────────
    /**
     * Mengatur mode CHASE vs SCATTER berdasarkan global timer.
     * Hanya aktif saat ghost bebas bergerak (bukan FRIGHTENED/EATEN/RESPAWNING).
     */
    public void updateAI(PacMan pacman, int globalTimer) {
        if (inHouse
                || mode == GhostMode.FRIGHTENED
                || mode == GhostMode.EATEN
                || mode == GhostMode.RESPAWNING) return;

        // Pola SCATTER/CHASE klasik Pac-Man (dalam frame @ 60fps):
        // 0–419: SCATTER | 420–1259: CHASE | 1260–1679: SCATTER | 1680+: CHASE
        int t = globalTimer % 3360;
        GhostMode targetMode;
        if      (t < 420)  targetMode = GhostMode.SCATTER;
        else if (t < 1260) targetMode = GhostMode.CHASE;
        else if (t < 1680) targetMode = GhostMode.SCATTER;
        else               targetMode = GhostMode.CHASE;

        if (targetMode != mode) {
            setDirection(getDirection().opposite()); // balik arah saat ganti mode
            mode = targetMode;
        }
    }

    // ── Event: Power Pellet dimakan → ghost masuk FRIGHTENED ─────────────────
    public void setFrightened() {
        if (mode == GhostMode.EATEN || mode == GhostMode.RESPAWNING) return;
        previousMode    = (mode == GhostMode.FRIGHTENED) ? previousMode : mode;
        mode            = GhostMode.FRIGHTENED;
        frightenedTimer = FRIGHTENED_DURATION;
        flashingWhite   = false;
        setSpeed(SPEED_FRIGHTENED);
        setDirection(getDirection().opposite()); // langsung balik arah
    }

    // ── Event: Ghost dimakan Pac-Man → mode EATEN ─────────────────────────────
    public void setEaten() {
        mode            = GhostMode.EATEN;
        frightenedTimer = 0;
        flashingWhite   = false;
        setSpeed(SPEED_EATEN); // bergerak lebih cepat kembali ke rumah
    }

    // ── Tiba di spawn point (dari EATEN) → masuk RESPAWNING ──────────────────
    private void arriveAtSpawn() {
        setX(spawnX);
        setY(spawnY);
        mode         = GhostMode.RESPAWNING;
        respawnTimer = RESPAWN_DURATION;
        setSpeed(0);
        setDirection(Direction.UP);
        previousMode = GhostMode.SCATTER; // setelah respawn mulai dari SCATTER
    }

    // ── Reset penuh (level baru / Pac-Man mati / restart) ────────────────────
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

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    public void render(Graphics2D g2d) {
        // Tidak terlihat saat sedang diam respawn di rumah
        if (mode == GhostMode.RESPAWNING) return;

        int x = getX(), y = getY(), w = tileSize - 2, h = tileSize - 2;
        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (mode == GhostMode.EATEN) {
            // Hanya tampilkan mata — ghost "terbang" kembali ke rumah
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

        // Gambar tubuh utama
        g.setColor(bodyColor);
        g.fillArc(x, y, w, h, 0, 180);           // setengah lingkaran atas
        g.fillRect(x, y + h / 2, w, h / 2);      // kotak bawah

        // Bawah bergelombang (3 lengkungan)
        int waveW = w / 3;
        for (int i = 0; i < 3; i++) {
            g.setColor(Color.BLACK);
            g.fillArc(x + i * waveW, y + h - waveW / 2, waveW, waveW, 0, -180);
            g.setColor(bodyColor);
        }

        // Gambar mata sesuai mode
        if (mode == GhostMode.FRIGHTENED) {
            drawFrightenedFace(g, x, y, w, h);
        } else {
            drawEyes(g, x, y, w, h);
        }

        g.dispose();
    }

    /** Mata normal — pupil mengikuti arah gerak */
    private void drawEyes(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(Color.WHITE);
        g.fillOval(x + w / 4 - 3, y + h / 4, 8, 8);
        g.fillOval(x + 3 * w / 4 - 5, y + h / 4, 8, 8);
        // Pupil mengikuti arah gerakan
        int px = getDirection().getDx() * 2;
        int py = getDirection().getDy() * 2;
        g.setColor(new Color(0, 0, 180));
        g.fillOval(x + w / 4 - 1 + px, y + h / 4 + 2 + py, 4, 4);
        g.fillOval(x + 3 * w / 4 - 3 + px, y + h / 4 + 2 + py, 4, 4);
    }

    /** Wajah ketakutan saat FRIGHTENED */
    private void drawFrightenedFace(Graphics2D g, int x, int y, int w, int h) {
        // Mata putih kecil
        g.setColor(Color.WHITE);
        g.fillOval(x + w / 4 - 2, y + h / 4, 6, 6);
        g.fillOval(x + 3 * w / 4 - 4, y + h / 4, 6, 6);
        // Pupil merah
        g.setColor(new Color(200, 0, 0));
        g.fillOval(x + w / 4,         y + h / 4 + 1, 3, 3);
        g.fillOval(x + 3 * w / 4 - 2, y + h / 4 + 1, 3, 3);
        // Mulut zigzag
        int mx = x + 3, my = y + h * 2 / 3;
        int seg = (w - 6) / 4;
        for (int i = 0; i < 4; i++) {
            int zx = mx + i * seg;
            int zy = my + (i % 2 == 0 ? 3 : -3);
            g.fillRect(zx, zy, seg, 2);
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public GhostMode getMode()          { return mode; }
    public void setMode(GhostMode m)    { this.mode = m; }
    public Color getNormalColor()       { return normalColor; }
    public boolean isInHouse()          { return inHouse; }
    public boolean isFrightened()       { return mode == GhostMode.FRIGHTENED; }
    public boolean isEaten()            { return mode == GhostMode.EATEN; }
    public boolean isRespawning()       { return mode == GhostMode.RESPAWNING; }
    public int getFrightenedTimer()     { return frightenedTimer; }
    public int getSpawnX()              { return spawnX; }
    public int getSpawnY()              { return spawnY; }
}