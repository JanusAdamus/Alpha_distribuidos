package mx.itam.alpha.server.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

public class ServerControlFrame extends JFrame {

    private static final Color BACKGROUND_COLOR = new Color(244, 240, 230);
    private static final Color PANEL_COLOR = new Color(255, 251, 243);
    private static final Color BORDER_COLOR = new Color(190, 177, 154);

    public interface Listener {
        void onOpenClient();

        void onRunStress();

        void onRunRepeatStress();

        void onOpenResults();

        void onOpenChart();

        void onOpenReadme();
    }

    private final JButton openClientButton = new JButton("Abrir cliente");
    private final JButton stressButton = new JButton("Correr estrés");
    private final JButton repeatStressButton = new JButton("Estrés 10 rep.");
    private final JButton openResultsButton = new JButton("Ver último CSV");
    private final JButton plotResultsButton = new JButton("Graficar CSV");
    private final JButton openReadmeButton = new JButton("Ver README");
    private final JLabel serverStateLabel = new JLabel("Servidor iniciado");
    private final JTextArea environmentArea = new JTextArea();
    private final JTextArea propertiesArea = new JTextArea();
    private final JTextArea requirementsArea = new JTextArea();
    private final JTextArea logArea = new JTextArea();

    private Listener listener;

    public ServerControlFrame() {
        super("Alpha - Control del servidor");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 720));

        JPanel contentPanel = new JPanel(new BorderLayout(16, 16));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        contentPanel.setBackground(BACKGROUND_COLOR);
        setContentPane(contentPanel);

        contentPanel.add(buildHeaderPanel(), BorderLayout.NORTH);
        contentPanel.add(buildMainPanel(), BorderLayout.CENTER);
        contentPanel.add(buildLogPanel(), BorderLayout.SOUTH);

        configureTextAreas();
        bindActions();

        pack();
        setLocationRelativeTo(null);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private JPanel buildHeaderPanel() {
        JPanel headerPanel = createSectionPanel("Estado general");
        headerPanel.add(serverStateLabel, BorderLayout.WEST);
        return headerPanel;
    }

    private JPanel buildMainPanel() {
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 16, 16));
        mainPanel.setOpaque(false);
        mainPanel.add(buildOverviewPanel());
        mainPanel.add(buildActionPanel());
        return mainPanel;
    }

    private JPanel buildOverviewPanel() {
        JPanel overviewPanel = new JPanel();
        overviewPanel.setLayout(new BoxLayout(overviewPanel, BoxLayout.Y_AXIS));
        overviewPanel.setOpaque(false);
        overviewPanel.add(createScrollSection("Entorno de ejecución", environmentArea, 160));
        overviewPanel.add(Box.createVerticalStrut(12));
        overviewPanel.add(createScrollSection("alpha.properties", propertiesArea, 170));
        overviewPanel.add(Box.createVerticalStrut(12));
        overviewPanel.add(createScrollSection("Checklist de requerimientos", requirementsArea, 210));
        return overviewPanel;
    }

    private JPanel buildActionPanel() {
        JPanel actionCard = createSectionPanel("Acciones y entregables");
        JPanel actionPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        actionPanel.setOpaque(false);
        actionPanel.add(openClientButton);
        actionPanel.add(stressButton);
        actionPanel.add(repeatStressButton);
        actionPanel.add(openResultsButton);
        actionPanel.add(plotResultsButton);
        actionPanel.add(openReadmeButton);
        actionCard.add(actionPanel, BorderLayout.CENTER);
        return actionCard;
    }

    private JScrollPane buildLogPanel() {
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createTitledBorder("Bitácora")));
        scrollPane.setPreferredSize(new Dimension(1000, 210));
        return scrollPane;
    }

    private void bindActions() {
        openClientButton.addActionListener(event -> fire(listener == null ? null : listener::onOpenClient));
        stressButton.addActionListener(event -> fire(listener == null ? null : listener::onRunStress));
        repeatStressButton.addActionListener(event -> fire(listener == null ? null : listener::onRunRepeatStress));
        openResultsButton.addActionListener(event -> fire(listener == null ? null : listener::onOpenResults));
        plotResultsButton.addActionListener(event -> fire(listener == null ? null : listener::onOpenChart));
        openReadmeButton.addActionListener(event -> fire(listener == null ? null : listener::onOpenReadme));
    }

    private void fire(Runnable action) {
        if (action != null) {
            action.run();
        }
    }

    public void appendLog(String message) {
        logArea.append(message + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void setServerState(String state) {
        serverStateLabel.setText(state);
    }

    public void setEnvironmentSummary(String content) {
        environmentArea.setText(content == null ? "" : content);
        environmentArea.setCaretPosition(0);
    }

    public void setRequirementsSummary(String content) {
        requirementsArea.setText(content == null ? "" : content);
        requirementsArea.setCaretPosition(0);
    }

    public void setPropertiesContent(String content) {
        propertiesArea.setText(content == null ? "" : content);
        propertiesArea.setCaretPosition(0);
    }

    public void showDocument(String title, String content) {
        JDialog dialog = new JDialog(this, title, false);
        dialog.setLayout(new BorderLayout(8, 8));
        JTextArea textArea = new JTextArea(content);
        textArea.setEditable(false);
        textArea.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(760, 520));
        scrollPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    public void showStressChart(String title, String csvContent) {
        StressResultsChartDialog dialog = new StressResultsChartDialog(this, title, csvContent);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void configureTextAreas() {
        environmentArea.setEditable(false);
        environmentArea.setLineWrap(true);
        environmentArea.setWrapStyleWord(true);
        environmentArea.setOpaque(false);

        requirementsArea.setEditable(false);
        requirementsArea.setLineWrap(true);
        requirementsArea.setWrapStyleWord(true);
        requirementsArea.setOpaque(false);

        propertiesArea.setEditable(false);
        propertiesArea.setLineWrap(false);
        propertiesArea.setWrapStyleWord(false);

        logArea.setBackground(PANEL_COLOR);
        logArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(PANEL_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(title),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10))));
        return panel;
    }

    private JPanel createScrollSection(String title, JTextArea textArea, int preferredHeight) {
        JPanel panel = createSectionPanel(title);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(480, preferredHeight));
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scrollPane.getViewport().setBackground(PANEL_COLOR);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
}
