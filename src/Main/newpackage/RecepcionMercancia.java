package Main.newpackage;

import conexion.ConexionDB;
import Main.newpackage.SessionManager;
import Main.newpackage.Components.EmpleadoScanField;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.DocumentEvent;

public class RecepcionMercancia extends JFrame {
    // --- INICIO: Singleton para evitar varias ventanas ---
    private static RecepcionMercancia instanciaUnica = null;

    public static void mostrarVentana() {
        if (instanciaUnica == null || !instanciaUnica.isDisplayable()) {
            instanciaUnica = new RecepcionMercancia();
        }
        instanciaUnica.setVisible(true);
        instanciaUnica.toFront();
        instanciaUnica.requestFocus();
    }
    // --- FIN ---

    private JTextField txtBuscarProducto, txtProveedor;
    private EmpleadoScanField campoEmpleadoRecibio;
    private JTable tablaRecepcion;
    private DefaultTableModel modeloRecepcion;
    private final List<ItemRecepcion> carrito = new ArrayList<>();
    private Timer debounceTimer = null;

    public RecepcionMercancia() {
        setTitle("Recibir Producto");
        setSize(900, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));

        // --- Panel superior: búsqueda de producto ---
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
            public void insertUpdate(DocumentEvent e) { debounceBuscar(); }
            @Override
            public void removeUpdate(DocumentEvent e) { debounceBuscar(); }
            @Override
            public void changedUpdate(DocumentEvent e) { debounceBuscar(); }
        });

        panelBusqueda.add(btnBuscar);
        add(panelBusqueda, BorderLayout.NORTH);

        // --- Tabla de productos agregados a la recepción ---
        modeloRecepcion = new DefaultTableModel(new String[]{"ID", "Código", "Modelo", "Cantidad"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaRecepcion = new JTable(modeloRecepcion);
        tablaRecepcion.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        tablaRecepcion.setRowHeight(26);
        JScrollPane scroll = new JScrollPane(tablaRecepcion);
        add(scroll, BorderLayout.CENTER);

        // Doble click para editar cantidad
        tablaRecepcion.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tablaRecepcion.getSelectedRow() != -1) {
                    editarCantidadRecepcion();
                }
            }
        });

        // --- Panel inferior: proveedor, empleado que recibe y finalizar ---
        JPanel panelInferior = new JPanel(new GridBagLayout());
        panelInferior.setBackground(new Color(38, 41, 48));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 10, 4, 10);
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Proveedor
        gbc.gridx = 0; gbc.weightx = 0;
        JLabel lblProveedor = new JLabel("Proveedor:");
        lblProveedor.setForeground(new Color(200, 230, 255));
        panelInferior.add(lblProveedor, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        txtProveedor = new JTextField();
        panelInferior.add(txtProveedor, gbc);

        // Empleado que recibe
        gbc.gridx = 2; gbc.weightx = 0;
        JLabel lblEmpleado = new JLabel("Empleado que recibe:");
        lblEmpleado.setForeground(new Color(200, 230, 255));
        panelInferior.add(lblEmpleado, gbc);

        gbc.gridx = 3; gbc.weightx = 1;
        campoEmpleadoRecibio = new EmpleadoScanField();
        campoEmpleadoRecibio.setEmpleados(obtenerListaEmpleados());
        panelInferior.add(campoEmpleadoRecibio, gbc);

        // Segunda fila para botones
        gbc.gridy = 1; gbc.gridx = 1; gbc.weightx = 0; gbc.gridwidth = 1;
        JButton btnFinalizar = new JButton("Finalizar recepción");
        btnFinalizar.setBackground(new Color(22, 160, 100));
        btnFinalizar.setForeground(Color.WHITE);
        btnFinalizar.addActionListener(e -> finalizarRecepcion());
        panelInferior.add(btnFinalizar, gbc);

        gbc.gridx = 2; gbc.gridwidth = 1;
        JButton btnLimpiar = new JButton("Limpiar");
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
                "SELECT id, codigo_barras, modelo FROM productos WHERE codigo_barras = ? OR modelo ILIKE ?")) {
            ps.setString(1, query);
            ps.setString(2, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String codigo = rs.getString("codigo_barras");
                    String modelo = rs.getString("modelo");
                    // Solicita cantidad
                    String cantidadStr = JOptionPane.showInputDialog(this,
                            "Producto: " + modelo + " (" + codigo + ")\nIngresa la cantidad a recibir:", "Cantidad", JOptionPane.PLAIN_MESSAGE);
                    if (cantidadStr != null && !cantidadStr.trim().isEmpty()) {
                        int cantidad = Integer.parseInt(cantidadStr.trim());
                        if (cantidad <= 0) { throw new NumberFormatException(); }
                        agregarARecepcion(id, codigo, modelo, cantidad);
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
                "SELECT id, codigo_barras, modelo FROM productos WHERE codigo_barras = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String cod = rs.getString("codigo_barras");
                    String modelo = rs.getString("modelo");

                    JTextField cantidadField = new JTextField(8);
                    cantidadField.setFont(new Font("Segoe UI", Font.BOLD, 18));
                    JPanel panel = new JPanel(new FlowLayout());
                    panel.add(new JLabel("Cantidad para: " + modelo + " (" + cod + "):"));
                    panel.add(cantidadField);

                    JOptionPane optionPane = new JOptionPane(panel,
                            JOptionPane.PLAIN_MESSAGE,
                            JOptionPane.OK_CANCEL_OPTION);

                    JDialog dialog = optionPane.createDialog(this, "Cantidad");
                    dialog.setModal(true);

                    final boolean[] cantidadIngresada = {false};
                    cantidadField.addActionListener(ev -> {
                        cantidadIngresada[0] = true;
                        dialog.setVisible(false);
                        dialog.dispose();
                        agregarYContinuarRecepcion(id, cod, modelo, cantidadField.getText());
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
                            agregarYContinuarRecepcion(id, cod, modelo, cantidadField.getText());
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

    private void agregarARecepcion(int id, String codigo, String modelo, int cantidad) {
        for (ItemRecepcion item : carrito) {
            if (item.id == id) {
                item.cantidad += cantidad;
                actualizarTablaRecepcion();
                return;
            }
        }
        carrito.add(new ItemRecepcion(id, codigo, modelo, cantidad));
        actualizarTablaRecepcion();
    }

    private void actualizarTablaRecepcion() {
        modeloRecepcion.setRowCount(0);
        for (ItemRecepcion item : carrito) {
            modeloRecepcion.addRow(new Object[]{item.id, item.codigo, item.modelo, item.cantidad});
        }
    }

    private void editarCantidadRecepcion() {
        int row = tablaRecepcion.getSelectedRow();
        if (row == -1) {
            return;
        }
        ItemRecepcion item = carrito.get(row);
        String cantidadStr = JOptionPane.showInputDialog(this,
                "Editar cantidad para: " + item.modelo + " (" + item.codigo + ")",
                item.cantidad);
        if (cantidadStr != null && !cantidadStr.trim().isEmpty()) {
            try {
                int cantidad = Integer.parseInt(cantidadStr.trim());
                if (cantidad <= 0) { throw new NumberFormatException(); }
                item.cantidad = cantidad;
                actualizarTablaRecepcion();
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

    private void finalizarRecepcion() {
        if (carrito.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay productos en la recepción.");
            return;
        }
        String proveedor = txtProveedor.getText().trim();
        if (proveedor.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debes capturar el proveedor.");
            txtProveedor.requestFocusInWindow();
            return;
        }
        // Validar empleado que recibe
        EmpleadoScanField.EmpleadoItem empleadoRecibio = campoEmpleadoRecibio.getEmpleadoSeleccionado();
        if (empleadoRecibio == null) {
            JOptionPane.showMessageDialog(this, "Debes escanear un empleado que recibe.");
            campoEmpleadoRecibio.requestFocusInWindow();
            return;
        }
        int idEmpleadoRecibio = empleadoRecibio.id;

        // ---- Nuevo: Preguntar por número de pedido ----
        String numeroPedido = "";
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Cuentas con número de pedido? (Si no tienes, presiona No)",
                "Número de Pedido", JOptionPane.YES_NO_OPTION);

        if (respuesta == JOptionPane.YES_OPTION) {
            numeroPedido = JOptionPane.showInputDialog(this, "Ingresa el número de pedido:", "Número de Pedido", JOptionPane.QUESTION_MESSAGE);
            if (numeroPedido == null) numeroPedido = ""; // Si cancela, dejarlo vacío
        }
        // Si seleccionó NO, numeroPedido queda en blanco

        try (Connection con = ConexionDB.conectar()) {
            con.setAutoCommit(false);

            for (ItemRecepcion item : carrito) {
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

                int existenciaDespues = existenciaAntes + item.cantidad;

                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO movimientos "
                        + "(id_producto, existencia_antes, tipo, cantidad, existencia_despues, usuario, fecha, proveedor, id_empleado, numero_pedido) "
                        + "VALUES (?, ?, ?, ?, ?, ?, NOW(), ?, ?, ?)")) {
                    ps.setInt(1, item.id);
                    ps.setInt(2, existenciaAntes);
                    ps.setString(3, "entrada");
                    ps.setInt(4, item.cantidad);
                    ps.setInt(5, existenciaDespues);
                    ps.setString(6, SessionManager.getUsuario()); // <--- USUARIO LOGUEADO DE SESIÓN
                    ps.setString(7, proveedor);
                    ps.setInt(8, idEmpleadoRecibio);
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
                    "Recepción registrada correctamente.\nProveedor: " + proveedor +
                            (numeroPedido == null || numeroPedido.trim().isEmpty() ? "" : "\nFolio: " + numeroPedido)
            );
            limpiarTodo();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al registrar la entrada: " + ex.getMessage());
        }
    }

    private void limpiarTodo() {
        txtBuscarProducto.setText("");
        txtProveedor.setText("");
        campoEmpleadoRecibio.limpiar();
        carrito.clear();
        actualizarTablaRecepcion();
        txtBuscarProducto.requestFocusInWindow();
    }

    // ------ Clase auxiliar ------
    private static class ItemRecepcion {
        int id;
        String codigo, modelo;
        int cantidad;

        ItemRecepcion(int id, String codigo, String modelo, int cantidad) {
            this.id = id;
            this.codigo = codigo;
            this.modelo = modelo;
            this.cantidad = cantidad;
        }
    }

    private void agregarYContinuarRecepcion(int id, String codigo, String modelo, String cantidadStr) {
        if (cantidadStr != null && !cantidadStr.trim().isEmpty()) {
            try {
                int cantidad = Integer.parseInt(cantidadStr.trim());
                if (cantidad <= 0) {
                    throw new NumberFormatException();
                }
                agregarARecepcion(id, codigo, modelo, cantidad);
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
