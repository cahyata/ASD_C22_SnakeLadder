import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

public class SnakeDijkstraGUI extends JFrame {

    // ==========================================
    // 1. CLASS NODE (Logika)
    // ==========================================
    private static class Node {
        int id;
        int row, col;
        public Node(int id, int row, int col) {
            this.id = id;
            this.row = row;
            this.col = col;
        }
    }

    // ==========================================
    // 2. CLASS GRADIENT PANEL (Visual Kotak Papan)
    // ==========================================
    private static class GradientPanel extends JPanel {
        private final Color centerColor;
        private final Color edgeColor;
        private List<Integer> playersHere = new ArrayList<>();

        public GradientPanel(Color centerColor, Color edgeColor) {
            this.centerColor = centerColor;
            this.edgeColor = edgeColor;
            setOpaque(false);
        }

        public void setPlayersHere(List<Integer> players) {
            this.playersHere.clear();
            this.playersHere.addAll(players);
            repaint();
        }

        // --- FITUR BARU: Add & Remove Player untuk Animasi ---
        public void addPlayer(int pId) {
            if (!playersHere.contains(pId)) {
                playersHere.add(pId);
                repaint();
            }
        }

        public void removePlayer(int pId) {
            // Hapus object integer (bukan index)
            playersHere.remove(Integer.valueOf(pId));
            repaint();
        }
        // ----------------------------------------------------

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

            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
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
    // 3. MAIN GUI CLASS
    // ==========================================

    private static final int SIZE = 8;
    private Node[][] logicBoard = new Node[SIZE][SIZE];
    private Map<Integer, GradientPanel> panelMap = new HashMap<>();

    private int playerCount = 2;
    private List<Stack<Integer>> allPlayerStacks = new ArrayList<>();
    private Deque<Integer> turnQueue = new ArrayDeque<>();

    private Map<Integer, Integer> shortcuts = new HashMap<>();
    private Random random = new Random();

    private JPanel mainContainer;
    private CardLayout cardLayout;
    private JLayeredPane layeredGamePane;
    private JPanel boardPanel;
    private AnimationPanel animationPanel;

    private JLabel statusLabel;
    private JLabel diceLabel;
    private JTextArea historyArea;
    private JButton rollButton;

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
        setTitle("Snake Game: Smooth Step-by-Step Animation");
        setSize(1150, 900);
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

                    g2.setColor(new Color(138, 43, 226, 150));
                    g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);

