package pacman.model;

/**
 * Manages overall game state: score, lives, level, high score.
 * Encapsulation: all fields private.
 * Single Responsibility Principle (SOLID).
 */
public class GameState {

    public enum State {
        MENU,
        PLAYING,
        PAUSED,
        GAME_OVER,
        LEVEL_COMPLETE,
        DEAD_ANIMATION
    }

    private State currentState;
    private int level;
    private int highScore;
    private int ghostEatenCombo; // consecutive ghosts eaten per power pellet (100, 200, 400, 800)

    private static final int[] GHOST_SCORE_TABLE = {200, 400, 800, 1600};

    public GameState() {
        this.currentState = State.MENU;
        this.level = 1;
        this.highScore = 0;
        this.ghostEatenCombo = 0;
    }

    public void startGame() {
        currentState = State.PLAYING;
        level = 1;
        ghostEatenCombo = 0;
    }

    public void nextLevel() {
        level++;
        currentState = State.PLAYING;
        ghostEatenCombo = 0;
    }

    public void gameOver() {
        currentState = State.GAME_OVER;
    }

    public void pause() {
        if (currentState == State.PLAYING) currentState = State.PAUSED;
    }

    public void resume() {
        if (currentState == State.PAUSED) currentState = State.PLAYING;
    }

    public void levelComplete() {
        currentState = State.LEVEL_COMPLETE;
    }

    public void triggerDeathAnimation() {
        currentState = State.DEAD_ANIMATION;
    }

    public int getGhostEatenScore() {
        int idx = Math.min(ghostEatenCombo, GHOST_SCORE_TABLE.length - 1);
        int score = GHOST_SCORE_TABLE[idx];
        ghostEatenCombo++;
        return score;
    }

    public void resetGhostCombo() {
        ghostEatenCombo = 0;
    }

    public void updateHighScore(int score) {
        if (score > highScore) highScore = score;
    }

    // Getters and Setters
    public State getCurrentState() { return currentState; }
    public void setCurrentState(State state) { this.currentState = state; }
    public int getLevel() { return level; }
    public int getHighScore() { return highScore; }
    public boolean isPlaying() { return currentState == State.PLAYING; }
    public boolean isPaused() { return currentState == State.PAUSED; }
    public boolean isGameOver() { return currentState == State.GAME_OVER; }
    public boolean isMenu() { return currentState == State.MENU; }
    public boolean isLevelComplete() { return currentState == State.LEVEL_COMPLETE; }
    public boolean isDeadAnimation() { return currentState == State.DEAD_ANIMATION; }
}
