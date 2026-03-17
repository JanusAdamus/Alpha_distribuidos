package mx.unam.alpha.client.ui;

import mx.unam.alpha.common.model.SessionInfo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class GameFrame extends JFrame {

    private static final Color BACKGROUND_COLOR = new Color(245, 241, 232);
    private static final Color PANEL_COLOR = new Color(255, 252, 245);
    private static final Color BORDER_COLOR = new Color(192, 181, 160);
    private static final Color TEXT_COLOR = new Color(44, 37, 30);
    private static final Color DEFAULT_CELL_COLOR = new Color(232, 226, 212);
    private static final Color ACTIVE_CELL_COLOR = new Color(191, 55, 55);
    private static final Color PRIMARY_BUTTON_COLOR = new Color(32, 104, 87);
    private static final Color SECONDARY_BUTTON_COLOR = new Color(109, 85, 58);

    public interface Listener {
        void onRegister(String username, String password);

        void onLogin(String username, String password);

        void onLogout();

        void onCellClicked(int row, int col);
    }

    private final JTextField usernameField = new JTextField("jugador1");
    private final JPasswordField passwordField = new JPasswordField("12345");
    private final JButton registerButton = new JButton("Registrar");
    private final JButton loginButton = new JButton("Entrar");
    private final JButton logoutButton = new JButton("Salir");
    private final JLabel playerLabel = new JLabel("Jugador: -");
    private final JLabel scoreLabel = new JLabel("Puntaje: 0");
    private final JLabel connectionLabel = new JLabel("Desconectado");
    private final JLabel objectiveLabel = new JLabel("Meta: conecta para recibir la sesión");
    private final JLabel networkLabel = new JLabel("Transporte: TCP para login/golpe, tópico para monstruos");
    private final JLabel boardLabel = new JLabel("Tablero: -");
    private final JLabel monsterLabel = new JLabel("Monstruo: esperando aparición");
    private final JTextArea scoreboardArea = new JTextArea("Aún no hay jugadores conectados.");
    private final JTextArea statusArea = new JTextArea();
    private final JButton[][] boardButtons;

    private Listener listener;
    private int activeRow = -1;
    private int activeCol = -1;

    public GameFrame(int rows, int cols) {
        super("Proyecto Alpha - Pégale al monstruo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1160, 760));

        JPanel contentPanel = new JPanel(new BorderLayout(16, 16));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        contentPanel.setBackground(BACKGROUND_COLOR);
        setContentPane(contentPanel);

        boardButtons = new JButton[rows][cols];

        contentPanel.add(buildSidebar(rows, cols), BorderLayout.WEST);
        contentPanel.add(buildBoardPanel(rows, cols), BorderLayout.CENTER);
        contentPanel.add(buildStatusPanel(), BorderLayout.SOUTH);

        bindActions();
        configureTextAreas();
        setBoardSummary(rows, cols);
        setConnected(false);
        pack();
        setLocationRelativeTo(null);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private JPanel buildSidebar(int rows, int cols) {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BACKGROUND_COLOR);
        sidebar.setPreferredSize(new Dimension(320, 620));
        sidebar.add(buildAuthPanel());
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(buildPlayerPanel());
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(buildSessionPanel(rows, cols));
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(buildScoreboardPanel());
        return sidebar;
    }

    private JPanel buildAuthPanel() {
        JPanel authPanel = createSectionPanel("Acceso TCP");
        JPanel fieldsPanel = new JPanel(new GridLayout(4, 1, 6, 6));
        fieldsPanel.setOpaque(false);
        fieldsPanel.add(new JLabel("Usuario"));
        fieldsPanel.add(usernameField);
        fieldsPanel.add(new JLabel("Contraseña"));
        fieldsPanel.add(passwordField);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 8, 8));
        buttonPanel.setOpaque(false);
        styleButton(registerButton, PRIMARY_BUTTON_COLOR);
        styleButton(loginButton, SECONDARY_BUTTON_COLOR);
        logoutButton.setMargin(new Insets(10, 10, 10, 10));
        buttonPanel.add(registerButton);
        buttonPanel.add(loginButton);
        buttonPanel.add(logoutButton);

        authPanel.add(fieldsPanel, BorderLayout.CENTER);
        authPanel.add(buttonPanel, BorderLayout.SOUTH);
        return authPanel;
    }

    private JPanel buildPlayerPanel() {
        JPanel playerPanel = createSectionPanel("Estado del jugador");
        JPanel infoPanel = new JPanel(new GridLayout(4, 1, 4, 4));
        infoPanel.setOpaque(false);
        infoPanel.add(styleInfoLabel(playerLabel));
        infoPanel.add(styleInfoLabel(scoreLabel));
        infoPanel.add(styleInfoLabel(connectionLabel));
        infoPanel.add(styleInfoLabel(monsterLabel));
        playerPanel.add(infoPanel, BorderLayout.CENTER);
        return playerPanel;
    }

    private JPanel buildSessionPanel(int rows, int cols) {
        JPanel sessionPanel = createSectionPanel("Sesión y partida");
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 4, 4));
        infoPanel.setOpaque(false);
        infoPanel.add(styleInfoLabel(objectiveLabel));
        infoPanel.add(styleInfoLabel(networkLabel));
        infoPanel.add(styleInfoLabel(boardLabel));
        sessionPanel.add(infoPanel, BorderLayout.CENTER);
        setBoardSummary(rows, cols);
        return sessionPanel;
    }

    private JPanel buildScoreboardPanel() {
        JPanel scoreboardPanel = createSectionPanel("Marcador en vivo");
        scoreboardPanel.add(wrapReadOnlyArea(scoreboardArea, 140), BorderLayout.CENTER);
        return scoreboardPanel;
    }

    private JPanel buildBoardPanel(int rows, int cols) {
        JPanel boardWrapper = createSectionPanel("Tablero compartido");
        JPanel boardPanel = new JPanel(new GridLayout(rows, cols, 10, 10));
        boardPanel.setOpaque(false);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                JButton button = createBoardButton(row, col);
                boardButtons[row][col] = button;
                boardPanel.add(button);
            }
        }
        boardWrapper.add(boardPanel, BorderLayout.CENTER);
        return boardWrapper;
    }

    private JButton createBoardButton(int row, int col) {
        JButton button = new JButton(cellText(row, col));
        button.setBackground(DEFAULT_CELL_COLOR);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 18f));
        button.setForeground(TEXT_COLOR);
        button.setMargin(new Insets(12, 12, 12, 12));
        button.addActionListener(event -> {
            if (listener != null) {
                listener.onCellClicked(row, col);
            }
        });
        return button;
    }

    private JScrollPane buildStatusPanel() {
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(statusArea);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createTitledBorder("Bitácora del juego")));
        scrollPane.setPreferredSize(new Dimension(900, 190));
        return scrollPane;
    }

    private void bindActions() {
        registerButton.addActionListener(event -> fireRegister());
        loginButton.addActionListener(event -> fireLogin());
        logoutButton.addActionListener(event -> {
            if (listener != null) {
                listener.onLogout();
            }
        });
    }

    public void setConnected(boolean connected) {
        connectionLabel.setText(connected ? "Conectado" : "Desconectado");
        logoutButton.setEnabled(connected);
        loginButton.setEnabled(!connected);
        registerButton.setEnabled(!connected);
        usernameField.setEnabled(!connected);
        passwordField.setEnabled(!connected);
        for (JButton[] row : boardButtons) {
            for (JButton button : row) {
                button.setEnabled(connected);
            }
        }
        if (!connected) {
            objectiveLabel.setText("Meta: conecta para recibir la sesión");
            networkLabel.setText("Transporte: TCP para login/golpe, tópico para monstruos");
            monsterLabel.setText("Monstruo: esperando aparición");
            scoreboardArea.setText("Aún no hay jugadores conectados.");
        }
    }

    public void appendStatus(String message) {
        statusArea.append(message + System.lineSeparator());
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }

    public void updatePlayer(String username, int score, boolean connected) {
        playerLabel.setText("Jugador: " + username);
        scoreLabel.setText("Puntaje: " + score);
        connectionLabel.setText(connected ? "Conectado" : "Desconectado");
    }

    public void updateScoreboard(Map<String, Integer> scoreboard) {
        if (scoreboard == null || scoreboard.isEmpty()) {
            scoreboardArea.setText("Aún no hay jugadores conectados.");
            return;
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(scoreboard.entrySet());
        entries.sort(Comparator.<Map.Entry<String, Integer>, Integer>comparing(Map.Entry::getValue)
                .reversed()
                .thenComparing(Map.Entry::getKey));
        StringBuilder builder = new StringBuilder();
        int position = 1;
        for (Map.Entry<String, Integer> entry : entries) {
            builder.append(position++)
                    .append(". ")
                    .append(entry.getKey())
                    .append("  ->  ")
                    .append(entry.getValue())
                    .append(" puntos")
                    .append(System.lineSeparator());
        }
        scoreboardArea.setText(builder.toString().trim());
        scoreboardArea.setCaretPosition(0);
    }

    public void applySessionInfo(SessionInfo sessionInfo) {
        if (sessionInfo == null) {
            objectiveLabel.setText("Meta: conecta para recibir la sesión");
            networkLabel.setText("Transporte: TCP para login/golpe, tópico para monstruos");
            return;
        }
        objectiveLabel.setText("Meta: " + sessionInfo.getTargetScore() + " golpes para ganar");
        networkLabel.setText("Canales: TCP " + sessionInfo.getTcpHost() + ":" + sessionInfo.getTcpPort()
                + " | JMS " + sessionInfo.getJmsBrokerUrl());
        setBoardSummary(sessionInfo.getBoardRows(), sessionInfo.getBoardCols());
    }

    public void highlightMonster(int row, int col) {
        clearMonster();
        if (!isInsideBoard(row, col)) {
            return;
        }
        activeRow = row;
        activeCol = col;
        JButton activeButton = boardButtons[row][col];
        activeButton.setBackground(ACTIVE_CELL_COLOR);
        activeButton.setForeground(Color.WHITE);
        activeButton.setText("MONSTRUO");
        monsterLabel.setText("Monstruo activo en [" + row + "," + col + "]");
    }

    public void clearMonster() {
        if (isInsideBoard(activeRow, activeCol)) {
            resetBoardButton(activeRow, activeCol);
        }
        activeRow = -1;
        activeCol = -1;
        monsterLabel.setText("Monstruo: esperando aparición");
    }

    private void resetBoardButton(int row, int col) {
        JButton button = boardButtons[row][col];
        button.setBackground(DEFAULT_CELL_COLOR);
        button.setForeground(Color.BLACK);
        button.setText(cellText(row, col));
    }

    private String cellText(int row, int col) {
        return row + "," + col;
    }

    private void fireRegister() {
        if (listener != null) {
            listener.onRegister(usernameField.getText().trim(), new String(passwordField.getPassword()));
        }
    }

    private void fireLogin() {
        if (listener != null) {
            listener.onLogin(usernameField.getText().trim(), new String(passwordField.getPassword()));
        }
    }

    private boolean isInsideBoard(int row, int col) {
        return row >= 0 && col >= 0 && row < boardButtons.length && col < boardButtons[0].length;
    }

    private void configureTextAreas() {
        scoreboardArea.setEditable(false);
        scoreboardArea.setLineWrap(true);
        scoreboardArea.setWrapStyleWord(true);
        scoreboardArea.setOpaque(false);
        scoreboardArea.setForeground(TEXT_COLOR);

        statusArea.setBackground(PANEL_COLOR);
        statusArea.setForeground(TEXT_COLOR);
        statusArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(true);
        panel.setBackground(PANEL_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(title),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10))));
        return panel;
    }

    private JLabel styleInfoLabel(JLabel label) {
        label.setForeground(TEXT_COLOR);
        return label;
    }

    private JScrollPane wrapReadOnlyArea(JTextArea textArea, int preferredHeight) {
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(280, preferredHeight));
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scrollPane.getViewport().setBackground(PANEL_COLOR);
        return scrollPane;
    }

    private void styleButton(JButton button, Color background) {
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setMargin(new Insets(10, 10, 10, 10));
    }

    private void setBoardSummary(int rows, int cols) {
        boardLabel.setText("Tablero: " + rows + "x" + cols + " | mínimo 5 jugadores soportados");
    }
}
