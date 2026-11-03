import java.awt.*;
import java.util.Random;

/**
 * Laser: เลเซอร์สีน้ำเงินจาก UFO ไปยังนก เป็นเส้นหยักแบบไฟฟ้า 3 ชั้น (เรืองแสง/สี/แกนขาว)
 * แสดงสั้นๆ แล้วจางหายไป
 */
public class Laser implements Drawable {
    private static final double LIFE = 0.22;
    private static final Random RND = new Random();
    private final int[] px, py;
    private double time = 0;

    public Laser(double x1, double y1, double x2, double y2) {
        int n = 9;
        px = new int[n + 1];
        py = new int[n + 1];
        double dx = x2 - x1, dy = y2 - y1, len = Math.max(1, Math.hypot(dx, dy));
        double nx = -dy / len, ny = dx / len;   // เวกเตอร์ตั้งฉากกับเส้น
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            double off = (i == 0 || i == n) ? 0 : (RND.nextDouble() - 0.5) * 16;
            px[i] = (int) (x1 + dx * t + nx * off);
            py[i] = (int) (y1 + dy * t + ny * off);
        }
    }

    public boolean isDone() { return time >= LIFE; }

    @Override public void update(double dt) { time += dt; }

    private void line(Graphics2D g, float width, Color c) {
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(c);
        g.drawPolyline(px, py, px.length);
    }

    @Override
    public void draw(Graphics2D g) {
        float a = (float) Math.max(0, 1 - time / LIFE);
        Stroke old = g.getStroke();
        line(g, 11f, new Color(60, 120, 255, (int) (80 * a)));
        line(g, 5f, new Color(120, 190, 255, (int) (220 * a)));
        line(g, 2f, new Color(255, 255, 255, (int) (255 * a)));
        g.setStroke(old);
    }
}
