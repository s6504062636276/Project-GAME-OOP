import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * BossBird: นกบอสของด่าน 5 (ตัวที่ 20) ตัวใหญ่กว่า สีแดงเข้ม ใช้คำ VERYLONG ได้คะแนน x2
 * ตัวอย่าง Polymorphism: Bird ตัวอื่นๆ เรียกเมธอดเดียวกัน แต่บอสตอบสนองต่างออกไป
 */
public class BossBird extends Bird {
    private static final BufferedImage SPRITE = ImageLoader.tint(ImageLoader.loadBird(), new Color(210, 40, 60), 0.38f);
    private static final BufferedImage SHOCK = ImageLoader.tint(SPRITE, new Color(170, 220, 255), 0.85f);

    public BossBird(String word, double laneY, int level, double ufoHitX, double ufoY) {
        super(word, laneY, level, ufoHitX, ufoY);
    }

    @Override protected BufferedImage getSprite() { return SPRITE; }
    @Override protected BufferedImage getShockSprite() { return SHOCK; }
    @Override protected double getWidthPx() { return Config.BIRD_WIDTH * 1.5; }
    @Override public int getScoreMultiplier() { return 2; }

    /** ป้าย BOSS สีแดงเหนือตัวนก */
    @Override
    protected void drawDecoration(Graphics2D g, int w, int h) {
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        String s = "BOSS  x2";
        int tw = g.getFontMetrics().stringWidth(s);
        int tx = (int) getX() - tw / 2, ty = (int) (getY() - h / 2.0) - 2;
        g.setColor(Color.BLACK);
        g.drawString(s, tx + 2, ty + 2);
        g.setColor(new Color(255, 70, 70));
        g.drawString(s, tx, ty);
    }
}
