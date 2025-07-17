package Main.newpackage;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import Main.newpackage.Components.PanelReportesProductosCriticos;
import Main.newpackage.Components.PanelTablaProductos;
import conexion.ConexionDB;

public class VerProductos extends JFrame {

    // --- Variables de instancia ---
    private JTable tabla;
    private DefaultTableModel modelo;
    private String usuarioActual;
    private RolUsuario rolActual; // USAMOS ENUM, NO STRING
    private JTextField txtBuscar;
    private PanelTablaProductos tablaPanel;

    // --- Variables de paginación ---
    private int paginaActual = 1;
    private final int tamanoPagina = 25;
    private int totalPaginas = 1;
    private JButton btnAnterior, btnSiguiente;
    private JLabel lblPaginacion;
    private String ultimoFiltro = "";

    // --- SINGLETON: Instancia única por usuario/rol ---
    private static VerProductos instanciaUnica = null;
    private static String usuarioUnico = null;
    private static RolUsuario rolUnico = null;

    public static void mostrarVentana(String usuario, RolUsuario rol) {
        if (instanciaUnica == null || !instanciaUnica.isDisplayable() ||
                !usuario.equals(usuarioUnico) || rol != rolUnico) {
            instanciaUnica = new VerProductos(usuario, rol);
            usuarioUnico = usuario;
            rolUnico = rol;
        }
        instanciaUnica.setVisible(true);
        instanciaUnica.toFront();
        instanciaUnica.requestFocus();
    }

