package Main.newpackage;

import conexion.ConexionDB;
import Main.newpackage.Components.EmpleadoScanField;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.DocumentEvent;
import Main.newpackage.SessionManager;

public class PrepararPedido extends JFrame {

    // --- INICIO: Singleton para evitar varias ventanas ---
    private static PrepararPedido instanciaUnica = null;

    public static void mostrarVentana() {
        if (instanciaUnica == null || !instanciaUnica.isDisplayable()) {
            instanciaUnica = new PrepararPedido();
        }
        instanciaUnica.setVisible(true);
        instanciaUnica.toFront();
        instanciaUnica.requestFocus();
    }
    // --- FIN ---

    private JTextField txtBuscarProducto;
    private EmpleadoScanField campoEmpleadoSolicitante;
    private EmpleadoScanField campoEmpleadoEntregador;
    private JTable tablaPedido;
    private DefaultTableModel modeloPedido;
    private final List<ItemPedido> carrito = new ArrayList<>();
    private Timer debounceTimer = null;

    public PrepararPedido() {
        setTitle("Entregar Producto");
        setSize(900, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));

        // Panel superior: búsqueda de producto
        JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panelBusqueda.setBackground(new Color(38, 41, 48));
        JLabel lblBuscar = new JLabel("Buscar producto:");
        lblBuscar.setForeground(Color.WHITE);
        txtBuscarProducto = new JTextField(20);
        txtBuscarProducto.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.addActionListener(e -> buscarProducto());
        panelBusqueda.add(lblBuscar);
        panelBusqueda.add(txtBuscarProducto);

