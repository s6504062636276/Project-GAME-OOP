import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Background: ฉากหลังภูเขาไฟตอนพระอาทิตย์ตก + สะเก็ดไฟตกลงมา
 * ถ้ามี res/images/background.png จะใช้ภาพจริง ถ้าไม่มีจะวาดฉาก pixel art ให้เอง
 * (วาดที่ 256x144 แล้วขยาย 4 เท่าแบบ nearest-neighbor ให้ดูเป็น pixel art)
 */
public class Background implements Drawable {
    private static class Ember {
        double x, y, vx, vy, len;
    }

    private final BufferedImage scene;
    private final Ember[] embers = new Ember[16];
    private final Random rnd = new Random();

    public Background() {
        BufferedImage real = ImageLoader.loadBackground();
        scene = real != null ? real : buildScene();
        for (int i = 0; i < embers.length; i++) {
            embers[i] = new Ember();
            respawn(embers[i], true);
        }
    }

    private void respawn(Ember e, boolean anywhere) {
        e.x = rnd.nextInt(Config.WIDTH + 300);
        e.y = anywhere ? rnd.nextInt(Config.HEIGHT) : -20 - rnd.nextInt(100);
        e.vx = -(90 + rnd.nextInt(60));
        e.vy = 110 + rnd.nextInt(70);
        e.len = 14 + rnd.nextInt(16);
    }

    @Override
    public void update(double dt) {
        for (Ember e : embers) {
            e.x += e.vx * dt;
            e.y += e.vy * dt;
            if (e.y > Config.HEIGHT || e.x < -40) respawn(e, false);
        }
    }

    @Override
    public void draw(Graphics2D g) {
        g.drawImage(scene, 0, 0, Config.WIDTH, Config.HEIGHT, null);
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        for (Ember e : embers) {
            double n = Math.hypot(e.vx, e.vy);
            int tx = (int) (e.x - e.vx / n * e.len), ty = (int) (e.y - e.vy / n * e.len);
            g.setColor(new Color(255, 120, 40, 140));
            g.drawLine(tx, ty, (int) e.x, (int) e.y);
            g.setColor(new Color(255, 220, 120, 230));
            g.drawLine((int) (e.x - e.vx / n * 5), (int) (e.y - e.vy / n * 5), (int) e.x, (int) e.y);
        }
        g.setStroke(old);
    }

    // ---------- วาดฉาก placeholder แบบ pixel art ----------
    private static Color lerp(Color a, Color b, double t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                         (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                         (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static BufferedImage buildScene() {
        final int W = 256, H = 144, HORIZON = 90;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Random r = new Random(7);
        // ท้องฟ้าเป็นแถบสี
        for (int y = 0; y < HORIZON; y += 3) {
            g.setColor(lerp(new Color(70, 45, 90), new Color(245, 150, 95), Math.pow(y / (double) HORIZON, 1.3)));
            g.fillRect(0, y, W, 3);
        }
        // พระอาทิตย์
        g.setColor(new Color(255, 215, 130));
        g.fillOval(46, HORIZON - 20, 36, 36);
        // ทะเล
        for (int y = HORIZON; y < H; y += 3) {
            g.setColor(lerp(new Color(150, 80, 120), new Color(45, 35, 110), (y - HORIZON) / (double) (H - HORIZON)));
            g.fillRect(0, y, W, 3);
        }
        g.setColor(new Color(255, 200, 120));
        for (int i = 0; i < 6; i++) g.fillRect(52 + r.nextInt(8), HORIZON + 3 + i * 5, 22 - i * 2, 2);
        // เมฆ
        g.setColor(new Color(95, 60, 100));
        for (int i = 0; i < 9; i++) g.fillOval(r.nextInt(W), 8 + r.nextInt(55), 30 + r.nextInt(40), 6 + r.nextInt(7));
        // ควัน
        for (int i = 0; i < 14; i++) {
            g.setColor(lerp(new Color(60, 40, 55), new Color(110, 70, 80), r.nextDouble()));
            g.fillOval(125 + r.nextInt(40) - i * 2, 40 - i * 3 + r.nextInt(8), 24 + r.nextInt(18), 14 + r.nextInt(8));
        }
        // เกาะ + ภูเขาไฟ
        g.setColor(new Color(48, 27, 36));
        g.fillPolygon(new int[]{30, 66, 98, 120, 131, 150, 161, 184, 212, 245, 200, 100},
                      new int[]{122, 110, 100, 72, 46, 46, 72, 100, 110, 124, 130, 130}, 12);
        // ลาวา
        for (int i = 0; i < 6; i++) {
            int x = 138 + (i - 3) * 4, y = 47;
            while (y < 118) {
                g.setColor(new Color(235, 90, 40));
                g.fillRect(x, y, 2, 2);
                if (r.nextInt(5) == 0) {
                    g.setColor(new Color(255, 175, 60));
                    g.fillRect(x, y, 1, 2);
                }
                x += (i - 3) * 1 + r.nextInt(3) - 1;
                y += 2;
            }
        }
        g.setColor(new Color(255, 150, 50));
        g.fillRect(131, 44, 19, 3);
        // ต้นปาล์มริมเกาะ
        g.setColor(new Color(20, 40, 35));
        for (int px : new int[]{40, 52, 220, 232}) {
            g.fillRect(px, 100, 2, 16);
            g.fillOval(px - 6, 96, 14, 5);
        }
        g.dispose();
        return img;
    }
}