                    int r = 5;
                    g2.setColor(Color.MAGENTA); g2.fillOval(p1.x - r, p1.y - r, r*2, r*2);
                    g2.setColor(Color.CYAN);    g2.fillOval(p2.x - r, p2.y - r, r*2, r*2);
                }
            }
        }
    }

    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(sidebarColor);

        JLabel title = new JLabel("SLOW ANIMATION SNAKE");
        title.setFont(new Font("Segoe UI", Font.BOLD, 40));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("<html><center>Pergerakan Kotak per Kotak (Smooth & Slow)</center></html>");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));
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

        statusLabel = new JLabel("PLAYER 1 TURN");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        statusLabel.setForeground(playerTextColors[0]);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        diceLabel = new JLabel("Roll the dice!");
        diceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        diceLabel.setForeground(Color.LIGHT_GRAY);
        diceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        rollButton = styleButton("ROLL DICE", accentColor);
        rollButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        rollButton.setMaximumSize(new Dimension(250, 60));
        rollButton.addActionListener(e -> playTurn());

        historyArea = new JTextArea(12, 1);
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        historyArea.setBackground(new Color(44, 62, 80));
        historyArea.setForeground(new Color(46, 204, 113));
        historyArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollStack = new JScrollPane(historyArea);
        scrollStack.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "Game Log",
                0, 0, new Font("Segoe UI", Font.BOLD, 12), Color.WHITE));
        scrollStack.setOpaque(false);
        scrollStack.getViewport().setOpaque(false);
        scrollStack.setAlignmentX(Component.CENTER_ALIGNMENT);

        sidePanel.add(statusLabel);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        sidePanel.add(diceLabel);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 30)));
        sidePanel.add(rollButton);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 30)));
        sidePanel.add(scrollStack);

        panel.add(layeredGamePane, BorderLayout.CENTER);
        panel.add(sidePanel, BorderLayout.EAST);

        return panel;
    }

    private JButton styleButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 18));
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
        for (int i = 0; i < playerCount; i++) {
            Stack<Integer> s = new Stack<>();
            s.push(1);
            allPlayerStacks.add(s);
        }
        turnQueue.clear();
        for (int i = 1; i <= playerCount; i++) {
            turnQueue.add(i);
        }
        rollButton.setEnabled(true);
        statusLabel.setText("PLAYER 1 TURN");
        statusLabel.setForeground(playerTextColors[0]);
        generateRandomLinks();
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
                GradientPanel cell = new GradientPanel(cCenter, cEdge);
                cell.setLayout(new BorderLayout());
                cell.setBorder(new MatteBorder(1, 1, 1, 1, Color.WHITE));

                JLabel numLabel = new JLabel(String.valueOf(node.id));
                numLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                numLabel.setForeground(new Color(80, 80, 80));
                numLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                numLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 8));

                cell.add(numLabel, BorderLayout.NORTH);
                boardPanel.add(cell);
                panelMap.put(node.id, cell);
            }
        }
    }

    // --- GENERATE PATH STEP-BY-STEP ---
    private List<Integer> generatePath(int start, int steps) {
        List<Integer> path = new ArrayList<>();
        int current = start;
        int moves = Math.abs(steps);
        int dir = steps > 0 ? 1 : -1;

        for (int i = 0; i < moves; i++) {
            if (current == 64 && dir == 1) {
                dir = -1; // Pantul
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

    // --- LOGIKA UTAMA ---

    private void playTurn() {
        if (turnQueue.isEmpty()) return;
        rollButton.setEnabled(false);

        int currentPlayer = turnQueue.pollFirst();
        Stack<Integer> currentStack = allPlayerStacks.get(currentPlayer - 1);
        int currentPos = currentStack.peek();
        boolean startIsPrime = isPrime(currentPos);

        // Dice
        double chance = random.nextDouble();
        boolean isGreen = chance < 0.7;
        int diceValue = random.nextInt(6) + 1;
        int steps = isGreen ? diceValue : -diceValue;

        // UI Update
        String hexColor = isGreen ? "#2ECC71" : "#E74C3C";
        String direction = isGreen ? "MAJU" : "MUNDUR";
        diceLabel.setText("<html><center><span style='font-size:12px; color:white;'>Hasil: " + direction + "</span><br>" +
                "<span style='font-size:24px; color:" + hexColor + "; font-weight:bold;'>" + diceValue + "</span></center></html>");

        // 1. Generate Path Step-by-Step
        List<Integer> movementPath = generatePath(currentPos, steps);

        // 2. Start Animation Sequence
        animateSequence(currentPlayer, currentPos, movementPath, 0, () -> {

            int finalDicePos = movementPath.isEmpty() ? currentPos : movementPath.get(movementPath.size()-1);
            String logMsg = "P" + currentPlayer + ": " + currentPos + " -> " + finalDicePos;

            boolean linkActivated = false;
            int linkDest = -1;

            if (shortcuts.containsKey(finalDicePos)) {
                if (startIsPrime) {
                    linkDest = shortcuts.get(finalDicePos);
                    logMsg += " (ACTIVE LINK -> " + linkDest + ")";
                    linkActivated = true;
                } else {
                    logMsg += " (Link Locked)";
                    JOptionPane.showMessageDialog(this, "Link Locked (Start not Prime)", "Info", JOptionPane.WARNING_MESSAGE);
                }
            }

            final int actualFinalPos = linkActivated ? linkDest : finalDicePos;
            final String finalLog = logMsg;

            if (linkActivated) {
                // Animasi Lompat Link (Langsung)
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

        // --- HAPUS DARI KOTAK ASAL ---
        startPanel.removePlayer(playerId);
        // -----------------------------

        Point pStart = SwingUtilities.convertPoint(startPanel, startPanel.getWidth()/2, startPanel.getHeight()/2, animationPanel);
        Point pEnd = SwingUtilities.convertPoint(endPanel, endPanel.getWidth()/2, endPanel.getHeight()/2, animationPanel);

        final int frames = 50;
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

                    // --- TAMBAH KE KOTAK TUJUAN ---
                    endPanel.addPlayer(playerId);
                    animationPanel.stopAnimation();
                    // ------------------------------

                    onComplete.run();
                }
            }
        });
        timer.start();
    }

    private void finalizeTurn(int player, int finalPos, Stack<Integer> stack, String log) {
        animationPanel.stopAnimation();
        stack.push(finalPos);
        historyArea.append(log + "\n");
        updatePlayerGraphics();

        if (finalPos == 64) {
            JOptionPane.showMessageDialog(this, "PLAYER " + player + " WINS!");
            statusLabel.setText("WINNER: P" + player);
            statusLabel.setForeground(Color.YELLOW);
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

    private void updateInfoPanel() {}

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new SnakeDijkstraGUI().setVisible(true));
    }
}