package pacman.controller;

import pacman.model.*;
import pacman.model.ghost.*;
import pacman.util.Direction;
import pacman.util.ScoreManager;
import pacman.view.GamePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Main game controller. Mengkoordinasikan model dan view.
 * MVC: Controller layer.
 * Single Responsibility & Open/Closed principles (SOLID).
 */
public class GameController {

    private static final int TILE_SIZE            = 20;
    private static final int FPS                  = 60;
    private static final int TIMER_DELAY          = 1000 / FPS;
    private static final int DEATH_ANIM_DURATION  = 90;   // frame
    private static final int LEVEL_COMPLETE_PAUSE = 120;  // frame

    // Posisi awal Pac-Man
    private static final int PACMAN_START_X = 13 * TILE_SIZE;
    private static final int PACMAN_START_Y = 23 * TILE_SIZE;

    // Posisi spawn masing-masing ghost (di dalam ghost house)
    private static final int BLINKY_SPAWN_X = 13 * TILE_SIZE;
    private static final int BLINKY_SPAWN_Y = 13 * TILE_SIZE;
    private static final int PINKY_SPAWN_X  = 14 * TILE_SIZE;
    private static final int PINKY_SPAWN_Y  = 13 * TILE_SIZE;
    private static final int INKY_SPAWN_X   = 12 * TILE_SIZE;
    private static final int INKY_SPAWN_Y   = 13 * TILE_SIZE;
    private static final int CLYDE_SPAWN_X  = 15 * TILE_SIZE;
    private static final int CLYDE_SPAWN_Y  = 13 * TILE_SIZE;

    private GameMap   gameMap;
    private PacMan    pacman;
    private List<Ghost> ghosts;
    private Blinky    blinky;
    private GameState gameState;
    private GamePanel gamePanel;

    private Timer gameTimer;
    private int   stateTimer;
    private int   globalTimer;  // untuk pola SCATTER/CHASE

    // Popup skor saat ghost dimakan
    private final List<ScorePopup> scorePopups = new ArrayList<>();

    public GameController(GamePanel panel) {
        this.gamePanel = panel;
        initGame();
        setupTimer();
    }

    // ── Inner class: popup skor saat ghost dimakan ────────────────────────────
    public static class ScorePopup {
        public final int x, y, score;
        public int timer;
        public ScorePopup(int x, int y, int score) {
            this.x = x; this.y = y; this.score = score;
            this.timer = 60; // tampil selama 1 detik
        }
    }

    // ── Init ─────────────────────────────────────────────────────────────────
    private void initGame() {
        gameMap      = new GameMap(TILE_SIZE);
        gameState    = new GameState();
        globalTimer  = 0;
        scorePopups.clear();

        gameState.setHighScore(ScoreManager.loadHighScore());

        pacman = new PacMan(PACMAN_START_X, PACMAN_START_Y, TILE_SIZE, gameMap);

        ghosts = new ArrayList<>();
        blinky = new Blinky(BLINKY_SPAWN_X, BLINKY_SPAWN_Y, TILE_SIZE, gameMap);
        Pinky pinky = new Pinky(PINKY_SPAWN_X, PINKY_SPAWN_Y, TILE_SIZE, gameMap);
        Inky  inky  = new Inky (INKY_SPAWN_X,  INKY_SPAWN_Y,  TILE_SIZE, gameMap, blinky);
        Clyde clyde = new Clyde(CLYDE_SPAWN_X, CLYDE_SPAWN_Y, TILE_SIZE, gameMap);

        ghosts.add(blinky);
        ghosts.add(pinky);
        ghosts.add(inky);
        ghosts.add(clyde);

        // FIX: berikan referensi PacMan ke tiap ghost agar AI CHASE berfungsi
        for (Ghost g : ghosts) {
            g.setTrackedPacman(pacman);
        }
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

        // Update popup skor
        Iterator<ScorePopup> it = scorePopups.iterator();
        while (it.hasNext()) {
            ScorePopup p = it.next();
            p.timer--;
            if (p.timer <= 0) it.remove();
        }
    }

