import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * ImageLoader: จุดโหลดภาพจุดเดียวของเกม
 * - ถ้ามีไฟล์ใน res/images/ จะใช้ไฟล์จริง
 * - ถ้าไม่มี จะวาด placeholder (รูปทรงสี) แทนอัตโนมัติ เกมไม่ crash
 */
public final class ImageLoader {
    private ImageLoader() {}

    private static BufferedImage read(String name) {
        try (InputStream in = Resources.open("images/" + name)) {
            if (in == null) return null;
            return ImageIO.read(in);
        } catch (Exception e) {
            System.err.println("โหลดภาพไม่สำเร็จ: " + name + " (" + e.getMessage() + ")");
            return null;
        }
    }

    /** UFO: red=true คือเวอร์ชันเอเลี่ยนสีแดง */
    public static BufferedImage loadUfo(boolean red) {
        BufferedImage img = read(red ? "ufo_red.png" : "ufo.png");
        if (img != null) return img;
        BufferedImage p = new BufferedImage(228, 225, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = p.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(red ? new Color(220, 50, 50) : new Color(70, 200, 80));
        g.fillOval(85, 20, 58, 70);
        g.setColor(new Color(170, 220, 255, 150));
        g.fillOval(60, 10, 108, 100);
        g.setColor(new Color(215, 205, 195));
        g.fillOval(5, 85, 218, 70);
        g.setColor(new Color(80, 190, 255));
        g.fillPolygon(new int[]{90, 138, 114}, new int[]{150, 150, 220}, 3);
        g.dispose();
        return p;
    }

    public static BufferedImage loadBird() {
        BufferedImage img = read("bird.png");
        if (img != null) return img;
        BufferedImage p = new BufferedImage(396, 204, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = p.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(130, 90, 170));
        g.fillOval(120, 90, 230, 80);
        g.fillPolygon(new int[]{10, 140, 140}, new int[]{130, 100, 150}, 3);
        g.setColor(new Color(255, 170, 40));
        g.fillPolygon(new int[]{150, 210, 260}, new int[]{100, 5, 100}, 3);
        g.dispose();
        return p;
    }

    /** พื้นหลังจริง (res/images/background.png) คืน null ถ้าไม่มี แล้วให้ Background วาดฉากแทน */
    public static BufferedImage loadBackground() {
        return read("background.png");
    }

    /** สร้างภาพเวอร์ชันย้อมสี: พิกเซลที่ไม่โปร่งใสจะผสมสี color ตาม amount (0..1) */
    public static BufferedImage tint(BufferedImage src, Color color, float amount) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int argb = src.getRGB(x, y);
                int a = argb >>> 24;
                if (a == 0) continue;
                int r = (argb >> 16) & 0xFF, gr = (argb >> 8) & 0xFF, b = argb & 0xFF;
                r = (int) (r + (color.getRed() - r) * amount);
                gr = (int) (gr + (color.getGreen() - gr) * amount);
                b = (int) (b + (color.getBlue() - b) * amount);
                out.setRGB(x, y, (a << 24) | (r << 16) | (gr << 8) | b);
            }
        }
        return out;
    }
}
