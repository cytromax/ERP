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

    public VerProductos(String usuarioActual, String rolActual) {
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

    // HEADER con logo y botones (¡ya incluye "Ver Gráficas"!)
    private JPanel crearPanelHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 32, 0, 32)); // MÁS ESPACIO ARRIBA

        // Logo centrado y con espacio
        JLabel lblLogo = new JLabel();
        lblLogo.setHorizontalAlignment(SwingConstants.CENTER);
        lblLogo.setVerticalAlignment(SwingConstants.CENTER);
        lblLogo.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0)); // MÁS ESPACIO ABAJO
        try {
            URL urlLogo = getClass().getResource("/images/viveza-textil-logo.png");
            if (urlLogo != null) {
                BufferedImage img = ImageIO.read(urlLogo);
                img = reemplazarTonosOscurosPorBlanco(img);
                // Ajusta el tamaño según el panel
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

        // Botones (¡ya incluye "Ver Gráficas"!)
        JPanel panelBotones = new JPanel(new GridLayout(2, 7, 18, 10)); // 7 columnas
        panelBotones.setOpaque(false);
        String[] botones = {
            "Entradas/Salidas", "Entregar Productos", "Recibir Productos",
            "Agregar Producto", "Historial de Movimientos", "Imprimir Cód. Barras",
            "Ajuste de Inventario", "Historial de Ajustes", "Editar Producto",
            "Eliminar Producto", "Ver Gráficas", "Salir"
        };
        java.awt.event.ActionListener[] acciones = {
            e -> abrirMovimientoProductos(), e -> abrirPrepararPedido(), e -> abrirRecepcionMercancia(),
            e -> abrirGestionProductos(), e -> abrirHistorialMovimientos(), e -> new ImprimirCodigoBarras().setVisible(true),
            e -> abrirAjusteInventario(), e -> abrirHistorialAjustes(), e -> abrirEditarProducto(),
            e -> eliminarProducto(), e -> abrirGraficas(), e -> dispose()
        };
        int totalBotones = rolActual.equalsIgnoreCase("administrador") ? botones.length : 7;
        for (int i = 0; i < totalBotones; i++) {
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
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        String[] cols = {"ID", "Código de barras", "Descripción", "Existencia", "Unidad", "Ubicación"};
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

        tabla.getColumnModel().getColumn(3).setCellRenderer(new ColorCantidadRenderer());

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
                            rs.getInt("id"),
                            rs.getString("codigo_barras"),
                            rs.getString("modelo"),
                            rs.getDouble("existencia"),
                            rs.getString("unidad"),
                            rs.getString("ubicacion")
                        });
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al buscar: " + e.getMessage());
            }
        });
    }

    // Métodos de apertura de ventanas...
    private void abrirMovimientoProductos() { new MovimientoProductos(rolActual, usuarioActual).setVisible(true); }
    private void abrirPrepararPedido()      { new PrepararPedido().setVisible(true);      }
    private void abrirRecepcionMercancia()  { new RecepcionMercancia().setVisible(true);  }
    private void abrirGestionProductos()    { new GestionProductos().setVisible(true);    }
    private void abrirHistorialMovimientos(){ new HistorialMovimientos().setVisible(true);}
    private void abrirAjusteInventario()    { new AjusteInventario(usuarioActual, rolActual).setVisible(true);}
    private void abrirHistorialAjustes()    { new HistorialAjustes().setVisible(true);    }
    private void abrirEditarProducto() {
        int selectedRow = tabla.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto para editar.");
            return;
        }
        int idProducto = (int) modelo.getValueAt(selectedRow, 0); // Columna 0 = ID
        EditarProducto dialog = new EditarProducto(idProducto, usuarioActual, rolActual);
        dialog.setVisible(true);
        cargarProductos(); // Refresca la tabla por si se editó algo
    }
    private void abrirGraficas() {
        new VentanaGraficas().setVisible(true);
    }
    private void eliminarProducto() {
        int selectedRow = tabla.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto para eliminar.");
            return;
        }

        int idProducto = (int) modelo.getValueAt(selectedRow, 0); // Columna 0 = ID

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
            // (Opcional) Guarda datos del producto para historial antes de eliminar
            String sqlSelect = "SELECT codigo_barras, modelo, unidad, ubicacion FROM productos WHERE id = ?";
            String codigo = "", modeloProd = "", unidad = "", ubicacion = "";
            try (PreparedStatement ps = con.prepareStatement(sqlSelect)) {
                ps.setInt(1, idProducto);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        codigo = rs.getString("codigo_barras");
                        modeloProd = rs.getString("modelo");
                        unidad = rs.getString("unidad");
                        ubicacion = rs.getString("ubicacion");
                    }
                }
            }

            // Elimina el producto
            String sqlDelete = "DELETE FROM productos WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlDelete)) {
                ps.setInt(1, idProducto);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    // (Opcional) Registrar en historial de eliminación
                    // Descomenta si tienes la tabla historial_eliminacion:
                    /*
                    String sqlHist = "INSERT INTO historial_eliminacion (id_producto, codigo_barras, modelo, unidad, ubicacion, usuario, fecha) VALUES (?, ?, ?, ?, ?, ?, NOW())";
                    try (PreparedStatement psHist = con.prepareStatement(sqlHist)) {
                        psHist.setInt(1, idProducto);
                        psHist.setString(2, codigo);
                        psHist.setString(3, modeloProd);
                        psHist.setString(4, unidad);
                        psHist.setString(5, ubicacion);
                        psHist.setString(6, usuarioActual);
                        psHist.executeUpdate();
                    }
                    */
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

    private void cargarProductos()          { buscarProductos(""); }

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
}
