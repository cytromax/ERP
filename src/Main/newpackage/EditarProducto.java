package Main.newpackage;

import conexion.ConexionDB;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;

public class EditarProducto extends JFrame {
    private int idProducto;
    private String usuarioActual;
    private RolUsuario rolActual;

    private JTextField txtCodigo, txtUnidad, txtUbicacion, txtMinima;
    private JTextArea txtDescripcion;
    private JTextField txtUsuarioAdmin;
    private JPasswordField txtContraAdmin;
    private JComboBox<String> comboPrioridad;
    private static final String[] PRIORIDADES = {"priorizado", "mas_pedidos", "stock_bajo", "stock_comprar", "normal"};

    private JButton btnGuardar, btnCancelar, btnHistorialEdiciones;

    public EditarProducto(int idProducto, String usuarioActual, RolUsuario rolActual) {
        this.idProducto = idProducto;
        this.usuarioActual = usuarioActual;
        this.rolActual = rolActual;

        setTitle("Editar Producto - ID: " + idProducto);
        setSize(600, 540);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Código de barras
        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        add(new JLabel("Código de barras:"), c);
        txtCodigo = new JTextField();
        c.gridx = 1; c.gridy = 0; c.weightx = 1.0;
        add(txtCodigo, c);

        // Descripción
        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        add(new JLabel("Descripción:"), c);

        txtDescripcion = new JTextArea(6, 30);
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        JScrollPane scrollDesc = new JScrollPane(txtDescripcion, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        c.gridx = 1; c.gridy = 1; c.weightx = 1.0; c.fill = GridBagConstraints.BOTH;
        add(scrollDesc, c);

        // Unidad (no editable)
        c.gridx = 0; c.gridy = 2; c.weightx = 0; c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        add(new JLabel("Unidad:"), c);
        txtUnidad = new JTextField();
        txtUnidad.setEditable(false);
        c.gridx = 1; c.gridy = 2; c.weightx = 1.0;
        add(txtUnidad, c);

        // Ubicación
        c.gridx = 0; c.gridy = 3;
        add(new JLabel("Ubicación:"), c);
        txtUbicacion = new JTextField();
        c.gridx = 1; c.gridy = 3;
        add(txtUbicacion, c);

        // Existencia Mínima
        c.gridx = 0; c.gridy = 4;
        add(new JLabel("Existencia Mínima:"), c);
        txtMinima = new JTextField();
        c.gridx = 1; c.gridy = 4;
        add(txtMinima, c);

        // Prioridad
        c.gridx = 0; c.gridy = 5;
        add(new JLabel("Prioridad:"), c);
        comboPrioridad = new JComboBox<>(PRIORIDADES);
        c.gridx = 1; c.gridy = 5;
        add(comboPrioridad, c);

        // Usuario administrador
        c.gridx = 0; c.gridy = 6;
        add(new JLabel("Usuario administrador:"), c);
        txtUsuarioAdmin = new JTextField();
        c.gridx = 1; c.gridy = 6;
        add(txtUsuarioAdmin, c);

        // Contraseña administrador
        c.gridx = 0; c.gridy = 7;
        add(new JLabel("Contraseña administrador:"), c);
        txtContraAdmin = new JPasswordField();
        c.gridx = 1; c.gridy = 7;
        add(txtContraAdmin, c);

        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnHistorialEdiciones = new JButton("Historial Ediciones");
        btnGuardar = new JButton("Guardar");
        btnCancelar = new JButton("Cancelar");

        panelBotones.add(btnHistorialEdiciones);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnCancelar);

        c.gridx = 0; c.gridy = 8; c.gridwidth = 2; c.weightx = 1.0; c.fill = GridBagConstraints.NONE;
        add(panelBotones, c);

        cargarDatosProducto();

        btnGuardar.addActionListener(this::guardarCambios);
        btnCancelar.addActionListener(e -> dispose());
        btnHistorialEdiciones.addActionListener(e -> abrirHistorialEdicionProductos());
    }

