/**
 * Config: เก็บค่าคงที่ทั้งหมดของเกมไว้ที่เดียว (ปรับสมดุลเกมได้ที่ไฟล์นี้ไฟล์เดียว)
 * หลัก Encapsulation: คลาสนี้สร้าง object ไม่ได้ (constructor เป็น private) ใช้ผ่านค่า static เท่านั้น
 */
public final class Config {
    private Config() {}

    // ---------- หน้าจอ ----------
    public static final int WIDTH = 1024;
    public static final int HEIGHT = 576;
    public static final int FPS = 60;

    // ---------- UFO ----------
    public static final int UFO_MAX_HP = 100;      // HP เริ่มต้น
    public static final int BIRD_DAMAGE = 20;      // นก 1 ตัวโจมตี = -20 HP
    public static final int ALIEN_RED_HP = 60;     // HP <= ค่านี้ เอเลี่ยนในยานเปลี่ยนเป็นสีแดง
    public static final int LOW_HP = 20;           // HP <= ค่านี้ UFO กะพริบ
    public static final int UFO_X = 110;           // ตำแหน่งกึ่งกลาง UFO แกน X
    public static final int UFO_Y = 300;           // ตำแหน่งกึ่งกลาง UFO แกน Y
    public static final int UFO_WIDTH = 125;       // ความกว้าง UFO บนจอ (px)
    public static final int UFO_HIT_OFFSET = 44;   // ขอบที่นกชนอยู่ห่างจากกลาง UFO ไปทางขวา (px)

    // ---------- นก ----------
    public static final int BIRD_WIDTH = 178;      // ความกว้างนกธรรมดาบนจอ (px)
    public static final double BASE_FLIGHT_TIME = 3.0;                       // flightTime = 3 + (ความยาวคำ x k)
    public static final double[] K_PER_LEVEL = {0.90, 0.75, 0.65, 0.55, 0.50};
    public static final double BIRD_SPEED_SCALE = 0.85;                      // ตัวคูณความเร็วนกทั้งเกม (1.0 = ตามสูตร, 0.85 = ช้าลง 15%)
    public static final double WRONG_SPEEDUP = 0.15;   // พิมพ์ผิดหนึ่งครั้ง นกตัวที่ล็อกเร็วขึ้น +15%
    public static final double MAX_SPEEDUP = 0.60;     // เร็วขึ้นสูงสุด +60%
    // ช่องทางบินของนก (แกน Y)
    public static final int[] LANES = {140, 210, 280, 350, 420};

    // ---------- ด่าน ----------
    public static final int TOTAL_LEVELS = 5;
    public static final int BIRDS_PER_LEVEL = 20;
    public static final int[] MIN_BIRDS = {1, 2, 2, 3, 3};  // นกบนจอพร้อมกัน (ต่ำสุด) ตามด่าน
    public static final int[] MAX_BIRDS = {2, 3, 3, 3, 4};  // นกบนจอพร้อมกัน (สูงสุด) ตามด่าน

    // ---------- คะแนน ----------
    public static final int SCORE_PER_CHAR = 10;   // คะแนนต่อนก = ความยาวคำ x 10
    // โบนัสตอนจบด่านตาม Accuracy ของด่านนั้น: >=95% +200, >=85% +100, >=70% +50, ต่ำกว่านั้น 0
    public static final double[] BONUS_ACCURACY = {95, 85, 70};
    public static final int[] BONUS_POINTS = {200, 100, 50};

    // ---------- จังหวะต่างๆ ----------
    public static final int HEAL_PER_LEVEL = 20;      // จบด่านฟื้น HP (ไม่เกิน UFO_MAX_HP)
    public static final double LEVEL_END_DELAY = 1.0;  // หน่วงหลังฆ่านกตัวที่ 20 ก่อนขึ้นหน้า Level Clear
    public static final double LEVEL_INTRO_TIME = 1.6; // แสดงป้าย "LEVEL n" ตอนเริ่มด่าน
}
