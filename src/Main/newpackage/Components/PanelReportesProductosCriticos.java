package Main.newpackage.Components;

import conexion.ConexionDB;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.sql.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.awt.Color;


public class PanelReportesProductosCriticos extends JPanel {
    // Colores oscuros
    private static final java.awt.Color BG_DARK = new java.awt.Color(31, 34, 45);
    private static final java.awt.Color PANEL_DARK = new java.awt.Color(38, 41, 53);
    private static final java.awt.Color HEADER_DARK = new java.awt.Color(44, 47, 60);
    private static final java.awt.Color BORDER_COLOR = new java.awt.Color(55, 60, 80);
    private static final java.awt.Color CARD_COLOR = new java.awt.Color(42, 47, 61);
    private static final java.awt.Color TITLE_BLUE = new java.awt.Color(80, 170, 255);
    private static final java.awt.Color BUTTON_GREEN = new java.awt.Color(40, 180, 99);
    private static final java.awt.Color BUTTON_BLUE = new java.awt.Color(35, 136, 229);
    private static final java.awt.Color CRITICAL_COLOR = new java.awt.Color(220, 80, 70);
    private static final java.awt.Color WARNING_COLOR = new java.awt.Color(255, 180, 70);
    private static final java.awt.Color FG_NORMAL = new java.awt.Color(230, 230, 240);

    // Fuentes
    private static final java.awt.Font HEADER_FONT = new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 17);
    private static final java.awt.Font TITLE_FONT = new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18);
    private static final java.awt.Font CELL_FONT   = new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 15);
    private static final java.awt.Font BTN_FONT    = new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14);

    private JTable tablaMasPedidos, tablaPorAcabarse, tablaUrgenteComprar;
    private DefaultTableModel modeloMasPedidos, modeloPorAcabarse, modeloUrgenteComprar;
    private Paginador paginadorMasPedidos, paginadorPorAcabarse, paginadorUrgenteComprar;
    private final int LIMITE = 30;

    public PanelReportesProductosCriticos() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 20));

        modeloMasPedidos = new DefaultTableModel(
                new String[]{"Código", "Modelo", "Pedidos Totales", "Existencia", "Unidad"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) { return (c == 2 || c == 3) ? Double.class : String.class; }
        };
        tablaMasPedidos = crearTabla(modeloMasPedidos);
        paginadorMasPedidos = new Paginador(tablaMasPedidos, modeloMasPedidos);
        JPanel cardMasPedidos = crearCard("PRODUCTOS MÁS SOLICITADOS (" + LIMITE + ")", tablaMasPedidos, paginadorMasPedidos, modeloMasPedidos);

        modeloPorAcabarse = new DefaultTableModel(
                new String[]{"Código", "Modelo", "Existencia", "Unidad", "Stock mínimo"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) { return (c == 2 || c == 4) ? Double.class : String.class; }
        };
        tablaPorAcabarse = crearTabla(modeloPorAcabarse);
        paginadorPorAcabarse = new Paginador(tablaPorAcabarse, modeloPorAcabarse);
        JPanel cardPorAcabarse = crearCard("PRODUCTOS POR AGOTARSE (" + LIMITE + ")", tablaPorAcabarse, paginadorPorAcabarse, modeloPorAcabarse);

        modeloUrgenteComprar = new DefaultTableModel(
                new String[]{"Código", "Modelo", "Existencia", "Unidad", "Stock mínimo"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) { return (c == 2 || c == 4) ? Double.class : String.class; }
        };
        tablaUrgenteComprar = crearTabla(modeloUrgenteComprar);
        paginadorUrgenteComprar = new Paginador(tablaUrgenteComprar, modeloUrgenteComprar);
        JPanel cardUrgente = crearCard("PRODUCTOS URGENTES (" + LIMITE + ")", tablaUrgenteComprar, paginadorUrgenteComprar, modeloUrgenteComprar);

        add(cardMasPedidos);
        add(cardPorAcabarse);
        add(cardUrgente);

        cargarDatos();
    }

    private JPanel crearCard(String titulo, JTable tabla, Paginador paginador, DefaultTableModel modelo) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(CARD_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
            new EmptyBorder(10,10,18,10),
            BorderFactory.createLineBorder(BORDER_COLOR, 2, true)
        ));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(TITLE_FONT);
        lblTitulo.setForeground(TITLE_BLUE);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(lblTitulo, BorderLayout.WEST);

        JPanel panelBtns = new JPanel();
        panelBtns.setOpaque(false);

        JButton btnExcel = new JButton("Exportar Excel");
       btnExcel.setBackground(BUTTON_GREEN);
