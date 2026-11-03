/**
 * ScoreManager: เก็บคะแนน เวลา จำนวนนกที่ฆ่า และความแม่นยำ (Accuracy)
 * Encapsulation: ตัวแปรเป็น private เปลี่ยนค่าผ่านเมธอดเท่านั้น
 */
public class ScoreManager {
    private int score;
    private int kills;
    private int correctKeys;
    private int totalKeys;
    private double elapsed;
    // ตัวนับเฉพาะด่านปัจจุบัน (ใช้คิดโบนัส Accuracy)
    private int levelStartScore;
    private int levelCorrect;
    private int levelTotal;

    public void reset() {
        levelStartScore = 0;
        levelCorrect = 0;
        levelTotal = 0;
        score = 0;
        kills = 0;
        correctKeys = 0;
        totalKeys = 0;
        elapsed = 0;
    }

    /** นกตาย 1 ตัว คืนคะแนนที่ได้ (ความยาวคำ x 10) */
    public int addKill(int wordLength) {
        return addKill(wordLength, 1);
    }

    /** นกตาย 1 ตัว พร้อมตัวคูณคะแนนของนกชนิดนั้น (บอส x2) */
    public int addKill(int wordLength, int multiplier) {
        kills++;
        int gained = wordLength * Config.SCORE_PER_CHAR * multiplier;
        score += gained;
        return gained;
    }

    public void keyCorrect() { totalKeys++; correctKeys++; levelTotal++; levelCorrect++; }
    public void keyWrong() { totalKeys++; levelTotal++; }

    /** เริ่มด่านใหม่: รีเซ็ตตัวนับของด่าน */
    public void startLevel() {
        levelStartScore = score;
        levelCorrect = 0;
        levelTotal = 0;
    }

    /** คะแนนที่ได้ในด่านนี้ (ยังไม่รวมโบนัส) */
    public int getLevelScore() { return score - levelStartScore; }

    public double getLevelAccuracy() {
        return levelTotal == 0 ? 100.0 : 100.0 * levelCorrect / levelTotal;
    }

    /** โบนัสจบด่านตาม Accuracy ของด่าน */
    public int computeBonus() {
        double acc = getLevelAccuracy();
        for (int i = 0; i < Config.BONUS_ACCURACY.length; i++) {
            if (acc >= Config.BONUS_ACCURACY[i]) return Config.BONUS_POINTS[i];
        }
        return 0;
    }

    public void addBonus(int bonus) { score += bonus; }
    public void addTime(double dt) { elapsed += dt; }

    /** Accuracy = ตัวอักษรที่พิมพ์ถูก / ตัวอักษรที่กดทั้งหมด (เป็น %) */
    public double getAccuracy() {
        return totalKeys == 0 ? 100.0 : 100.0 * correctKeys / totalKeys;
    }

    public String formatTime() {
        int t = (int) elapsed;
        return String.format("%d:%02d", t / 60, t % 60);
    }

    public int getScore() { return score; }
    public int getKills() { return kills; }
    public double getElapsed() { return elapsed; }
}
