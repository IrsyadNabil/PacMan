package pacman.util;

import java.io.*;
import java.nio.file.*;

/**
 * Manages high score persistence to file.
 * Single Responsibility Principle (SOLID): only handles score I/O.
 */
public class ScoreManager {

    private static final String SCORE_FILE = "highscore.dat";

    public static int loadHighScore() {
        try {
            Path path = Paths.get(SCORE_FILE);
            if (Files.exists(path)) {
                String content = new String(Files.readAllBytes(path)).trim();
                return Integer.parseInt(content);
            }
        } catch (Exception e) {
            System.err.println("Could not load high score: " + e.getMessage());
        }
        return 0;
    }

    public static void saveHighScore(int score) {
        try {
            Files.write(Paths.get(SCORE_FILE), String.valueOf(score).getBytes());
        } catch (IOException e) {
            System.err.println("Could not save high score: " + e.getMessage());
        }
    }
}