    private void cargarDatosProducto() {
        String sql = "SELECT codigo_barras, modelo, unidad, ubicacion, existencia_minima, prioridad FROM productos WHERE id = ?";
        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    txtCodigo.setText(rs.getString("codigo_barras"));
                    txtDescripcion.setText(rs.getString("modelo"));
                    txtUnidad.setText(rs.getString("unidad"));
                    txtUbicacion.setText(rs.getString("ubicacion"));
                    txtMinima.setText(rs.getString("existencia_minima"));
                    String prioridad = rs.getString("prioridad");
                    if (prioridad != null) comboPrioridad.setSelectedItem(prioridad);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar producto: " + e.getMessage());
        }
    }

    private void guardarCambios(ActionEvent e) {
        if (rolActual != RolUsuario.ADMINISTRADOR) {
            JOptionPane.showMessageDialog(this, "Solo administradores pueden guardar cambios.");
            return;
        }

        String usuarioAdmin = txtUsuarioAdmin.getText().trim();
        String contraAdmin = new String(txtContraAdmin.getPassword()).trim();

        if (usuarioAdmin.isEmpty() || contraAdmin.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresa usuario y contraseña del administrador.");
            return;
        }

        if (!validarAdministrador(usuarioAdmin, contraAdmin)) {
            JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos.");
            return;
        }

        String codigo = txtCodigo.getText().trim();
        String descripcion = txtDescripcion.getText().trim();
        String ubicacion = txtUbicacion.getText().trim();
        String minimaStr = txtMinima.getText().trim();
        String prioridad = (String) comboPrioridad.getSelectedItem();

        if (codigo.isEmpty() || descripcion.isEmpty() || minimaStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Completa los campos obligatorios.");
            return;
        }

        try {
            double minima = Double.parseDouble(minimaStr);

            try (Connection con = ConexionDB.conectar()) {
                con.setAutoCommit(false);

                // Obtener valores antiguos para historial
                String sqlOld = "SELECT codigo_barras, modelo, ubicacion, existencia_minima, prioridad FROM productos WHERE id = ?";
                String codigoAnt = "", descAnt = "", ubicAnt = "", minimaAnt = "", prioridadAnt = "";
                try (PreparedStatement psOld = con.prepareStatement(sqlOld)) {
                    psOld.setInt(1, idProducto);
                    try (ResultSet rsOld = psOld.executeQuery()) {
                        if (rsOld.next()) {
                            codigoAnt = rsOld.getString("codigo_barras");
                            descAnt = rsOld.getString("modelo");
                            ubicAnt = rsOld.getString("ubicacion");
                            minimaAnt = rsOld.getString("existencia_minima");
                            prioridadAnt = rsOld.getString("prioridad");
                        }
                    }
                }

                // Actualizar producto (incluye existencia_minima y prioridad)
                String sqlUpdate = "UPDATE productos SET codigo_barras = ?, modelo = ?, ubicacion = ?, existencia_minima = ?, prioridad = ?::prioridad_tipo WHERE id = ?";
                PreparedStatement ps = con.prepareStatement(sqlUpdate);
                ps.setString(1, codigo);
                ps.setString(2, descripcion);
                ps.setString(3, ubicacion);
                ps.setDouble(4, minima);
                ps.setString(5, prioridad);
                ps.setInt(6, idProducto);
                ps.executeUpdate();

                // Insertar historial por cada campo modificado
                String sqlInsertHist = "INSERT INTO historial_edicion (id_producto, campo_modificado, valor_anterior, valor_nuevo, usuario, fecha) VALUES (?, ?, ?, ?, ?, NOW())";
                try (PreparedStatement psHist = con.prepareStatement(sqlInsertHist)) {
                    if (!codigo.equals(codigoAnt)) {
                        psHist.setInt(1, idProducto);
                        psHist.setString(2, "codigo_barras");
                        psHist.setString(3, codigoAnt);
                        psHist.setString(4, codigo);
                        psHist.setString(5, usuarioActual);
                        psHist.executeUpdate();
                    }
                    if (!descripcion.equals(descAnt)) {
                        psHist.setInt(1, idProducto);
                        psHist.setString(2, "modelo");
                        psHist.setString(3, descAnt);
                        psHist.setString(4, descripcion);
                        psHist.setString(5, usuarioActual);
                        psHist.executeUpdate();
                    }
                    if (!ubicacion.equals(ubicAnt)) {
                        psHist.setInt(1, idProducto);
                        psHist.setString(2, "ubicacion");
                        psHist.setString(3, ubicAnt);
                        psHist.setString(4, ubicacion);
                        psHist.setString(5, usuarioActual);
                        psHist.executeUpdate();
                    }
                    if (!minimaStr.equals(minimaAnt)) {
                        psHist.setInt(1, idProducto);
                        psHist.setString(2, "existencia_minima");
                        psHist.setString(3, minimaAnt);
                        psHist.setString(4, minimaStr);
                        psHist.setString(5, usuarioActual);
                        psHist.executeUpdate();
                    }
                    if (!prioridad.equals(prioridadAnt)) {
                        psHist.setInt(1, idProducto);
                        psHist.setString(2, "prioridad");
                        psHist.setString(3, prioridadAnt);
                        psHist.setString(4, prioridad);
                        psHist.setString(5, usuarioActual);
                        psHist.executeUpdate();
                    }
                }

                con.commit();
                JOptionPane.showMessageDialog(this, "Producto actualizado correctamente.");
                dispose();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "La existencia mínima debe ser un número.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error guardando producto: " + ex.getMessage());
        }
    }

    private boolean validarAdministrador(String usuario, String contra) {
        String sql = "SELECT password, rol FROM usuarios WHERE username = ?";
        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String dbPass = rs.getString("password");
                    String dbRol = rs.getString("rol");
                    boolean passCorrecto = org.mindrot.jbcrypt.BCrypt.checkpw(contra, dbPass);
                    return passCorrecto && dbRol.equalsIgnoreCase("administrador");
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error validando administrador: " + e.getMessage());
        }
        return false;
    }

    private void abrirHistorialEdicionProductos() {
        HistorialEdicionProductos historial = new HistorialEdicionProductos(rolActual);
        historial.setVisible(true);
    }
}
