import java.awt.Color;
import java.awt.image.BufferedImage;

/** NormalBird: นกธรรมดา (ลูกของ Bird) กำหนดภาพและขนาด ส่วนพฤติกรรมอื่นสืบทอดจาก Bird */
public class NormalBird extends Bird {
    private static final BufferedImage SPRITE = ImageLoader.loadBird();
    private static final BufferedImage SHOCK = ImageLoader.tint(SPRITE, new Color(170, 220, 255), 0.85f);

    public NormalBird(String word, double laneY, int level, double ufoHitX, double ufoY) {
        super(word, laneY, level, ufoHitX, ufoY);
    }

    @Override protected BufferedImage getSprite() { return SPRITE; }
    @Override protected BufferedImage getShockSprite() { return SHOCK; }
    @Override protected double getWidthPx() { return Config.BIRD_WIDTH; }
}