btnExcel.setForeground(java.awt.Color.WHITE);
btnExcel.setFont(BTN_FONT);

        btnExcel.setFocusPainted(false);
        btnExcel.setBorder(new RoundBorder(12));
        btnExcel.addActionListener(e -> exportarTabla(tabla, modelo, "excel"));
        panelBtns.add(btnExcel);

        JButton btnCSV = new JButton("Exportar CSV");
        btnCSV.setBackground(BUTTON_BLUE); btnCSV.setForeground(Color.WHITE); btnCSV.setFont(BTN_FONT);
        btnCSV.setFocusPainted(false);
        btnCSV.setBorder(new RoundBorder(12));
        btnCSV.addActionListener(e -> exportarTabla(tabla, modelo, "csv"));
        panelBtns.add(btnCSV);

        topPanel.add(panelBtns, BorderLayout.CENTER);

        card.add(topPanel, BorderLayout.NORTH);
        card.add(new JScrollPane(tabla), BorderLayout.CENTER);
        card.add(paginador.getPanel(), BorderLayout.SOUTH);

        card.setMaximumSize(new Dimension(1200, 220));
        card.setPreferredSize(new Dimension(1150, 210));
        return card;
    }

    private JTable crearTabla(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(CELL_FONT);
        table.setForeground(FG_NORMAL);
        table.setBackground(PANEL_DARK);
        table.setGridColor(BORDER_COLOR);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(HEADER_FONT);
        header.setBackground(HEADER_DARK);
        header.setForeground(TITLE_BLUE);
        header.setReorderingAllowed(false);

        table.setDefaultRenderer(Object.class, new CriticalCellRenderer());
        return table;
    }

    private class CriticalCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (isSelected) {
                c.setBackground(new java.awt.Color(80,110,160));
                c.setForeground(Color.WHITE);
            } else {
                c.setBackground(PANEL_DARK);
                c.setForeground(FG_NORMAL);
            }

            if (value instanceof Number) {
                double v = ((Number)value).doubleValue();
                if (v <= 0) c.setForeground(CRITICAL_COLOR);
                else if (v <= 5) c.setForeground(WARNING_COLOR);
            }
            return c;
        }
    }

    private class Paginador {
        private JPanel panel;
        private JLabel lblPagina;
        private JButton btnAnterior, btnSiguiente;
        private int paginaActual = 1, totalPaginas = 1;
        public Paginador(JTable tabla, DefaultTableModel modelo) {
            panel = new JPanel();
            panel.setOpaque(false);
            panel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 3));
            btnAnterior = new JButton("<");
            btnAnterior.setFont(BTN_FONT);
            btnAnterior.setBackground(BORDER_COLOR);
            btnAnterior.setForeground(Color.WHITE);
            btnAnterior.setFocusPainted(false);
            btnAnterior.setBorder(new RoundBorder(10));
            btnAnterior.setEnabled(false);

            btnSiguiente = new JButton(">");
            btnSiguiente.setFont(BTN_FONT);
            btnSiguiente.setBackground(BORDER_COLOR);
            btnSiguiente.setForeground(Color.WHITE);
            btnSiguiente.setFocusPainted(false);
            btnSiguiente.setBorder(new RoundBorder(10));
            btnSiguiente.setEnabled(false);

            lblPagina = new JLabel("Página 1/1");
            lblPagina.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
            lblPagina.setForeground(TITLE_BLUE);

            panel.add(btnAnterior);
            panel.add(lblPagina);
            panel.add(btnSiguiente);
        }
        public JPanel getPanel() { return panel; }
    }

    private void exportarTabla(JTable tabla, DefaultTableModel modelo, String tipo) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar tabla a " + (tipo.equals("excel") ? "Excel" : "CSV"));
        int resp = chooser.showSaveDialog(this);
        if (resp != JFileChooser.APPROVE_OPTION) return;
        String path = chooser.getSelectedFile().getAbsolutePath();
        if (tipo.equals("excel") && !path.endsWith(".xlsx")) path += ".xlsx";
        if (tipo.equals("csv") && !path.endsWith(".csv")) path += ".csv";

        try {
            if (tipo.equals("csv")) {
                try (FileWriter fw = new FileWriter(path)) {
                    for (int c = 0; c < modelo.getColumnCount(); c++) {
                        fw.write(modelo.getColumnName(c));
                        if (c < modelo.getColumnCount() - 1) fw.write(",");
                    }
                    fw.write("\n");
                    for (int r = 0; r < modelo.getRowCount(); r++) {
                        for (int c = 0; c < modelo.getColumnCount(); c++) {
                            fw.write(String.valueOf(modelo.getValueAt(r, c)));
                            if (c < modelo.getColumnCount() - 1) fw.write(",");
                        }
                        fw.write("\n");
                    }
                }
            } else { // Excel
                Workbook wb = new XSSFWorkbook();
                Sheet sheet = wb.createSheet("Reporte");
                Row header = sheet.createRow(0);
                for (int c = 0; c < modelo.getColumnCount(); c++) {
                    header.createCell(c).setCellValue(modelo.getColumnName(c));
                }
                for (int r = 0; r < modelo.getRowCount(); r++) {
                    Row row = sheet.createRow(r + 1);
                    for (int c = 0; c < modelo.getColumnCount(); c++) {
                        Object val = modelo.getValueAt(r, c);
                        if (val instanceof Number)
                            row.createCell(c).setCellValue(((Number)val).doubleValue());
                        else
                            row.createCell(c).setCellValue(val.toString());
                    }
                }
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(path)) {
                    wb.write(fos);
                }
            }
            JOptionPane.showMessageDialog(this, "Exportación exitosa.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error exportando: " + e.getMessage());
        }
    }

    public void cargarDatos() {
        cargarMasPedidos();
        cargarPorAcabarse();
        cargarUrgenteComprar();
    }
    private void cargarMasPedidos() {
        modeloMasPedidos.setRowCount(0);
        String sql = """
            SELECT p.codigo_barras, p.modelo, SUM(m.cantidad) AS pedidos_totales,
                   p.existencia, p.unidad
            FROM movimientos m
            JOIN productos p ON m.id_producto = p.id
            WHERE m.tipo = 'salida'
            GROUP BY p.codigo_barras, p.modelo, p.existencia, p.unidad
            ORDER BY pedidos_totales DESC
            LIMIT ?
        """;
        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, LIMITE);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                modeloMasPedidos.addRow(new Object[]{
                    rs.getString("codigo_barras"),
                    rs.getString("modelo"),
                    rs.getDouble("pedidos_totales"),
                    rs.getDouble("existencia"),
                    rs.getString("unidad")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    private void cargarPorAcabarse() {
        modeloPorAcabarse.setRowCount(0);
        String sql = """
            SELECT codigo_barras, modelo, existencia, unidad, stock_minimo
            FROM productos
            WHERE existencia > 0 AND existencia <= stock_minimo + 3
            ORDER BY existencia ASC
            LIMIT ?
        """;
        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, LIMITE);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                modeloPorAcabarse.addRow(new Object[]{
                    rs.getString("codigo_barras"),
                    rs.getString("modelo"),
                    rs.getDouble("existencia"),
                    rs.getString("unidad"),
                    rs.getDouble("stock_minimo")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    private void cargarUrgenteComprar() {
        modeloUrgenteComprar.setRowCount(0);
        String sql = """
            SELECT codigo_barras, modelo, existencia, unidad, stock_minimo
            FROM productos
            WHERE existencia <= 0 OR existencia <= stock_minimo
            ORDER BY existencia ASC
            LIMIT ?
        """;
        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, LIMITE);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                modeloUrgenteComprar.addRow(new Object[]{
                    rs.getString("codigo_barras"),
                    rs.getString("modelo"),
                    rs.getDouble("existencia"),
                    rs.getString("unidad"),
                    rs.getDouble("stock_minimo")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Bordes redondeados para los botones
    static class RoundBorder extends AbstractBorder {
        private int radius;
        public RoundBorder(int radius) { this.radius = radius; }
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BORDER_COLOR);
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
        }
        public Insets getBorderInsets(Component c) { return new Insets(4, 8, 4, 8); }
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.right = 8;
            insets.top = insets.bottom = 4;
            return insets;
        }
    }
}
