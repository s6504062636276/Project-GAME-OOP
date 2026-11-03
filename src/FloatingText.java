import java.awt.*;

/** FloatingText: ข้อความที่ลอยขึ้นแล้วจางหาย เช่น "-20" ตอนโดนจิก หรือ "+30" ตอนได้คะแนน */
public class FloatingText implements Drawable {
    private static final double LIFE = 0.9;
    private final String text;
    private final double x;
    private double y, time = 0;
    private final Color color;

    public FloatingText(String text, double x, double y, Color color) {
        this.text = text; this.x = x; this.y = y; this.color = color;
    }

    public boolean isDone() { return time >= LIFE; }

    @Override
    public void update(double dt) {
        time += dt;
        y -= 55 * dt;
    }

    @Override
    public void draw(Graphics2D g) {
        float a = (float) Math.max(0, 1 - time / LIFE);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 28));
        int w = g.getFontMetrics().stringWidth(text);
        g.setColor(new Color(0, 0, 0, (int) (200 * a)));
        g.drawString(text, (int) (x - w / 2.0) + 2, (int) y + 2);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255 * a)));
        g.drawString(text, (int) (x - w / 2.0), (int) y);
    }
}
