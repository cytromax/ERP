package Main.newpackage;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ChartUtils;
import org.jfree.data.category.*;
import org.jfree.data.general.*;
import java.awt.image.BufferedImage;
import org.jfree.chart.ui.RectangleInsets;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class VentanaGraficas extends JFrame {
    // --- INICIO: Singleton para evitar varias ventanas ---
private static VentanaGraficas instanciaUnica = null;

public static void mostrarVentana() {
    if (instanciaUnica == null || !instanciaUnica.isDisplayable()) {
        instanciaUnica = new VentanaGraficas();
    }
    instanciaUnica.setVisible(true);
    instanciaUnica.toFront();
    instanciaUnica.requestFocus();
}
// --- FIN: Singleton ---


    private JComboBox<String> comboMes;
    private JButton btnMenosUsados, btnPieChart, btnLineChart, btnExportar, btnSalir, btnBarChart;
    private ChartPanel chartPanel;
    private boolean mostrandoMenosUsados = false;
    private String chartType = "bar"; // bar, pie, line

    public VentanaGraficas() {
        setTitle("Dashboards y Gráficas de Productos");
        setSize(1100, 670);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));
        getContentPane().setBackground(new Color(24, 27, 33)); // TEMA OSCURO

        // --- PANEL SUPERIOR PROFESIONAL CON SCROLL DE BOTONES ---
        JPanel panelFiltros = new JPanel(new BorderLayout());
        panelFiltros.setBackground(new Color(24, 27, 33));

        // 1) Filtros (label + combo)
        JPanel panelFiltroMes = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 14));
        panelFiltroMes.setBackground(new Color(24, 27, 33));
        comboMes = new JComboBox<>();
        comboMes.addItem("Todo el año");
        for (int i = 1; i <= 12; i++) {
            comboMes.addItem(mesNombre(i));
        }
        panelFiltroMes.add(estiliza(new JLabel("Filtrar por mes:"), 16, Font.PLAIN, Color.WHITE));
        panelFiltroMes.add(comboMes);

        // 2) Botones alineados en un panel con FlowLayout
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 14));
        panelBotones.setBackground(new Color(24, 27, 33));
        btnMenosUsados = estilizaBtn(new JButton("Ver productos menos pedidos"));
        btnBarChart    = estilizaBtn(new JButton("Gráfica Barras"));
        btnPieChart    = estilizaBtn(new JButton("Gráfica Pie"));
        btnLineChart   = estilizaBtn(new JButton("Gráfica Lineal"));
        btnExportar    = estilizaBtn(new JButton("Exportar gráfica"));
        btnSalir       = estilizaBtn(new JButton("Salir"));

        panelBotones.add(btnMenosUsados);
        panelBotones.add(btnBarChart);
        panelBotones.add(btnPieChart);
        panelBotones.add(btnLineChart);
        panelBotones.add(btnExportar);
        panelBotones.add(btnSalir);

        // 3) Envuelve panelBotones en un JScrollPane horizontal
        JScrollPane scrollBotones = new JScrollPane(
            panelBotones,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollBotones.setBorder(null);
        scrollBotones.setPreferredSize(new Dimension(900, 65));
        scrollBotones.getViewport().setBackground(new Color(24, 27, 33));

        // 4) Agrega ambos a panelFiltros
        panelFiltros.add(panelFiltroMes, BorderLayout.WEST);
        panelFiltros.add(scrollBotones, BorderLayout.CENTER);

        add(panelFiltros, BorderLayout.NORTH);

        // Panel gráfico
        chartPanel = new ChartPanel(null);
        chartPanel.setPreferredSize(new Dimension(1000, 540));
        chartPanel.setBackground(new Color(28, 32, 38));
        add(chartPanel, BorderLayout.CENTER);

        // Eventos
        comboMes.addActionListener(e -> actualizarGrafica());
        btnMenosUsados.addActionListener(e -> {
            mostrandoMenosUsados = !mostrandoMenosUsados;
            btnMenosUsados.setText(mostrandoMenosUsados
                    ? "Ver productos más pedidos"
                    : "Ver productos menos pedidos");
            actualizarGrafica();
        });
        btnBarChart.addActionListener(e -> {
            chartType = "bar";
            actualizarGrafica();
        });
        btnPieChart.addActionListener(e -> {
            chartType = "pie";
            actualizarGrafica();
        });
        btnLineChart.addActionListener(e -> {
            chartType = "line";
            actualizarGrafica();
        });
        btnExportar.addActionListener(e -> exportarGrafica());
        btnSalir.addActionListener(e -> dispose());

        // Inicial
        actualizarGrafica();
    }

    private JButton estilizaBtn(JButton btn) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(new Color(38, 92, 182));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel estiliza(JLabel lbl, int size, int style, Color color) {
        lbl.setFont(new Font("Segoe UI", style, size));
        lbl.setForeground(color);
        return lbl;
    }

    // --- CONSULTA USANDO ConexionDB.conectar() ---
    private void obtenerDatosGrafica(int mesSeleccionado, boolean menosUsados, List<String> productos, List<Double> valores) {
        productos.clear();
        valores.clear();

        String sql
            = "SELECT p.modelo, ABS(SUM(m.cantidad)) AS total "
            + "FROM productos p JOIN movimientos m ON p.id = m.id_producto "
            + "WHERE m.tipo = 'salida' "
            + (mesSeleccionado > 0 ? "AND EXTRACT(MONTH FROM m.fecha) = ? " : "")
            + "GROUP BY p.modelo "
            + "ORDER BY total " + (menosUsados ? "ASC" : "DESC") + " LIMIT 10";

        try (Connection con = conexion.ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (mesSeleccionado > 0) {
                ps.setInt(1, mesSeleccionado);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    productos.add(rs.getString("modelo"));
                    valores.add(rs.getDouble("total"));
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar datos de gráfica: " + e.getMessage());
        }
    }

    private void actualizarGrafica() {
        int mesSeleccionado = comboMes.getSelectedIndex();
        List<String> productos = new ArrayList<>();
        List<Double> valores  = new ArrayList<>();

        obtenerDatosGrafica(mesSeleccionado, mostrandoMenosUsados, productos, valores);

        String titulo = (mostrandoMenosUsados ? "Productos menos pedidos" : "Productos más pedidos");
        if (mesSeleccionado > 0) {
            titulo += " en " + comboMes.getSelectedItem();
        }

        JFreeChart chart = null;

        if (productos.isEmpty()) {
            DefaultCategoryDataset vacio = new DefaultCategoryDataset();
            vacio.addValue(0, "Sin datos", "");
            chart = ChartFactory.createBarChart(
                titulo, "Producto", "Veces pedido", vacio,
                PlotOrientation.VERTICAL, false, true, false
            );
            TextTitle t = new TextTitle("NO HAY DATOS PARA MOSTRAR", new Font("Segoe UI", Font.BOLD, 21));
            t.setPaint(Color.WHITE);
            chart.setTitle(t);
            chart.getCategoryPlot().setBackgroundPaint(new Color(38, 44, 53));
            ponerMarcaAgua(chart);
            estilizaGrafica(chart);
            chartPanel.setChart(chart);
            return;
        }

        switch (chartType) {
            case "pie":
                DefaultPieDataset pieDS = new DefaultPieDataset();
                for (int i = 0; i < productos.size(); i++) {
                    pieDS.setValue(productos.get(i), valores.get(i));
                }
                chart = ChartFactory.createPieChart(titulo, pieDS, true, true, false);
                PiePlot plot = (PiePlot) chart.getPlot();
                plot.setBackgroundPaint(new Color(38, 44, 53));
                plot.setOutlineVisible(false);
                plot.setLabelGenerator(null);
                if (chart.getLegend() != null) {
                    chart.getLegend().setItemFont(new Font("Segoe UI", Font.BOLD, 13));
                    chart.getLegend().setBackgroundPaint(new Color(38, 44, 53));
                    chart.getLegend().setItemPaint(Color.WHITE);
                }
                break;

            case "line":
                DefaultCategoryDataset lineDS = new DefaultCategoryDataset();
                for (int i = 0; i < productos.size(); i++) {
                    lineDS.addValue(valores.get(i), "Veces pedido", productos.get(i));
                }
                chart = ChartFactory.createLineChart(
                    titulo, "Producto", "Veces pedido", lineDS,
                    PlotOrientation.VERTICAL, false, true, false
                );
                break;

            default: // "bar"
                DefaultCategoryDataset barDS = new DefaultCategoryDataset();
                for (int i = 0; i < productos.size(); i++) {
                    barDS.addValue(valores.get(i), "Veces pedido", productos.get(i));
                }
                chart = ChartFactory.createBarChart(
                    titulo, "Producto", "Veces pedido", barDS,
                    PlotOrientation.VERTICAL, false, true, false
                );
                break;
        }

        ponerMarcaAgua(chart);
        estilizaGrafica(chart);
        chartPanel.setChart(chart);
    }

    private void estilizaGrafica(JFreeChart chart) {
        chart.setBackgroundPaint(new Color(24, 27, 33));
        TextTitle tt = new TextTitle(
            chart.getTitle().getText(),
            new Font("Segoe UI", Font.BOLD, 22)
        );
        tt.setPaint(Color.WHITE);
        chart.setTitle(tt);

        if (chart.getPlot() instanceof CategoryPlot) {
            CategoryPlot p = (CategoryPlot) chart.getPlot();
            p.setBackgroundPaint(new Color(38, 44, 53));
            p.setDomainGridlinePaint(new Color(90, 90, 100));
            p.setRangeGridlinePaint(new Color(90, 90, 100));
            p.getRenderer().setSeriesPaint(0, new Color(80, 164, 255));
            p.getDomainAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 14));
            p.getRangeAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 14));
            p.getDomainAxis().setLabelFont(new Font("Segoe UI", Font.BOLD, 15));
            p.getDomainAxis().setLabelPaint(Color.WHITE);
            p.getRangeAxis().setLabelFont(new Font("Segoe UI", Font.BOLD, 15));
            p.getRangeAxis().setLabelPaint(Color.WHITE);
            p.getDomainAxis().setTickLabelPaint(Color.WHITE);
            p.getRangeAxis().setTickLabelPaint(Color.WHITE);
        } else if (chart.getPlot() instanceof PiePlot) {
            PiePlot p = (PiePlot) chart.getPlot();
            p.setBackgroundPaint(new Color(38, 44, 53));
            p.setOutlineVisible(false);
        }
    }

    private void ponerMarcaAgua(JFreeChart chart) {
        try {
            URL logoURL = getClass().getResource("/images/viveza-textil-logo.png");
            if (logoURL == null) return;
            Image logo = ImageIO.read(logoURL);
            int ancho = 300, alto = 80;
            BufferedImage marca = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = marca.createGraphics();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
            int w = 150, h = 40;
            g2.drawImage(logo.getScaledInstance(w, h, Image.SCALE_SMOOTH),
                         (ancho - w) / 2, (alto - h) / 2, w, h, null);
            g2.dispose();
            chart.getPlot().setBackgroundImage(marca);
            chart.getPlot().setBackgroundImageAlignment(0);
        } catch (Exception ex) {
            System.err.println("No se pudo cargar el logo: " + ex.getMessage());
        }
    }

    private void exportarGrafica() {
        JFreeChart chart = chartPanel.getChart();
        if (chart == null) {
            JOptionPane.showMessageDialog(this, "No hay gráfica para exportar.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar gráfica como imagen PNG");
        chooser.setSelectedFile(new java.io.File("grafica.png"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ChartUtils.saveChartAsPNG(
                    chooser.getSelectedFile(),
                    chart,
                    chartPanel.getWidth(),
                    chartPanel.getHeight()
                );
                JOptionPane.showMessageDialog(this, "¡Exportado con éxito!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al exportar: " + ex.getMessage());
            }
        }
    }

    private String mesNombre(int mes) {
        String[] meses = {
            "Enero","Febrero","Marzo","Abril","Mayo","Junio",
            "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
        };
        return meses[mes - 1];
    }
}
