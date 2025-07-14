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

import conexion.ConexionDB;

public class VerProductos extends JFrame {

    private JTable tabla;
    private DefaultTableModel modelo;
    private String usuarioActual;
    private String rolActual;
    private JTextField txtBuscar;

    // --- SINGLETON: Instancia única por usuario/rol ---
    private static VerProductos instanciaUnica = null;
    private static String usuarioUnico = null;
    private static String rolUnico = null;

    public static void mostrarVentana(String usuario, String rol) {
        if (instanciaUnica == null || !instanciaUnica.isDisplayable() ||
                !usuario.equals(usuarioUnico) || !rol.equals(rolUnico)) {
            instanciaUnica = new VerProductos(usuario, rol);
            usuarioUnico = usuario;
            rolUnico = rol;
        }
        instanciaUnica.setVisible(true);
        instanciaUnica.toFront();
        instanciaUnica.requestFocus();
    }

    private VerProductos(String usuarioActual, String rolActual) {
        this.usuarioActual = usuarioActual;
        this.rolActual = rolActual;

        setTitle("Gestión de Productos");
        setSize(1280, 720);
        setMinimumSize(new Dimension(960, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(new Color(30, 33, 40));
        setLayout(new BorderLayout(0, 0));

        add(crearPanelHeader(), BorderLayout.NORTH);
        add(crearPanelBusqueda(), BorderLayout.CENTER);
        add(crearTablaProductos(), BorderLayout.SOUTH);

        cargarProductos();
    }

    private JPanel crearPanelHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 32, 0, 32));

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
                double scale = Math.min((double)maxW / w, (double)maxH / h);
                int newW = (int)(w * scale);
                int newH = (int)(h * scale);
                Image scaled = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
                lblLogo.setIcon(new ImageIcon(scaled));
            }
        } catch (IOException e) { System.err.println("Logo error: " + e.getMessage()); }
        panel.add(lblLogo, BorderLayout.NORTH);

        JPanel panelBotones = new JPanel(new GridLayout(2, 5, 18, 10));
        panelBotones.setOpaque(false);
        String[] botones =  {
    "Entregar Productos",       // 0
    "Recibir Productos",        // 1
    "Agregar Producto",         // 2
    "Historial de Movimientos", // 3
    "Imprimir Cód. Barras",     // 4

    "Ajuste de Inventario",     // 5
    "Historial de Ajustes",     // 6
    "Ver Gráficas",             // 7
    "Salir"                     // 8
};

java.awt.event.ActionListener[] acciones = {
    e -> abrirPrepararPedido(),        // Entregar Productos
    e -> abrirRecepcionMercancia(),             // Recibir Productos
    e -> abrirGestionProductos(),           // Agregar Producto
    e -> abrirHistorialMovimientos(),       // Historial de Movimientos
    e -> new ImprimirCodigoBarras().setVisible(true), // Imprimir Cód. Barras

    e -> abrirAjusteInventario(),           // Ajuste de Inventario
    e -> abrirHistorialAjustes(),           // Historial de Ajustes
    e -> abrirGraficas(),                   // Ver Gráficas
    e -> dispose()                          // Salir
};

