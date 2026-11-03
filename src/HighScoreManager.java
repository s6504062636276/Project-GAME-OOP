import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HighScoreManager: เก็บ 5 คะแนนสูงสุดลงไฟล์ highscore.txt (อยู่ในโฟลเดอร์ที่รันเกม)
 * รูปแบบแต่ละบรรทัด: score;level;kills;date  เรียงจากมากไปน้อย
 */
public class HighScoreManager {
    public static final int MAX_ENTRIES = 5;

    /** Entry: คะแนนหนึ่งรายการ (immutable) */
    public static class Entry {
        public final int score, level, kills;
        public final String date;

        public Entry(int score, int level, int kills, String date) {
            this.score = score; this.level = level; this.kills = kills; this.date = date;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final File file;

    public HighScoreManager() {
        this(new File("highscore.txt"));
    }

    public HighScoreManager(File file) {
        this.file = file;
        load();
    }

    public List<Entry> getEntries() { return Collections.unmodifiableList(entries); }

    public int getBest() { return entries.isEmpty() ? 0 : entries.get(0).score; }

    /** ส่งคะแนนเข้าตาราง คืนอันดับ (0 = ที่ 1) ถ้าติดอันดับ หรือ -1 ถ้าไม่ติด */
    public int submit(int score, int level, int kills) {
        if (score <= 0) return -1;
        int pos = entries.size();
        for (int i = 0; i < entries.size(); i++) {
            if (score > entries.get(i).score) { pos = i; break; }
        }
        if (pos >= MAX_ENTRIES) return -1;
        entries.add(pos, new Entry(score, level, kills, LocalDate.now().toString()));
        while (entries.size() > MAX_ENTRIES) entries.remove(entries.size() - 1);
        save();
        return pos;
    }

    private void load() {
        entries.clear();
        if (!file.isFile()) return;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.trim().split(";");
                if (p.length < 4) continue;
                try {
                    entries.add(new Entry(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), p[3]));
                } catch (NumberFormatException ignored) { }   // ข้ามบรรทัดที่เสีย
            }
        } catch (IOException e) {
            System.err.println("อ่าน highscore.txt ไม่สำเร็จ: " + e.getMessage());
        }
        entries.sort((a, b) -> Integer.compare(b.score, a.score));
        while (entries.size() > MAX_ENTRIES) entries.remove(entries.size() - 1);
    }

    private void save() {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (Entry e : entries) pw.println(e.score + ";" + e.level + ";" + e.kills + ";" + e.date);
        } catch (IOException e) {
            System.err.println("บันทึก highscore.txt ไม่สำเร็จ: " + e.getMessage());
        }
    }
}
