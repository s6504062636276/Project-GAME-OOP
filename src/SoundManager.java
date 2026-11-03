import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * SoundManager: เสียงของเกมทั้งหมด
 * - ไม่ต้องมีไฟล์เสียง: เกมสังเคราะห์เสียง chiptune เองในโค้ด (เสียงพิมพ์ถูก/ผิด, เลเซอร์, โดนจิก, จบด่าน, ชนะ, แพ้ และเพลงประกอบวนซ้ำ)
 * - อยากใช้เสียงของตัวเอง: วางไฟล์ .wav ใน res/sounds/ ชื่อ correct, wrong, laser, hit, levelClear, victory, gameOver, music
 * - ถ้าเครื่องไม่มีลำโพง/ระบบเสียงมีปัญหา เกมจะข้ามเสียงเงียบๆ ไม่ crash
 * - กด F2 เพื่อเปิด/ปิดเสียง
 */
public final class SoundManager {
    private SoundManager() {}

    private static final float RATE = 22050f;
    private static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 1, true, false);
    private static final Map<String, byte[]> SFX = new HashMap<>();
    private static final Random RND = new Random(3);
    private static byte[] musicData;
    private static Clip music;
    private static boolean muted = false;
    private static int failures = 0;
    private static int active = 0;

    static {
        SFX.put("correct", bytes(tone(900, 1250, 0.045, 0, 0.22)));
        SFX.put("wrong", bytes(tone(170, 100, 0.16, 1, 0.30)));
        SFX.put("laser", bytes(concat(tone(1600, 350, 0.16, 0, 0.26), tone(700, 140, 0.30, 3, 0.20))));
        SFX.put("hit", bytes(concat(tone(300, 60, 0.12, 2, 0.45), tone(110, 45, 0.25, 4, 0.45))));
        SFX.put("levelClear", bytes(arpeggio(new double[]{523.25, 659.25, 783.99, 1046.5}, 0.11, 0.30)));
        SFX.put("victory", bytes(arpeggio(new double[]{523.25, 659.25, 783.99, 1046.5, 783.99, 1046.5, 1318.5, 1568}, 0.13, 0.30)));
        SFX.put("gameOver", bytes(arpeggio(new double[]{440, 349.23, 293.66, 220, 146.83}, 0.22, 0.30)));
        musicData = bytes(buildMusic());
    }

    // =========================================================== API
    /** เล่นเสียงเอฟเฟกต์ตามชื่อ ถ้าไม่รู้จักชื่อจะข้ามไป */
    public static void play(String name) {
        if (muted || failures > 3 || active > 14) return;
        try {
            AudioInputStream ais = customStream(name);
            if (ais == null) {
                byte[] d = SFX.get(name);
                if (d == null) return;
                ais = new AudioInputStream(new ByteArrayInputStream(d), FORMAT, d.length / 2);
            }
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            active++;
            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP) { active--; clip.close(); }
            });
            clip.start();
        } catch (Exception e) {
            failures++;
        }
    }

    /** เริ่มเพลงประกอบ (วนซ้ำ) */
    public static void playMusic() {
        if (failures > 3) return;
        try {
            if (music == null) {
                AudioInputStream custom = customStream("music");
                AudioInputStream ais = custom != null ? custom
                        : new AudioInputStream(new ByteArrayInputStream(musicData), FORMAT, musicData.length / 2);
                music = AudioSystem.getClip();
                music.open(ais);
                setGain(music, custom != null ? 0f : -4f);
            }
            if (!muted && !music.isRunning()) music.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            failures++;
        }
    }

    /** โหลดไฟล์เสียงที่ผู้ใช้วางเองใน res/sounds/ (หรือใน .jar) คืน null ถ้าไม่มี */
    private static AudioInputStream customStream(String name) throws Exception {
        java.io.InputStream in = Resources.open("sounds/" + name + ".wav");
        if (in == null) return null;
        return AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(in));
    }

    public static void toggleMute() {
        muted = !muted;
        if (music != null) {
            if (muted) music.stop();
            else music.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public static boolean isMuted() { return muted; }

    private static void setGain(Clip c, float db) {
        try {
            FloatControl g = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
            g.setValue(Math.max(g.getMinimum(), Math.min(g.getMaximum(), db)));
        } catch (Exception ignored) { }
    }

    // =========================================================== สังเคราะห์เสียง
    /** wave: 0 = square, 1 = sawtooth, 2 = noise, 3 = sine, 4 = triangle; ความถี่เลื่อนจาก f0 ไป f1 */
    private static double[] tone(double f0, double f1, double dur, int wave, double vol) {
        int n = (int) (dur * RATE);
        double[] out = new double[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double f = f0 * Math.pow(f1 / f0, t);
            phase += f / RATE;
            double ph = phase - Math.floor(phase);
            double s;
            switch (wave) {
                case 0: s = ph < 0.5 ? 1 : -1; break;
                case 1: s = 2 * ph - 1; break;
                case 2: s = RND.nextDouble() * 2 - 1; break;
                case 3: s = Math.sin(2 * Math.PI * ph); break;
                default: s = 4 * Math.abs(ph - 0.5) - 1; break;
            }
            double env = Math.pow(1 - t, 1.4) * Math.min(1, i / (RATE * 0.004));
            out[i] = s * env * vol;
        }
        return out;
    }

    private static double[] concat(double[]... parts) {
        int n = 0;
        for (double[] p : parts) n += p.length;
        double[] out = new double[n];
        int pos = 0;
        for (double[] p : parts) { System.arraycopy(p, 0, out, pos, p.length); pos += p.length; }
        return out;
    }

    private static double[] arpeggio(double[] freqs, double step, double vol) {
        double[][] parts = new double[freqs.length][];
        for (int i = 0; i < freqs.length; i++) parts[i] = tone(freqs[i], freqs[i], step * (i == freqs.length - 1 ? 3 : 1), 0, vol);
        return concat(parts);
    }

    private static void addAt(double[] buf, int start, double[] src) {
        for (int i = 0; i < src.length && start + i < buf.length; i++) buf[start + i] += src[i];
    }

    /** เพลงประกอบ 4 ห้อง (Am - F - C - G) อาร์เปจโจ + เบส ความยาวราว 7 วินาที วนซ้ำได้ */
    private static double[] buildMusic() {
        double[][] chords = {
            {440.00, 523.25, 659.25}, {349.23, 440.00, 523.25},
            {523.25, 659.25, 783.99}, {392.00, 493.88, 587.33}
        };
        double[] bass = {110.00, 87.31, 130.81, 98.00};
        int[] pattern = {0, 1, 2, 1, 0, 1, 2, 1};
        int eighth = (int) (RATE * 60 / 140 / 2);
        double[] buf = new double[eighth * 8 * chords.length];
        for (int bar = 0; bar < chords.length; bar++) {
            for (int s = 0; s < 8; s++) {
                int start = (bar * 8 + s) * eighth;
                double f = chords[bar][pattern[s]];
                addAt(buf, start, tone(f, f, 0.19, 0, 0.09));
                if (s % 2 == 0) addAt(buf, start, tone(bass[bar], bass[bar], 0.26, 4, 0.22));
            }
        }
        return buf;
    }

    private static byte[] bytes(double[] samples) {
        byte[] out = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            int v = (int) (Math.max(-1, Math.min(1, samples[i])) * 32000);
            out[2 * i] = (byte) (v & 0xFF);
            out[2 * i + 1] = (byte) ((v >> 8) & 0xFF);
        }
        return out;
    }

    /** ใช้ทดสอบ: ส่งออกเสียงเป็นไฟล์ .wav (ไม่ต้องมีลำโพง) */
    static void exportAll(File dir) throws Exception {
        dir.mkdirs();
        for (Map.Entry<String, byte[]> e : SFX.entrySet()) write(new File(dir, e.getKey() + ".wav"), e.getValue());
        write(new File(dir, "music.wav"), musicData);
    }

    private static void write(File f, byte[] d) throws Exception {
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(d), FORMAT, d.length / 2), AudioFileFormat.Type.WAVE, f);
    }
}
