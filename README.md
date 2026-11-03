# Type-Skysaurus (ฉบับสมบูรณ์ เฟส 1-4)

เกมพิมพ์ดีด arcade เขียนด้วย Java + Swing (ไม่ต้องใช้ไลบรารีเพิ่ม ใช้ JDK 17 ขึ้นไป)
มนุษย์ต่างดาวขับ UFO ไปปลุกฝูงนกไดโนเสาร์ ต้องพิมพ์คำศัพท์ Java บนตัวนกให้ถูกเพื่อยิงเลเซอร์เอาตัวรอด

## วิธีรัน
Windows: ดับเบิลคลิก `run.bat`   |   Mac/Linux: `./run.sh`

หรือพิมพ์เอง (ที่โฟลเดอร์โปรเจกต์):
    javac -encoding UTF-8 -d out src/*.java
    java -cp out Main

ถ้าใช้ IDE (IntelliJ/Eclipse/NetBeans): สร้างโปรเจกต์ใหม่ ชี้ source ไปที่ `src/`
ตั้ง working directory เป็นโฟลเดอร์โปรเจกต์ (ที่มี `res/`) และตั้ง encoding เป็น UTF-8

## วิธีเล่น
- พิมพ์คำบนตัวนก (สลับคีย์บอร์ดเป็นภาษาอังกฤษก่อน) พิมพ์ตัวแรกของคำไหน = ล็อกเป้านกตัวนั้น
- **ตัวพิมพ์เล็ก-ใหญ่ต้องตรงเป๊ะ** เช่น `Polymorphism` ต้องกด Shift พิมพ์ P ตัวใหญ่ พิมพ์ผิดตัวถือว่าผิด
- พิมพ์ผิด นกตัวที่ล็อกจะเร็วขึ้น | นกบินถึง UFO = HP -20 (HP 100)
- ฆ่านกครบ 20 ตัวผ่านด่าน ทั้งหมด 5 ด่าน (ด่าน 5 ตัวที่ 20 เป็นนกบอส ได้คะแนน x2)
- Backspace = ปลดล็อกเป้า | Esc = Pause | F2 = เปิด/ปิดเสียง

## ไฟล์ที่ปรับแต่งได้
- `src/Config.java`: ค่าสมดุลเกมทั้งหมด (HP, ความเร็วนก `BIRD_SPEED_SCALE`, ค่า k ต่อด่าน, โบนัส ฯลฯ)
- `res/words.txt`: คำศัพท์ 150 คำ หนึ่งคำต่อบรรทัด (ระดับคำแบ่งตามความยาวอัตโนมัติ)
  ง่าย SHORT 2-4 ตัว | กลาง MEDIUM 5-8 ตัว | ยาก LONG 9-11, XLONG 12-14, VERYLONG 15+
  ระดับคำตามด่าน (แก้ได้ที่ `LevelManager.chooseWordType`):
  ด่าน 1 ง่าย | ด่าน 2 ง่าย+กลาง | ด่าน 3 กลาง+ยาก(9-11) | ด่าน 4 ยาก 9-11 + 12-14 | ด่าน 5 ยาก 12-14 + 15+ และบอส
- `res/images/`: ufo.png, ufo_red.png, bird.png, background.png (ถ้าลบ/ไม่มี เกมวาด placeholder ให้)
- `res/sounds/`: (ไม่บังคับ) วาง .wav ชื่อ correct, wrong, laser, hit, levelClear, victory, gameOver, music
  เพื่อแทนเสียงที่เกมสังเคราะห์เอง
- `highscore.txt`: สร้างอัตโนมัติ เก็บ 5 คะแนนสูงสุด (ลบไฟล์นี้เพื่อล้างคะแนน)

## โครงสร้างคลาส (OOP)
- Interface: `Drawable` (update/draw), `InputHandler.Listener`
- Abstract class + Inheritance + Polymorphism: `Bird` -> `NormalBird`, `BossBird`
- Encapsulation: ตัวแปร private + getter/เมธอดควบคุม ในทุกคลาส (เช่น `Ufo`, `ScoreManager`, `Bird`)
- Manager: `WordManager`, `ScoreManager`, `LevelManager`, `HighScoreManager`, `SoundManager`, `Spawner`
- UI/วาด: `GamePanel`, `GameFrame`, `Background`, `Laser`, `Particle`, `FloatingText`
- ค่าคงที่/ตัวช่วย: `Config`, `GameState`, `WordType`, `ImageLoader`, `Resources`
