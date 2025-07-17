package Main.newpackage;

import conexion.ConexionDB;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class AjusteInventario extends JFrame {
    private static AjusteInventario instanciaUnica = null;

    public static void mostrarVentana(String usuarioActual, RolUsuario rolActual) {
        if (instanciaUnica == null || !instanciaUnica.isDisplayable()) {
            instanciaUnica = new AjusteInventario(usuarioActual, rolActual);
        }
        instanciaUnica.setVisible(true);
        instanciaUnica.toFront();
        instanciaUnica.requestFocus();
    }

    private JTextField txtCodigo, txtCantidad;
    private JButton btnAplicar, btnHistorial, btnSalir;
    private JTable tablaProductos;
    private DefaultTableModel modeloProductos;

    private String usuarioActual;
    private RolUsuario rolActual;

    // Producto actualmente seleccionado (por la tabla de búsqueda)
    private ProductoSeleccionado productoSeleccionado = null;

    public AjusteInventario(String usuarioActual, RolUsuario rolActual) {
        this.usuarioActual = usuarioActual;
        this.rolActual = rolActual;

        setTitle("Ajuste de Inventario");
        setSize(800, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
    }

    private void initUI() {
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel superior: Formulario y tabla de productos
        JPanel panelArriba = new JPanel(new BorderLayout(0, 10));

        JPanel panelFormulario = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Código o Nombre (campo búsqueda)
        c.gridx = 0; c.gridy = 0;
        panelFormulario.add(new JLabel("Código de barras o Nombre:"), c);
        txtCodigo = new JTextField();
        c.gridx = 1; c.gridy = 0; c.weightx = 1.0;
        panelFormulario.add(txtCodigo, c);

        // Cantidad
        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        panelFormulario.add(new JLabel("Cantidad (positiva o negativa):"), c);
        txtCantidad = new JTextField();
        c.gridx = 1; c.gridy = 1; c.weightx = 1.0;
        panelFormulario.add(txtCantidad, c);

        // Botones
        btnAplicar = new JButton("Aplicar ajuste");
        btnAplicar.addActionListener(e -> aplicarAjuste());

        btnHistorial = new JButton("Historial de Ajustes");
        btnHistorial.addActionListener(e -> {
            HistorialAjustes.mostrarVentana();
        });

        btnSalir = new JButton("Salir");
        btnSalir.addActionListener(e -> dispose());

        JPanel panelBotones = new JPanel(new GridLayout(1, 3, 10, 0));
        panelBotones.add(btnAplicar);
        panelBotones.add(btnHistorial);
        panelBotones.add(btnSalir);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.weightx = 1.0;
        panelFormulario.add(panelBotones, c);

        // Tabla productos (búsqueda en tiempo real)
        modeloProductos = new DefaultTableModel(
            new String[]{"ID", "Código", "Modelo", "Existencia"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaProductos = new JTable(modeloProductos);
        tablaProductos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaProductos.setPreferredScrollableViewportSize(new Dimension(700, 100));
        JScrollPane scrollProductos = new JScrollPane(tablaProductos);

        // Cuando el usuario hace click en una fila, selecciona el producto y llena el campo de código
        tablaProductos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tablaProductos.getSelectedRow();
                if (row >= 0) {
                    productoSeleccionado = new ProductoSeleccionado(
                            (int) modeloProductos.getValueAt(row, 0),
                            (String) modeloProductos.getValueAt(row, 1),
                            (String) modeloProductos.getValueAt(row, 2),
                            Double.parseDouble(modeloProductos.getValueAt(row, 3).toString())
                    );
                    txtCodigo.setText(productoSeleccionado.codigoBarras);
                }
            }
        });

        // Buscar productos al escribir
        txtCodigo.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void buscar() {
                buscarYMostrarProductos(txtCodigo.getText().trim());
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { buscar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { buscar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { buscar(); }
        });

        // Para Enter: selecciona el primer producto
        txtCodigo.addActionListener(e -> {
            if (modeloProductos.getRowCount() > 0) {
                tablaProductos.setRowSelectionInterval(0, 0);
                tablaProductos.dispatchEvent(new MouseEvent(tablaProductos, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 1, false));
            }
        });

        // Panel superior = Formulario + tabla productos
        panelArriba.add(panelFormulario, BorderLayout.NORTH);
        panelArriba.add(scrollProductos, BorderLayout.CENTER);

        panelPrincipal.add(panelArriba, BorderLayout.CENTER);
        add(panelPrincipal);

        // Buscar productos al iniciar
        buscarYMostrarProductos("");
    }

    private void buscarYMostrarProductos(String query) {
        modeloProductos.setRowCount(0);
        productoSeleccionado = null;

        if (query.isEmpty()) return;

        try (Connection con = ConexionDB.conectar()) {
            String sql = "SELECT id, codigo_barras, modelo, existencia FROM productos WHERE codigo_barras ILIKE ? OR modelo ILIKE ? ORDER BY modelo LIMIT 25";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, "%" + query + "%");
                ps.setString(2, "%" + query + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        modeloProductos.addRow(new Object[]{
                                rs.getInt("id"),
                                rs.getString("codigo_barras"),
                                rs.getString("modelo"),
                                rs.getDouble("existencia")
                        });
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error buscando productos: " + e.getMessage());
        }

        // Selecciona automáticamente la primera fila si hay resultados
        if (modeloProductos.getRowCount() > 0) {
            tablaProductos.setRowSelectionInterval(0, 0);
            // Simula click para actualizar productoSeleccionado
            int row = 0;
            productoSeleccionado = new ProductoSeleccionado(
                (int) modeloProductos.getValueAt(row, 0),
                (String) modeloProductos.getValueAt(row, 1),
                (String) modeloProductos.getValueAt(row, 2),
                Double.parseDouble(modeloProductos.getValueAt(row, 3).toString())
            );
            txtCodigo.setText(productoSeleccionado.codigoBarras);
            tablaProductos.requestFocusInWindow();
        }
    }

    private void aplicarAjuste() {
        if (rolActual != RolUsuario.ADMINISTRADOR) {
            JOptionPane.showMessageDialog(this, "Solo los administradores pueden hacer ajustes de inventario.");
            return;
        }

        String cantidadStr = txtCantidad.getText().trim();

        if (productoSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto de la lista de búsqueda.");
            return;
        }
        if (cantidadStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresa la cantidad.");
            return;
        }

        double cantidad;
        try {
            cantidad = Double.parseDouble(cantidadStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida.");
            return;
        }

        int idProducto = productoSeleccionado.id;
        String codBarras = productoSeleccionado.codigoBarras;
        String descripcion = productoSeleccionado.modelo;
        double existenciaActual = productoSeleccionado.existencia;

        double existenciaNueva = existenciaActual + cantidad;
        if (existenciaNueva < 0) {
            JOptionPane.showMessageDialog(this, "El ajuste dejaría existencia negativa.");
            return;
        }

        // Confirmación y contraseña admin
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Código: " + codBarras));
        panel.add(new JLabel("Descripción: " + descripcion));
        panel.add(new JLabel("Existencia actual: " + existenciaActual));
        panel.add(new JLabel("Cantidad a ajustar: " + cantidad));
        panel.add(new JLabel("Nueva existencia: " + existenciaNueva));
        panel.add(new JLabel(" "));
        panel.add(new JLabel("Confirma con tu contraseña de administrador:"));
        JPasswordField pwd = new JPasswordField(12);
        panel.add(pwd);

        int confirm = JOptionPane.showConfirmDialog(
                this, panel,
                "Confirmar ajuste de inventario",
                JOptionPane.OK_CANCEL_OPTION);

        if (confirm != JOptionPane.OK_OPTION) return;

        String pass = new String(pwd.getPassword()).trim();
        if (pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debes ingresar tu contraseña.");
            return;
        }

        // Validar contraseña contra la base de datos (usuario actual debe ser admin)
        boolean valida = false;
        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT password FROM usuarios WHERE username = ? AND rol = 'administrador' AND activo = true")) {
            ps.setString(1, usuarioActual);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hashed = rs.getString("password");
                // Si usas BCrypt para las contraseñas:
                if (org.mindrot.jbcrypt.BCrypt.checkpw(pass, hashed)) {
                    valida = true;
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error validando contraseña: " + e.getMessage());
            return;
        }
        if (!valida) {
            JOptionPane.showMessageDialog(this, "Contraseña de administrador incorrecta.");
            return;
        }

        // Guardar ajuste
        try (Connection con = ConexionDB.conectar()) {
            con.setAutoCommit(false);

            // Actualizar existencia
            String sqlUpdate = "UPDATE productos SET existencia = ? WHERE id = ?";
            try (PreparedStatement psUpdate = con.prepareStatement(sqlUpdate)) {
                psUpdate.setDouble(1, existenciaNueva);
                psUpdate.setInt(2, idProducto);
                psUpdate.executeUpdate();
            }

            // Insertar movimiento tipo ajuste
            String sqlInsert = "INSERT INTO movimientos (id_producto, id_empleado, existencia_antes, tipo, cantidad, existencia_despues, fecha, usuario) VALUES (?, NULL, ?, 'ajuste', ?, ?, NOW(), ?)";
            try (PreparedStatement psInsert = con.prepareStatement(sqlInsert)) {
                psInsert.setInt(1, idProducto);
                psInsert.setDouble(2, existenciaActual);
                psInsert.setDouble(3, cantidad);
                psInsert.setDouble(4, existenciaNueva);
                psInsert.setString(5, usuarioActual);
                psInsert.executeUpdate();
            }

            con.commit();
            JOptionPane.showMessageDialog(this, "Ajuste aplicado correctamente.");
            txtCodigo.setText("");
            txtCantidad.setText("");
            buscarYMostrarProductos("");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error en ajuste: " + e.getMessage());
        }
    }

    // Clase auxiliar
    private static class ProductoSeleccionado {
        int id;
        String codigoBarras;
        String modelo;
        double existencia;
        ProductoSeleccionado(int id, String codigoBarras, String modelo, double existencia) {
            this.id = id;
            this.codigoBarras = codigoBarras;
            this.modelo = modelo;
            this.existencia = existencia;
        }
    }
}
