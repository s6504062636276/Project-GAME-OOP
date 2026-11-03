import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Resources: หาและเปิดไฟล์ทรัพยากรของเกม (รูป, เสียง, words.txt)
 * ลำดับการค้นหา: 1) โฟลเดอร์ res/ ข้างนอก (แก้ไฟล์ได้ง่าย)  2) ภายในไฟล์ .jar (ตอนรันจาก .jar เดี่ยวๆ)
 */
public final class Resources {
    private Resources() {}

    /** @param rel path ย่อยใต้ res/ เช่น "images/ufo.png" คืน null ถ้าไม่พบในโฟลเดอร์ */
    public static File find(String rel) {
        String[] bases = {"res/", "../res/", "../../res/", "TypeSkysaurus/res/"};
        for (String b : bases) {
            File f = new File(b + rel);
            if (f.isFile()) return f;
        }
        return null;
    }

    /** เปิดไฟล์เป็น InputStream (โฟลเดอร์ res/ ก่อน แล้วค่อยหาใน .jar) คืน null ถ้าไม่พบเลย */
    public static InputStream open(String rel) {
        File f = find(rel);
        if (f != null) {
            try {
                return new FileInputStream(f);
            } catch (IOException e) {
                // ถ้าเปิดไม่ได้ ลองอ่านจากใน .jar ต่อ
            }
        }
        return Resources.class.getResourceAsStream("/res/" + rel);
    }
}