for (int i = 0; i < botones.length; i++) {
    JButton btn = crearBotonBarra(botones[i], acciones[i]);
    panelBotones.add(btn);
}


        panel.add(panelBotones, BorderLayout.CENTER);
        return panel;
    }

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

        panelBusqueda.add(icon);
        panelBusqueda.add(txtBuscar);
        panelBusqueda.add(Box.createRigidArea(new Dimension(14, 0)));
        panelBusqueda.add(btnBuscar);
        return panelBusqueda;
    }

    private JScrollPane crearTablaProductos() {
        modelo = new DefaultTableModel() {
            @Override public boolean isCellEditable(int row, int col) {
                return col == 5 || col == 6; // Solo botones
            }
        };
        String[] cols = {"Código de barras", "Descripción", "Existencia", "Unidad", "Ubicación", "Editar", "Eliminar"};
        for (String c : cols) modelo.addColumn(c);

        tabla = new JTable(modelo);
        tabla.setRowHeight(27);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 16));
        tabla.getTableHeader().setBackground(new Color(25, 41, 61));
        tabla.getTableHeader().setForeground(Color.WHITE);

        tabla.setBackground(new Color(37, 41, 52));
        tabla.setForeground(Color.WHITE);

        tabla.setShowGrid(true);
        tabla.setGridColor(new Color(70, 80, 100, 40));
        tabla.setIntercellSpacing(new Dimension(0, 1));

        tabla.getColumnModel().getColumn(2).setCellRenderer(new ColorCantidadRenderer());

        tabla.getColumn("Editar").setCellRenderer(new ButtonRenderer("Editar"));
        tabla.getColumn("Editar").setCellEditor(
            new ButtonEditor(new JCheckBox(), "Editar", tabla, this));
        tabla.getColumn("Eliminar").setCellRenderer(new ButtonRenderer("Eliminar"));
        tabla.getColumn("Eliminar").setCellEditor(
            new ButtonEditor(new JCheckBox(), "Eliminar", tabla, this));

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new CompoundBorder(
            new EmptyBorder(10, 32, 10, 32),
            BorderFactory.createLineBorder(new Color(55, 65, 80), 1)
        ));
        return scroll;
    }

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
        if (!t.equals("Buscar...")) buscarProductos(t);
    }

    private void buscarProductos(String texto) {
        SwingUtilities.invokeLater(() -> {
            modelo.setRowCount(0);
            String sql = "SELECT id, codigo_barras, modelo, existencia, unidad, ubicacion " +
                         "FROM productos WHERE codigo_barras ILIKE ? OR modelo ILIKE ?";
            try (Connection con = ConexionDB.conectar();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, "%" + texto + "%");
                ps.setString(2, "%" + texto + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        modelo.addRow(new Object[]{
                            rs.getString("codigo_barras"),
                            rs.getString("modelo"),
                            rs.getDouble("existencia"),
                            rs.getString("unidad"),
                            rs.getString("ubicacion"),
                            "Editar",
                            "Eliminar"
                        });
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al buscar: " + e.getMessage());
            }
        });
    }

    // Métodos para abrir otras ventanas
    
    private void abrirPrepararPedido()      { PrepararPedido.mostrarVentana(); }
    private void abrirRecepcionMercancia()  { RecepcionMercancia.mostrarVentana(); }
    private void abrirGestionProductos()    { GestionProductos.mostrarVentana(); }
    private void abrirHistorialMovimientos(){ HistorialMovimientos.mostrarVentana(); }
    private void abrirAjusteInventario()    { AjusteInventario.mostrarVentana(usuarioActual, rolActual);}
    private void abrirHistorialAjustes()    { HistorialAjustes.mostrarVentana(); }
    private void abrirGraficas()            { VentanaGraficas.mostrarVentana(); }

    // Métodos para los botones de la tabla
    public void abrirEditarProducto(int row) {
        String codigoBarras = modelo.getValueAt(row, 0).toString();
        int idProducto = obtenerIdPorCodigoBarras(codigoBarras);
        if (idProducto == -1) {
            JOptionPane.showMessageDialog(this, "No se encontró el producto.");
            return;
        }
        EditarProducto dialog = new EditarProducto(idProducto, usuarioActual, rolActual);
        dialog.setVisible(true);
        cargarProductos();
    }

    public void eliminarProducto(int row) {
        String codigoBarras = modelo.getValueAt(row, 0).toString();
        int idProducto = obtenerIdPorCodigoBarras(codigoBarras);
        if (idProducto == -1) {
            JOptionPane.showMessageDialog(this, "No se encontró el producto.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "¿Estás seguro de que deseas eliminar este producto?\nEsta acción no se puede deshacer.",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection con = ConexionDB.conectar()) {
            String sqlDelete = "DELETE FROM productos WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlDelete)) {
                ps.setInt(1, idProducto);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Producto eliminado correctamente.");
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo eliminar el producto.");
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al eliminar producto: " + e.getMessage());
        }
        cargarProductos();
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

    private void cargarProductos() { buscarProductos(""); }

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

    // Render especial para existencia
    static class ColorCantidadRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            try {
                double d = Double.parseDouble(value.toString());
                if (d > 0) {
                    c.setBackground(new Color(65, 180, 90));
                    c.setForeground(Color.BLACK);
                } else if (d == 0) {
                    c.setBackground(new Color(240, 220, 80));
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(new Color(235, 87, 87));
                    c.setForeground(Color.WHITE);
                }
            } catch (Exception ex) {
                c.setBackground(Color.WHITE);
                c.setForeground(Color.BLACK);
            }
            return c;
        }
    }

    // Bordes redondeados para botones
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
}
