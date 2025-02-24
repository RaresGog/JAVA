import javax.swing.JFrame;

public class Main {

    private static final int DIMENSIUNE = 600;

    public static void main(String[] args){

        final JFrame frame = new JFrame("SNAKE");
        frame.setSize(DIMENSIUNE, DIMENSIUNE);
        GAME game = new GAME(DIMENSIUNE,DIMENSIUNE);
        frame.add(game);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
        frame.pack();
        game.startGame();

    }
}