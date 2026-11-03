import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * GamePanel: หัวใจของเกม ทำหน้าที่
 *  1) วนเกมด้วย javax.swing.Timer (~60 FPS)  2) จัดการสถานะ GameState  3) วาดทุกหน้าจอ
 * ครบทุกหน้า: MENU, HOW_TO_PLAY, HIGH_SCORE, PLAYING (5 ด่าน + นกบอส), PAUSED, LEVEL_CLEAR, GAME_OVER, VICTORY
 */
public class GamePanel extends JPanel implements InputHandler.Listener {
    // ---------- สี ----------
    private static final Color C_ORANGE = new Color(255, 170, 40);
    private static final Color C_INK = new Color(20, 12, 28);
    private static final Color C_PAPER = new Color(250, 246, 240);
    private static final Color C_PAPER_SHADE = new Color(205, 195, 215);

    // ---------- เมนู ----------
    private static final String[] MENU_LABELS = {"PLAY", "HOW TO PLAY", "HIGH SCORE", "QUIT"};
    private static final boolean[] MENU_ENABLED = {true, true, true, true};
    private final Rectangle[] menuRects = new Rectangle[4];
    private final Rectangle[] overRects = new Rectangle[2];
    private final Rectangle continueRect = new Rectangle(Config.WIDTH / 2 - 150, 430, 300, 54);
    private static final String[] PAUSE_LABELS = {"RESUME", "RESTART", "MAIN MENU"};
    private final Rectangle[] pauseRects = new Rectangle[3];
    private final Rectangle backRect = new Rectangle(Config.WIDTH / 2 - 120, 482, 240, 50);
    private int pauseIndex = 0;
    private int newRank = -1;            // อันดับ High Score ที่เพิ่งทำได้ (-1 = ไม่ติด)
    private int menuIndex = 0;
    private int overIndex = 0;

    // ---------- องค์ประกอบเกม ----------
    private GameState state = GameState.MENU;
    private final Background background = new Background();
    private final WordManager words = new WordManager();
    private final ScoreManager score = new ScoreManager();
    private final Spawner spawner = new Spawner();
    private final LevelManager levels = new LevelManager();
    private final HighScoreManager scores = new HighScoreManager();
    private final InputHandler input = new InputHandler(this);
    private final List<Bird> birds = new ArrayList<>();
    private final List<Laser> lasers = new ArrayList<>();
    private final List<FloatingText> texts = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private Ufo ufo = new Ufo(Config.UFO_X, Config.UFO_Y);
    private final Ufo menuUfo = new Ufo(170, 400);
    private final BufferedImage silhouette = ImageLoader.tint(ImageLoader.loadBird(), Color.BLACK, 1f);
    private final double[] silX = {900, 1500, 2100};
    private final double[] silY = {110, 190, 330};
    private double shake = 0;
    private double hitFlash = 0;         // วูบสีแดงที่ขอบจอตอนโดนจิก
    private double endTimer = 0;
    private double clock = 0;            // เวลาสะสม ใช้ทำอนิเมชันกะพริบ
    private double levelEndTimer = -1;   // >= 0 คือกำลังหน่วงก่อนขึ้นหน้า Level Clear
    private double introTimer = 0;       // เวลาแสดงป้าย LEVEL n
    // สรุปผลด่านล่าสุด (แสดงในหน้า Level Clear)
    private int sumLevelScore, sumBonus, sumHeal;
    private double sumAccuracy;
    private final Random rnd = new Random();

    private final javax.swing.Timer timer;
    private long lastNanos;

