import java.util.Random;

/**
 * Spawner: ควบคุมจังหวะการเกิดของนก
 * - ถ้านกบนจอน้อยกว่าขั้นต่ำ จะเกิดเร็ว
 * - ถ้าถึงจำนวนสูงสุดแล้วจะไม่เกินเพิ่ม
 */
public class Spawner {
    private final Random rnd = new Random();
    private double timer = 0.8;

    public void reset() { timer = 0.8; }

    /** @return true เมื่อถึงเวลาที่ควรเกินนกตัวใหม่ */
    public boolean update(double dt, int flyingCount, int minBirds, int maxBirds) {
        timer -= dt;
        if (timer > 0) return false;
        if (flyingCount >= maxBirds) {
            timer = 0.3;
            return false;
        }
        timer = flyingCount < minBirds ? 0.5 + rnd.nextDouble() * 0.6
                                       : 1.8 + rnd.nextDouble() * 1.8;
        return true;
    }
}
