import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class SnakeDijkstraGUI extends JFrame {

    // ==========================================
    // 1. CONFIG & THEME
    // ==========================================
    public static class UITheme {
        public static final Color BG_DARK = Color.decode("#1E1E2E");
        public static final Color BG_PANEL = Color.decode("#252538");
        public static final Color TEXT_MAIN = Color.decode("#CDD6F4");
        public static final Color TEXT_SUB = Color.decode("#A6ADC8");
        public static final Color ACCENT = Color.decode("#FAB387");
        public static final Color BUTTON_GREEN = Color.decode("#A6E3A1");
        public static final Color BUTTON_RED = Color.decode("#F38BA8");
        public static final Color BUTTON_BLUE = Color.decode("#89B4FA");

        public static void applyTheme() {
            UIManager.put("Panel.background", BG_PANEL);
            UIManager.put("Label.foreground", TEXT_MAIN);
            UIManager.put("OptionPane.background", BG_PANEL);
            UIManager.put("OptionPane.messageForeground", TEXT_MAIN);
            UIManager.put("TextField.background", BG_DARK);
            UIManager.put("TextField.foreground", Color.WHITE);
            UIManager.put("TextField.caretForeground", Color.WHITE);
            UIManager.put("Button.background", Color.WHITE);
        }
    }

    // ==========================================
    // 2. HIGH SCORE MANAGER
    // ==========================================
    public static class HighScoreManager {
        private static final String FILE_NAME = "snake_highscores.properties";
        private Properties scores = new Properties();

        public HighScoreManager() {
            loadScores();
        }

        private void loadScores() {
            try (FileInputStream fis = new FileInputStream(FILE_NAME)) {
                scores.load(fis);
            } catch (IOException e) {
                System.out.println("Creating new highscore file.");
            }
        }

        public int getScore(String name) {
            return Integer.parseInt(scores.getProperty(name.toLowerCase(), "0"));
        }

        public void saveScore(String name, int newScore) {
            String key = name.toLowerCase();
            int currentBest = getScore(key);
            if (newScore > currentBest) {
                scores.setProperty(key, String.valueOf(newScore));
                try (FileOutputStream fos = new FileOutputStream(FILE_NAME)) {
                    scores.store(fos, "Snake Game High Scores");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class Node {
        int id, row, col, pointValue;
        public Node(int id, int row, int col) {
            this.id = id; this.row = row; this.col = col;
            this.pointValue = (int)(Math.random() * 3) + 1;
        }
    }

    public static class AppFonts {
        public static Font REGULAR = new Font("Segoe UI", Font.PLAIN, 14);
        public static Font BOLD    = new Font("Segoe UI", Font.BOLD, 14);
        public static Font MONO    = new Font("Consolas", Font.PLAIN, 12);
        static {
            loadCustomFont("Geist-Regular.ttf", "REGULAR");
            loadCustomFont("Geist-Bold.ttf", "BOLD");
            loadCustomFont("GeistMono-Regular.ttf", "MONO");
        }
        private static void loadCustomFont(String fName, String type) {
            try {
                File f = new File(fName);
                if (f.exists()) {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, f).deriveFont(type.equals("MONO")?12f:14f);
                    if (type.equals("BOLD")) font = font.deriveFont(Font.BOLD);
                    if (type.equals("REGULAR")) REGULAR = font; else if (type.equals("BOLD")) BOLD = font; else MONO = font;
                }
            } catch (Exception e) {}
        }
    }

    public static class SoundManager {
        public static void play(String fName) {
            new Thread(() -> {
                try {
                    File f = new File(fName);
                    if (f.exists()) {
                        javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
                        clip.open(javax.sound.sampled.AudioSystem.getAudioInputStream(f));
                        clip.start();
                    }
                } catch (Exception e) {}
            }).start();
        }
    }

    // ==========================================
    // 3. VISUAL COMPONENTS
    // ==========================================

    private static class AnimatedBackgroundPanel extends JPanel {
        private final List<Point> stars = new ArrayList<>();
        private final Random rand = new Random();

        public AnimatedBackgroundPanel() {
            for(int i=0; i<50; i++) stars.add(new Point(rand.nextInt(1200), rand.nextInt(900)));
            new javax.swing.Timer(100, e -> repaint()).start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gp = new GradientPaint(0, 0, Color.decode("#0f2027"), getWidth(), getHeight(), Color.decode("#2c5364"));
            g2.setPaint(gp); g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(255, 255, 255, 10));
            g2.fillOval(-50, -50, 300, 300); g2.fillOval(getWidth()-200, getHeight()-200, 400, 400);
            g2.setColor(new Color(255,255,255, 100));
            for(Point p : stars) {
                g2.fillOval(p.x, p.y, 3, 3);
                p.y -= 1; if(p.y < 0) p.y = getHeight();
            }
        }
    }

    private static class GradientPanel extends JPanel {
        private final Color centerColor, edgeColor;
        private List<Integer> playersHere = new ArrayList<>();
        private int pointValue;

        public GradientPanel(Color c, Color e, int p) {
            this.centerColor = c; this.edgeColor = e; this.pointValue = p;
            setOpaque(false);
        }
        public void setPlayersHere(List<Integer> p) { playersHere.clear(); playersHere.addAll(p); repaint(); }
        public void addPlayer(int p) { if(!playersHere.contains(p)) { playersHere.add(p); repaint(); } }
        public void removePlayer(int p) { playersHere.remove(Integer.valueOf(p)); repaint(); }

        public static Color getPlayerColor(int id) {
            switch (id) {
                case 1: return Color.decode("#FF5252"); case 2: return Color.decode("#448AFF");
                case 3: return Color.decode("#69F0AE"); case 4: return Color.decode("#FFAB40");
                default: return Color.GRAY;
            }
        }

        public static void drawPawnStatic(Graphics2D g2, int x, int y, int size, Color color, String label) {
            g2.setColor(new Color(0,0,0,60)); g2.fillOval(x+2, y+2, size, size);
            g2.setColor(color); g2.fillOval(x, y, size, size);
            g2.setColor(new Color(255,255,255,150)); g2.setStroke(new BasicStroke(2f)); g2.drawOval(x, y, size, size);
            g2.setFont(AppFonts.BOLD.deriveFont(10f)); g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, x+(size-fm.stringWidth(label))/2, y+(size-fm.getHeight())/2+fm.getAscent());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            RadialGradientPaint p = new RadialGradientPaint(new Point2D.Float(w/2f, h/2f), Math.max(w,h),
                    new float[]{0.0f, 1.0f}, new Color[]{centerColor, edgeColor});
            g2.setPaint(p); g2.fillRect(0, 0, w, h);

            int ptS = 20, ptX = 4, ptY = h - 24;
            g2.setColor(new Color(255, 215, 0)); g2.fillOval(ptX, ptY, ptS, ptS);
            g2.setColor(new Color(184, 134, 11)); g2.setStroke(new BasicStroke(1f)); g2.drawOval(ptX, ptY, ptS, ptS);
            g2.setColor(Color.BLACK); g2.setFont(AppFonts.BOLD.deriveFont(10f));
            String pts = String.valueOf(pointValue);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(pts, ptX+(ptS-fm.stringWidth(pts))/2, ptY+(ptS-fm.getHeight())/2+fm.getAscent()-2);

            int size = w / 3; int margin = 6;
            int[][] pos = {{margin, margin + 8}, {w - size - margin, margin + 8}, {margin, h - size - margin}, {w - size - margin, h - size - margin}};

            for(int pid : playersHere) {
                if(pid <= 4) drawPawnStatic(g2, pos[pid-1][0], pos[pid-1][1], size, getPlayerColor(pid), "P"+pid);
            }
        }
    }

    private class AnimationPanel extends JPanel {
        private boolean anim = false; private int pid, ax, ay;
        public AnimationPanel() { setOpaque(false); }
        public void updatePawn(int id, int x, int y) { anim = true; pid = id; ax = x; ay = y; repaint(); }
        public void stop() { anim = false; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if(anim) {
                Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int boxW = getWidth()/8; int sz = boxW / 3;
                GradientPanel.drawPawnStatic(g2, ax-sz/2, ay-sz/2, sz, GradientPanel.getPlayerColor(pid), "P"+pid);
            }
        }
    }

    private class BoardDrawingPanel extends JPanel {
        public BoardDrawingPanel(GridLayout l) { super(l); }
        @Override public void paint(Graphics g) {
            super.paint(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for(Map.Entry<Integer, Integer> e : shortcuts.entrySet()) {
                GradientPanel s = panelMap.get(e.getKey());
                GradientPanel d = panelMap.get(e.getValue());
                if(s!=null && d!=null) {
                    Point p1 = SwingUtilities.convertPoint(s, s.getWidth()/2, s.getHeight()/2, this);
                    Point p2 = SwingUtilities.convertPoint(d, d.getWidth()/2, d.getHeight()/2, this);
                    drawLadder(g2, p1, p2);
                }
            }
        }
        private void drawLadder(Graphics2D g2, Point p1, Point p2) {
            double dx = p2.x-p1.x, dy = p2.y-p1.y, dist = Math.sqrt(dx*dx+dy*dy);
            if(dist<20) return;
            double ux = dx/dist, uy = dy/dist, px = -uy*12, py = ux*12;
            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(0,0,0,80));
            g2.drawLine((int)(p1.x+px+2), (int)(p1.y+py+2), (int)(p2.x+px+2), (int)(p2.y+py+2));
            g2.drawLine((int)(p1.x-px+2), (int)(p1.y-py+2), (int)(p2.x-px+2), (int)(p2.y-py+2));
            g2.setColor(new Color(101,67,33));
            g2.drawLine((int)(p1.x+px), (int)(p1.y+py), (int)(p2.x+px), (int)(p2.y+py));
            g2.drawLine((int)(p1.x-px), (int)(p1.y-py), (int)(p2.x-px), (int)(p2.y-py));
            g2.setStroke(new BasicStroke(4f));
            for(double t=25; t<dist-10; t+=25) {
                double cx = p1.x+ux*t, cy = p1.y+uy*t;
                g2.setColor(new Color(0,0,0,80)); g2.drawLine((int)(cx+px+1), (int)(cy+py+2), (int)(cx-px+1), (int)(cy-py+2));
                g2.setColor(new Color(160,112,66)); g2.drawLine((int)(cx+px), (int)(cy+py), (int)(cx-px), (int)(cy-py));
            }
        }
    }

    private class SportsScoreboardPanel extends JPanel {
        private JPanel[] playerPanels;
        private JLabel[] scoreLabels;
        private JLabel[] nameLabels;
        private JLabel[] highscoreLabels;

        public SportsScoreboardPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(UITheme.BG_PANEL); setOpaque(false);
        }
        public void init(int num, String[] names, HighScoreManager mgr) {
            removeAll();
            playerPanels = new JPanel[num]; scoreLabels = new JLabel[num];
            nameLabels = new JLabel[num]; highscoreLabels = new JLabel[num];

            JLabel title = new JLabel("LIVE SCORE");
            title.setFont(AppFonts.BOLD.deriveFont(16f));
            title.setForeground(UITheme.TEXT_SUB);
            title.setAlignmentX(CENTER_ALIGNMENT);
            add(title); add(Box.createRigidArea(new Dimension(0, 10)));
            JPanel grid = new JPanel(new GridLayout(0, 2, 10, 10));
            grid.setOpaque(false);

            for(int i=0; i<num; i++) {
                int pid = i+1;
                JPanel p = new JPanel(new BorderLayout());
                p.setBackground(UITheme.BG_DARK);
                p.setBorder(new CompoundBorder(new LineBorder(GradientPanel.getPlayerColor(pid), 1, true), new EmptyBorder(5,10,5,10)));
                p.setPreferredSize(new Dimension(130, 70));

                JPanel info = new JPanel(new GridLayout(2, 1));
                info.setOpaque(false);
                JLabel n = new JLabel(names[i]);
                n.setFont(AppFonts.BOLD.deriveFont(13f)); n.setForeground(GradientPanel.getPlayerColor(pid));
                JLabel best = new JLabel("Best: " + mgr.getScore(names[i]));
                best.setFont(AppFonts.REGULAR.deriveFont(10f)); best.setForeground(Color.GRAY);
                info.add(n); info.add(best);

                JLabel s = new JLabel("0");
                s.setFont(AppFonts.MONO.deriveFont(28f)); s.setForeground(Color.WHITE); s.setHorizontalAlignment(SwingConstants.RIGHT);

                p.add(info, BorderLayout.WEST); p.add(s, BorderLayout.EAST);
                playerPanels[i] = p; scoreLabels[i] = s; nameLabels[i] = n; highscoreLabels[i] = best;
                grid.add(p);
            }
            add(grid); revalidate(); repaint();
        }
        public void updateScores(int[] scores) {
            if(scoreLabels==null) return;
            for(int i=0; i<scores.length && i<scoreLabels.length; i++) scoreLabels[i].setText(String.valueOf(scores[i]));
        }
        public void highlight(int pid) {
            if(playerPanels==null) return;
            for(int i=0; i<playerPanels.length; i++) playerPanels[i].setBackground(i==pid-1 ? UITheme.BG_DARK.brighter() : UITheme.BG_DARK);
        }
    }

    // ==========================================
    // 4. MAIN GUI & LOGIC
    // ==========================================
    private static final int SIZE = 8;
    private Node[][] logicBoard = new Node[SIZE][SIZE];
    private Map<Integer, GradientPanel> panelMap = new HashMap<>();
    private int playerCount = 2;
    private String[] playerNames;
    private List<Stack<Integer>> allPlayerStacks = new ArrayList<>();
    private int[] playerScores;
    private Deque<Integer> turnQueue = new ArrayDeque<>();
    private Map<Integer, Integer> shortcuts = new HashMap<>();
    private Random random = new Random();
    private HighScoreManager highScoreManager;
    private boolean inputEnabled = false; // FLAG INPUT

    // UI Components
    private CardLayout cardLayout;
    private JPanel mainContainer, boardPanel;
    private AnimationPanel animationPanel;
    private JLabel statusLabel, diceImageLabel, diceTextLabel;
    private JTextArea historyArea;
    private JButton restartButton;
    private SportsScoreboardPanel scoreboardPanel;

    private final Color blueCenter = Color.decode("#E3F2FD"), blueEdge = Color.decode("#90CAF9");
    private final Color creamCenter = Color.decode("#FFFDE7"), creamEdge = Color.decode("#FFF59D");
    private final Color[] playerTextColors = {Color.decode("#FF5252"), Color.decode("#448AFF"), Color.decode("#69F0AE"), Color.decode("#FFAB40")};

    public SnakeDijkstraGUI() {
        UITheme.applyTheme();
        highScoreManager = new HighScoreManager();
        setTitle("Snake Game: Ultimate Edition");
        setSize(1280, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        mainContainer.add(createMenuPanel(), "MENU");
        mainContainer.add(createGamePanel(), "GAME");
        add(mainContainer);
        setLocationRelativeTo(null);
    }

    private JPanel createMenuPanel() {
        AnimatedBackgroundPanel p = new AnimatedBackgroundPanel();
        p.setLayout(new GridBagLayout());
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(30, 30, 46, 200));
        card.setBorder(new CompoundBorder(new LineBorder(new Color(255,255,255,50), 1, true), new EmptyBorder(40, 60, 40, 60)));

        JLabel t = new JLabel("DICE MASTER"); t.setFont(AppFonts.BOLD.deriveFont(48f)); t.setForeground(UITheme.ACCENT); t.setAlignmentX(CENTER_ALIGNMENT);
        JLabel t2 = new JLabel("SNAKE & LADDER"); t2.setFont(AppFonts.BOLD.deriveFont(32f)); t2.setForeground(Color.WHITE); t2.setAlignmentX(CENTER_ALIGNMENT);
        JLabel s = new JLabel("Strategy Board Game"); s.setFont(AppFonts.REGULAR.deriveFont(16f)); s.setForeground(UITheme.TEXT_SUB); s.setAlignmentX(CENTER_ALIGNMENT);

        JButton b = styleButton("START ADVENTURE", UITheme.BUTTON_GREEN, Color.BLACK);
        b.setPreferredSize(new Dimension(200, 50)); b.setAlignmentX(CENTER_ALIGNMENT);

        b.addActionListener(e -> showPlayerSelectionDialog(count -> {
            playerCount = count;
            askPlayerNames();
            initGameData(); updateGraphics();
            cardLayout.show(mainContainer, "GAME");
            boardPanel.repaint();
            // SETUP SHORTCUT ENTER FOR GAME
            setupGameInput();
        }));

        card.add(t); card.add(t2); card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(s); card.add(Box.createRigidArea(new Dimension(0, 40))); card.add(b);
        p.add(card); return p;
    }

    private void setupGameInput() {
        // GLOBAL ENTER KEY BINDING FOR ROLLING DICE
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "ROLL_DICE");
        am.put("ROLL_DICE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Only roll if input is enabled (not animating)
                if (inputEnabled) playTurn();
            }
        });
    }

    private JPanel createGamePanel() {
        JPanel p = new JPanel(new BorderLayout());
        JLayeredPane lp = new JLayeredPane(); lp.setLayout(new OverlayLayout(lp));
        animationPanel = new AnimationPanel(); lp.add(animationPanel, JLayeredPane.PALETTE_LAYER);
        boardPanel = new BoardDrawingPanel(new GridLayout(SIZE, SIZE));
        boardPanel.setBorder(new LineBorder(UITheme.BG_PANEL, 5));
        initBoardLogic(); initBoardVisual();
        lp.add(boardPanel, JLayeredPane.DEFAULT_LAYER);

        JPanel side = new JPanel(); side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(320, 0)); side.setBorder(new EmptyBorder(30,20,30,20));
        side.setBackground(UITheme.BG_PANEL);

        statusLabel = new JLabel("PLAYER 1 TURN"); statusLabel.setFont(AppFonts.BOLD.deriveFont(24f));
        statusLabel.setForeground(playerTextColors[0]); statusLabel.setAlignmentX(CENTER_ALIGNMENT);

        scoreboardPanel = new SportsScoreboardPanel(); scoreboardPanel.setAlignmentX(CENTER_ALIGNMENT);

        diceImageLabel = new JLabel(createDiceImage(1, 100, Color.BLACK)); diceImageLabel.setAlignmentX(CENTER_ALIGNMENT);
        diceImageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        diceImageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (inputEnabled) playTurn();
            }
        });

        diceTextLabel = new JLabel("Click Dice or Enter"); diceTextLabel.setFont(AppFonts.REGULAR.deriveFont(14f));
        diceTextLabel.setForeground(UITheme.TEXT_MAIN); diceTextLabel.setAlignmentX(CENTER_ALIGNMENT);

        restartButton = styleButton("RESTART GAME", UITheme.BUTTON_RED, Color.BLACK);
        restartButton.setAlignmentX(CENTER_ALIGNMENT); restartButton.setMaximumSize(new Dimension(280, 45));
        restartButton.setFont(AppFonts.BOLD.deriveFont(14f));
        restartButton.setFocusable(false); // CRITICAL: PREVENT ENTER KEY TRIGGER
        restartButton.addActionListener(e -> showRestartConfirmDialog(() -> {
            askPlayerNames();
            initGameData(); updateGraphics(); boardPanel.repaint();
        }));

        historyArea = new JTextArea(10, 1); historyArea.setEditable(false);
        historyArea.setFont(AppFonts.MONO.deriveFont(12f)); historyArea.setBackground(new Color(30,30,46));
        historyArea.setForeground(new Color(166,227,161)); historyArea.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        JScrollPane sp = new JScrollPane(historyArea);
        sp.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.GRAY), "Game Log", 0, 0, AppFonts.BOLD.deriveFont(12f), Color.WHITE));
        sp.setOpaque(false); sp.getViewport().setOpaque(false); sp.setAlignmentX(CENTER_ALIGNMENT);

        side.add(statusLabel); side.add(Box.createRigidArea(new Dimension(0, 15)));
        side.add(scoreboardPanel); side.add(Box.createRigidArea(new Dimension(0, 20)));
        side.add(diceImageLabel); side.add(diceTextLabel); side.add(Box.createRigidArea(new Dimension(0, 20)));
        side.add(Box.createRigidArea(new Dimension(0, 20)));
        side.add(restartButton); side.add(Box.createRigidArea(new Dimension(0, 20)));
        side.add(sp);

        p.add(lp, BorderLayout.CENTER); p.add(side, BorderLayout.EAST);
        return p;
    }

    private void askPlayerNames() {
        playerNames = new String[playerCount];
        for (int i = 0; i < playerCount; i++) {
            String def = "Player " + (i + 1);
            String input = (String) JOptionPane.showInputDialog(this,
                    "Masukkan Nama Player " + (i + 1), "Player Name",
                    JOptionPane.PLAIN_MESSAGE, null, null, def);
            playerNames[i] = (input != null && !input.trim().isEmpty()) ? input.trim() : def;
        }
    }

    private void initGameData() {
        allPlayerStacks.clear(); playerScores = new int[playerCount];
        for(int i=0; i<playerCount; i++) {
            Stack<Integer> s = new Stack<>(); s.push(1);
            allPlayerStacks.add(s); playerScores[i] = 0;
        }
        turnQueue.clear(); for(int i=1; i<=playerCount; i++) turnQueue.add(i);

        inputEnabled = true; // Allow input
        scoreboardPanel.init(playerCount, playerNames, highScoreManager);
        scoreboardPanel.updateScores(playerScores);
        historyArea.setText("Game Started!\n");
        statusLabel.setText(playerNames[0].toUpperCase() + " TURN");
        statusLabel.setForeground(playerTextColors[0]);
        scoreboardPanel.highlight(1); genShortcuts();
        diceImageLabel.setIcon(createDiceImage(1, 100, Color.BLACK));
        diceTextLabel.setText("Click Dice or Enter"); diceTextLabel.setForeground(UITheme.TEXT_MAIN);
    }

    private void genShortcuts() {
        shortcuts.clear();
        while(shortcuts.size()<5) {
            int a = random.nextInt(62)+2, b = random.nextInt(62)+2;
            if(a!=b && !shortcuts.containsKey(Math.min(a,b)) && !shortcuts.containsValue(Math.min(a,b)))
                shortcuts.put(Math.min(a,b), Math.max(a,b));
        }
    }

    private void initBoardLogic() {
        int id = 1;
        for(int r=SIZE-1; r>=0; r--) {
            boolean l2r = (SIZE-1-r)%2==0;
            if(l2r) for(int c=0; c<SIZE; c++) logicBoard[r][c] = new Node(id++, r, c);
            else for(int c=SIZE-1; c>=0; c--) logicBoard[r][c] = new Node(id++, r, c);
        }
    }

    private void initBoardVisual() {
        boardPanel.removeAll(); panelMap.clear();
        for(int r=0; r<SIZE; r++) {
            for(int c=0; c<SIZE; c++) {
                Node n = logicBoard[r][c];
                Color bg = ((r+c)%2==0)? blueCenter : creamCenter;
                Color bd = ((r+c)%2==0)? blueEdge : creamEdge;
                GradientPanel p = new GradientPanel(bg, bd, n.pointValue);
                p.setLayout(new BorderLayout()); p.setBorder(new MatteBorder(1,1,1,1,Color.WHITE));
                JLabel l = new JLabel(String.valueOf(n.id));
                l.setFont(AppFonts.BOLD.deriveFont(14f)); l.setForeground(new Color(80,80,80));
                l.setHorizontalAlignment(SwingConstants.RIGHT); l.setBorder(BorderFactory.createEmptyBorder(6,0,0,8));
                p.add(l, BorderLayout.NORTH); boardPanel.add(p); panelMap.put(n.id, p);
            }
        }
    }

    private void playTurn() {
        if(turnQueue.isEmpty()) return;
        inputEnabled = false; // Disable input
        SoundManager.play("dice.wav");

        javax.swing.Timer t = new javax.swing.Timer(50, null);
        final int[] f = {0};
        t.addActionListener(e -> {
            diceImageLabel.setIcon(createDiceImage(random.nextInt(6)+1, 100, Color.LIGHT_GRAY));
            if(++f[0]>=10) { ((javax.swing.Timer)e.getSource()).stop(); execLogic(); }
        });
        t.start();
    }

    private void execLogic() {
        int pid = turnQueue.pollFirst();
        Stack<Integer> stk = allPlayerStacks.get(pid-1);
        int cur = stk.peek();
        boolean prime = isPrime(cur);
        boolean green = random.nextDouble() < 0.9;
        int val = random.nextInt(6)+1;
        int step = green ? val : -val;

        Color dc = green ? UITheme.BUTTON_GREEN : UITheme.BUTTON_RED;
        diceImageLabel.setIcon(createDiceImage(val, 100, dc));
        diceTextLabel.setText((green?"MAJU":"MUNDUR") + " " + val + " Langkah");
        diceTextLabel.setForeground(green ? Color.GREEN : Color.RED);

        List<Integer> path = genPath(cur, step);
        int linkTarget = -1;

        if(green) {
            for(int i=0; i<path.size(); i++) {
                int n = path.get(i);
                if(shortcuts.containsKey(n) && prime && val > (n-cur)) {
                    linkTarget = shortcuts.get(n); path = path.subList(0, i+1); break;
                }
            }
        }

        final List<Integer> finalPath = path;
        final int finalLink = linkTarget;

        animSeq(pid, cur, finalPath, 0, () -> {
            int end = finalPath.isEmpty() ? cur : finalPath.get(finalPath.size()-1);
            String log = playerNames[pid-1] + ": " + cur + " -> " + end;
            if(finalLink != -1) {
                SoundManager.play("magic.wav");
                log += " (LINK -> "+finalLink+")";
                showStyledInfoDialog("PRIME OVERFLOW!", "Shortest Path Activated!", false);
                String finalLog = log;
                javax.swing.Timer d = new javax.swing.Timer(500, ev -> {
                    ((javax.swing.Timer)ev.getSource()).stop();
                    animMove(pid, end, finalLink, () -> finalizeTurn(pid, finalLink, stk, finalLog));
                });
                d.setRepeats(false); d.start();
            } else {
                finalizeTurn(pid, end, stk, log);
            }
        });
    }

    private void animSeq(int pid, int cur, List<Integer> path, int idx, Runnable done) {
        if(idx>=path.size()) { done.run(); return; }
        int next = path.get(idx);
        animMove(pid, cur, next, () -> animSeq(pid, next, path, idx+1, done));
    }

    private void animMove(int pid, int s, int e, Runnable done) {
        if(s==e) { done.run(); return; }
        SoundManager.play("step.wav");
        GradientPanel ps = panelMap.get(s), pe = panelMap.get(e);
        if(ps==null || pe==null) { done.run(); return; }
        ps.removePlayer(pid);
        Point p1 = SwingUtilities.convertPoint(ps, ps.getWidth()/2, ps.getHeight()/2, animationPanel);
        Point p2 = SwingUtilities.convertPoint(pe, pe.getWidth()/2, pe.getHeight()/2, animationPanel);

        final int frames = 30;
        javax.swing.Timer t = new javax.swing.Timer(10, null);
        t.addActionListener(new ActionListener() {
            int f = 0;
            public void actionPerformed(ActionEvent ev) {
                f++; float r = (float)f/frames; r = r*r*(3-2*r);
                int x = (int)(p1.x + (p2.x-p1.x)*r);
                int y = (int)(p1.y + (p2.y-p1.y)*r);
                animationPanel.updatePawn(pid, x, y);
                if(f>=frames) { ((javax.swing.Timer)ev.getSource()).stop(); pe.addPlayer(pid); animationPanel.stop(); done.run(); }
            }
        });
        t.start();
    }

    private void finalizeTurn(int pid, int pos, Stack<Integer> stk, String log) {
        animationPanel.stop(); stk.push(pos);
        int pts = getPointOfNode(pos);
        playerScores[pid-1] += pts;
        log += " [+" + pts + " pts]";
        historyArea.append(log+"\n");
        scoreboardPanel.updateScores(playerScores);
        updateGraphics();

        if(pos==64) {
            highScoreManager.saveScore(playerNames[pid-1], playerScores[pid-1]);
            showCustomGameOverDialog(pid); return;
        }

        if(pos%5==0 && pos!=1) {
            showStyledInfoDialog("DOUBLE TURN!", "Kelipatan 5 detected.", false);
            turnQueue.addFirst(pid);
        } else {
            turnQueue.addLast(pid);
        }

        int next = turnQueue.peekFirst();
        statusLabel.setText(playerNames[next-1].toUpperCase() + " TURN");
        statusLabel.setForeground(playerTextColors[(next-1)%playerTextColors.length]);
        scoreboardPanel.highlight(next);

        inputEnabled = true; // Re-enable input
    }

    private void showStyledInfoDialog(String title, String msg, boolean warn) {
        JDialog d = new JDialog(this, true); d.setUndecorated(true); d.setBackground(new Color(0,0,0,0));
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(UITheme.BG_DARK);
        p.setBorder(new CompoundBorder(new LineBorder(warn?UITheme.BUTTON_RED:UITheme.ACCENT, 2), new EmptyBorder(20,20,20,20)));
        JLabel t = new JLabel(title); t.setFont(AppFonts.BOLD.deriveFont(20f)); t.setForeground(warn?UITheme.BUTTON_RED:UITheme.ACCENT); t.setAlignmentX(CENTER_ALIGNMENT);
        JLabel m = new JLabel("<html><center>"+msg+"</center></html>"); m.setFont(AppFonts.REGULAR.deriveFont(14f)); m.setForeground(UITheme.TEXT_MAIN); m.setAlignmentX(CENTER_ALIGNMENT);
        JButton b = styleButton("OK", UITheme.BUTTON_BLUE, Color.BLACK); b.setAlignmentX(CENTER_ALIGNMENT); b.addActionListener(e->d.dispose());
        p.add(t); p.add(Box.createRigidArea(new Dimension(0,15))); p.add(m); p.add(Box.createRigidArea(new Dimension(0,20))); p.add(b);
        d.add(p); d.pack(); d.setLocationRelativeTo(this); d.setVisible(true);
    }

    private void showPlayerSelectionDialog(Consumer<Integer> onSel) {
        JDialog d = new JDialog(this, true); d.setUndecorated(true); d.setBackground(new Color(0,0,0,0));
        JPanel p = new JPanel(new GridLayout(0,1,10,10)); p.setBackground(UITheme.BG_DARK);
        p.setBorder(new CompoundBorder(new LineBorder(UITheme.BUTTON_GREEN, 2), new EmptyBorder(20,40,20,40)));
        JLabel l = new JLabel("Pilih Jumlah Pemain"); l.setFont(AppFonts.BOLD.deriveFont(18f)); l.setForeground(Color.WHITE); l.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(l);
        for(int i=2; i<=4; i++) {
            int c = i; JButton b = styleButton(i+" Players", UITheme.BG_PANEL, Color.WHITE);
            b.addActionListener(e->{ d.dispose(); onSel.accept(c); }); p.add(b);
        }
        d.add(p); d.pack(); d.setLocationRelativeTo(this); d.setVisible(true);
    }

    private void showRestartConfirmDialog(Runnable onYes) {
        JDialog d = new JDialog(this, true); d.setUndecorated(true); d.setBackground(new Color(0,0,0,0));
        JPanel c = new JPanel(); c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS)); c.setBackground(UITheme.BG_DARK);
        c.setBorder(new CompoundBorder(new LineBorder(UITheme.BUTTON_RED, 2), new EmptyBorder(20,20,20,20)));
        JLabel l = new JLabel("Ulangi Permainan?"); l.setFont(AppFonts.BOLD.deriveFont(16f)); l.setForeground(Color.WHITE); l.setAlignmentX(CENTER_ALIGNMENT);
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0)); bp.setOpaque(false);
        JButton by = styleButton("Ya", UITheme.BUTTON_GREEN, Color.BLACK); by.addActionListener(e->{d.dispose(); onYes.run();});
        JButton bn = styleButton("Batal", UITheme.BUTTON_RED, Color.BLACK); bn.addActionListener(e->d.dispose());
        bp.add(by); bp.add(bn);
        c.add(l); c.add(Box.createRigidArea(new Dimension(0,20))); c.add(bp);
        d.add(c); d.pack(); d.setLocationRelativeTo(this); d.setVisible(true);
    }

    private void showCustomGameOverDialog(int fin) {
        SoundManager.play("hidup-jokowi.wav");
        JDialog d = new JDialog(this, true); d.setUndecorated(true); d.setBackground(new Color(0,0,0,0));
        JPanel m = new JPanel(); m.setLayout(new BoxLayout(m, BoxLayout.Y_AXIS)); m.setBackground(UITheme.BG_DARK);
        m.setBorder(new CompoundBorder(new LineBorder(UITheme.ACCENT, 2), new EmptyBorder(20,30,20,30)));
        JLabel h = new JLabel("GAME OVER!"); h.setFont(AppFonts.BOLD.deriveFont(32f)); h.setForeground(UITheme.BUTTON_RED); h.setAlignmentX(CENTER_ALIGNMENT);
        JLabel s = new JLabel(playerNames[fin-1] + " Finished!"); s.setFont(AppFonts.REGULAR.deriveFont(16f)); s.setForeground(UITheme.TEXT_MAIN); s.setAlignmentX(CENTER_ALIGNMENT);

        List<Integer> rk = new ArrayList<>(); for(int i=0; i<playerCount; i++) rk.add(i+1);
        rk.sort((p1,p2)->Integer.compare(playerScores[p2-1], playerScores[p1-1]));

        StringBuilder ht = new StringBuilder("<html><table style='width:300px; border-collapse:collapse;'>");
        for(int i=0; i<rk.size(); i++) {
            int pid = rk.get(i), sc = playerScores[pid-1];
            String name = playerNames[pid-1];
            String cl = (i==0)?"#A6E3A1":"#CDD6F4", md=(i==0)?"🏆":"";
            ht.append(String.format("<tr><td style='padding:5px; color:%s; font-size:14px;'>#%d %s</td><td style='text-align:right; color:%s; font-weight:bold;'>%d pts %s</td></tr>", cl, i+1, name, cl, sc, md));
        }
        ht.append("</table></html>");
        JLabel tbl = new JLabel(ht.toString()); tbl.setAlignmentX(CENTER_ALIGNMENT);

        JPanel bp = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); bp.setOpaque(false);
        JButton br = styleButton("Main Lagi", UITheme.BUTTON_GREEN, Color.BLACK); br.setPreferredSize(new Dimension(100,35));
        br.addActionListener(e->{d.dispose(); askPlayerNames(); initGameData(); updateGraphics(); boardPanel.repaint();});
        JButton be = styleButton("Keluar", UITheme.BUTTON_RED, Color.BLACK); be.setPreferredSize(new Dimension(100,35));
        be.addActionListener(e->System.exit(0));
        bp.add(br); bp.add(be);

        m.add(h); m.add(Box.createRigidArea(new Dimension(0,10))); m.add(s); m.add(Box.createRigidArea(new Dimension(0,20)));
        m.add(tbl); m.add(Box.createRigidArea(new Dimension(0,25))); m.add(bp);
        d.add(m); d.pack(); d.setLocationRelativeTo(this); d.setVisible(true);
    }

    private boolean isPrime(int n) { if(n<=1)return false; for(int i=2; i*i<=n; i++) if(n%i==0) return false; return true; }
    private int getPointOfNode(int id) { for(int r=0; r<SIZE; r++) for(int c=0; c<SIZE; c++) if(logicBoard[r][c].id==id) return logicBoard[r][c].pointValue; return 0; }

    // LOGIC BARU: NO BOUNCE
    private List<Integer> genPath(int s, int st) {
        List<Integer> p = new ArrayList<>(); int c = s, m = Math.abs(st), d = st>0?1:-1;
        for(int i=0; i<m; i++) {
            if(c == 64) break; // Finish Condition (Stop at 64)
            if(c == 1 && d == -1) {} else c += d;
            p.add(c);
        }
        return p;
    }

    private void updateGraphics() {
        for(GradientPanel p : panelMap.values()) p.setPlayersHere(new ArrayList<>());
        Map<Integer, List<Integer>> pos = new HashMap<>();
        for(int i=0; i<playerCount; i++) {
            int pid = i+1, loc = allPlayerStacks.get(i).peek();
            pos.putIfAbsent(loc, new ArrayList<>()); pos.get(loc).add(pid);
        }
        for(Map.Entry<Integer, List<Integer>> e : pos.entrySet()) if(panelMap.containsKey(e.getKey())) panelMap.get(e.getKey()).setPlayersHere(e.getValue());
    }

    private JButton styleButton(String t, Color bg, Color fg) {
        JButton b = new JButton(t); b.setFont(AppFonts.BOLD.deriveFont(18f)); b.setBackground(bg); b.setForeground(fg);
        b.setFocusPainted(false); b.setBorderPainted(false); b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setContentAreaFilled(false); b.setOpaque(true);
        b.addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent e){b.setBackground(bg.brighter());} public void mouseExited(MouseEvent e){b.setBackground(bg);} });
        return b;
    }
    private ImageIcon createDiceImage(int v, int s, Color c) {
        BufferedImage i = new BufferedImage(s,s,2); Graphics2D g = i.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0,0,0,50)); g.fillRoundRect(4,4,s-4,s-4,20,20);
        g.setColor(Color.WHITE); g.fillRoundRect(0,0,s-4,s-4,20,20);
        g.setColor(c); g.setStroke(new BasicStroke(4)); g.drawRoundRect(0,0,s-4,s-4,20,20);
        g.setColor(Color.BLACK); int ds=s/5, m=s/2-2, l=s/4-2, r=s*3/4-2;
        if(v%2!=0) g.fillOval(m-ds/2, m-ds/2, ds, ds);
        if(v>=2){ g.fillOval(l-ds/2, l-ds/2, ds, ds); g.fillOval(r-ds/2, r-ds/2, ds, ds); }
        if(v>=4){ g.fillOval(r-ds/2, l-ds/2, ds, ds); g.fillOval(l-ds/2, r-ds/2, ds, ds); }
        if(v==6){ g.fillOval(l-ds/2, m-ds/2, ds, ds); g.fillOval(r-ds/2, m-ds/2, ds, ds); }
        g.dispose(); return new ImageIcon(i);
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception e){}
        SwingUtilities.invokeLater(() -> new SnakeDijkstraGUI().setVisible(true));
    }
}