import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Bird (abstract class): แม่แบบของนกทุกชนิด (NormalBird, และ BossBird ในเฟสหลัง)
 * - เก็บคำศัพท์, ความคืบหน้าการพิมพ์, ความเร็ว, สถานะ
 * - ลูกคลาสกำหนดแค่หน้าตา/ขนาด (getSprite, getWidthPx) -> Polymorphism
 * เวลาบินถึง UFO = 3 + (ความยาวคำ x k) วินาที โดย k ขึ้นกับด่าน
 */
public abstract class Bird implements Drawable {
    public enum State { FLYING, DYING, GONE }

    private final String word;
    private int typed = 0;
    private double x, y;                 // จุดกึ่งกลางตัวนก
    private final double laneY, ufoY, spawnX, targetX, baseSpeed;
    private double speedUp = 0;          // เร็วขึ้นสะสมจากการพิมพ์ผิด (0..MAX_SPEEDUP)
    private boolean locked = false;
    private State state = State.FLYING;
    private double age = Math.random() * 10;
    private double wrongFlash = 0, shockTimer = 0, deathTime = 0, vy = 0, rotation = 0, alpha = 1;

    protected Bird(String word, double laneY, int level, double ufoHitX, double ufoY) {
        this.word = word;
        this.laneY = laneY;
        this.ufoY = ufoY;
        double w = getWidthPx();   // ลูกคลาสต้องคืนค่าคงที่ได้ตั้งแต่ตอนสร้าง
        this.spawnX = Config.WIDTH + w / 2 + 10;
        this.targetX = ufoHitX + w / 2;
        int idx = Math.max(1, Math.min(level, Config.TOTAL_LEVELS)) - 1;
        double flightTime = Config.BASE_FLIGHT_TIME + word.length() * Config.K_PER_LEVEL[idx];
        this.baseSpeed = (spawnX - targetX) / flightTime * Config.BIRD_SPEED_SCALE;
        this.x = spawnX;
        this.y = laneY;
    }

    // ----- ลูกคลาสต้องกำหนด -----
    protected abstract BufferedImage getSprite();
    protected abstract BufferedImage getShockSprite();
    protected abstract double getWidthPx();

    protected double getHeightPx() {
        return getWidthPx() * getSprite().getHeight() / getSprite().getWidth();
    }