    public VerProductos(String usuarioActual, RolUsuario rolActual) {
        this.usuarioActual = usuarioActual;
        this.rolActual = rolActual;

        setTitle("Productos Críticos: Más Pedidos y Bajo Stock");
        setSize(1280, 720);
        setMinimumSize(new Dimension(960, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(new Color(30, 33, 40));
        setLayout(new BorderLayout(0, 0));

        add(crearPanelHeader(), BorderLayout.NORTH);
        add(crearPanelBusqueda(), BorderLayout.CENTER);
        tablaPanel = crearPanelTablaProductos();
        add(tablaPanel, BorderLayout.SOUTH);

        cargarPaginaProductos("", 1);
    }

    // --- Panel Tabla Productos (usa tu componente modular) ---
    private PanelTablaProductos crearPanelTablaProductos() {
        modelo = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                boolean mostrarAcciones = !Permisos.esSoloConsulta(rolActual) && rolActual != RolUsuario.TRABAJADOR;
                return mostrarAcciones && (column == 5 || column == 6);
            }
        };

        // Columnas base
        modelo.addColumn("Código de barras");
        modelo.addColumn("Descripción");
        modelo.addColumn("Existencia");
        modelo.addColumn("Unidad");
        modelo.addColumn("Ubicación");

        // Solo roles con permisos ven acciones
        boolean mostrarAcciones = !Permisos.esSoloConsulta(rolActual) && rolActual != RolUsuario.TRABAJADOR;
        if (mostrarAcciones) {
            modelo.addColumn("Editar");
            modelo.addColumn("Eliminar");
        }

        tabla = new JTable(modelo);
        tabla.setRowHeight(27);
        tabla.setBackground(new Color(37, 41, 52));
        tabla.setForeground(Color.WHITE);
        tabla.setGridColor(new Color(70, 80, 100, 40));
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 16));
        tabla.getTableHeader().setBackground(new Color(25, 41, 61));
        tabla.getTableHeader().setForeground(Color.WHITE);

        tabla.getColumnModel().getColumn(2).setCellRenderer(new ColorCantidadRenderer());

        // Renderizadores y editores SOLO si hay columnas de acción
        if (mostrarAcciones) {
            tabla.getColumn("Editar").setCellRenderer(new ButtonRenderer("Editar"));
            tabla.getColumn("Editar").setCellEditor(new ButtonEditor(new JCheckBox(), "Editar", tabla, this));
            tabla.getColumn("Eliminar").setCellRenderer(new ButtonRenderer("Eliminar"));
            tabla.getColumn("Eliminar").setCellEditor(new ButtonEditor(new JCheckBox(), "Eliminar", tabla, this));
        }

        // Retorna el panel modular con el modelo y la tabla
        return new PanelTablaProductos(rolActual, modelo, tabla);
    }

    // --- Panel Header/Botones principales ---
    private JPanel crearPanelHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 32, 0, 32));

        // Logo
        JLabel lblLogo = new JLabel();
        lblLogo.setHorizontalAlignment(SwingConstants.CENTER);
        lblLogo.setVerticalAlignment(SwingConstants.CENTER);
        lblLogo.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));
        try {
            URL urlLogo = getClass().getResource("/images/viveza-textil-logo.png");
            if (urlLogo != null) {
                BufferedImage img = ImageIO.read(urlLogo);
                img = reemplazarTonosOscurosPorBlanco(img);
                int maxW = 240, maxH = 60;
                int w = img.getWidth(), h = img.getHeight();
                double scale = Math.min((double) maxW / w, (double) maxH / h);
                int newW = (int) (w * scale);
                int newH = (int) (h * scale);
                Image scaled = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
                lblLogo.setIcon(new ImageIcon(scaled));
            }
        } catch (IOException e) {
            System.err.println("Logo error: " + e.getMessage());
        }
        panel.add(lblLogo, BorderLayout.NORTH);

        // Botones principales
        JPanel panelBotones = new JPanel(new GridLayout(2, 5, 18, 10));
        panelBotones.setOpaque(false);

        String[] botones = {
            "Entregar Productos",
            "Recibir Productos",
            "Agregar Producto",
            "Historial de Movimientos",
            "Imprimir Cód. Barras",
            "Ajuste de Inventario",
            "Historial de Ajustes",
            "Ver Gráficas",
            "Productos Criticos",
            "Salir"
        };

        java.awt.event.ActionListener[] acciones = {
            e -> abrirPrepararPedido(),
            e -> abrirRecepcionMercancia(),
            e -> abrirGestionProductos(),
            e -> abrirHistorialMovimientos(),
            e -> new ImprimirCodigoBarras().setVisible(true),
            e -> abrirAjusteInventario(),
            e -> abrirHistorialAjustes(),
            e -> abrirGraficas(),
            e -> abrirProductosCriticos(),
            e -> dispose()
        };

        // Agrega solo los botones permitidos por rol
        for (int i = 0; i < botones.length && i < acciones.length; i++) {
            boolean mostrar = true;
            if (rolActual == RolUsuario.TRABAJADOR) {
                mostrar = (i == 0 || i == 1 || i == 4 || i == 9);
            } else if (Permisos.esSoloConsulta(rolActual)) {
                mostrar = (i == 8 || i == 9);
            }
            if (mostrar) {
                JButton btn = crearBotonBarra(botones[i], acciones[i]);
                panelBotones.add(btn);
            }
        }
        panel.add(panelBotones, BorderLayout.CENTER);
        return panel;
    }

    // --- Panel de búsqueda y paginación ---
    private JPanel crearPanelBusqueda() {
        JPanel panelBusqueda = new JPanel();
        panelBusqueda.setOpaque(false);
        panelBusqueda.setLayout(new BoxLayout(panelBusqueda, BoxLayout.LINE_AXIS));
        panelBusqueda.setBorder(BorderFactory.createEmptyBorder(18, 32, 12, 32));

        txtBuscar = new JTextField("Buscar...");
        txtBuscar.setPreferredSize(new Dimension(360, 32));
        txtBuscar.setMaximumSize(new Dimension(360, 32));
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtBuscar.setForeground(Color.GRAY);

        txtBuscar.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (txtBuscar.getText().equals("Buscar...")) {
                    txtBuscar.setText("");
                    txtBuscar.setForeground(Color.BLACK);
                }
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (txtBuscar.getText().isEmpty()) {
                    txtBuscar.setText("Buscar...");
                    txtBuscar.setForeground(Color.GRAY);
                }
            }
        });
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { buscarSiNoPlaceholder(); }
            public void removeUpdate(DocumentEvent e) { buscarSiNoPlaceholder(); }
            public void changedUpdate(DocumentEvent e) { buscarSiNoPlaceholder(); }
        });

        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.setFont(new Font("Segoe UI Semibold", Font.BOLD, 15));
        btnBuscar.setBackground(new Color(35, 136, 229));
        btnBuscar.setForeground(Color.WHITE);
        btnBuscar.setFocusPainted(false);
        btnBuscar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBuscar.setBorder(new RoundBorder(12));
        btnBuscar.setPreferredSize(new Dimension(110, 32));
        btnBuscar.addActionListener(e -> buscarSiNoPlaceholder());

        JLabel icon = new JLabel("\uD83D\uDD0D ");
        icon.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        icon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        btnAnterior = new JButton("<");
        btnAnterior.setEnabled(false);
        btnAnterior.addActionListener(e -> {
            if (paginaActual > 1) cargarPaginaProductos(ultimoFiltro, paginaActual - 1);
        });

        btnSiguiente = new JButton(">");
        btnSiguiente.setEnabled(false);
        btnSiguiente.addActionListener(e -> {
            if (paginaActual < totalPaginas) cargarPaginaProductos(ultimoFiltro, paginaActual + 1);
        });

        lblPaginacion = new JLabel("Página 1/1");
        lblPaginacion.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblPaginacion.setForeground(new Color(35, 136, 229));
        lblPaginacion.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

        panelBusqueda.add(icon);
        panelBusqueda.add(txtBuscar);
        panelBusqueda.add(Box.createRigidArea(new Dimension(14, 0)));
        panelBusqueda.add(btnBuscar);
        panelBusqueda.add(Box.createHorizontalGlue());
        panelBusqueda.add(btnAnterior);
        panelBusqueda.add(lblPaginacion);
        panelBusqueda.add(btnSiguiente);
        return panelBusqueda;
    }

    // --- Lógica para cargar productos, ordenados según reglas ---
    private void cargarPaginaProductos(String filtro, int pagina) {
        SwingUtilities.invokeLater(() -> {
            tablaPanel.getModelo().setRowCount(0);
            ultimoFiltro = filtro;
            paginaActual = pagina;

            int totalRegistros = contarTotalProductos(filtro);
            totalPaginas = (int) Math.ceil((double) totalRegistros / tamanoPagina);
            if (totalPaginas == 0) totalPaginas = 1;
            lblPaginacion.setText("Página " + paginaActual + " / " + totalPaginas);
            btnAnterior.setEnabled(paginaActual > 1);
            btnSiguiente.setEnabled(paginaActual < totalPaginas);

            int offset = (paginaActual - 1) * tamanoPagina;

            String sql =
                "SELECT p.codigo_barras, p.modelo, p.existencia, p.unidad, p.ubicacion, " +
                "COALESCE(SUM(CASE WHEN m.tipo = 'salida' THEN m.cantidad ELSE 0 END), 0) AS total_salidas " +
                "FROM productos p " +
                "LEFT JOIN movimientos m ON m.id_producto = p.id " +
                "GROUP BY p.id, p.codigo_barras, p.modelo, p.existencia, p.unidad, p.ubicacion " +
                "ORDER BY " +
                "CASE " +
                "    WHEN p.existencia < 0 THEN 4 " +
                "    WHEN p.existencia = 0 THEN 3 " +
                "    WHEN p.existencia <= 10 THEN 2 " +
                "    ELSE 1 " +
                "END, total_salidas DESC " +
                "LIMIT 130";

            try (Connection con = ConexionDB.conectar();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                try (ResultSet rs = ps.executeQuery()) {
                    int count = 0, skip = offset;
                    boolean mostrarAcciones = !Permisos.esSoloConsulta(rolActual) && rolActual != RolUsuario.TRABAJADOR;
                    while (rs.next()) {
                        String codigo = rs.getString("codigo_barras");
                        String modeloStr = rs.getString("modelo");
                        if (!filtro.isEmpty() && !codigo.toLowerCase().contains(filtro.toLowerCase())
                                && !modeloStr.toLowerCase().contains(filtro.toLowerCase())) {
                            continue;
                        }
                        if (skip > 0) { skip--; continue; }
                        if (count++ >= tamanoPagina) break;
                        // Solo muestra botones de acción si el rol puede
                        if (mostrarAcciones) {
                            tablaPanel.getModelo().addRow(new Object[] {
                                codigo,
                                modeloStr,
                                rs.getDouble("existencia"),
                                rs.getString("unidad"),
                                rs.getString("ubicacion"),
                                "Editar",
                                "Eliminar"
                            });
                        } else {
                            tablaPanel.getModelo().addRow(new Object[] {
                                codigo,
                                modeloStr,
                                rs.getDouble("existencia"),
                                rs.getString("unidad"),
                                rs.getString("ubicacion")
                            });
                        }
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al buscar: " + e.getMessage());
            }
        });
    }

    private int contarTotalProductos(String filtro) {
        String sql =
            "SELECT p.codigo_barras, p.modelo, p.existencia " +
            "FROM productos p ";
        int total = 0;
        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String codigo = rs.getString("codigo_barras");
                    String modeloStr = rs.getString("modelo");
                    if (filtro.isEmpty() ||
                        codigo.toLowerCase().contains(filtro.toLowerCase()) ||
                        modeloStr.toLowerCase().contains(filtro.toLowerCase())) {
                        total++;
                    }
                }
            }
        } catch (SQLException e) { }
        return total;
    }

    // --- Métodos para abrir otras ventanas ---
    private void abrirPrepararPedido()      { PrepararPedido.mostrarVentana(); }
    private void abrirRecepcionMercancia()  { RecepcionMercancia.mostrarVentana(); }
    private void abrirGestionProductos()    { GestionProductos.mostrarVentana(); }
    private void abrirHistorialMovimientos(){ HistorialMovimientos.mostrarVentana(); }
    private void abrirAjusteInventario()    { AjusteInventario.mostrarVentana(usuarioActual, rolActual);}
    private void abrirHistorialAjustes()    { HistorialAjustes.mostrarVentana(); }
    private void abrirGraficas()            { VentanaGraficas.mostrarVentana(); }
    private void abrirProductosCriticos()   {
        JDialog dialog = new JDialog(this, "Productos Críticos", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(900, 700);
        dialog.add(new PanelReportesProductosCriticos());
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // --- Render especial para existencia ---
    static class ColorCantidadRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            try {
                double d = Double.parseDouble(value.toString());
                if (d > 0) {
                    c.setBackground(new Color(65, 180, 90)); // Verde
                    c.setForeground(Color.BLACK);
                } else if (d == 0) {
                    c.setBackground(new Color(240, 220, 80)); // Amarillo
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(new Color(235, 87, 87)); // Rojo
                    c.setForeground(Color.WHITE);
                }
            } catch (Exception ex) {
                c.setBackground(Color.WHITE);
                c.setForeground(Color.BLACK);
            }
            return c;
        }
    }

    // --- Botones bonitos ---
    private JButton crearBotonBarra(String texto, java.awt.event.ActionListener al) {
        JButton btn = new JButton(texto);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI Semibold", Font.BOLD, 15));
        btn.setBackground(new Color(30, 119, 200));
        btn.setForeground(Color.WHITE);
        btn.setBorder(new RoundBorder(16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(172, 38));
        btn.addActionListener(al);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(35, 136, 229));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(30, 119, 200));
            }
        });
        return btn;
    }

    private void buscarSiNoPlaceholder() {
        String t = txtBuscar.getText().trim();
        if (!t.equals("Buscar...")) cargarPaginaProductos(t, 1);
    }

    private BufferedImage reemplazarTonosOscurosPorBlanco(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = img.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xFF;
                if (alpha != 0) {
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;
                    double lum = 0.2126 * r + 0.7152 * g + 0.0722 * b;
                    if (lum < 200) {
                        img.setRGB(x, y, (alpha << 24) | 0x00FFFFFF);
                    }
                }
            }
        }
        return img;
    }

    // --- Renderers y editores para botones en tabla ---
    static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer(String label) {
            setText(label);
            setOpaque(true);
            setBackground(new Color(30, 119, 200));
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                      boolean hasFocus, int row, int column) {
            return this;
        }
    }

    static class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private JTable table;
        private VerProductos parent;

        public ButtonEditor(JCheckBox checkBox, String label, JTable table, VerProductos parent) {
            super(checkBox);
            this.button = new JButton(label);
            this.label = label;
            this.table = table;
            this.parent = parent;

            button.setBackground(new Color(30, 119, 200));
            button.setForeground(Color.WHITE);
            button.setFont(new Font("Segoe UI", Font.BOLD, 13));
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                     int row, int column) {
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                int row = table.getSelectedRow();
                if (label.equals("Editar")) {
                    parent.abrirEditarProducto(row);
                } else if (label.equals("Eliminar")) {
                    parent.eliminarProducto(row);
                }
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    // Métodos para los botones de la tabla
    public void abrirEditarProducto(int row) {
        if (Permisos.esSoloConsulta(rolActual)) {
            JOptionPane.showMessageDialog(this, "No tienes permiso para editar productos.");
            return;
        }
        String codigoBarras = modelo.getValueAt(row, 0).toString();
        int idProducto = obtenerIdPorCodigoBarras(codigoBarras);
        if (idProducto == -1) {
            JOptionPane.showMessageDialog(this, "No se encontró el producto.");
            return;
        }
        EditarProducto dialog = new EditarProducto(idProducto, usuarioActual, rolActual);
        dialog.setVisible(true);
        cargarPaginaProductos(ultimoFiltro, paginaActual);
    }

    public void eliminarProducto(int row) {
        if (Permisos.esSoloConsulta(rolActual)) {
            JOptionPane.showMessageDialog(this, "No tienes permiso para eliminar productos.");
            return;
        }
        String codigoBarras = modelo.getValueAt(row, 0).toString();
        int idProducto = obtenerIdPorCodigoBarras(codigoBarras);
        if (idProducto == -1) {
            JOptionPane.showMessageDialog(this, "No se encontró el producto.");
            return;
        }
        // Aquí tu lógica de eliminación
        cargarPaginaProductos(ultimoFiltro, paginaActual);
    }

    private int obtenerIdPorCodigoBarras(String codigoBarras) {
        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement("SELECT id FROM productos WHERE codigo_barras = ?")) {
            ps.setString(1, codigoBarras);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (Exception e) { }
        return -1;
    }

    // --- Bordes redondeados para botones ---
    static class RoundBorder extends AbstractBorder {
        private int radius;
        public RoundBorder(int radius) { this.radius = radius; }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(30, 119, 200));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }
        @Override
        public Insets getBorderInsets(Component c) { return new Insets(4, 8, 4, 8); }
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.right = 8;
            insets.top = insets.bottom = 4;
            return insets;
        }
    }
}