    public GamePanel() {
        setPreferredSize(new Dimension(Config.WIDTH, Config.HEIGHT));
        setFocusable(true);
        setBackground(Color.BLACK);
        addKeyListener(input);
        // หน้าต่างเสียโฟกัสระหว่างเล่น -> Pause อัตโนมัติ
        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                if (state == GameState.PLAYING && !ufo.isDead()) pauseGame();
            }
        });
        for (int i = 0; i < 4; i++) menuRects[i] = new Rectangle((Config.WIDTH - 300) / 2, 240 + i * 66, 300, 52);
        overRects[0] = new Rectangle(Config.WIDTH / 2 - 265, 400, 250, 54);
        overRects[1] = new Rectangle(Config.WIDTH / 2 + 15, 400, 250, 54);
        for (int i = 0; i < 3; i++) pauseRects[i] = new Rectangle((Config.WIDTH - 300) / 2, 235 + i * 70, 300, 52);

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { hover(e.getPoint()); }
            @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); click(e.getPoint()); }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        timer = new javax.swing.Timer(1000 / Config.FPS, e -> tick());
    }

    public void start() {
        SoundManager.playMusic();
        lastNanos = System.nanoTime();
        timer.start();
    }

    public InputHandler getInputHandler() { return input; }
    public GameState getState() { return state; }

    // =====================================================================
    //  วนเกม
    // =====================================================================
    private void tick() {
        long now = System.nanoTime();
        double dt = Math.min((now - lastNanos) / 1e9, 0.05);
        lastNanos = now;
        update(dt);
        repaint();
    }

    /** อัปเดตหนึ่งเฟรม (package-private เพื่อให้ทดสอบได้) */
    void update(double dt) {
        clock += dt;
        background.update(dt);
        switch (state) {
            case MENU:
                menuUfo.update(dt);
                for (int i = 0; i < silX.length; i++) {
                    silX[i] -= 70 * dt;
                    if (silX[i] < -250) silX[i] = Config.WIDTH + 100 + rnd.nextInt(500);
                }
                break;
            case PLAYING:
                updatePlaying(dt);
                break;
            case LEVEL_CLEAR:
            case GAME_OVER:
            case VICTORY:
                updateEffects(dt);
                break;
            default:
                break;
        }
    }

    private void updatePlaying(double dt) {
        ufo.update(dt);
        if (introTimer > 0) {
            introTimer -= dt;                       // กำลังแสดงป้าย LEVEL n ยังไม่เกิดนก ยังไม่นับเวลา
        } else if (!ufo.isDead() && levelEndTimer < 0) {
            score.addTime(dt);
            int flying = 0;
            for (Bird b : birds) if (b.isFlying()) flying++;
            // ด่านบอส: นกตัวที่ 20 ต้องเป็นบอส จึงหยุดเกิดนกธรรมดาเมื่อ (ฆ่าแล้ว + ที่บินอยู่) ครบ 19
            boolean bossTime = levels.isBossLevel() && levels.getKills() >= Config.BIRDS_PER_LEVEL - 1;
            boolean hold = levels.isBossLevel() && !bossTime && levels.getKills() + flying >= Config.BIRDS_PER_LEVEL - 1;
            if (bossTime) {
                if (flying == 0 && spawner.update(dt, 0, 1, 1)) spawnBoss();
            } else if (!hold && spawner.update(dt, flying, levels.getMinBirds(), levels.getMaxBirds())) {
                spawnBird();
            }
        }
        input.update();
        for (Iterator<Bird> it = birds.iterator(); it.hasNext(); ) {
            Bird b = it.next();
            b.update(dt);
            if (b.hasReachedUfo()) {
                if (!ufo.isDead()) {       // นกจิก UFO: HP ลด นกหายไป
                    ufo.takeDamage(Config.BIRD_DAMAGE);
                    texts.add(new FloatingText("-" + Config.BIRD_DAMAGE, Config.UFO_X, Config.UFO_Y - 70, new Color(255, 80, 80)));
                    shake = 0.35;
                    hitFlash = 0.35;
                    burst(Config.UFO_X + 40, ufo.getBaseY(), 18, new Color(255, 110, 40), new Color(255, 220, 90), 220, 300);
                    if (ufo.isDead()) burst(Config.UFO_X, ufo.getBaseY(), 50, new Color(255, 90, 40), new Color(255, 240, 160), 330, 350);
                    SoundManager.play("hit");
                }
                b.attackDone();
            }
            if (b.isRemovable()) it.remove();
        }
        updateEffects(dt);
        if (levelEndTimer >= 0 && !ufo.isDead()) {
            levelEndTimer -= dt;
            if (levelEndTimer < 0) finishLevel();
            return;
        }
        if (ufo.isDead()) {
            endTimer -= dt;
            if (endTimer <= 0) {
                state = GameState.GAME_OVER;
                newRank = scores.submit(score.getScore(), levels.getLevel(), score.getKills());
                birds.clear();            // นกที่ยังบินอยู่หายไป ไม่ให้คำค้างบนหน้า Game Over
                input.reset();
                overIndex = 0;
                SoundManager.play("gameOver");
            }
        }
    }

    private void updateEffects(double dt) {
        ufo.update(dt);
        if (state == GameState.GAME_OVER) {
            for (Iterator<Bird> it = birds.iterator(); it.hasNext(); ) {
                Bird b = it.next();
                b.update(dt);
                if (b.isRemovable()) it.remove();
            }
        }
        for (Laser l : lasers) l.update(dt);
        lasers.removeIf(Laser::isDone);
        for (Particle pt : particles) pt.update(dt);
        particles.removeIf(Particle::isDone);
        hitFlash = Math.max(0, hitFlash - dt);
        for (FloatingText t : texts) t.update(dt);
        texts.removeIf(FloatingText::isDone);
        shake = Math.max(0, shake - dt);
    }

    // =====================================================================
    //  ควบคุมเกม
    // =====================================================================
    public void startGame() {
        ufo = new Ufo(Config.UFO_X, Config.UFO_Y);
        birds.clear();
        lasers.clear();
        texts.clear();
        particles.clear();
        score.reset();
        words.reset();
        spawner.reset();
        input.reset();
        levels.reset();
        score.startLevel();
        shake = 0;
        endTimer = 0.9;
        newRank = -1;
        levelEndTimer = -1;
        introTimer = Config.LEVEL_INTRO_TIME;
        state = GameState.PLAYING;
    }

    /** ฆ่านกครบ 20 ตัว: นกที่เหลือบนจอโดนช็อตหมด แล้วหน่วงสั้นๆ ก่อนขึ้นหน้าสรุป */
    private void beginLevelEnd() {
        input.reset();
        for (Bird b : birds) if (b.isFlying()) b.kill();
        levelEndTimer = Config.LEVEL_END_DELAY;
        SoundManager.play("levelClear");
    }

    /** สรุปด่าน: คิดโบนัส Accuracy, ฟื้น HP แล้วไปหน้า Level Clear (หรือ Victory ถ้าเป็นด่านสุดท้าย) */
    private void finishLevel() {
        sumLevelScore = score.getLevelScore();
        sumAccuracy = score.getLevelAccuracy();
        sumBonus = score.computeBonus();
        score.addBonus(sumBonus);
        sumHeal = ufo.heal(Config.HEAL_PER_LEVEL);
        birds.clear();
        lasers.clear();
        overIndex = 0;
        if (levels.isLastLevel()) {
            newRank = scores.submit(score.getScore(), levels.getLevel(), score.getKills());
            SoundManager.play("victory");
            state = GameState.VICTORY;
        } else {
            state = GameState.LEVEL_CLEAR;
        }
    }

    /** เริ่มด่านถัดไป */
    private void startNextLevel() {
        levels.nextLevel();
        score.startLevel();
        birds.clear();
        lasers.clear();
        texts.clear();
        particles.clear();
        spawner.reset();
        words.reset();
        input.reset();
        levelEndTimer = -1;
        introTimer = Config.LEVEL_INTRO_TIME;
        state = GameState.PLAYING;
    }

    /** Pause (กด Esc หรือหน้าต่างเสียโฟกัส) */
    private void pauseGame() {
        pauseIndex = 0;
        state = GameState.PAUSED;
    }

    private void resumeGame() {
        lastNanos = System.nanoTime();
        state = GameState.PLAYING;
    }

    /** ระเบิดประกาย: สุ่มสีระหว่าง c1 กับ c2 กระจายรอบจุด (x,y) */
    private void burst(double x, double y, int count, Color c1, Color c2, double speed, double gravity) {
        for (int i = 0; i < count; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double sp = speed * (0.3 + rnd.nextDouble() * 0.7);
            double t = rnd.nextDouble();
            Color c = new Color((int) (c1.getRed() + (c2.getRed() - c1.getRed()) * t),
                                (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t),
                                (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * t));
            particles.add(new Particle(x, y, Math.cos(ang) * sp, Math.sin(ang) * sp,
                    0.35 + rnd.nextDouble() * 0.45, gravity, 3 + rnd.nextInt(4), c));
        }
    }

    private void returnToMenu() {
        input.reset();
        state = GameState.MENU;
    }


    /** นกบอส: ใช้คำ VERYLONG ตัวใหญ่ ได้คะแนน x2 */
    private void spawnBoss() {
        String word = words.pickWord(WordType.VERYLONG, birds);
        birds.add(new BossBird(word, Config.LANES[2], levels.getLevel(), ufo.getHitX(), ufo.getBaseY()));
        texts.add(new FloatingText("BOSS!", Config.WIDTH / 2, 130, new Color(255, 80, 80)));
        shake = 0.25;
    }

    private void spawnBird() {
        String word = words.pickWord(levels.chooseWordType(rnd), birds);
        birds.add(new NormalBird(word, chooseLaneY(), levels.getLevel(), ufo.getHitX(), ufo.getBaseY()));
    }

    /** เลือกช่องบินที่ว่างที่สุด (ไม่มีนกอยู่ใกล้ขอบขวา) */
    private double chooseLaneY() {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < Config.LANES.length; i++) idx.add(i);
        Collections.shuffle(idx, rnd);
        double bestLane = Config.LANES[idx.get(0)], bestGap = -1;
        for (int i : idx) {
            double gap = Double.MAX_VALUE;
            for (Bird b : birds) {
                if (b.isFlying() && Math.abs(b.getLaneY() - Config.LANES[i]) < 1) gap = Math.min(gap, Config.WIDTH + 100 - b.getX());
            }
            if (gap > bestGap) {
                bestGap = gap;
                bestLane = Config.LANES[i];
            }
        }
        return bestLane;
    }

    // =====================================================================
    //  InputHandler.Listener
    // =====================================================================
    @Override public List<Bird> getBirds() { return birds; }

    @Override public boolean isAcceptingInput() { return state == GameState.PLAYING && !ufo.isDead() && introTimer <= 0 && levelEndTimer < 0; }

    @Override public void onCorrectKey() {
        score.keyCorrect();
        SoundManager.play("correct");
    }

    @Override public void onWrongKey(Bird target) {
        score.keyWrong();            // ตัวนกเร่งความเร็วถูกจัดการใน InputHandler แล้ว
        SoundManager.play("wrong");
    }

    @Override public void onBackspaceUnlock() { }

    @Override public void onWordCompleted(Bird bird) {
        int gained = score.addKill(bird.getWord().length(), bird.getScoreMultiplier());
        lasers.add(new Laser(ufo.getMuzzleX(), ufo.getMuzzleY(), bird.getX(), bird.getY()));
        texts.add(new FloatingText("+" + gained, bird.getX(), bird.getY() - 40, new Color(255, 225, 90)));
        burst(ufo.getMuzzleX(), ufo.getMuzzleY(), 8, new Color(120, 190, 255), Color.WHITE, 160, 0);
        burst(bird.getX(), bird.getY(), 22, new Color(120, 190, 255), new Color(255, 245, 150), 260, 400);
        bird.kill();
        SoundManager.play("laser");
        levels.onKill();
        if (levels.isLevelComplete()) beginLevelEnd();
    }

    @Override public void onNavigationKey(int k) {
        if (k == KeyEvent.VK_F2) {      // F2 = เปิด/ปิดเสียง
            SoundManager.toggleMute();
            return;
        }
        switch (state) {
            case MENU:
                if (k == KeyEvent.VK_UP) moveMenu(-1);
                else if (k == KeyEvent.VK_DOWN) moveMenu(1);
                else if (k == KeyEvent.VK_ENTER) activateMenu(menuIndex);
                break;
            case PLAYING:
                if (k == KeyEvent.VK_ESCAPE && !ufo.isDead()) pauseGame();
                break;
            case PAUSED:
                if (k == KeyEvent.VK_UP) pauseIndex = (pauseIndex + 2) % 3;
                else if (k == KeyEvent.VK_DOWN) pauseIndex = (pauseIndex + 1) % 3;
                else if (k == KeyEvent.VK_ENTER) activatePause(pauseIndex);
                else if (k == KeyEvent.VK_ESCAPE) resumeGame();
                break;
            case HIGH_SCORE:
            case HOW_TO_PLAY:
                if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_ESCAPE) state = GameState.MENU;
                break;
            case LEVEL_CLEAR:
                if (k == KeyEvent.VK_ENTER) startNextLevel();
                else if (k == KeyEvent.VK_ESCAPE) returnToMenu();
                break;
            case VICTORY:
            case GAME_OVER:
                if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_RIGHT) overIndex = 1 - overIndex;
                else if (k == KeyEvent.VK_ENTER) activateOver(overIndex);
                else if (k == KeyEvent.VK_ESCAPE) returnToMenu();
                break;
            default:
                break;
        }
    }

    private void moveMenu(int d) {
        for (int i = 1; i <= MENU_LABELS.length; i++) {
            int n = (menuIndex + d * i + MENU_LABELS.length * 4) % MENU_LABELS.length;
            if (MENU_ENABLED[n]) {
                menuIndex = n;
                return;
            }
        }
    }

    private void activateMenu(int i) {
        if (!MENU_ENABLED[i]) return;
        if (i == 0) startGame();
        else if (i == 1) state = GameState.HOW_TO_PLAY;
        else if (i == 2) state = GameState.HIGH_SCORE;
        else if (i == 3) System.exit(0);
    }

    private void activatePause(int i) {
        if (i == 0) resumeGame();
        else if (i == 1) startGame();
        else returnToMenu();
    }

    private void activateOver(int i) {
        if (i == 0) startGame();
        else returnToMenu();
    }

    private void hover(Point p) {
        if (state == GameState.MENU) {
            for (int i = 0; i < menuRects.length; i++) if (MENU_ENABLED[i] && menuRects[i].contains(p)) menuIndex = i;
        } else if (state == GameState.GAME_OVER || state == GameState.VICTORY) {
            for (int i = 0; i < overRects.length; i++) if (overRects[i].contains(p)) overIndex = i;
        } else if (state == GameState.PAUSED) {
            for (int i = 0; i < pauseRects.length; i++) if (pauseRects[i].contains(p)) pauseIndex = i;
        }
    }

    private void click(Point p) {
        if (state == GameState.MENU) {
            for (int i = 0; i < menuRects.length; i++) if (menuRects[i].contains(p)) activateMenu(i);
        } else if (state == GameState.GAME_OVER || state == GameState.VICTORY) {
            for (int i = 0; i < overRects.length; i++) if (overRects[i].contains(p)) activateOver(i);
        } else if (state == GameState.LEVEL_CLEAR && continueRect.contains(p)) {
            startNextLevel();
        } else if (state == GameState.PAUSED) {
            for (int i = 0; i < pauseRects.length; i++) if (pauseRects[i].contains(p)) activatePause(i);
        } else if ((state == GameState.HOW_TO_PLAY || state == GameState.HIGH_SCORE) && backRect.contains(p)) {
            state = GameState.MENU;
        }
    }

    // =====================================================================
    //  วาดภาพ
    // =====================================================================
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        switch (state) {
            case MENU: paintMenu(g); break;
            case PLAYING: paintPlaying(g); break;
            case HOW_TO_PLAY: paintHowToPlay(g); break;
            case HIGH_SCORE: paintHighScore(g); break;
            case PAUSED: paintPause(g); break;
            case LEVEL_CLEAR: paintLevelClear(g); break;
            case GAME_OVER: paintResult(g, "GAME OVER", "Reached LEVEL " + levels.getLevel() + " / " + Config.TOTAL_LEVELS); break;
            case VICTORY: paintResult(g, "VICTORY!", "All " + Config.TOTAL_LEVELS + " levels cleared"); break;
            default: break;
        }
        g.dispose();
        Toolkit.getDefaultToolkit().sync();
    }

    private void paintScene(Graphics2D g) {
        Graphics2D s = (Graphics2D) g.create();
        if (shake > 0 && state != GameState.PAUSED) {
            double m = 7 * shake / 0.35;
            s.translate((rnd.nextDouble() - 0.5) * 2 * m, (rnd.nextDouble() - 0.5) * 2 * m);
        }
        background.draw(s);
        ufo.draw(s);
        for (Bird b : birds) b.draw(s);
        for (Laser l : lasers) l.draw(s);
        for (Particle pt : particles) pt.draw(s);
        for (FloatingText t : texts) t.draw(s);
        s.dispose();
    }

    private void paintPlaying(Graphics2D g) {
        paintScene(g);
        paintVignette(g);
        paintHud(g);
        if (introTimer > 0) {   // ป้าย LEVEL n ตอนเริ่มด่าน (ค่อยๆ จางหาย)
            float a = (float) Math.min(1.0, introTimer / 0.5);
            Graphics2D s = (Graphics2D) g.create();
            s.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            drawOutlined(s, "LEVEL " + levels.getLevel(), Config.WIDTH / 2, 280, new Font(Font.MONOSPACED, Font.BOLD, 80), new Color(255, 205, 70), new Color(60, 20, 40));
            s.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
            s.setColor(Color.WHITE);
            drawCentered(s, "Type the words on the birds!", Config.WIDTH / 2, 330);
            s.dispose();
        }
    }

    private void paintHud(Graphics2D g) {
        Font f = new Font(Font.MONOSPACED, Font.BOLD, 16);
        g.setFont(f);
        // HP (ซ้ายบน)
        drawBox(g, 16, 14, 300, 32, C_PAPER);
        g.setColor(C_INK);
        g.drawString("HP", 28, 36);
        int bx = 62, by = 22, bw = 240, bh = 16;
        g.setColor(new Color(60, 50, 70));
        g.fillRect(bx, by, bw, bh);
        int hp = ufo.getHp();
        g.setColor(hp > 60 ? new Color(80, 215, 90) : hp > 30 ? new Color(240, 200, 60) : new Color(230, 60, 50));
        g.fillRect(bx, by, bw * hp / Config.UFO_MAX_HP, bh);
        g.setColor(Color.WHITE);
        String hpText = hp + "/" + Config.UFO_MAX_HP;
        g.drawString(hpText, bx + (bw - g.getFontMetrics().stringWidth(hpText)) / 2, by + 13);
        // LEVEL + ความคืบหน้า x/20 (กลางบน)
        drawBox(g, Config.WIDTH / 2 - 100, 14, 200, 50, C_PAPER);
        g.setColor(C_INK);
        drawCentered(g, "LEVEL " + levels.getLevel() + "/" + Config.TOTAL_LEVELS, Config.WIDTH / 2, 36);
        drawCentered(g, levels.getKills() + "/" + Config.BIRDS_PER_LEVEL, Config.WIDTH / 2, 56);
        // SCORE / TIME (ขวาบน)
        drawBox(g, Config.WIDTH - 246, 14, 230, 32, C_PAPER);
        drawBox(g, Config.WIDTH - 246, 52, 230, 32, C_PAPER);
        g.setColor(C_INK);
        g.drawString("SCORE", Config.WIDTH - 232, 36);
        g.drawString(String.valueOf(score.getScore()), Config.WIDTH - 130, 36);
        g.drawString("TIME", Config.WIDTH - 232, 74);
        g.drawString(score.formatTime(), Config.WIDTH - 130, 74);
        g.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString("ESC = Pause", 17, Config.HEIGHT - 13);
        g.setColor(new Color(255, 235, 200));
        g.drawString("ESC = Pause", 16, Config.HEIGHT - 14);
    }

    private void paintMenu(Graphics2D g) {
        background.draw(g);
        g.setColor(new Color(0, 0, 0, 90));
        for (int i = 0; i < silX.length; i++) {
            int w = 150, h = w * silhouette.getHeight() / silhouette.getWidth();
            g.drawImage(silhouette, (int) silX[i], (int) silY[i], w, h, null);
        }
        menuUfo.draw(g);
        drawOutlined(g, "TYPE-SKYSAURUS", Config.WIDTH / 2, 135, new Font(Font.MONOSPACED, Font.BOLD, 68), new Color(255, 205, 70), new Color(60, 20, 40));
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        g.setColor(new Color(255, 235, 200));
        drawCentered(g, "Java OOP Typing Shooter", Config.WIDTH / 2, 176);
        for (int i = 0; i < menuRects.length; i++) {
            Rectangle r = menuRects[i];
            boolean sel = i == menuIndex && MENU_ENABLED[i];
            drawBox(g, r.x, r.y, r.width, r.height, sel ? new Color(255, 225, 120) : C_PAPER);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
            g.setColor(MENU_ENABLED[i] ? C_INK : new Color(150, 140, 160));
            String label = MENU_LABELS[i] + (MENU_ENABLED[i] ? "" : "  (soon)");
            drawCentered(g, label, r.x + r.width / 2, r.y + 34);
        }
        g.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
        g.setColor(new Color(255, 235, 200));
        drawCentered(g, "Switch your keyboard to English before playing!  (เปลี่ยนคีย์บอร์ดเป็นภาษาอังกฤษก่อนเล่น)", Config.WIDTH / 2, 536);
        drawCentered(g, "UP / DOWN + ENTER or click      F2 = Sound " + (SoundManager.isMuted() ? "OFF" : "ON"), Config.WIDTH / 2, 558);
        if (scores.getBest() > 0) {
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
            g.setColor(new Color(255, 215, 80));
            drawCentered(g, "BEST SCORE  " + scores.getBest(), Config.WIDTH / 2, 212);
        }
    }

    /** หน้าสรุปผลสุดท้าย ใช้ร่วมกันระหว่าง GAME OVER และ VICTORY */
    private void paintResult(Graphics2D g, String title, String subtitle) {
        paintScene(g);
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, 0, Config.WIDTH, Config.HEIGHT);
        int cx = Config.WIDTH / 2;
        drawBox(g, cx - 220, 56, 440, 130, C_PAPER);
        g.setColor(C_INK);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 52));
        drawCentered(g, title, cx, 124);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        drawCentered(g, subtitle, cx, 160);
        String[][] stats = {
            {"SCORE", String.valueOf(score.getScore())},
            {"TIME", score.formatTime()},
            {"BIRDS KILLED", String.valueOf(score.getKills())},
            {"ACCURACY", String.format("%.1f%%", score.getAccuracy())}
        };
        for (int i = 0; i < 4; i++) {
            int x = i % 2 == 0 ? cx - 265 : cx + 15;
            int y = 215 + (i / 2) * 70;
            drawBox(g, x, y, 250, 52, C_PAPER);
            g.setColor(C_INK);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
            g.drawString(stats[i][0], x + 16, y + 32);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
            int w = g.getFontMetrics().stringWidth(stats[i][1]);
            g.drawString(stats[i][1], x + 250 - 16 - w, y + 33);
        }
        String[] labels = {"PLAY AGAIN", "MAIN MENU"};
        for (int i = 0; i < 2; i++) {
            Rectangle r = overRects[i];
            drawBox(g, r.x, r.y, r.width, r.height, i == overIndex ? new Color(255, 225, 120) : C_PAPER);
            g.setColor(C_INK);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
            drawCentered(g, labels[i], r.x + r.width / 2, r.y + 35);
        }
        g.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
        g.setColor(new Color(255, 235, 200));
        if (newRank >= 0) {
            drawOutlined(g, "NEW HIGH SCORE!  RANK #" + (newRank + 1), cx, 486, new Font(Font.MONOSPACED, Font.BOLD, 26), new Color(255, 215, 80), new Color(60, 20, 40));
            g.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
        }
        g.setColor(new Color(255, 235, 200));
        drawCentered(g, "ENTER = Play Again     ESC = Main Menu", cx, 524);
    }

    /** หน้า LEVEL n CLEAR: คะแนนด่าน, Accuracy, โบนัส, HP ที่ฟื้น, คะแนนรวม */
    private void paintLevelClear(Graphics2D g) {
        paintScene(g);
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, 0, Config.WIDTH, Config.HEIGHT);
        int cx = Config.WIDTH / 2;
        drawBox(g, cx - 250, 50, 500, 100, C_PAPER);
        g.setColor(C_INK);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 44));
        drawCentered(g, "LEVEL " + levels.getLevel() + " CLEAR", cx, 115);
        drawBox(g, cx - 250, 175, 500, 230, C_PAPER);
        String[][] rows = {
            {"LEVEL SCORE", String.valueOf(sumLevelScore)},
            {"ACCURACY", String.format("%.1f%%", sumAccuracy)},
            {"ACCURACY BONUS", "+" + sumBonus},
            {"HP RESTORED", "+" + sumHeal},
            {"TOTAL SCORE", String.valueOf(score.getScore())}
        };
        for (int i = 0; i < rows.length; i++) {
            int y = 213 + i * 38;
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, i == rows.length - 1 ? 22 : 18));
            g.setColor(i == rows.length - 1 ? new Color(190, 90, 20) : C_INK);
            g.drawString(rows[i][0], cx - 225, y + 14);
            int w = g.getFontMetrics().stringWidth(rows[i][1]);
            g.drawString(rows[i][1], cx + 225 - w, y + 14);
        }
        drawBox(g, continueRect.x, continueRect.y, continueRect.width, continueRect.height, new Color(255, 225, 120));
        g.setColor(C_INK);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        drawCentered(g, "NEXT LEVEL (ENTER)", continueRect.x + continueRect.width / 2, continueRect.y + 35);
        g.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
        g.setColor(new Color(255, 235, 200));
        drawCentered(g, "ESC = Main Menu", cx, 512);
    }

    /** ขอบจอแดง: วูบตอนโดนจิก และเต้นเป็นจังหวะเมื่อ HP ต่ำ */
    private void paintVignette(Graphics2D g) {
        double a = hitFlash > 0 ? hitFlash / 0.35 * 0.75 : 0;
        if (ufo.getHp() > 0 && ufo.getHp() <= Config.LOW_HP) a = Math.max(a, 0.25 + 0.2 * Math.sin(clock * 6));
        if (a <= 0.01) return;
        Paint old = g.getPaint();
        g.setPaint(new RadialGradientPaint(new Point2D.Float(Config.WIDTH / 2f, Config.HEIGHT / 2f), 640f,
                new float[]{0.5f, 1f}, new Color[]{new Color(255, 0, 0, 0), new Color(255, 20, 20, (int) (255 * Math.min(1, a)))}));
        g.fillRect(0, 0, Config.WIDTH, Config.HEIGHT);
        g.setPaint(old);
    }

    private void paintPause(Graphics2D g) {
        paintScene(g);
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect(0, 0, Config.WIDTH, Config.HEIGHT);
        int cx = Config.WIDTH / 2;
        drawBox(g, cx - 200, 90, 400, 100, C_PAPER);
        g.setColor(C_INK);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 48));
        drawCentered(g, "PAUSED", cx, 155);
        for (int i = 0; i < pauseRects.length; i++) {
            Rectangle r = pauseRects[i];
            drawBox(g, r.x, r.y, r.width, r.height, i == pauseIndex ? new Color(255, 225, 120) : C_PAPER);
            g.setColor(C_INK);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
            drawCentered(g, PAUSE_LABELS[i], r.x + r.width / 2, r.y + 34);
        }
        g.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
        g.setColor(new Color(255, 235, 200));
        drawCentered(g, "UP / DOWN + ENTER     ESC = Resume", cx, 470);
    }

    private void paintHowToPlay(Graphics2D g) {
        background.draw(g);
        g.setColor(new Color(0, 0, 0, 110));
        g.fillRect(0, 0, Config.WIDTH, Config.HEIGHT);
        int cx = Config.WIDTH / 2;
        drawBox(g, cx - 380, 24, 760, 66, C_PAPER);
        g.setColor(C_INK);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 36));
        drawCentered(g, "HOW TO PLAY", cx, 72);
        drawBox(g, cx - 380, 104, 760, 364, C_PAPER);
        String[] lines = {
            "1.  นกไดโนเสาร์บินเข้ามาจากขวา พร้อมคำศัพท์ Java บนตัวนก",
            "2.  พิมพ์ตามคำให้ตรงเป๊ะ รวมตัวใหญ่ (กด Shift) ตัวที่ถูกจะเป็นสีส้ม",
            "3.  พิมพ์ครบทั้งคำ UFO จะยิงเลเซอร์ใส่นกตัวนั้นทันที",
            "4.  พิมพ์ตัวอักษรแรกของคำไหน = ล็อกเป้านกตัวนั้น (Backspace = ปลดล็อก)",
            "5.  พิมพ์ผิด (รวมถึงตัวพิมพ์เล็ก-ใหญ่ไม่ตรง) นกตัวที่ล็อกอยู่จะบินเร็วขึ้น",
            "6.  นกบินถึง UFO = HP -20 (HP เริ่มต้น 100 หมดแล้วจบเกม)",
            "7.  ฆ่านกครบ 20 ตัวผ่านด่าน (ทั้งหมด 5 ด่าน) ได้โบนัสตาม Accuracy และฟื้น HP +20",
            "8.  กด ESC ระหว่างเล่นเพื่อ Pause"
        };
        g.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        g.setColor(C_INK);
        for (int i = 0; i < lines.length; i++) g.drawString(lines[i], cx - 355, 144 + i * 36);
        // ตัวอย่างสีของตัวอักษร
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
        int ex = cx - 355, ey = 446;
        g.setColor(C_INK);
        g.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
        g.drawString("ตัวอย่าง:", ex, ey);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
        g.setColor(new Color(20, 10, 30, 190));
        g.fillRoundRect(ex + 90, ey - 22, 110, 32, 10, 10);
        g.setColor(C_ORANGE);
        g.drawString("ma", ex + 100, ey);
        g.setColor(Color.WHITE);
        g.drawString("in", ex + 100 + 2 * g.getFontMetrics().charWidth('M'), ey);
        g.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
        g.setColor(C_INK);
        g.drawString("ส้ม = พิมพ์ถูกแล้ว   ขาว = ยังไม่ได้พิมพ์", ex + 230, ey);
        drawBox(g, backRect.x, backRect.y, backRect.width, backRect.height, new Color(255, 225, 120));
        g.setColor(C_INK);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        drawCentered(g, "BACK (ENTER)", backRect.x + backRect.width / 2, backRect.y + 33);
    }

    private void paintHighScore(Graphics2D g) {
        background.draw(g);
        g.setColor(new Color(0, 0, 0, 110));
        g.fillRect(0, 0, Config.WIDTH, Config.HEIGHT);
        int cx = Config.WIDTH / 2;
        drawBox(g, cx - 380, 24, 760, 66, C_PAPER);
        g.setColor(C_INK);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 36));
        drawCentered(g, "HIGH SCORE", cx, 72);
        drawBox(g, cx - 380, 104, 760, 364, C_PAPER);
        int[] colX = {cx - 340, cx - 250, cx - 60, cx + 70, cx + 170};
        String[] head = {"#", "SCORE", "LEVEL", "KILLS", "DATE"};
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        g.setColor(new Color(120, 90, 140));
        for (int i = 0; i < head.length; i++) g.drawString(head[i], colX[i], 150);
        java.util.List<HighScoreManager.Entry> list = scores.getEntries();
        for (int r = 0; r < HighScoreManager.MAX_ENTRIES; r++) {
            int y = 205 + r * 50;
            g.setColor(r == 0 ? new Color(190, 90, 20) : C_INK);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
            g.drawString(String.valueOf(r + 1), colX[0], y);
            if (r < list.size()) {
                HighScoreManager.Entry e = list.get(r);
                g.drawString(String.valueOf(e.score), colX[1], y);
                g.drawString(e.level + "/" + Config.TOTAL_LEVELS, colX[2], y);
                g.drawString(String.valueOf(e.kills), colX[3], y);
                g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
                g.drawString(e.date, colX[4], y);
            } else {
                g.setColor(new Color(160, 150, 170));
                g.drawString("---", colX[1], y);
            }
        }
        drawBox(g, backRect.x, backRect.y, backRect.width, backRect.height, new Color(255, 225, 120));
        g.setColor(C_INK);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        drawCentered(g, "BACK (ENTER)", backRect.x + backRect.width / 2, backRect.y + 33);
    }

    // ---------- ตัวช่วยวาด ----------
    private static void notched(Graphics2D g, int x, int y, int w, int h, int n) {
        g.fillRect(x + n, y, w - 2 * n, h);
        g.fillRect(x, y + n, w, h - 2 * n);
    }

    /** กล่อง pixel มุมบากและขอบดำ */
    private static void drawBox(Graphics2D g, int x, int y, int w, int h, Color fill) {
        g.setColor(Color.BLACK);
        notched(g, x, y, w, h, 4);
        g.setColor(fill);
        notched(g, x + 4, y + 4, w - 8, h - 8, 4);
        g.setColor(C_PAPER_SHADE);
        g.fillRect(x + 4, y + h - 10, w - 8, 6);
    }

    private static void drawCentered(Graphics2D g, String s, int cx, int baseline) {
        g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, baseline);
    }

    private static void drawOutlined(Graphics2D g, String s, int cx, int baseline, Font font, Color fill, Color outline) {
        g.setFont(font);
        int x = cx - g.getFontMetrics().stringWidth(s) / 2;
        g.setColor(outline);
        for (int dx = -3; dx <= 3; dx += 3) for (int dy = -3; dy <= 3; dy += 3) g.drawString(s, x + dx, baseline + dy);
        g.setColor(new Color(200, 70, 40));
        g.drawString(s, x + 2, baseline + 4);
        g.setColor(fill);
        g.drawString(s, x, baseline);
    }
}
