import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * WordManager: โหลดคำจาก res/words.txt จัดประเภทตามความยาว และสุ่มคำให้นก
 * กติกา: ตัวอักษรแรกของคำต้องไม่ซ้ำกับนกที่อยู่บนจอ (เช็กแบบไม่สนตัวพิมพ์เล็ก-ใหญ่ เพื่อความสบายของผู้เล่น)
 * ส่วนการพิมพ์จริงใน InputHandler ตัวพิมพ์เล็ก-ใหญ่ต้องตรงเป๊ะ
 */
public class WordManager {
    private static final String[] FALLBACK = {
        "if", "for", "int", "new", "void", "main", "loop", "node",
        "class", "public", "static", "method", "Interface", "Container", "Inheritance", "Constructor",
        "Polymorphism", "Encapsulation", "Multithreading", "Implementation",
        "ArrayIndexOutOfBoundsException", "IllegalArgumentException"
    };

    private final Map<WordType, List<String>> pools = new EnumMap<>(WordType.class);
    private final Deque<String> recent = new ArrayDeque<>();
    private final Random rnd = new Random();

    public WordManager() {
        for (WordType t : WordType.values()) pools.put(t, new ArrayList<>());
        List<String> all = loadFromFile();
        if (all.isEmpty()) {
            System.err.println("ไม่พบ res/words.txt ใช้คำสำรองแทน");
            all = Arrays.asList(FALLBACK);
        }
        for (String w : all) pools.get(WordType.fromLength(w.length())).add(w);
    }

    private List<String> loadFromFile() {
        List<String> list = new ArrayList<>();
        InputStream in = Resources.open("words.txt");
        if (in == null) return list;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.matches("[A-Za-z]+")) list.add(line);   // รับเฉพาะตัวอักษร A-Z
            }
        } catch (Exception e) {
            System.err.println("อ่าน words.txt ไม่สำเร็จ: " + e.getMessage());
        }
        return list;
    }

    /** ถ้าประเภทที่ขอไม่มีคำเลย ให้ใช้ประเภทที่ใกล้ที่สุดแทน */
    private List<String> poolFor(WordType type) {
        List<String> p = pools.get(type);
        if (!p.isEmpty()) return p;
        WordType[] all = WordType.values();
        for (int d = 1; d < all.length; d++) {
            int lo = type.ordinal() - d, hi = type.ordinal() + d;
            if (hi < all.length && !pools.get(all[hi]).isEmpty()) return pools.get(all[hi]);
            if (lo >= 0 && !pools.get(all[lo]).isEmpty()) return pools.get(all[lo]);
        }
        return p;
    }

    /** สุ่มคำประเภท type ที่ตัวแรกไม่ซ้ำกับนกบนจอ และไม่ซ้ำคำที่เพิ่งใช้ไป */
    public String pickWord(WordType type, Collection<Bird> onScreen) {
        Set<Character> usedFirst = new HashSet<>();
        Set<String> usedWords = new HashSet<>();
        for (Bird b : onScreen) {
            if (b.getState() != Bird.State.FLYING) continue;
            usedFirst.add(Character.toLowerCase(b.getWord().charAt(0)));
            usedWords.add(b.getWord());
        }
        List<String> pool = poolFor(type);
        List<String> cand = new ArrayList<>();
        for (String w : pool) {
            if (!usedFirst.contains(Character.toLowerCase(w.charAt(0))) && !recent.contains(w)) cand.add(w);
        }
        if (cand.isEmpty()) {   // ผ่อนเงื่อนไข "คำที่เพิ่งใช้"
            for (String w : pool) {
                if (!usedFirst.contains(Character.toLowerCase(w.charAt(0)))) cand.add(w);
            }
        }
        if (cand.isEmpty()) {   // ยังไม่มี: หยิบจากระดับอื่นที่ตัวแรกไม่ซ้ำ (ดีกว่าให้ตัวแรกชนกัน)
            for (List<String> other : pools.values()) {
                for (String w : other) {
                    if (!usedFirst.contains(Character.toLowerCase(w.charAt(0))) && !recent.contains(w)) cand.add(w);
                }
            }
        }
        if (cand.isEmpty()) {   // กรณีสุดท้าย: ยอมให้ตัวแรกซ้ำ แต่ไม่ซ้ำคำ
            for (String w : pool) if (!usedWords.contains(w)) cand.add(w);
        }
        if (cand.isEmpty()) cand = pool;
        String pick = cand.get(rnd.nextInt(cand.size()));
        recent.addLast(pick);
        while (recent.size() > 8) recent.removeFirst();
        return pick;
    }

    public void reset() { recent.clear(); }
}
