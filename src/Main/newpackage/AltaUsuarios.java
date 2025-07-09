package Main.newpackage;

import conexion.ConexionDB;
import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

public class AltaUsuarios extends JFrame {
    private JComboBox<EmpleadoItem> comboEmpleado;
    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JComboBox<String> comboRol;
    private JButton btnGenerarUsuario, btnGuardar;

    static class EmpleadoItem {
        int id;
        String nombreCompleto, codigoEmpleado;

        EmpleadoItem(int id, String nombreCompleto, String codigoEmpleado) {
            this.id = id;
            this.nombreCompleto = nombreCompleto;
            this.codigoEmpleado = codigoEmpleado;
        }

        @Override
        public String toString() {
            return codigoEmpleado + " - " + nombreCompleto;
        }
    }

    public AltaUsuarios() {
        setTitle("Alta de Usuarios del Sistema");
        setSize(480, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
        cargarEmpleadosSinUsuario();
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(42, 44, 55));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 10, 8, 10);
        c.fill = GridBagConstraints.HORIZONTAL;

        comboEmpleado = new JComboBox<>();
        txtUsuario = new JTextField(18);
        txtUsuario.setEditable(false);
        txtUsuario.setBackground(new Color(60, 63, 70));
        txtUsuario.setForeground(Color.WHITE);
        txtUsuario.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtUsuario.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 180, 255), 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        txtPassword = new JPasswordField(18);
        txtPassword.setBackground(new Color(60, 63, 70));
        txtPassword.setForeground(Color.WHITE);
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 180, 255), 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        comboRol = new JComboBox<>(new String[]{"administrador", "empleado"});

        btnGenerarUsuario = new JButton("Generar usuario");
        btnGenerarUsuario.setBackground(new Color(24, 175, 255));
        btnGenerarUsuario.setForeground(Color.WHITE);
        btnGenerarUsuario.setFocusPainted(false);

        btnGuardar = new JButton("Guardar");
        btnGuardar.setBackground(new Color(24, 175, 255));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false);

        int y = 0;
        c.gridx = 0; c.gridy = y; panel.add(new JLabel("Empleado:"), c);
        c.gridx = 1;              panel.add(comboEmpleado, c);
        y++;
        c.gridx = 0; c.gridy = y; panel.add(new JLabel("Usuario:"), c);
        c.gridx = 1;              panel.add(txtUsuario, c);
        c.gridx = 2;              panel.add(btnGenerarUsuario, c);
        y++;
        c.gridx = 0; c.gridy = y; panel.add(new JLabel("Contraseña:"), c);
        c.gridx = 1;              panel.add(txtPassword, c);
        y++;
        c.gridx = 0; c.gridy = y; panel.add(new JLabel("Rol:"), c);
        c.gridx = 1;              panel.add(comboRol, c);
        y++;
        c.gridx = 0; c.gridy = y; c.gridwidth = 3; panel.add(btnGuardar, c);

        add(panel);

        btnGenerarUsuario.addActionListener(e -> generarUsuario());
        btnGuardar.addActionListener(e -> guardarUsuario());
    }

    private void cargarEmpleadosSinUsuario() {
        comboEmpleado.removeAllItems();
        try (Connection con = ConexionDB.conectar()) {
            String sql = "SELECT e.id, e.nombre, e.codigo_empleado FROM empleados e WHERE e.id NOT IN (SELECT empleado_id FROM usuarios)";
            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comboEmpleado.addItem(new EmpleadoItem(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("codigo_empleado")
                    ));
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar empleados: " + ex.getMessage());
        }
    }

    private void generarUsuario() {
        EmpleadoItem emp = (EmpleadoItem) comboEmpleado.getSelectedItem();
        if (emp == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un empleado.");
            return;
        }
        // Separar nombre completo en partes
        String[] partes = emp.nombreCompleto.trim().split("\\s+");
        if (partes.length < 3) {
            JOptionPane.showMessageDialog(this, "El nombre completo debe contener al menos tres palabras (nombre y dos apellidos).");
            return;
        }

        String primerNombre = partes[0];
        String primerApellido = partes[partes.length - 2]; // penúltima palabra
        String segundoApellido = partes[partes.length - 1]; // última palabra

        // Obtener iniciales en mayúsculas
        String iniciales = ("" +
            Character.toUpperCase(primerApellido.charAt(0)) +
            Character.toUpperCase(segundoApellido.charAt(0)) +
            Character.toUpperCase(primerNombre.charAt(0))
        );

        // Primer nombre capitalizado (solo primera letra mayúscula)
        String nombreCapitalizado = capitalizar(primerNombre);

        String base = iniciales + "_" + nombreCapitalizado;

        List<String> malas = Arrays.asList(
            "pene", "culo", "puto", "mierda", "pedo", "puta", "nazi", "gay", "sex", "xxx"
        );

        String user = base;
        int cnt = 1;
        try (Connection con = ConexionDB.conectar()) {
            while (usuarioExiste(user, con) || contieneMalaPalabra(user.toLowerCase(), malas)) {
                user = base + cnt++;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al verificar duplicados: " + ex.getMessage());
            return;
        }

        txtUsuario.setText(user);
    }

    private String capitalizar(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private boolean contieneMalaPalabra(String usuario, List<String> malas) {
        return malas.stream().anyMatch(usuario::contains);
    }

    private boolean usuarioExiste(String usuario, Connection con) throws SQLException {
        String sql = "SELECT 1 FROM usuarios WHERE username = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario);
            return ps.executeQuery().next();
        }
    }

    private void guardarUsuario() {
        EmpleadoItem emp = (EmpleadoItem) comboEmpleado.getSelectedItem();
        String user = txtUsuario.getText().trim();
        String pass = new String(txtPassword.getPassword()).trim();
        String rol  = (String) comboRol.getSelectedItem();

        if (emp == null || user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.");
            return;
        }

        try (Connection con = ConexionDB.conectar()) {
            if (usuarioExiste(user, con)) {
                JOptionPane.showMessageDialog(this, "El usuario ya existe.");
                return;
            }
            String hash = BCrypt.hashpw(pass, BCrypt.gensalt());
            String sql = "INSERT INTO usuarios (empleado_id, username, password, rol, activo, fecha_registro) VALUES (?, ?, ?, ?, true, NOW())";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, emp.id);
                ps.setString(2, user);
                ps.setString(3, hash);
                ps.setString(4, rol);
                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Usuario registrado correctamente.");
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar usuario: " + ex.getMessage());
        }
    }
}
