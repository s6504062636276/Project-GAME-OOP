import java.util.Random;

/**
 * LevelManager: เก็บข้อมูลด่าน (1-5) และจำนวนนกที่ฆ่าในด่านนั้น (x/20)
 * - ด่านกำหนดประเภทคำที่สุ่ม, จำนวนนกบนจอพร้อมกัน และค่า k (ผ่าน Config + Bird)
 * - จบด่านเมื่อพิมพ์ถูกครบ 20 ตัว (นกที่จิก UFO ไม่นับ)
 */
public class LevelManager {
    private int level = 1;
    private int kills = 0;

    public void reset() {
        level = 1;
        kills = 0;
    }

    public int getLevel() { return level; }
    public int getKills() { return kills; }
    public boolean isLastLevel() { return level >= Config.TOTAL_LEVELS; }
    /** ด่านสุดท้ายมีนกบอสเป็นตัวที่ 20 */
    public boolean isBossLevel() { return level >= Config.TOTAL_LEVELS; }
    public boolean isLevelComplete() { return kills >= Config.BIRDS_PER_LEVEL; }

    public void onKill() { kills++; }

    public void nextLevel() {
        if (level < Config.TOTAL_LEVELS) level++;
        kills = 0;
    }

    public int getMinBirds() { return Config.MIN_BIRDS[level - 1]; }
    public int getMaxBirds() { return Config.MAX_BIRDS[level - 1]; }

    /**
     * ระดับคำไล่ตามด่าน (ง่าย -> กลาง -> ยาก) ด่านละ 2 ระดับผสมกันอย่างละครึ่ง:
     * ด่าน 1: SHORT 100% | ด่าน 2: SHORT + MEDIUM | ด่าน 3: MEDIUM + LONG |
     * ด่าน 4: LONG + XLONG | ด่าน 5: XLONG + VERYLONG (ตัวที่ 20 เป็นนกบอส)
     */
    public WordType chooseWordType(Random rnd) {
        boolean half = rnd.nextBoolean();
        switch (level) {
            case 1: return WordType.SHORT;
            case 2: return half ? WordType.SHORT : WordType.MEDIUM;
            case 3: return half ? WordType.MEDIUM : WordType.LONG;
            case 4: return half ? WordType.LONG : WordType.XLONG;
            default: return half ? WordType.XLONG : WordType.VERYLONG;
        }
    }
}