        txtBuscarProducto.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void debounceBuscar() {
                if (debounceTimer != null) {
                    debounceTimer.stop();
                }
                debounceTimer = new Timer(1000, e -> {
                    String texto = txtBuscarProducto.getText().trim();
                    if (!texto.isEmpty()) {
                        buscarProductoPorCodigo(texto);
                    }
                });
                debounceTimer.setRepeats(false);
                debounceTimer.start();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                debounceBuscar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                debounceBuscar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                debounceBuscar();
            }
        });

        panelBusqueda.add(btnBuscar);
        add(panelBusqueda, BorderLayout.NORTH);

        // Tabla de productos agregados al pedido
        modeloPedido = new DefaultTableModel(new String[]{"ID", "Código", "Modelo", "Cantidad"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaPedido = new JTable(modeloPedido);
        tablaPedido.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        tablaPedido.setRowHeight(26);
        JScrollPane scroll = new JScrollPane(tablaPedido);
        add(scroll, BorderLayout.CENTER);

        // Doble click para editar cantidad
        tablaPedido.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tablaPedido.getSelectedRow() != -1) {
                    editarCantidadPedido();
                }
            }
        });

        // --- Panel Inferior: Empleado que entrega y recibe ---
        JPanel panelInferior = new JPanel(new GridBagLayout());
        panelInferior.setBackground(new Color(38, 41, 48));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 10, 4, 10);
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Empleado que entrega:
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel lblEmpEntregador = new JLabel("Empleado que entrega:");
        lblEmpEntregador.setForeground(new Color(200, 230, 255));
        panelInferior.add(lblEmpEntregador, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        campoEmpleadoEntregador = new EmpleadoScanField();
        campoEmpleadoEntregador.setEmpleados(obtenerListaEmpleados());
        panelInferior.add(campoEmpleadoEntregador, gbc);

        // Empleado que recibe:
        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel lblEmpSolicitante = new JLabel("Empleado que recibe:");
        lblEmpSolicitante.setForeground(new Color(200, 230, 255));
        panelInferior.add(lblEmpSolicitante, gbc);

        gbc.gridx = 3;
        gbc.weightx = 1;
        campoEmpleadoSolicitante = new EmpleadoScanField();
        campoEmpleadoSolicitante.setEmpleados(obtenerListaEmpleados());
        panelInferior.add(campoEmpleadoSolicitante, gbc);

        // Segunda fila (los botones)
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        JButton btnFinalizar = new JButton("Finalizar pedido");
        btnFinalizar.setBackground(new Color(22, 160, 100));
        btnFinalizar.setForeground(Color.WHITE);
        btnFinalizar.addActionListener(e -> finalizarPedido());
        panelInferior.add(btnFinalizar, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 2;
        JButton btnLimpiar = new JButton("Limpiar pedido");
        btnLimpiar.addActionListener(e -> limpiarTodo());
        panelInferior.add(btnLimpiar, gbc);

        add(panelInferior, BorderLayout.SOUTH);

        // Buscar producto con Enter
        txtBuscarProducto.addActionListener(e -> buscarProducto());
    }

    // ---------- Lógica principal -----------
    private void buscarProducto() {
        String query = txtBuscarProducto.getText().trim();
        if (query.isEmpty()) {
            return;
        }
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(
                "SELECT id, codigo_barras, modelo, unidad FROM productos WHERE codigo_barras = ? OR modelo ILIKE ?")) {
            ps.setString(1, query);
            ps.setString(2, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String codigo = rs.getString("codigo_barras");
                    String modelo = rs.getString("modelo");
                    String unidad = rs.getString("unidad");

                    JTextField cantidadField = new JTextField(8);
                    cantidadField.setFont(new Font("Segoe UI", Font.BOLD, 18));
                    JPanel panel = new JPanel(new FlowLayout());
                    panel.add(new JLabel("Cantidad para: " + modelo + " (" + codigo + "):"));
                    panel.add(cantidadField);
                    panel.add(new JLabel(unidad != null ? unidad : ""));

                    JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
                    JDialog dialog = optionPane.createDialog(this, "Cantidad");
                    dialog.setModal(true);

                    final boolean[] cantidadIngresada = {false};
                    cantidadField.addActionListener(ev -> {
                        cantidadIngresada[0] = true;
                        dialog.setVisible(false);
                        dialog.dispose();
                        agregarYContinuarPedido(id, codigo, modelo, cantidadField.getText());
                    });

                    dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                        @Override
                        public void windowOpened(java.awt.event.WindowEvent e) {
                            cantidadField.requestFocusInWindow();
                        }
                    });

                    dialog.setVisible(true);

                    if (!cantidadIngresada[0]) {
                        Object selectedValue = optionPane.getValue();
                        boolean okPressed = false;
                        if (selectedValue != null) {
                            if (selectedValue instanceof Integer) {
                                okPressed = ((Integer) selectedValue) == JOptionPane.OK_OPTION;
                            } else if (selectedValue instanceof String) {
                                okPressed = ((String) selectedValue).equalsIgnoreCase("OK");
                            }
                        }
                        if (okPressed) {
                            agregarYContinuarPedido(id, codigo, modelo, cantidadField.getText());
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Producto no encontrado.");
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al buscar producto: " + e.getMessage());
        }
        txtBuscarProducto.setText("");
        txtBuscarProducto.requestFocusInWindow();
    }

    private void buscarProductoPorCodigo(String codigo) {
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(
                "SELECT id, codigo_barras, modelo, unidad FROM productos WHERE codigo_barras = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String cod = rs.getString("codigo_barras");
                    String modelo = rs.getString("modelo");
                    String unidad = rs.getString("unidad");

                    JTextField cantidadField = new JTextField(8);
                    cantidadField.setFont(new Font("Segoe UI", Font.BOLD, 18));
                    JPanel panel = new JPanel(new FlowLayout());
                    panel.add(new JLabel("Cantidad para: " + modelo + " (" + cod + "):"));
                    panel.add(cantidadField);
                    panel.add(new JLabel(unidad != null ? unidad : ""));

                    JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
                    JDialog dialog = optionPane.createDialog(this, "Cantidad");
                    dialog.setModal(true);

                    final boolean[] cantidadIngresada = {false};
                    cantidadField.addActionListener(ev -> {
                        cantidadIngresada[0] = true;
                        dialog.setVisible(false);
                        dialog.dispose();
                        agregarYContinuarPedido(id, cod, modelo, cantidadField.getText());
                    });

                    dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                        @Override
                        public void windowOpened(java.awt.event.WindowEvent e) {
                            cantidadField.requestFocusInWindow();
                        }
                    });

                    dialog.setVisible(true);

                    if (!cantidadIngresada[0]) {
                        Object selectedValue = optionPane.getValue();
                        boolean okPressed = false;
                        if (selectedValue != null) {
                            if (selectedValue instanceof Integer) {
                                okPressed = ((Integer) selectedValue) == JOptionPane.OK_OPTION;
                            } else if (selectedValue instanceof String) {
                                okPressed = ((String) selectedValue).equalsIgnoreCase("OK");
                            }
                        }
                        if (okPressed) {
                            agregarYContinuarPedido(id, cod, modelo, cantidadField.getText());
                        }
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al buscar producto: " + e.getMessage());
        }
        txtBuscarProducto.setText("");
        SwingUtilities.invokeLater(() -> txtBuscarProducto.requestFocusInWindow());
    }

    private void agregarAlPedido(int id, String codigo, String modelo, int cantidad) {
        for (ItemPedido item : carrito) {
            if (item.id == id) {
                item.cantidad += cantidad;
                actualizarTablaPedido();
                return;
            }
        }
        carrito.add(new ItemPedido(id, codigo, modelo, cantidad));
        actualizarTablaPedido();
    }

    private void actualizarTablaPedido() {
        modeloPedido.setRowCount(0);
        for (ItemPedido item : carrito) {
            modeloPedido.addRow(new Object[]{item.id, item.codigo, item.modelo, item.cantidad});
        }
    }

    private void editarCantidadPedido() {
        int row = tablaPedido.getSelectedRow();
        if (row == -1) {
            return;
        }
        ItemPedido item = carrito.get(row);
        String cantidadStr = JOptionPane.showInputDialog(this,
                "Editar cantidad para: " + item.modelo + " (" + item.codigo + ")",
                item.cantidad);
        if (cantidadStr != null && !cantidadStr.trim().isEmpty()) {
            try {
                int cantidad = Integer.parseInt(cantidadStr.trim());
                if (cantidad <= 0) {
                    throw new NumberFormatException();
                }
                item.cantidad = cantidad;
                actualizarTablaPedido();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Cantidad inválida.");
            }
        }
    }

    // Obtener lista de empleados desde BD
    private List<EmpleadoScanField.EmpleadoItem> obtenerListaEmpleados() {
        List<EmpleadoScanField.EmpleadoItem> lista = new ArrayList<>();
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(
                "SELECT id, nombre, codigo_empleado FROM empleados ORDER BY nombre")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new EmpleadoScanField.EmpleadoItem(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("codigo_empleado")
                    ));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return lista;
    }

    private void finalizarPedido() {
        if (carrito.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay productos en el pedido.");
            return;
        }
        EmpleadoScanField.EmpleadoItem entregador = campoEmpleadoEntregador.getEmpleadoSeleccionado();
        if (entregador == null) {
            JOptionPane.showMessageDialog(this, "Debes escanear el empleado que ENTREGA.");
            campoEmpleadoEntregador.requestFocusInWindow();
            return;
        }
        EmpleadoScanField.EmpleadoItem solicitante = campoEmpleadoSolicitante.getEmpleadoSeleccionado();
        if (solicitante == null) {
            JOptionPane.showMessageDialog(this, "Debes escanear el empleado que RECIBE.");
            campoEmpleadoSolicitante.requestFocusInWindow();
            return;
        }
        int idEntregador = entregador.id;
        int idSolicitante = solicitante.id;

        // Nuevo: Preguntar por número de pedido
        String numeroPedido = "";
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Cuentas con número de pedido? (Si no tienes, presiona No)",
                "Número de Pedido", JOptionPane.YES_NO_OPTION);

        if (respuesta == JOptionPane.YES_OPTION) {
            numeroPedido = JOptionPane.showInputDialog(this, "Ingresa el número de pedido:", "Número de Pedido", JOptionPane.QUESTION_MESSAGE);
            if (numeroPedido == null) {
                numeroPedido = ""; // Si cancela, dejarlo vacío
            }
        }
        // Si seleccionó NO, numeroPedido queda en blanco

        try (Connection con = ConexionDB.conectar()) {
            con.setAutoCommit(false);

            for (ItemPedido item : carrito) {
                int existenciaAntes = 0;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT existencia FROM productos WHERE id = ?")) {
                    ps.setInt(1, item.id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            existenciaAntes = rs.getInt("existencia");
                        }
                    }
                }
                int existenciaDespues = existenciaAntes - item.cantidad;

                // Insertar con número de pedido (puede ser vacío)
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO movimientos "
                        + "(id_producto, existencia_antes, tipo, cantidad, existencia_despues, usuario, fecha, id_empleado_entregador, id_empleado_solicitante, numero_pedido) "
                        + "VALUES (?, ?, ?, ?, ?, ?, NOW(), ?, ?, ?)")) {
                    ps.setInt(1, item.id);
                    ps.setInt(2, existenciaAntes);
                    ps.setString(3, "salida");
                    ps.setInt(4, item.cantidad);
                    ps.setInt(5, existenciaDespues);
                    ps.setString(6, SessionManager.getUsuario());

                    ps.setInt(7, idEntregador);
                    ps.setInt(8, idSolicitante);
                    if (numeroPedido == null || numeroPedido.trim().isEmpty()) {
                        ps.setNull(9, java.sql.Types.VARCHAR);
                    } else {
                        ps.setString(9, numeroPedido.trim());
                    }
                    ps.executeUpdate();
                }
                try (PreparedStatement ps2 = con.prepareStatement(
                        "UPDATE productos SET existencia = ? WHERE id = ?")) {
                    ps2.setInt(1, existenciaDespues);
                    ps2.setInt(2, item.id);
                    ps2.executeUpdate();
                }
            }

            con.commit();
            JOptionPane.showMessageDialog(this,
                    "Pedido preparado correctamente."
                    + (numeroPedido == null || numeroPedido.trim().isEmpty() ? "" : "\nFolio: " + numeroPedido)
                    + "\nEntregó: " + entregador.codigo + " - " + entregador.nombre
                    + "\nRecibió: " + solicitante.codigo + " - " + solicitante.nombre);
            limpiarTodo();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al registrar la salida: " + ex.getMessage());
        }
    }

    private void limpiarTodo() {
        txtBuscarProducto.setText("");
        campoEmpleadoSolicitante.limpiar();
        campoEmpleadoEntregador.limpiar();
        carrito.clear();
        actualizarTablaPedido();
        txtBuscarProducto.requestFocusInWindow();
    }

    // ------ Clase auxiliar ------
    private static class ItemPedido {

        int id;
        String codigo, modelo;
        int cantidad;

        ItemPedido(int id, String codigo, String modelo, int cantidad) {
            this.id = id;
            this.codigo = codigo;
            this.modelo = modelo;
            this.cantidad = cantidad;
        }
    }

    private void agregarYContinuarPedido(int id, String codigo, String modelo, String cantidadStr) {
        if (cantidadStr != null && !cantidadStr.trim().isEmpty()) {
            try {
                int cantidad = Integer.parseInt(cantidadStr.trim());
                if (cantidad <= 0) {
                    throw new NumberFormatException();
                }
                agregarAlPedido(id, codigo, modelo, cantidad);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Cantidad inválida.");
            }
        }
        SwingUtilities.invokeLater(() -> {
            txtBuscarProducto.setText("");
            txtBuscarProducto.requestFocusInWindow();
        });
    }
}
