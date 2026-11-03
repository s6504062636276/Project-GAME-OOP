import java.awt.Graphics2D;

/** Drawable: interface ของทุกสิ่งที่อัปเดตและวาดบนจอได้ (UFO, นก, เลเซอร์, ข้อความลอย ฯลฯ) */
public interface Drawable {
    /** อัปเดตสถานะ dt = เวลาที่ผ่านไป (วินาที) */
    void update(double dt);

    /** วาดตัวเองลงบนจอ */
    void draw(Graphics2D g);
}
