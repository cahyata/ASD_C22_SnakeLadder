import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class SnakeDijkstraGUI extends JFrame {

    // ==========================================
    // 1. CLASS NODE (Logika)
    // ==========================================
    private static class Node {
        int id;
        int row, col;
        int pointValue;

        public Node(int id, int row, int col) {
            this.id = id;
            this.row = row;
            this.col = col;
            this.pointValue = (int)(Math.random() * 3) + 1;
        }
    }

    // ==========================================
    // 2. HELPER FONT LOADER (BARU)
    // ==========================================
    // Menyediakan akses global ke font Geist
    public static class AppFonts {
        public static Font REGULAR;
        public static Font BOLD;
        public static Font MONO;

        static {
            REGULAR = loadFont("Geist-Regular.ttf", "Segoe UI", Font.PLAIN, 14f);
            BOLD    = loadFont("Geist-Bold.ttf", "Segoe UI", Font.BOLD, 14f);
            MONO    = loadFont("GeistMono-Regular.ttf", "Consolas", Font.PLAIN, 12f);
        }

        private static Font loadFont(String fileName, String fallbackName, int style, float size) {
            try {
                // Coba load dari file lokal (pastikan file ada di root project)
                File fontFile = new File(fileName);
                if (fontFile.exists()) {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                    return font.deriveFont(style, size);
                }
            } catch (FontFormatException | IOException e) {
                System.err.println("Gagal memuat font: " + fileName + ". Menggunakan fallback.");
            }
            // Fallback jika file tidak ada
            return new Font(fallbackName, style, (int)size);
        }
    }

    // ==========================================
    // 3. CLASS GRADIENT PANEL (Visual Kotak)
    // ==========================================
    private static class GradientPanel extends JPanel {
        private final Color centerColor;
        private final Color edgeColor;
        private List<Integer> playersHere = new ArrayList<>();
        private int pointValue;

        public GradientPanel(Color centerColor, Color edgeColor, int pointValue) {
            this.centerColor = centerColor;
            this.edgeColor = edgeColor;
            this.pointValue = pointValue;
            setOpaque(false);
        }

        public void setPlayersHere(List<Integer> players) {
            this.playersHere.clear();
            this.playersHere.addAll(players);
            repaint();
        }

        public void addPlayer(int pId) {
            if (!playersHere.contains(pId)) {
                playersHere.add(pId);
                repaint();
            }
        }

        public void removePlayer(int pId) {
            playersHere.remove(Integer.valueOf(pId));
            repaint();
        }

        public static Color getPlayerColor(int id) {
            switch (id) {
                case 1: return Color.decode("#FF5252"); // Merah
                case 2: return Color.decode("#448AFF"); // Biru
                case 3: return Color.decode("#69F0AE"); // Hijau
                case 4: return Color.decode("#FFAB40"); // Oranye
                default: return Color.GRAY;
            }
        }

        public static void drawPawnStatic(Graphics2D g2, int x, int y, int size, Color color, String label) {
            g2.setColor(new Color(0, 0, 0, 60)); // Shadow
            g2.fillOval(x + 3, y + 3, size, size);
            g2.setColor(color); // Body
            g2.fillOval(x, y, size, size);
            g2.setColor(Color.WHITE); // Border
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawOval(x, y, size, size);

            // GANTI FONT PAWN KE GEIST BOLD
            g2.setFont(AppFonts.BOLD.deriveFont(11f));
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (size - fm.stringWidth(label)) / 2;
            int ty = y + (size - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(label, tx, ty);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            Point2D center = new Point2D.Float(w / 2.0f, h / 2.0f);
            float radius = Math.max(w, h);
            float[] dist = {0.0f, 1.0f};
            Color[] colors = {centerColor, edgeColor};
            RadialGradientPaint p = new RadialGradientPaint(center, radius, dist, colors);
            g2d.setPaint(p);
            g2d.fillRect(0, 0, w, h);

            // Point Indicator
            int ptSize = 20;
            int ptX = 5;
            int ptY = h - 25;
            g2d.setColor(new Color(255, 215, 0));
            g2d.fillOval(ptX, ptY, ptSize, ptSize);
            g2d.setColor(new Color(184, 134, 11));
            g2d.setStroke(new BasicStroke(1f));
            g2d.drawOval(ptX, ptY, ptSize, ptSize);
            g2d.setColor(Color.BLACK);

            // GANTI FONT POIN KE GEIST BOLD
            g2d.setFont(AppFonts.BOLD.deriveFont(10f));

            String ptStr = String.valueOf(pointValue);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(ptStr, ptX + (ptSize - fm.stringWidth(ptStr))/2, ptY + (ptSize - fm.getHeight())/2 + fm.getAscent());

            // Players
            int pawnSize = w / 3;
            int padding = 4;
            int x1 = (w / 2) - pawnSize - padding; int y1 = (h / 2) - pawnSize - padding + 10;
            int x2 = (w / 2) + padding;            int y2 = (h / 2) - pawnSize - padding + 10;
            int x3 = (w / 2) - pawnSize - padding; int y3 = (h / 2) + padding + 5;
            int x4 = (w / 2) + padding;            int y4 = (h / 2) + padding + 5;

            for (int playerId : playersHere) {
                Color c = getPlayerColor(playerId);
                int px = 0, py = 0;
                if(playerId == 1) { px = x1; py = y1; }
                else if(playerId == 2) { px = x2; py = y2; }
                else if(playerId == 3) { px = x3; py = y3; }
                else if(playerId == 4) { px = x4; py = y4; }
                drawPawnStatic(g2d, px, py, pawnSize, c, "P" + playerId);
            }
        }
    }

    // ==========================================
    // 4. MAIN GUI CLASS
    // ==========================================

    private static final int SIZE = 8;
    private Node[][] logicBoard = new Node[SIZE][SIZE];
    private Map<Integer, GradientPanel> panelMap = new HashMap<>();

    private int playerCount = 2;
    private List<Stack<Integer>> allPlayerStacks = new ArrayList<>();
    private int[] playerScores;
    private Deque<Integer> turnQueue = new ArrayDeque<>();

    private Map<Integer, Integer> shortcuts = new HashMap<>();
    private Random random = new Random();

    private JPanel mainContainer;
    private CardLayout cardLayout;
    private JLayeredPane layeredGamePane;
    private JPanel boardPanel;
    private AnimationPanel animationPanel;

    private JLabel statusLabel;
    private JLabel diceImageLabel;
    private JLabel diceTextLabel;
    private JTextArea historyArea;
    private JButton rollButton;
    private JLabel scoresLabel;

    private final Color blueCenter = Color.decode("#E3F2FD");
    private final Color blueEdge   = Color.decode("#BBDEFB");
    private final Color creamCenter = Color.decode("#FFFDE7");
    private final Color creamEdge   = Color.decode("#FFF9C4");
    private final Color sidebarColor = Color.decode("#2C3E50");
    private final Color accentColor  = Color.decode("#E67E22");

    private final Color[] playerTextColors = {
            Color.decode("#FF5252"), Color.decode("#448AFF"),
            Color.decode("#69F0AE"), Color.decode("#FFAB40")
    };

    public SnakeDijkstraGUI() {
        setTitle("Snake Game: Geist Edition");
        setSize(1200, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        mainContainer.add(createMenuPanel(), "MENU");
        mainContainer.add(createGamePanel(), "GAME");

        add(mainContainer);
        setLocationRelativeTo(null);
    }

    private boolean isPrime(int num) {
        if (num <= 1) return false;
        for (int i = 2; i <= Math.sqrt(num); i++) {
            if (num % i == 0) return false;
        }
        return true;
    }

    private int getPointOfNode(int id) {
        for(int r=0; r<SIZE; r++){
            for(int c=0; c<SIZE; c++){
                if(logicBoard[r][c].id == id) return logicBoard[r][c].pointValue;
            }
        }
        return 0;
    }

    private ImageIcon createDiceImage(int value, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(2, 2, size-4, size-4, 15, 15);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(2, 2, size-4, size-4, 15, 15);

        g2.setColor(Color.BLACK);
        int dotSize = size / 5;
        int mid = size / 2;
        int left = size / 4;
        int right = size * 3 / 4;

        if (value % 2 != 0) {
            g2.fillOval(mid - dotSize/2, mid - dotSize/2, dotSize, dotSize);
        }
        if (value >= 2) {
            g2.fillOval(left - dotSize/2, left - dotSize/2, dotSize, dotSize);
            g2.fillOval(right - dotSize/2, right - dotSize/2, dotSize, dotSize);
        }
        if (value >= 4) {
            g2.fillOval(right - dotSize/2, left - dotSize/2, dotSize, dotSize);
            g2.fillOval(left - dotSize/2, right - dotSize/2, dotSize, dotSize);
        }
        if (value == 6) {
            g2.fillOval(left - dotSize/2, mid - dotSize/2, dotSize, dotSize);
            g2.fillOval(right - dotSize/2, mid - dotSize/2, dotSize, dotSize);
        }

        g2.dispose();
        return new ImageIcon(img);
    }

    // --- ANIMATION PANEL ---
    private class AnimationPanel extends JPanel {
        private boolean isAnimating = false;
        private int animPlayerId;
        private int animX, animY;

        public AnimationPanel() {
            setOpaque(false);
        }

        public void updatePawnPosition(int playerId, int x, int y) {
            this.isAnimating = true;
            this.animPlayerId = playerId;
            this.animX = x;
            this.animY = y;
            repaint();
        }

        public void stopAnimation() {
            this.isAnimating = false;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (isAnimating) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cellWidth = getWidth() / SIZE;
                int pawnSize = cellWidth / 3;
                GradientPanel.drawPawnStatic(g2,
                        animX - (pawnSize/2),
                        animY - (pawnSize/2),
                        pawnSize,
                        GradientPanel.getPlayerColor(animPlayerId),
                        "P" + animPlayerId);
            }
        }
    }

    // --- BOARD DRAWING PANEL ---
    private class BoardDrawingPanel extends JPanel {
        public BoardDrawingPanel(GridLayout layout) { super(layout); }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (Map.Entry<Integer, Integer> entry : shortcuts.entrySet()) {
                int startId = entry.getKey();
                int endId = entry.getValue();
                GradientPanel startPanel = panelMap.get(startId);
                GradientPanel endPanel = panelMap.get(endId);

                if (startPanel != null && endPanel != null) {
                    Point p1 = SwingUtilities.convertPoint(startPanel, startPanel.getWidth()/2, startPanel.getHeight()/2, this);
                    Point p2 = SwingUtilities.convertPoint(endPanel, endPanel.getWidth()/2, endPanel.getHeight()/2, this);

                    drawRealisticLadder(g2, p1, p2);

                    int r = 5;
                    g2.setColor(new Color(101, 67, 33));
                    g2.fillOval(p1.x - r, p1.y - r, r*2, r*2);
                    g2.fillOval(p2.x - r, p2.y - r, r*2, r*2);
                }
            }
        }

        private void drawRealisticLadder(Graphics2D g2, Point p1, Point p2) {
            double dx = p2.x - p1.x;
            double dy = p2.y - p1.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance < 20) return;

            double ux = dx / distance;
            double uy = dy / distance;
            double railWidthOffset = 12;
            double px = -uy * railWidthOffset;
            double py = ux * railWidthOffset;

            Color woodDark = new Color(101, 67, 33);
            Color woodLight = new Color(160, 112, 66);
            Color shadow = new Color(0,0,0, 80);

            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Rails
            g2.setColor(shadow);
            g2.drawLine((int)(p1.x+px+2), (int)(p1.y+py+2), (int)(p2.x+px+2), (int)(p2.y+py+2));
            g2.drawLine((int)(p1.x-px+2), (int)(p1.y-py+2), (int)(p2.x-px+2), (int)(p2.y-py+2));
            g2.setColor(woodDark);
            g2.drawLine((int)(p1.x+px), (int)(p1.y+py), (int)(p2.x+px), (int)(p2.y+py));
            g2.drawLine((int)(p1.x-px), (int)(p1.y-py), (int)(p2.x-px), (int)(p2.y-py));

            // Rungs
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double stepSize = 25;
            for (double t = stepSize; t < distance - 10; t += stepSize) {
                double cx = p1.x + ux * t;
                double cy = p1.y + uy * t;
                int lx = (int)(cx + px); int ly = (int)(cy + py);
                int rx = (int)(cx - px); int ry = (int)(cy - py);

                g2.setColor(shadow);
                g2.drawLine(lx+1, ly+2, rx+1, ry+2);
                g2.setColor(woodLight);
                g2.drawLine(lx, ly, rx, ry);
            }
        }
    }

    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(sidebarColor);

        // GANTI FONT MENU KE GEIST
        JLabel title = new JLabel("DICE MASTER SNAKE");
        title.setFont(AppFonts.BOLD.deriveFont(48f)); // Geist Bold
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("<html><center>Shortcut: Tekan ENTER untuk Roll Dadu!</center></html>");
        subtitle.setFont(AppFonts.REGULAR.deriveFont(18f)); // Geist Regular
        subtitle.setForeground(Color.LIGHT_GRAY);

        JButton startButton = styleButton("START NEW GAME", new Color(46, 204, 113));
        startButton.setPreferredSize(new Dimension(250, 60));

        startButton.addActionListener(e -> {
            String[] options = {"2 Players", "3 Players", "4 Players"};
            int choice = JOptionPane.showOptionDialog(this, "Pilih Jumlah Pemain:", "Setup Game",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            if (choice != -1) {
                playerCount = choice + 2;
                initGameData();
                updatePlayerGraphics();
                cardLayout.show(mainContainer, "GAME");
                boardPanel.repaint();

                getRootPane().setDefaultButton(rollButton);
                rollButton.requestFocusInWindow();
            }
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(0, 0, 10, 0);
        panel.add(title, gbc);
        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 50, 0);
        panel.add(subtitle, gbc);
        gbc.gridy = 2;
        panel.add(startButton, gbc);
        return panel;
    }

    private JPanel createGamePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        layeredGamePane = new JLayeredPane();
        layeredGamePane.setLayout(new OverlayLayout(layeredGamePane));

        animationPanel = new AnimationPanel();
        layeredGamePane.add(animationPanel, JLayeredPane.PALETTE_LAYER);

        boardPanel = new BoardDrawingPanel(new GridLayout(SIZE, SIZE));
        boardPanel.setBorder(new LineBorder(sidebarColor, 5));
        initializeLogicBoard();
        initializeVisualBoard(boardPanel);
        layeredGamePane.add(boardPanel, JLayeredPane.DEFAULT_LAYER);

        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setPreferredSize(new Dimension(300, 0));
        sidePanel.setBorder(new EmptyBorder(30, 20, 30, 20));
        sidePanel.setBackground(sidebarColor);

        // GANTI FONT SIDEBAR KE GEIST
        statusLabel = new JLabel("PLAYER 1 TURN");
        statusLabel.setFont(AppFonts.BOLD.deriveFont(24f));
        statusLabel.setForeground(playerTextColors[0]);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        scoresLabel = new JLabel("Scores: P1(0) P2(0)");
        scoresLabel.setFont(AppFonts.MONO.deriveFont(14f)); // Geist Mono untuk angka
        scoresLabel.setForeground(Color.ORANGE);
        scoresLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        diceImageLabel = new JLabel(createDiceImage(1, 80));
        diceImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        diceTextLabel = new JLabel("Press Enter to Roll");
        diceTextLabel.setFont(AppFonts.REGULAR.deriveFont(14f));
        diceTextLabel.setForeground(Color.LIGHT_GRAY);
        diceTextLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        rollButton = styleButton("ROLL DICE", accentColor);
        rollButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        rollButton.setMaximumSize(new Dimension(250, 60));
        rollButton.addActionListener(e -> playTurn());

        rollButton.setMnemonic(KeyEvent.VK_ENTER);

        historyArea = new JTextArea(10, 1);
        historyArea.setEditable(false);
        // Geist Mono untuk Log
        historyArea.setFont(AppFonts.MONO.deriveFont(12f));
        historyArea.setBackground(new Color(44, 62, 80));
        historyArea.setForeground(new Color(46, 204, 113));
        historyArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollStack = new JScrollPane(historyArea);
        scrollStack.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "Game Log",
                0, 0, AppFonts.BOLD.deriveFont(12f), Color.WHITE));
        scrollStack.setOpaque(false);
        scrollStack.getViewport().setOpaque(false);
        scrollStack.setAlignmentX(Component.CENTER_ALIGNMENT);

        sidePanel.add(statusLabel);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        sidePanel.add(scoresLabel);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 20)));
        sidePanel.add(diceImageLabel);
        sidePanel.add(diceTextLabel);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 20)));
        sidePanel.add(rollButton);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 20)));
        sidePanel.add(scrollStack);

        panel.add(layeredGamePane, BorderLayout.CENTER);
        panel.add(sidePanel, BorderLayout.EAST);

        return panel;
    }

    private JButton styleButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(AppFonts.BOLD.deriveFont(18f)); // Geist Bold Button
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        return btn;
    }

    private void initGameData() {
        allPlayerStacks.clear();
        playerScores = new int[playerCount];

        for (int i = 0; i < playerCount; i++) {
            Stack<Integer> s = new Stack<>();
            s.push(1);
            allPlayerStacks.add(s);
            playerScores[i] = 0;
        }
        turnQueue.clear();
        for (int i = 1; i <= playerCount; i++) {
            turnQueue.add(i);
        }
        rollButton.setEnabled(true);
        updateScoreLabel();

        statusLabel.setText("PLAYER 1 TURN");
        statusLabel.setForeground(playerTextColors[0]);
        generateRandomLinks();

        diceImageLabel.setIcon(createDiceImage(1, 80));
    }

    private void updateScoreLabel() {
        StringBuilder sb = new StringBuilder("Scores: ");
        for(int i=0; i<playerCount; i++) {
            sb.append("P").append(i+1).append("(").append(playerScores[i]).append(") ");
        }
        scoresLabel.setText(sb.toString());
    }

    private void generateRandomLinks() {
        shortcuts.clear();
        while (shortcuts.size() < 5) {
            int start = random.nextInt(62) + 2;
            int end = random.nextInt(62) + 2;
            if (start != end && !shortcuts.containsKey(start) && !shortcuts.containsValue(start)) {
                shortcuts.put(start, end);
            }
        }
    }

    private void initializeLogicBoard() {
        int idCounter = 1;
        for (int r = SIZE - 1; r >= 0; r--) {
            boolean leftToRight = (SIZE - 1 - r) % 2 == 0;
            if (leftToRight) {
                for (int c = 0; c < SIZE; c++) {
                    logicBoard[r][c] = new Node(idCounter++, r, c);
                }
            } else {
                for (int c = SIZE - 1; c >= 0; c--) {
                    logicBoard[r][c] = new Node(idCounter++, r, c);
                }
            }
        }
    }

    private void initializeVisualBoard(JPanel boardPanel) {
        boardPanel.removeAll();
        panelMap.clear();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                Node node = logicBoard[r][c];
                Color cCenter = ((r + c) % 2 == 0) ? blueCenter : creamCenter;
                Color cEdge = ((r + c) % 2 == 0) ? blueEdge : creamEdge;

                GradientPanel cell = new GradientPanel(cCenter, cEdge, node.pointValue);
                cell.setLayout(new BorderLayout());
                cell.setBorder(new MatteBorder(1, 1, 1, 1, Color.WHITE));

                JLabel numLabel = new JLabel(String.valueOf(node.id));
                numLabel.setFont(AppFonts.BOLD.deriveFont(14f)); // Geist Bold untuk nomor kotak
                numLabel.setForeground(new Color(80, 80, 80));
                numLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                numLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 8));

                cell.add(numLabel, BorderLayout.NORTH);
                boardPanel.add(cell);
                panelMap.put(node.id, cell);
            }
        }
    }

    private List<Integer> generatePath(int start, int steps) {
        List<Integer> path = new ArrayList<>();
        int current = start;
        int moves = Math.abs(steps);
        int dir = steps > 0 ? 1 : -1;

        for (int i = 0; i < moves; i++) {
            if (current == 64 && dir == 1) {
                dir = -1;
            }
            if (current == 1 && dir == -1) {
                // Mentok
            } else {
                current += dir;
            }
            path.add(current);
        }
        return path;
    }

    private void playTurn() {
        if (turnQueue.isEmpty()) return;
        rollButton.setEnabled(false);

        // --- ANIMASI ROLL DADU ---
        javax.swing.Timer rollTimer = new javax.swing.Timer(50, null);
        final int[] rolls = {0};

        rollTimer.addActionListener(e -> {
            int randFace = random.nextInt(6) + 1;
            diceImageLabel.setIcon(createDiceImage(randFace, 80));
            rolls[0]++;

            if (rolls[0] >= 10) {
                rollTimer.stop();
                executeGameLogic();
            }
        });
        rollTimer.start();
    }

    private void executeGameLogic() {
        int currentPlayer = turnQueue.pollFirst();
        Stack<Integer> currentStack = allPlayerStacks.get(currentPlayer - 1);
        int currentPos = currentStack.peek();
        boolean startIsPrime = isPrime(currentPos);

        double chance = random.nextDouble();
        boolean isGreen = chance < 0.7;
        int diceValue = random.nextInt(6) + 1;
        int steps = isGreen ? diceValue : -diceValue;

        diceImageLabel.setIcon(createDiceImage(diceValue, 80));
        String direction = isGreen ? "MAJU (Hijau)" : "MUNDUR (Merah)";
        diceTextLabel.setText(direction + " " + diceValue + " Langkah");
        diceTextLabel.setForeground(isGreen ? new Color(0, 128, 0) : Color.RED);

        List<Integer> movementPath = generatePath(currentPos, steps);

        int truncatedTarget = -1;
        int linkTarget = -1;

        if (isGreen) {
            for (int i = 0; i < movementPath.size(); i++) {
                int node = movementPath.get(i);
                if (shortcuts.containsKey(node)) {
                    int distToLink = node - currentPos;
                    if (startIsPrime && diceValue > distToLink) {
                        truncatedTarget = node;
                        linkTarget = shortcuts.get(node);
                        movementPath = movementPath.subList(0, i + 1);
                        break;
                    }
                }
            }
        }

        final int targetShortcutStart = truncatedTarget;
        final int targetShortcutEnd = linkTarget;
        final List<Integer> finalPath = movementPath;

        animateSequence(currentPlayer, currentPos, finalPath, 0, () -> {
            int finalDicePos = finalPath.isEmpty() ? currentPos : finalPath.get(finalPath.size()-1);
            String logMsg = "P" + currentPlayer + ": " + currentPos + " -> " + finalDicePos;

            boolean jumpActivated = false;

            if (targetShortcutStart != -1 && finalDicePos == targetShortcutStart) {
                jumpActivated = true;
                logMsg += " (OVERFLOW LINK -> " + targetShortcutEnd + ")";
                JOptionPane.showMessageDialog(this, "PRIME OVERFLOW! Link Activated.");
            }

            final int actualFinalPos = jumpActivated ? targetShortcutEnd : finalDicePos;
            final String finalLog = logMsg;

            if (jumpActivated) {
                javax.swing.Timer delay = new javax.swing.Timer(500, e -> {
                    ((javax.swing.Timer)e.getSource()).stop();
                    animateMove(currentPlayer, finalDicePos, actualFinalPos, () -> {
                        finalizeTurn(currentPlayer, actualFinalPos, currentStack, finalLog);
                    });
                });
                delay.setRepeats(false);
                delay.start();
            } else {
                finalizeTurn(currentPlayer, actualFinalPos, currentStack, finalLog);
            }
        });
    }

    private void animateSequence(int playerId, int currentVisPos, List<Integer> path, int index, Runnable onComplete) {
        if (index >= path.size()) {
            onComplete.run();
            return;
        }
        int nextTarget = path.get(index);
        animateMove(playerId, currentVisPos, nextTarget, () -> {
            animateSequence(playerId, nextTarget, path, index + 1, onComplete);
        });
    }

    private void animateMove(int playerId, int startId, int endId, Runnable onComplete) {
        if (startId == endId) { onComplete.run(); return; }

        GradientPanel startPanel = panelMap.get(startId);
        GradientPanel endPanel = panelMap.get(endId);
        if (startPanel == null || endPanel == null) { onComplete.run(); return; }

        startPanel.removePlayer(playerId);

        Point pStart = SwingUtilities.convertPoint(startPanel, startPanel.getWidth()/2, startPanel.getHeight()/2, animationPanel);
        Point pEnd = SwingUtilities.convertPoint(endPanel, endPanel.getWidth()/2, endPanel.getHeight()/2, animationPanel);

        final int frames = 40;
        final int delay = 10;

        javax.swing.Timer timer = new javax.swing.Timer(delay, null);
        timer.addActionListener(new ActionListener() {
            int currentFrame = 0;
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                currentFrame++;
                float t = (float) currentFrame / frames;
                t = t * t * (3f - 2f * t);

                int curX = (int) (pStart.x + (pEnd.x - pStart.x) * t);
                int curY = (int) (pStart.y + (pEnd.y - pStart.y) * t);

                animationPanel.updatePawnPosition(playerId, curX, curY);

                if (currentFrame >= frames) {
                    ((javax.swing.Timer)e.getSource()).stop();
                    endPanel.addPlayer(playerId);
                    animationPanel.stopAnimation();
                    onComplete.run();
                }
            }
        });
        timer.start();
    }

    private void finalizeTurn(int player, int finalPos, Stack<Integer> stack, String log) {
        animationPanel.stopAnimation();
        stack.push(finalPos);

        int pointsGained = getPointOfNode(finalPos);
        playerScores[player-1] += pointsGained;

        log += " [+" + pointsGained + " pts]";
        historyArea.append(log + "\n");
        updateScoreLabel();
        updatePlayerGraphics();

        if (finalPos == 64) {
            showEndGameDialog(player);
            return;
        }

        if (finalPos % 5 == 0 && finalPos != 1) {
            JOptionPane.showMessageDialog(this, "Kelipatan 5! Double Turn!");
            turnQueue.addFirst(player);
        } else {
            turnQueue.addLast(player);
        }

        int nextPlayer = turnQueue.peekFirst();
        statusLabel.setText("PLAYER " + nextPlayer + " TURN");
        int colorIdx = (nextPlayer - 1) % playerTextColors.length;
        statusLabel.setForeground(playerTextColors[colorIdx]);

        rollButton.setEnabled(true);
        rollButton.requestFocusInWindow();
    }

    private void showEndGameDialog(int finisher) {
        List<Integer> rank = new ArrayList<>();
        for(int i=0; i<playerCount; i++) rank.add(i+1);
        rank.sort((p1, p2) -> Integer.compare(playerScores[p2-1], playerScores[p1-1]));

        StringBuilder msg = new StringBuilder();
        msg.append("GAME OVER!\n");
        msg.append("Player ").append(finisher).append(" reached Finish!\n\n");
        msg.append("=== SCOREBOARD ===\n");

        for(int i=0; i<rank.size(); i++) {
            int pid = rank.get(i);
            int score = playerScores[pid-1];
            msg.append(i+1).append(". Player ").append(pid).append(" : ").append(score).append(" pts");
            if(i==0) msg.append(" 🏆 (WINNER)");
            msg.append("\n");
        }

        JOptionPane.showMessageDialog(this, msg.toString(), "Final Results", JOptionPane.INFORMATION_MESSAGE);
        statusLabel.setText("WINNER: PLAYER " + rank.get(0));
        statusLabel.setForeground(Color.YELLOW);
    }

    private void updatePlayerGraphics() {
        for (GradientPanel panel : panelMap.values()) {
            panel.setPlayersHere(new ArrayList<>());
        }
        Map<Integer, List<Integer>> positions = new HashMap<>();
        for (int i = 0; i < playerCount; i++) {
            int pId = i + 1;
            int pos = allPlayerStacks.get(i).peek();
            positions.putIfAbsent(pos, new ArrayList<>());
            positions.get(pos).add(pId);
        }
        for (Map.Entry<Integer, List<Integer>> entry : positions.entrySet()) {
            GradientPanel panel = panelMap.get(entry.getKey());
            if (panel != null) panel.setPlayersHere(entry.getValue());
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new SnakeDijkstraGUI().setVisible(true));
    }
}