    // ----- การพิมพ์ -----
    public String getWord() { return word; }
    public int getTyped() { return typed; }
    public char getNextChar() { return word.charAt(typed); }
    public boolean isComplete() { return typed >= word.length(); }
    public void typeNext() { typed++; }
    public void resetProgress() { typed = 0; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    /** พิมพ์ผิด: นกตัวนี้เร็วขึ้น +15% (สูงสุด +60%) และตัวอักษรกะพริบแดง */
    public void wrongKey() {
        speedUp = Math.min(Config.MAX_SPEEDUP, speedUp + Config.WRONG_SPEEDUP);
        wrongFlash = 0.25;
    }

    // ----- สถานะ -----
    public State getState() { return state; }
    public boolean isFlying() { return state == State.FLYING; }
    public boolean isRemovable() { return state == State.GONE; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getLaneY() { return laneY; }
    public double getSpeedUp() { return speedUp; }
    public boolean hasReachedUfo() { return state == State.FLYING && x <= targetX; }

    /** นกโดนเลเซอร์: ช็อตแล้วร่วง */
    public void kill() {
        state = State.DYING;
        locked = false;
        shockTimer = 0.3;
        vy = -140;
    }

    /** นกจิก UFO เสร็จแล้วหายไป */
    public void attackDone() {
        state = State.GONE;
        locked = false;
    }

    @Override
    public void update(double dt) {
        age += dt;
        if (wrongFlash > 0) wrongFlash -= dt;
        if (state == State.FLYING) {
            x -= baseSpeed * (1 + speedUp) * dt;
            double p = (spawnX - x) / (spawnX - targetX);
            double t = Math.max(0, Math.min(1, (p - 0.55) / 0.45));
            t = t * t * (3 - 2 * t);                       // เข้าใกล้ระดับความสูงของ UFO ช่วงท้าย
            y = laneY + (ufoY - laneY) * t + Math.sin(age * 3) * 8 * (1 - t);
        } else if (state == State.DYING) {
            deathTime += dt;
            if (shockTimer > 0) shockTimer -= dt;
            else {
                vy += 1000 * dt;
                y += vy * dt;
                x += 30 * dt;
                rotation += 4 * dt;
                alpha = Math.max(0, alpha - dt * 1.4);
            }
            if (alpha <= 0 || y > Config.HEIGHT + 100) state = State.GONE;
        }
    }

    @Override
    public void draw(Graphics2D g) {
        if (state == State.GONE) return;
        int w = (int) getWidthPx(), h = (int) getHeightPx();
        Composite oldC = g.getComposite();
        AffineTransform oldT = g.getTransform();
        if (alpha < 1) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));
        g.rotate(rotation, x, y);
        if (locked && state == State.FLYING) {   // เรืองแสงรอบนกที่ล็อกเป้า
            int pulse = (int) (25 * Math.sin(age * 8));
            g.setColor(new Color(255, 230, 90, 60 + pulse));
            g.fillOval((int) (x - w * 0.6), (int) (y - h * 0.75), (int) (w * 1.2), (int) (h * 1.5));
        }
        boolean shock = state == State.DYING && shockTimer > 0 && ((int) (shockTimer * 40) % 2 == 0);
        g.drawImage(shock ? getShockSprite() : getSprite(), (int) (x - w / 2.0), (int) (y - h / 2.0), w, h, null);
        g.setTransform(oldT);
        g.setComposite(oldC);
        if (state == State.FLYING) {
            drawDecoration(g, w, h);
            drawWord(g, w, h);
        }
    }

    /** ลูกคลาสวาดของตกแต่งเพิ่มได้ (เช่น ป้าย BOSS) ค่าเริ่มต้นไม่ทำอะไร */
    protected void drawDecoration(Graphics2D g, int w, int h) { }

    /** ตัวคูณคะแนนของนกชนิดนี้ (นกธรรมดา x1, บอส x2) */
    public int getScoreMultiplier() { return 1; }

    /** วาดคำบนตัวนก: ตัวที่พิมพ์ถูก = ส้ม, ตัวที่รอพิมพ์ = ขาว, พิมพ์ผิด = ตัวถัดไปกะพริบแดง */
    private void drawWord(Graphics2D g, int w, int h) {
        int len = word.length();
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, len <= 10 ? 20 : len <= 14 ? 18 : len <= 20 ? 16 : 14));
        FontMetrics fm = g.getFontMetrics();
        int cw = fm.charWidth('M');
        int tw = cw * word.length(), th = fm.getHeight();
        int left = (int) (x + w * 0.04) - tw / 2;
        int top = (int) (y + h * 0.16) - th / 2;
        g.setColor(new Color(20, 10, 30, 150));
        g.fillRoundRect(left - 8, top - 1, tw + 16, th + 2, 10, 10);
        if (locked) {
            Stroke old = g.getStroke();
            g.setStroke(new BasicStroke(2f));
            g.setColor(new Color(255, 225, 80));
            g.drawRoundRect(left - 8, top - 1, tw + 16, th + 2, 10, 10);
            g.setStroke(old);
        }
        int base = top + fm.getAscent();
        for (int i = 0; i < word.length(); i++) {
            String s = String.valueOf(word.charAt(i));
            int px = left + i * cw + (cw - fm.stringWidth(s)) / 2;
            g.setColor(Color.BLACK);
            g.drawString(s, px + 1, base + 1);
            if (i < typed) g.setColor(new Color(255, 170, 40));
            else if (i == typed && wrongFlash > 0) g.setColor(new Color(255, 70, 70));
            else g.setColor(Color.WHITE);
            g.drawString(s, px, base);
        }
    }
}
