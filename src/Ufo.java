import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Ufo: ยานของผู้เล่น เก็บ HP, ลอยขึ้นลงเบาๆ, กะพริบแดงเมื่อโดนจิก/HP ต่ำ
 * เอเลี่ยนในยานเปลี่ยนเป็นสีแดงเมื่อ HP <= Config.ALIEN_RED_HP
 */
public class Ufo implements Drawable {
    private static final BufferedImage GREEN = ImageLoader.loadUfo(false);
    private static final BufferedImage RED = ImageLoader.loadUfo(true);
    private static final BufferedImage HIT_GREEN = ImageLoader.tint(GREEN, new Color(255, 60, 60), 0.55f);
    private static final BufferedImage HIT_RED = ImageLoader.tint(RED, new Color(255, 60, 60), 0.55f);

    private final double x;
    private final double baseY;
    private int hp = Config.UFO_MAX_HP;
    private double flashTimer = 0;
    private double age = 0;

    public Ufo(double x, double baseY) {
        this.x = x;
        this.baseY = baseY;
    }

    public void takeDamage(int amount) {
        hp = Math.max(0, hp - amount);
        flashTimer = 0.45;
    }

    /** ฟื้น HP (ไม่เกินค่าสูงสุด) คืนจำนวนที่ฟื้นจริง */
    public int heal(int amount) {
        int before = hp;
        hp = Math.min(Config.UFO_MAX_HP, hp + amount);
        return hp - before;
    }

    public boolean isDead() { return hp <= 0; }
    public int getHp() { return hp; }
    public double getBaseY() { return baseY; }

    /** ขอบขวาของ UFO ที่นกต้องบินมาชน */
    public double getHitX() { return x + Config.UFO_HIT_OFFSET; }

    private double bobY() { return baseY + Math.sin(age * 2.2) * 7; }
    private int widthPx() { return Config.UFO_WIDTH; }
    private int heightPx() { return widthPx() * GREEN.getHeight() / GREEN.getWidth(); }

    /** จุดปลายปืนเลเซอร์ */
    public double getMuzzleX() { return x + widthPx() * 0.30; }
    public double getMuzzleY() { return bobY() + heightPx() * 0.12; }

    @Override
    public void update(double dt) {
        age += dt;
        if (flashTimer > 0) flashTimer -= dt;
    }

    @Override
    public void draw(Graphics2D g) {
        boolean red = hp <= Config.ALIEN_RED_HP;
        boolean flash = flashTimer > 0 && ((int) (flashTimer * 20) % 2 == 0);
        boolean lowBlink = hp > 0 && hp <= Config.LOW_HP && ((int) (age * 6) % 2 == 0);
        BufferedImage img = (flash || lowBlink) ? (red ? HIT_RED : HIT_GREEN) : (red ? RED : GREEN);
        int w = widthPx(), h = heightPx();
        // เงาใต้ยาน
        g.setColor(new Color(0, 0, 0, 40));
        g.fillOval((int) (x - w * 0.35), (int) (baseY + h * 0.62), (int) (w * 0.7), 10);
        g.drawImage(img, (int) (x - w / 2.0), (int) (bobY() - h / 2.0), w, h, null);
    }
}
