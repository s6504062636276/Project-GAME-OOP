import javax.swing.JFrame;

/** GameFrame: หน้าต่างหลักของเกม ขนาดคงที่ 1024x576 */
public class GameFrame extends JFrame {
    public GameFrame() {
        super("Type-Skysaurus");
        GamePanel panel = new GamePanel();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(panel);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        panel.requestFocusInWindow();
        panel.start();
    }
}
