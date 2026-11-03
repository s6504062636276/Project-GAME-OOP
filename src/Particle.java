import java.awt.*;

/** Particle: จุดสี่เหลี่ยมเล็กๆ สำหรับเอฟเฟกต์ (ประกายไฟ, ระเบิด, เศษไฟ) ลอยออกแล้วจางหาย */
public class Particle implements Drawable {
    private double x, y, vx, vy;
    private final double maxLife, gravity;
    private double life;
    private final int size;
    private final Color color;

    public Particle(double x, double y, double vx, double vy, double life, double gravity, int size, Color color) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.life = life; this.maxLife = life;
        this.gravity = gravity; this.size = size; this.color = color;
    }

    public boolean isDone() { return life <= 0; }

    @Override
    public void update(double dt) {
        life -= dt;
        vy += gravity * dt;
        x += vx * dt;
        y += vy * dt;
    }

    @Override
    public void draw(Graphics2D g) {
        float a = (float) Math.max(0, Math.min(1, life / maxLife));
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255 * a)));
        g.fillRect((int) x, (int) y, size, size);
    }
}
