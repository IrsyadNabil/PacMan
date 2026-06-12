package pacman.controller;

import pacman.model.*;
import pacman.model.ghost.*;
import pacman.util.Direction;
import pacman.util.ScoreManager;
import pacman.view.GamePanel;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Main game controller. Coordinates model and view.
 * MVC: Controller layer.
 * Single Responsibility & Open/Closed principles (SOLID).
 */
public class GameController {

    private static final int TILE_SIZE            = 20;
    private static final int FPS                  = 60;
    private static final int TIMER_DELAY          = 1000 / FPS;
    private static final int DEATH_ANIM_DURATION  = 90;   // frames
    private static final int LEVEL_COMPLETE_PAUSE = 120;  // frames

    // Pac-Man start position
    private static final int PACMAN_START_X = 13 * TILE_SIZE;
    private static final int PACMAN_START_Y = 23 * TILE_SIZE;

    // Ghost house center (titik exit ghost house)
    private static final int GHOST_HOUSE_X = 13 * TILE_SIZE;
    private static final int GHOST_HOUSE_Y = 13 * TILE_SIZE;

    private GameMap   gameMap;
    private PacMan    pacman;
    private List<Ghost> ghosts;
    private Blinky    blinky;
    private GameState gameState;
    private GamePanel gamePanel;

    private Timer gameTimer;
    private int   stateTimer;

    public GameController(GamePanel panel) {
        this.gamePanel = panel;
        initGame();
        setupTimer();
    }

    private void initGame() {
        gameMap   = new GameMap(TILE_SIZE);
        gameState = new GameState();

        // FIX #4: Load high score dari file saat game dimulai
        gameState.setHighScore(ScoreManager.loadHighScore());

        pacman = new PacMan(PACMAN_START_X, PACMAN_START_Y, TILE_SIZE, gameMap);

        ghosts  = new ArrayList<>();
        blinky  = new Blinky(GHOST_HOUSE_X,              GHOST_HOUSE_Y, TILE_SIZE, gameMap);
        Pinky pinky = new Pinky(GHOST_HOUSE_X + TILE_SIZE,   GHOST_HOUSE_Y, TILE_SIZE, gameMap);
        Inky  inky  = new Inky (GHOST_HOUSE_X - TILE_SIZE,   GHOST_HOUSE_Y, TILE_SIZE, gameMap, blinky);
        Clyde clyde = new Clyde(GHOST_HOUSE_X + 2 * TILE_SIZE, GHOST_HOUSE_Y, TILE_SIZE, gameMap);

        ghosts.add(blinky);
        ghosts.add(pinky);
        ghosts.add(inky);
        ghosts.add(clyde);
    }

    private void setupTimer() {
        gameTimer = new Timer(TIMER_DELAY, e -> {
            update();
            gamePanel.repaint();
        });
    }

    // ── Loop utama ────────────────────────────────────────────────────────────
    private void update() {
        GameState.State state = gameState.getCurrentState();
        if      (state == GameState.State.PLAYING)        updatePlaying();
        else if (state == GameState.State.DEAD_ANIMATION) updateDeathAnimation();
        else if (state == GameState.State.LEVEL_COMPLETE) updateLevelComplete();
    }

    private void updatePlaying() {
        pacman.move();

        for (Ghost g : ghosts) {
            g.move();
            g.updateAI(pacman);
        }

        checkCollisions();

        if (gameMap.allPelletsEaten()) {
            gameState.levelComplete();
            stateTimer = LEVEL_COMPLETE_PAUSE;
        }
    }

