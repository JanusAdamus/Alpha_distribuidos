package mx.itam.alpha.server.ui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class StressResultsChartDialog extends JDialog {

    private static final Color PANEL_COLOR = new Color(255, 251, 243);
    private static final Color BORDER_COLOR = new Color(190, 177, 154);
    private static final Color AXIS_COLOR = new Color(98, 86, 72);
    private static final Color GRID_COLOR = new Color(224, 214, 197);
    private static final Color REGISTER_COLOR = new Color(42, 120, 98);
    private static final Color GAME_COLOR = new Color(196, 90, 53);

    public StressResultsChartDialog(JDialog owner, String title, String csvContent) {
        super(owner, title, false);
        configure(csvContent);
    }

    public StressResultsChartDialog(java.awt.Frame owner, String title, String csvContent) {
        super(owner, title, false);
        configure(csvContent);
    }

    private void configure(String csvContent) {
        setLayout(new BorderLayout(12, 12));
        getContentPane().setBackground(PANEL_COLOR);
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        List<AggregatedRow> rows = aggregate(csvContent);
        add(buildSummaryPanel(rows), BorderLayout.NORTH);
        add(buildChartsPanel(rows), BorderLayout.CENTER);

        setPreferredSize(new Dimension(980, 760));
        pack();
    }

    private JPanel buildSummaryPanel(List<AggregatedRow> rows) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(true);
        panel.setBackground(PANEL_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel title = new JLabel("Resumen del último CSV");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        panel.add(title);

        if (rows.isEmpty()) {
            panel.add(new JLabel("No se encontraron filas válidas para graficar."));
            return panel;
        }

        int minClients = rows.stream().mapToInt(AggregatedRow::clients).min().orElse(0);
        int maxClients = rows.stream().mapToInt(AggregatedRow::clients).max().orElse(0);
        int totalRepetitions = rows.stream().mapToInt(AggregatedRow::samples).sum();

        panel.add(new JLabel("Configuraciones de clientes: " + minClients + " a " + maxClients));
        panel.add(new JLabel("Puntos graficados: " + rows.size() + " | repeticiones agregadas: " + totalRepetitions));
        panel.add(new JLabel("Series mostradas: latencia promedio de registro/juego y éxito de registro/juego."));
        return panel;
    }

    private JPanel buildChartsPanel(List<AggregatedRow> rows) {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);

        if (rows.isEmpty()) {
            JLabel emptyLabel = new JLabel("No hay datos suficientes para generar la gráfica.", SwingConstants.CENTER);
            panel.add(emptyLabel, BorderLayout.CENTER);
            return panel;
        }

        JPanel charts = new JPanel();
        charts.setOpaque(false);
        charts.setLayout(new BoxLayout(charts, BoxLayout.Y_AXIS));
        charts.add(new MetricChartPanel(
                "Latencia promedio (ms)",
                "ms",
                rows,
                Map.of(
                        "Registro", REGISTER_COLOR,
                        "Juego", GAME_COLOR),
                Map.of(
                        "Registro", rows.stream().map(row -> new ChartPoint(row.clients(), row.registerAvgMs())).toList(),
                        "Juego", rows.stream().map(row -> new ChartPoint(row.clients(), row.gameAvgMs())).toList())));
        charts.add(new MetricChartPanel(
                "Éxito promedio (%)",
                "%",
                rows,
                Map.of(
                        "Registro", REGISTER_COLOR,
                        "Juego", GAME_COLOR),
                Map.of(
                        "Registro", rows.stream().map(row -> new ChartPoint(row.clients(), row.registerSuccessPct())).toList(),
                        "Juego", rows.stream().map(row -> new ChartPoint(row.clients(), row.gameSuccessPct())).toList())));

        panel.add(charts, BorderLayout.CENTER);
        return panel;
    }

    private List<AggregatedRow> aggregate(String csvContent) {
        List<ParsedRow> parsedRows = parseRows(csvContent);
        Map<Integer, AggregateAccumulator> grouped = new TreeMap<>();
        for (ParsedRow row : parsedRows) {
            grouped.computeIfAbsent(row.clients(), ignored -> new AggregateAccumulator()).add(row);
        }

        List<AggregatedRow> aggregatedRows = new ArrayList<>();
        for (Map.Entry<Integer, AggregateAccumulator> entry : grouped.entrySet()) {
            aggregatedRows.add(entry.getValue().build(entry.getKey()));
        }
        aggregatedRows.sort(Comparator.comparingInt(AggregatedRow::clients));
        return aggregatedRows;
    }

    private List<ParsedRow> parseRows(String csvContent) {
        List<ParsedRow> rows = new ArrayList<>();
        if (csvContent == null || csvContent.isBlank()) {
            return rows;
        }

        String[] lines = csvContent.split("\\R");
        if (lines.length < 2) {
            return rows;
        }

        String[] header = lines[0].split(",");
        Map<String, Integer> indexes = new LinkedHashMap<>();
        for (int index = 0; index < header.length; index++) {
            indexes.put(header[index].trim(), index);
        }

        for (int lineIndex = 1; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] columns = line.split(",");
            try {
                rows.add(new ParsedRow(
                        parseInt(columns, indexes, "clients"),
                        parseDouble(columns, indexes, "register_avg_ms"),
                        parseDouble(columns, indexes, "register_success_pct"),
                        parseDouble(columns, indexes, "game_avg_ms"),
                        parseDouble(columns, indexes, "game_success_pct")));
            } catch (RuntimeException ignored) {
            }
        }
        return rows;
    }

    private int parseInt(String[] columns, Map<String, Integer> indexes, String column) {
        return Integer.parseInt(readColumn(columns, indexes, column));
    }

    private double parseDouble(String[] columns, Map<String, Integer> indexes, String column) {
        return Double.parseDouble(readColumn(columns, indexes, column));
    }

    private String readColumn(String[] columns, Map<String, Integer> indexes, String column) {
        Integer index = indexes.get(column);
        if (index == null || index >= columns.length) {
            throw new IllegalArgumentException("Columna faltante: " + column);
        }
        return columns[index].trim();
    }

    private record ParsedRow(
            int clients,
            double registerAvgMs,
            double registerSuccessPct,
            double gameAvgMs,
            double gameSuccessPct) {
    }

    private record AggregatedRow(
            int clients,
            int samples,
            double registerAvgMs,
            double registerSuccessPct,
            double gameAvgMs,
            double gameSuccessPct) {
    }

    private record ChartPoint(int x, double y) {
    }

    private static final class AggregateAccumulator {
        private int samples;
        private double registerAvgMsSum;
        private double registerSuccessPctSum;
        private double gameAvgMsSum;
        private double gameSuccessPctSum;

        private void add(ParsedRow row) {
            samples++;
            registerAvgMsSum += row.registerAvgMs();
            registerSuccessPctSum += row.registerSuccessPct();
            gameAvgMsSum += row.gameAvgMs();
            gameSuccessPctSum += row.gameSuccessPct();
        }

        private AggregatedRow build(int clients) {
            return new AggregatedRow(
                    clients,
                    samples,
                    registerAvgMsSum / samples,
                    registerSuccessPctSum / samples,
                    gameAvgMsSum / samples,
                    gameSuccessPctSum / samples);
        }
    }

    private static final class MetricChartPanel extends JPanel {

        private final String title;
        private final String unit;
        private final List<AggregatedRow> rows;
        private final Map<String, Color> colors;
        private final Map<String, List<ChartPoint>> series;

        private MetricChartPanel(String title, String unit, List<AggregatedRow> rows,
                                 Map<String, Color> colors, Map<String, List<ChartPoint>> series) {
            this.title = title;
            this.unit = unit;
            this.rows = rows;
            this.colors = colors;
            this.series = series;
            setOpaque(true);
            setBackground(PANEL_COLOR);
            setPreferredSize(new Dimension(920, 280));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR),
                    BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int left = 64;
            int right = getWidth() - 24;
            int top = 42;
            int bottom = getHeight() - 52;
            int chartWidth = Math.max(1, right - left);
            int chartHeight = Math.max(1, bottom - top);

            g2.setColor(AXIS_COLOR);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
            g2.drawString(title, left, 24);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
            g2.drawString("Clientes", right - 46, bottom + 34);
            g2.drawString(unit, 18, top - 10);

            double maxY = 0.0;
            for (List<ChartPoint> points : series.values()) {
                for (ChartPoint point : points) {
                    maxY = Math.max(maxY, point.y());
                }
            }
            maxY = maxY <= 0.0 ? 1.0 : maxY * 1.15;

            drawGrid(g2, left, top, chartWidth, chartHeight, maxY);
            drawAxes(g2, left, top, bottom, right);
            drawXTicks(g2, left, bottom, chartWidth);
            drawSeries(g2, left, top, chartWidth, chartHeight, maxY);
            drawLegend(g2, right - 170, top - 6);

            g2.dispose();
        }

        private void drawGrid(Graphics2D g2, int left, int top, int chartWidth, int chartHeight, double maxY) {
            g2.setColor(GRID_COLOR);
            g2.setFont(g2.getFont().deriveFont(11f));
            for (int step = 0; step <= 4; step++) {
                int y = top + (chartHeight * step / 4);
                g2.drawLine(left, y, left + chartWidth, y);
                double labelValue = maxY - (maxY * step / 4.0);
                g2.setColor(AXIS_COLOR);
                g2.drawString(String.format(Locale.US, "%.1f", labelValue), 20, y + 4);
                g2.setColor(GRID_COLOR);
            }
        }

        private void drawAxes(Graphics2D g2, int left, int top, int bottom, int right) {
            g2.setColor(AXIS_COLOR);
            g2.drawLine(left, top, left, bottom);
            g2.drawLine(left, bottom, right, bottom);
        }

        private void drawXTicks(Graphics2D g2, int left, int bottom, int chartWidth) {
            int minX = rows.stream().mapToInt(AggregatedRow::clients).min().orElse(0);
            int maxX = rows.stream().mapToInt(AggregatedRow::clients).max().orElse(minX);
            for (AggregatedRow row : rows) {
                int x = projectX(row.clients(), minX, maxX, left, chartWidth);
                g2.setColor(AXIS_COLOR);
                g2.drawLine(x, bottom, x, bottom + 5);
                g2.drawString(String.valueOf(row.clients()), x - 10, bottom + 20);
            }
        }

        private void drawSeries(Graphics2D g2, int left, int top, int chartWidth, int chartHeight, double maxY) {
            int minX = rows.stream().mapToInt(AggregatedRow::clients).min().orElse(0);
            int maxX = rows.stream().mapToInt(AggregatedRow::clients).max().orElse(minX);

            for (Map.Entry<String, List<ChartPoint>> entry : series.entrySet()) {
                Color color = colors.get(entry.getKey());
                List<ChartPoint> points = entry.getValue();
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2.5f));
                for (int index = 0; index < points.size(); index++) {
                    ChartPoint point = points.get(index);
                    int x = projectX(point.x(), minX, maxX, left, chartWidth);
                    int y = top + chartHeight - (int) Math.round((point.y() / maxY) * chartHeight);
                    g2.fill(new Ellipse2D.Double(x - 4, y - 4, 8, 8));
                    if (index > 0) {
                        ChartPoint previous = points.get(index - 1);
                        int previousX = projectX(previous.x(), minX, maxX, left, chartWidth);
                        int previousY = top + chartHeight - (int) Math.round((previous.y() / maxY) * chartHeight);
                        g2.draw(new Line2D.Double(previousX, previousY, x, y));
                    }
                }
            }
        }

        private void drawLegend(Graphics2D g2, int x, int y) {
            g2.setFont(g2.getFont().deriveFont(12f));
            int rowIndex = 0;
            for (Map.Entry<String, Color> entry : colors.entrySet()) {
                int rowY = y + (rowIndex * 18);
                g2.setColor(entry.getValue());
                g2.fillRect(x, rowY, 12, 12);
                g2.setColor(AXIS_COLOR);
                g2.drawString(entry.getKey(), x + 18, rowY + 11);
                rowIndex++;
            }
        }

        private int projectX(int value, int minX, int maxX, int left, int chartWidth) {
            if (minX == maxX) {
                return left + chartWidth / 2;
            }
            return left + (int) Math.round(((value - minX) * 1.0 / (maxX - minX)) * chartWidth);
        }
    }
}
