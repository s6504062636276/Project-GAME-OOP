import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * InputHandler: รับการพิมพ์ ล็อกเป้า และตรวจถูก/ผิด
 * - ยังไม่ล็อก: พิมพ์ตัวอักษรแรกของคำใดคำหนึ่ง -> ล็อกนกตัวนั้นอัตโนมัติ
 * - ระหว่างล็อก: พิมพ์ได้เฉพาะคำของนกตัวนั้น ถูก = ไฮไลต์, ผิด = นกตัวนั้นเร็วขึ้น
 * - ตัวพิมพ์เล็ก-ใหญ่ต้องตรงตามคำบนตัวนก (เช่น Polymorphism ต้องพิมพ์ P ตัวใหญ่ ต้องกด Shift)
 * - Backspace: ปลดล็อก (ความคืบหน้าของคำนั้นรีเซ็ต)
 * ใช้ interface Listener เพื่อไม่ต้องผูกกับ GamePanel โดยตรง (ลดการพึ่งพากันของคลาส)
 */
public class InputHandler extends KeyAdapter {

    /** สิ่งที่ InputHandler ต้องการจากเกม */
    public interface Listener {
        List<Bird> getBirds();
        boolean isAcceptingInput();              // ตอนนี้รับการพิมพ์ไหม (กำลังเล่นและ UFO ยังไม่ตาย)
        void onCorrectKey();
        void onWrongKey(Bird target);            // target เป็น null ถ้ายังไม่ได้ล็อกเป้า
        void onWordCompleted(Bird bird);
        void onNavigationKey(int keyCode);       // ปุ่มเมนู: ลูกศร, Enter, Esc ฯลฯ
        void onBackspaceUnlock();
    }

    private final Listener listener;
    private Bird locked;

    public InputHandler(Listener listener) {
        this.listener = listener;
    }

    public Bird getLockedBird() { return locked; }

    public void reset() { unlock(); }

    /** เรียกทุกเฟรม: ถ้านกที่ล็อกอยู่ตายหรือหายไปแล้ว ให้ปลดล็อก */
    public void update() {
        if (locked != null && !locked.isFlying()) unlock();
    }

    private void unlock() {
        if (locked != null) {
            locked.setLocked(false);
            locked.resetProgress();
            locked = null;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) processLetter(c);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_BACK_SPACE && listener.isAcceptingInput()) {
            if (locked != null) {
                unlock();
                listener.onBackspaceUnlock();
            }
            return;
        }
        listener.onNavigationKey(k);
    }

    /** ประมวลผลตัวอักษรหนึ่งตัว (public เพื่อให้ทดสอบได้โดยไม่ต้องมีคีย์บอร์ด) */
    public void processLetter(char c) {
        if (!listener.isAcceptingInput()) return;
        if (locked != null && !locked.isFlying()) unlock();

        if (locked == null) {
            Bird target = null;
            for (Bird b : listener.getBirds()) {
                if (!b.isFlying()) continue;
                if (b.getWord().charAt(0) == c) {
                    if (target == null || b.getX() < target.getX()) target = b;   // ถ้ามีหลายตัว เลือกตัวที่ใกล้ UFO สุด
                }
            }
            if (target == null) {
                listener.onWrongKey(null);        // ไม่มีนกตัวไหนขึ้นต้นด้วยตัวนี้
                return;
            }
            locked = target;
            locked.setLocked(true);
        }

        if (locked.getNextChar() == c) {
            locked.typeNext();
            listener.onCorrectKey();
            if (locked.isComplete()) {
                Bird done = locked;
                locked = null;
                done.setLocked(false);
                listener.onWordCompleted(done);
            }
        } else {
            locked.wrongKey();
            listener.onWrongKey(locked);
        }
    }
}
