import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.font.TextLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class GAME extends JPanel implements ActionListener {

    private record GamePoint(int x, int y) {}

    private enum Direction {
        UP, DOWN, RIGHT, LEFT
    }

    final int width;
    final int height;
    final int cell_size;
    final Random rand = new Random();
    static final int FRAME_RATE = 20;
    boolean game_start = false;
    boolean game_over = false;
    int highscore;
    GamePoint food;
    Direction dir = Direction.RIGHT;
    Direction newDir = Direction.RIGHT;
    final List<GamePoint> snake = new ArrayList<>();
    int pID;

    static final String DATABASE_URL = "jdbc:postgresql://localhost:5432/snake";
    static final String DATABASE_USER = "postgres";
    static final String DATABASE_PASSWORD = "aici vine parola sql";

    public GAME(final int width, final int height) {

        this.width = width;
        this.height = height;
        this.cell_size = width / (FRAME_RATE * 2);
        setPreferredSize(new Dimension(width, height));
        setBackground(Color.BLACK);

        System.out.println("ENTERING USERNAME");
        String username = JOptionPane.showInputDialog(null, "Enter username:");
        if (username != null && !username.trim().isEmpty()) {
            pID = databasePlayer(username.trim());
            if (pID == -1) {
                System.out.println("DATABASE ERROR");
            }
        }
    }

    public void startGame() {

        reset();
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        requestFocusInWindow();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                GAME.this.keyPressed(e.getKeyCode());
            }
        });
        new Timer(1000 / FRAME_RATE, this).start();
        // > DEPLAY FOR A SLOWER SNAKE
    }

    private void keyPressed(final int keyCode) {

        if (!game_start) {
            if (keyCode == KeyEvent.VK_SPACE) {
                game_start = true;
                System.out.println("GAME STARTED");
            }
        }
        else if (!game_over) {
            System.out.println("PLAYING GAME");
            switch (keyCode) {
                case KeyEvent.VK_DOWN:
                    if (dir != Direction.UP) {
                        newDir = Direction.DOWN;
                    }
                    break;
                case KeyEvent.VK_UP:
                    if (dir != Direction.DOWN) {
                        newDir = Direction.UP;
                    }
                    break;
                case KeyEvent.VK_LEFT:
                    if (dir != Direction.RIGHT) {
                        newDir = Direction.LEFT;
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if (dir != Direction.LEFT) {
                        newDir = Direction.RIGHT;
                    }
                    break;
            }
        }
        else if (keyCode == KeyEvent.VK_SPACE) {
            game_start = false;
            game_over = false;
            reset();
            System.out.println("GAME RESTARTED");
        }
    }

    private void reset() {

        snake.clear();
        snake.add(new GamePoint(width / 2, height / 2));
        generateFood();

    }

    private void generateFood() {

        do {
            food = new GamePoint(rand.nextInt(width / cell_size) * cell_size,
                    rand.nextInt(height / cell_size) * cell_size);
        } while (snake.contains(food));

    }

    @Override
    protected void paintComponent(final Graphics graphics) {

        super.paintComponent(graphics);

        if (!game_start) {
            List<String> scores = selectScores();
            StringBuilder message = new StringBuilder("PRESS SPACE TO START\n\nTOP SCORES:\n");
            for (String i : scores) {
                message.append(i).append("\n");
            }
            messPrint(graphics, message.toString());
        }
        else {
            graphics.setColor(Color.RED);
            graphics.fillRect(food.x, food.y, cell_size, cell_size);

            Color snakeColor = Color.GREEN;
            for (final var point : snake) {
                graphics.setColor(snakeColor);
                graphics.fillRect(point.x, point.y, cell_size, cell_size);
            }

            if (game_over) {
                final int currScore = snake.size();
                if (currScore > highscore) {
                    highscore = currScore;
                }
                updateScore(currScore);
                messPrint(graphics, "SCORE: " + currScore + "\nBEST: " + highscore + "\nPRESS SPACE TO RESTART");
                System.out.println("GAME ENDED PAGE");
            }
        }
    }

    private void messPrint(final Graphics graphics, final String message) {

        graphics.setFont(graphics.getFont().deriveFont(30F));
        final var graphics2D = (Graphics2D) graphics;
        int currHeight = height / 5;

        for (final var line : message.split("\n")) {
            if (line.trim().isEmpty()) {
                continue;
            }
            if (line.equals("PRESS SPACE TO START")) {
                graphics.setColor(Color.GREEN);
                System.out.println("START PAGE");
            }
            else {
                graphics.setColor(Color.WHITE);
            }

            final var layout = new TextLayout(line, graphics.getFont(), graphics2D.getFontRenderContext());
            final var bounds = layout.getBounds();
            final var targetWidth = (float) (width - bounds.getWidth()) / 2;
            layout.draw(graphics2D, targetWidth, currHeight);
            currHeight += graphics.getFontMetrics().getHeight();
        }
    }

    private void movement() {

        dir = newDir;

        final GamePoint head = snake.getFirst();
        final GamePoint newHead = switch (dir)
        {
            case UP -> new GamePoint(head.x, head.y - cell_size);
            case DOWN -> new GamePoint(head.x, head.y + cell_size);
            case LEFT -> new GamePoint(head.x - cell_size, head.y);
            case RIGHT -> new GamePoint(head.x + cell_size, head.y);
        };

        snake.addFirst(newHead);

        if (newHead.equals(food)) {
            generateFood();
        }
        else if (isCollision()) {
            game_over = true;
            snake.removeLast();
        }
        else {
            snake.removeLast();
        }
    }

    private boolean isCollision() {

        final GamePoint head = snake.getFirst();
        final var invalidWidth = (head.x < 0) || (head.x >= width);
        final var invalidHeight = (head.y < 0) || (head.y >= height);

        if (invalidWidth || invalidHeight) {
            return true;
        }

        return snake.size() != new HashSet<>(snake).size();

    }

    @Override
    public void actionPerformed(final ActionEvent e) {

        if (game_start && !game_over) {
            movement();
        }
        repaint();
    }

    private int databasePlayer(String playerName) {

        String sqlCode = "INSERT INTO player_scores (player_name) VALUES (?) RETURNING player_id;";
        try (Connection connect = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
             PreparedStatement mes = connect.prepareStatement(sqlCode)) {
            mes.setString(1, playerName);
            ResultSet set = mes.executeQuery();
            if (set.next()) {
                return set.getInt("player_id");
            }
        } catch (SQLException e) {
            System.out.println("DATABASE ERROR: " + e.getMessage());
        }
        return -1;
    }

    private void updateScore(int currentScore) {

        String sqlCode = "UPDATE player_scores " + "SET score = ?, high_score = GREATEST(high_score, ?) " + "WHERE player_id = ?;";
        try (Connection connect = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
             PreparedStatement mes = connect.prepareStatement(sqlCode)) {
            mes.setInt(1, currentScore);
            mes.setInt(2, currentScore);
            mes.setInt(3, pID);
            mes.executeUpdate();
        } catch (SQLException e) {
            System.out.println("DATABASE ERROR: " + e.getMessage());
        }
    }

    private List<String> selectScores() {

        List<String> scores = new ArrayList<>();
        String sqlCode = "SELECT player_name, high_score FROM player_scores WHERE high_score > 0 ORDER BY high_score DESC LIMIT 10;";
        try (Connection connect = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
             PreparedStatement mes = connect.prepareStatement(sqlCode);
             ResultSet set = mes.executeQuery()) {
            while (set.next()) {
                String playerName = set.getString("player_name");
                int highscore = set.getInt("high_score");
                scores.add(playerName + " ~ " + highscore);
            }
        } catch (SQLException e) {
            System.out.println("DATABASE ERROR: " + e.getMessage());
        }
        return scores;
    }
}
