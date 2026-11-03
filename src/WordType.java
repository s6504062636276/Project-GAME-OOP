/**
 * WordType: ระดับคำศัพท์ แบ่งตามความยาวอัตโนมัติ (ไม่ต้องแยกไฟล์ words.txt)
 * SHORT 2-4 (ง่าย) | MEDIUM 5-8 (กลาง) | LONG 9-11, XLONG 12-14, VERYLONG 15+ (ยาก ไล่ระดับ)
 */
public enum WordType {
    SHORT(2, 4), MEDIUM(5, 8), LONG(9, 11), XLONG(12, 14), VERYLONG(15, Integer.MAX_VALUE);

    private final int minLen;
    private final int maxLen;

    WordType(int minLen, int maxLen) {
        this.minLen = minLen;
        this.maxLen = maxLen;
    }

    /** หาประเภทจากความยาวคำ ถ้าไม่เข้าช่วงไหน (เช่น 1 ตัว) ให้ใช้ช่วงที่ใกล้ที่สุด */
    public static WordType fromLength(int len) {
        WordType best = SHORT;
        int bestDist = Integer.MAX_VALUE;
        for (WordType t : values()) {
            int dist = len < t.minLen ? t.minLen - len : (len > t.maxLen ? len - t.maxLen : 0);
            if (dist < bestDist) {
                bestDist = dist;
                best = t;
            }
        }
        return best;
    }
}