    private void updatePlaying() {
        globalTimer++;
        pacman.move();

        for (Ghost g : ghosts) {
            g.updateAI(pacman, globalTimer);
            g.move();
        }

        checkCollisions();

        if (gameMap.allPelletsEaten()) {
            gameState.levelComplete();
            stateTimer = LEVEL_COMPLETE_PAUSE;
        }
    }

    private void checkCollisions() {
        // Power Pellet baru dimakan → frighten semua ghost
        // setFrightened() di Ghost.java sudah menjaga ghost EATEN/IN_HOUSE/EXITING
        if (pacman.hasPowerPelletJustEaten()) {
            gameState.resetGhostCombo();
            for (Ghost g : ghosts) {
                g.setFrightened();
            }
        }

        for (Ghost ghost : ghosts) {
            if (!pacman.collidesWith(ghost)) continue;

            if (ghost.isFrightened()) {
                // Pac-Man memakan ghost → ghost masuk EATEN → kembali ke spawn
                int points = gameState.getGhostEatenScore();
                pacman.addScore(points);
                scorePopups.add(new ScorePopup(ghost.getX(), ghost.getY(), points));
                ghost.setEaten();

            } else if (!ghost.isEaten() && pacman.isAlive()) {
                // Ghost normal (SCATTER/CHASE) menyentuh Pac-Man → hilang 1 nyawa
                pacman.die();
                gameState.triggerDeathAnimation();
                stateTimer = DEATH_ANIM_DURATION;
                return;
            }
        }
    }

    private void updateDeathAnimation() {
        stateTimer--;
        if (stateTimer <= 0) {
            if (pacman.getLives() <= 0) {
                gameState.updateHighScore(pacman.getScore());
                ScoreManager.saveHighScore(gameState.getHighScore());
                gameState.gameOver();
            } else {
                respawnAfterDeath();
            }
        }
    }

    private void respawnAfterDeath() {
        pacman.respawn(PACMAN_START_X, PACMAN_START_Y);
        // Tiap ghost kembali ke spawn point-nya sendiri dengan delay bertahap
        blinky.respawn(BLINKY_SPAWN_X, BLINKY_SPAWN_Y, 0);
        ((Pinky) ghosts.get(1)).respawn(PINKY_SPAWN_X, PINKY_SPAWN_Y, 30);
        ((Inky)  ghosts.get(2)).respawn(INKY_SPAWN_X,  INKY_SPAWN_Y,  60);
        ((Clyde) ghosts.get(3)).respawn(CLYDE_SPAWN_X, CLYDE_SPAWN_Y, 90);
        globalTimer = 0;
        scorePopups.clear();
        gameState.setCurrentState(GameState.State.PLAYING);
    }

    private void updateLevelComplete() {
        stateTimer--;
        if (stateTimer <= 0) nextLevel();
    }

    private void nextLevel() {
        gameMap.reset();
        pacman.respawn(PACMAN_START_X, PACMAN_START_Y);
        blinky.respawn(BLINKY_SPAWN_X, BLINKY_SPAWN_Y, 0);
        ((Pinky) ghosts.get(1)).respawn(PINKY_SPAWN_X, PINKY_SPAWN_Y, 30);
        ((Inky)  ghosts.get(2)).respawn(INKY_SPAWN_X,  INKY_SPAWN_Y,  60);
        ((Clyde) ghosts.get(3)).respawn(CLYDE_SPAWN_X, CLYDE_SPAWN_Y, 90);
        globalTimer = 0;
        scorePopups.clear();
        gameState.nextLevel();
    }

    // ── Render popup skor (dipanggil GamePanel) ───────────────────────────────
    public void renderScorePopups(Graphics2D g2d) {
        g2d.setFont(new Font("Courier New", Font.BOLD, 12));
        for (ScorePopup p : scorePopups) {
            float alpha = Math.min(1f, p.timer / 30f);
            g2d.setColor(new Color(1f, 1f, 0f, alpha));
            g2d.drawString(String.valueOf(p.score), p.x, p.y);
        }
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
    public GameMap      getGameMap()    { return gameMap;    }
    public PacMan       getPacman()     { return pacman;     }
    public List<Ghost>  getGhosts()     { return ghosts;     }
    public GameState    getGameState()  { return gameState;  }
    public int          getTileSize()   { return TILE_SIZE;  }
}