    private void checkCollisions() {
        // FIX #1: Gunakan flag powerPelletJustEaten (bukan hardcode powerTimer == 199)
        // sehingga ghost masuk Frightened Mode tepat di frame yang sama Pac-Man
        // memakan Power Pellet, terlepas dari nilai timer yang tersisa.
        if (pacman.hasPowerPelletJustEaten()) {
            gameState.resetGhostCombo();
            for (Ghost g : ghosts) {
                // FIX #2: Ghost yang sedang EATEN atau RESPAWNING tidak difrightened
                if (!g.isEaten() && !g.isRespawning()) {
                    g.setFrightened();
                }
            }
        }

        for (Ghost ghost : ghosts) {
            if (!pacman.collidesWith(ghost)) continue;

            if (ghost.isFrightened()) {
                // Pac-Man memakan ghost → ghost masuk EATEN lalu menuju spawn
                int points = gameState.getGhostEatenScore();
                pacman.addScore(points);
                ghost.setEaten();

            } else if (!ghost.isEaten() && !ghost.isRespawning() && pacman.isAlive()) {
                // FIX #2: Ghost EATEN atau RESPAWNING tidak boleh membunuh Pac-Man
                pacman.die();
                gameState.triggerDeathAnimation();
                stateTimer = DEATH_ANIM_DURATION;
                return; // hentikan pengecekan ghost lain di frame ini
            }
        }
    }

    private void updateDeathAnimation() {
        stateTimer--;
        if (stateTimer <= 0) {
            if (pacman.getLives() <= 0) {
                // FIX #4: Simpan high score ke file saat game over
                int finalScore = pacman.getScore();
                gameState.updateHighScore(finalScore);
                ScoreManager.saveHighScore(gameState.getHighScore());
                gameState.gameOver();
            } else {
                respawnAfterDeath();
            }
        }
    }

    private void respawnAfterDeath() {
        pacman.respawn(PACMAN_START_X, PACMAN_START_Y);

        // FIX #3: Tiap ghost dikembalikan ke spawn point-nya sendiri (bukan semua
        // ke GHOST_HOUSE_X/Y yang sama), dengan delay bertahap agar tidak keluar
        // bersamaan.
        int delay = 0;
        for (Ghost g : ghosts) {
            g.respawn(g.getSpawnX(), g.getSpawnY(), delay);
            delay += 30;
        }
        gameState.setCurrentState(GameState.State.PLAYING);
    }

    private void updateLevelComplete() {
        stateTimer--;
        if (stateTimer <= 0) nextLevel();
    }

    private void nextLevel() {
        gameMap.reset();
        pacman.respawn(PACMAN_START_X, PACMAN_START_Y);

        // FIX #3: sama seperti respawnAfterDeath — tiap ghost ke spawn-nya sendiri
        int delay = 0;
        for (Ghost g : ghosts) {
            g.respawn(g.getSpawnX(), g.getSpawnY(), delay);
            delay += 30;
        }
        gameState.nextLevel();
    }

    // ── Public API untuk View ─────────────────────────────────────────────────
    public void startGame() {
        initGame();
        gameState.startGame();
        gameTimer.start();
    }

    public void restartGame() {
        gameTimer.stop();
        startGame();
    }

    public void togglePause() {
        if (gameState.isPaused()) {
            gameState.resume();
            gameTimer.start();
        } else if (gameState.isPlaying()) {
            gameState.pause();
            gameTimer.stop();
        }
    }

    public void handleKeyPress(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP:    case KeyEvent.VK_W: pacman.setNextDirection(Direction.UP);    break;
            case KeyEvent.VK_DOWN:  case KeyEvent.VK_S: pacman.setNextDirection(Direction.DOWN);  break;
            case KeyEvent.VK_LEFT:  case KeyEvent.VK_A: pacman.setNextDirection(Direction.LEFT);  break;
            case KeyEvent.VK_RIGHT: case KeyEvent.VK_D: pacman.setNextDirection(Direction.RIGHT); break;
            case KeyEvent.VK_P:      togglePause(); break;
            case KeyEvent.VK_ENTER:
                if (gameState.isMenu() || gameState.isGameOver()) startGame();
                break;
            case KeyEvent.VK_ESCAPE:
                if (gameState.isPlaying()) togglePause();
                break;
        }
    }

    // ── Getters untuk View ────────────────────────────────────────────────────
    public GameMap    getGameMap()    { return gameMap;    }
    public PacMan     getPacman()     { return pacman;     }
    public List<Ghost> getGhosts()   { return ghosts;     }
    public GameState  getGameState() { return gameState;  }
    public int        getTileSize()  { return TILE_SIZE;  }
}