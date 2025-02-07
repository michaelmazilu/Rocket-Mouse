/*
 * RocketMouse ;)
 * 
 * Author: Michael Alexander Mazilu
 * Date: Wed. June 12th, 2024
 * 
 * Description:
 * This applet-based game involves a mouse character (RocketMouse) navigating through 
 * a series of obstacles. The game features background music, power-ups, a scoring system, 
 * and high score tracking. The player controls the mouse using the spacebar to jump and 
 * can pause the game with the 'P' key.
 * 
 * Key Features:
 * - Smooth animation using double buffering
 * - Multiple power-ups that affect gameplay
 * - Collision detection for obstacles and power-ups
 * - Background music and sound effects
 * - High score tracking and saving to a file
 * 
 */

// Import necessary classes for applet, graphics, events, and file handling
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class RocketMouse extends Applet implements KeyListener, Runnable {

    // All variable defintions inlcluding images, sounds, fonts, etc.
    Graphics bufferGraphics;
    Image offscreen;
    Image back1, back2, back3;
    int screenH, screenW, back1x, back2x, back3x;
    Thread animator; 
    Image ratrun, currentRatImage, ratfly, ratfall, ratdead, ratrun2;
    Image ratrunfire, ratflyfire, ratfallfire;
    Image ratrunforce, ratflyforce, ratfallforce;
    Image power;
    int ratX, ratY;
    boolean spacePressed;
    int score;
    int highScore; 
    Font large;
    Image[] obstacles;
    int numObstacles = 4;
    int[] obstacleX, obstacleY, obstacleSpeed;
    int[] obstacleWidth, obstacleHeight;

    Image pauseImage;
    boolean isPaused;

    boolean stopGame;
    boolean hasForcefield;
    int powerUpEffect;
    int powerUpDuration;
    int powerX, powerY;
    int nextPowerUpScore;
    boolean powerUpActive;

    AudioClip backgroundMusic, backgroundMusic2;

    @Override
    public void init() {
        addKeyListener(this); // To listen for events

        // Setting up screen and images
        screenH = 1024;
        screenW = 1280;

        large = new Font("Monospaced", Font.BOLD, 30);

        setSize(screenW, screenH);

        offscreen = createImage(screenW, screenH);
        bufferGraphics = offscreen.getGraphics(); // So objects don't flicker when moving

        setBackground(Color.blue);

        back1 = this.getImage(getDocumentBase(), "bg_spaceship_1.png");
        back2 = this.getImage(getDocumentBase(), "bg_spaceship_2.png");
        back3 = this.getImage(getDocumentBase(), "bg_spaceship_3.png");

        back1x = 0;
        back2x = -screenW;
        back3x = -2 * screenW;

        power = this.getImage(getDocumentBase(), "powerup3.png");

        ratrunfire = this.getImage(getDocumentBase(), "large_runningfire.png");
        ratflyfire = this.getImage(getDocumentBase(), "large_flyingfire.png");
        ratfallfire = this.getImage(getDocumentBase(), "rocketmouse_fallfire.png");

        ratrunforce = this.getImage(getDocumentBase(), "rocketmouse_runforce.png");
        ratflyforce = this.getImage(getDocumentBase(), "large_flyingforce.png");
        ratfallforce = this.getImage(getDocumentBase(), "rocketmouse_fallforce.png");

        backgroundMusic = getAudioClip(getDocumentBase(), "backmusic111.wav");
        backgroundMusic2 = getAudioClip(getDocumentBase(), "backmusic2.wav");

        ratrun = this.getImage(getDocumentBase(), "large_running.png");
        ratrun2 = this.getImage(getDocumentBase(), "rocketmouse_run02.png");
        ratfly = this.getImage(getDocumentBase(), "large_flying.png");
        ratfall = this.getImage(getDocumentBase(), "rocketmouse_fall01.png");
        ratdead = this.getImage(getDocumentBase(), "rocketmouse_dead02@2x.png");
        currentRatImage = ratrun;
        ratX = 200;
        ratY = 650;

        pauseImage = this.getImage(getDocumentBase(), "pause.png");
        isPaused = false;

        obstacles = new Image[]{
            this.getImage(getDocumentBase(), "obstacle1.png"),
            this.getImage(getDocumentBase(), "obstacle2.png"),
            this.getImage(getDocumentBase(), "obstacle3.png"),
            this.getImage(getDocumentBase(), "obstacle4.png")
        };

        obstacleX = new int[numObstacles];
        obstacleY = new int[numObstacles];
        obstacleSpeed = new int[numObstacles];
        obstacleWidth = new int[numObstacles];
        obstacleHeight = new int[numObstacles];

        score = 0;

        // Initialize obstacles with random positions and set their speed
        for (int i = 0; i < numObstacles; i++) {
            obstacleX[i] = screenW + (int) (Math.random() * screenW);

            do {
                obstacleY[i] = (int) (Math.random() * screenH);
            } while (obstacleY[i] > 650);

            obstacleSpeed[i] = 5;
        }

        // Set obstacle dimensions
        obstacleWidth[0] = 160;
        obstacleHeight[0] = 160;
        obstacleWidth[1] = 90;
        obstacleHeight[1] = 180;
        obstacleWidth[2] = 140;
        obstacleHeight[2] = 140;
        obstacleWidth[3] = 300;
        obstacleHeight[3] = 75;

        nextPowerUpScore = 4000; // Initial threshold for the first power-up
        powerUpActive = false;

        // Load high score from file
        try {
            BufferedReader reader = new BufferedReader(new FileReader("highscore.txt"));
            highScore = Integer.parseInt(reader.readLine());
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            highScore = 0;
        }
    }

    @Override
    public void start() {
        animator = new Thread(this);
        animator.start();
        if (backgroundMusic != null) {
            backgroundMusic.loop(); // Loop the background music
        }
    }

    @Override
    public void stop() {
        animator = null;
        if (backgroundMusic != null) {
            backgroundMusic.stop(); // Stop the background music
        }
    }

    // Main game loop
    public synchronized void run() {
        spacePressed = false;
        stopGame = false;
        hasForcefield = false;
        powerUpEffect = 0;
        powerUpDuration = 0;

        // Game loop runs until stopGame is true
        while (!stopGame) {
            if (!isPaused) {
                // Rat falls if space is not pressed
                if (!spacePressed && ratY < 650) {
                    ratY += 5;
                    updateRatImage();
                }

                if (ratY == 650) {
                    updateRatImage();
                }

                score += 2; // Increment score

                // Generate power-up if score reaches threshold
                if (!powerUpActive && score >= nextPowerUpScore) {
                    generatePowerUp();
                    nextPowerUpScore += 4000; // Set the threshold for the next power-up
                }

                // Move backgrounds for parallax effect
                back1x -= 5;
                back2x -= 5;
                back3x -= 5;
                if (back1x <= -screenW) {
                    back1x = back3x + screenW;
                }
                if (back2x <= -screenW) {
                    back2x = back1x + screenW;
                }
                if (back3x <= -screenW) {
                    back3x = back2x + screenW;
                }

                // Move obstacles and check for collision with rat
                for (int i = 0; i < numObstacles; i++) {
                    obstacleX[i] -= obstacleSpeed[i];
                    if (obstacleX[i] + obstacleWidth[i] < 0) {
                        obstacleX[i] = screenW + (int) (Math.random() * screenW);
                        obstacleY[i] = (int) (Math.random() * screenH);
                    }
                    // Collision detection with forcefield logic --uses rats position and hitbox, and obstacles postion and hitbox
                    if (collision(ratX, ratY, 110, 145, obstacleX[i], obstacleY[i], obstacleWidth[i], obstacleHeight[i])) {
                        if (hasForcefield) {
                            hasForcefield = false;
                            obstacleX[i] = -obstacleWidth[i]; // Move obstacle off-screen
                        } else {
                            stopGame = true;
                            currentRatImage = ratdead;
                        }
                    }
                }

                // Move power-up and check for collision with rat
                if (powerUpActive) {
                    powerX -= 5;
                    if (powerX + 100 < 0) {
                        powerUpActive = false;
                    }
                    if (collision(ratX, ratY, 110, 145, powerX, powerY, 100, 100)) {
                        applyPowerUpEffect();
                        powerUpActive = false; // Power-up is collected and deactivated
                    }
                }

                // Decrease power-up duration and deactivate effect if duration is over
                if (powerUpDuration > 0) {
                    powerUpDuration--;
                    if (powerUpDuration == 0) {
                        powerUpEffect = 0;
                        updateRatImage();
                    }
                }
            }

            repaint(); // Repaint the applet
            try {
                Thread.sleep(10); // Delay for smooth animation
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Save the high score to file if it's a new high score
        if (score > highScore) {
            highScore = score;
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter("highscore.txt"));
                writer.write(String.valueOf(highScore));
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Handle key press events
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == 32 && ratY > 0 && !isPaused) { // Space key for jumping
            spacePressed = true;
            if (powerUpEffect == 3) {
                ratY -= 80; // Faster ascent with fire power-up
            } else {
                ratY -= 40;
            }
            updateRatImage();
            e.consume();
        }

        if (key == 80) { // 'P' key for pause/resume
            isPaused = !isPaused;
        }
    }

    // Handle key release events
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == 32) { // Space key released
            spacePressed = false;
        }
    }

    public void keyTyped(KeyEvent e) {
    }

    // Paint the game screen
    public void paint(Graphics g) {
        bufferGraphics.clearRect(0, 0, screenW, screenH); // Clear the offscreen buffer
        bufferGraphics.drawImage(back1, back1x, 0, screenW, screenH, this);
        bufferGraphics.drawImage(back2, back2x, 0, screenW, screenH, this);
        bufferGraphics.drawImage(back3, back3x, 0, screenW, screenH, this);

        // Draw obstacles
        for (int i = 0; i < numObstacles; i++) {
            bufferGraphics.drawImage(obstacles[i], obstacleX[i], obstacleY[i], obstacleWidth[i], obstacleHeight[i], this);
        }

        // Draw power-up if active
        if (powerUpActive) {
            bufferGraphics.drawImage(power, powerX, powerY, 100, 100, this);
        }

        // Draw score and high score
        bufferGraphics.setFont(large);
        bufferGraphics.setColor(Color.red);
        bufferGraphics.drawString("Score: " + score, 950, 75);
        bufferGraphics.drawString("High Score: " + highScore, 950, 110); // Display high score

        // Draw rat with different dimensions if power-up is active
        if (powerUpEffect == 1) {
            bufferGraphics.drawImage(currentRatImage, ratX, ratY, 55, 72, this); // Smaller rat
        } else {
            bufferGraphics.drawImage(currentRatImage, ratX, ratY, 110, 145, this);
        }

        // Draw pause image if game is paused
        if (isPaused) {
            bufferGraphics.drawImage(pauseImage, (screenW - pauseImage.getWidth(this)) / 2, (screenH - pauseImage.getHeight(this)) / 2, this);
        }

        g.drawImage(offscreen, 0, 0, this); // Draw the offscreen image to the screen

        if (stopGame) {
            bufferGraphics.setColor(Color.black);
            bufferGraphics.drawString("Game Over", 550, 400); // Display game over message
        }
    }

    public void update(Graphics g) {
        paint(g); // Update the screen
    }

    // Check for collision between two rectangles
    public boolean collision(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
        return (x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2);
    }

    // Generate a new power-up
    private void generatePowerUp() {
        powerX = screenW + (int) (Math.random() * screenW);
        powerY = (int) (Math.random() * screenH);
        while (powerY > 650) {
            powerY = (int) (Math.random() * screenH);
        }
        powerUpActive = true;
    }

    // Apply the effect of the collected power-up
    private void applyPowerUpEffect() {
        int effect = (int) (Math.random() * 3) + 1;
        powerUpEffect = effect;
        switch (effect) {
            case 1: // Smaller rat 
                powerUpDuration = 900; // Duration in game ticks
                break;
            case 2: // Forcefield (Bubble)
                hasForcefield = true;
                powerUpDuration = 900;
                break;
            case 3: // Fast ascent (fire)
                powerUpDuration = 900;
                break;
        }
        updateRatImage();
    }

    // Update the image of the rat based on its state
    private void updateRatImage() {
        if (powerUpEffect == 3) {
            if (spacePressed) {
                currentRatImage = ratflyfire;
            } else if (ratY < 650) {
                currentRatImage = ratfallfire;
            } else {
                currentRatImage = ratrunfire;
            }
        } else if (hasForcefield) {
            if (spacePressed) {
                currentRatImage = ratflyforce;
            } else if (ratY < 650) {
                currentRatImage = ratfallforce;
            } else {
                currentRatImage = ratrunforce;
            }
        } else {
            if (spacePressed) {
                currentRatImage = ratfly;
            } else if (ratY < 650) {
                currentRatImage = ratfall;
            } else {
                currentRatImage = ratrun;
            }
        }
    }
}
