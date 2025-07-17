package Main.newpackage;

import conexion.ConexionDB;
import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class AltaUsuarios extends JFrame {

    private static AltaUsuarios instanciaUnica = null;
    private JComboBox<EmpleadoItem> comboEmpleado;
    private JTextField txtUsuario, txtBuscarEmpleado;
    private JPasswordField txtPassword;
    private JComboBox<String> comboRol;
    private JButton btnGenerarUsuario, btnGuardar, btnEditar, btnDesactivar, btnActivar, btnCambiarPass, btnEliminar, btnSalir;
    private List<EmpleadoItem> empleadosDisponibles = new ArrayList<>();

    private DefaultTableModel modelo;
    private JTable tablaUsuarios;
    private String usuarioAdmin;
    private RolUsuario rolAdmin;


    // Roles técnicos (valores en BD) y visuales
    static final String[] ROLE_VALUES = {
        "administrador",
        "trabajador",
        "consultor_externo",
        "compras"
    };
    static final String[] ROLE_DISPLAY = {
        "Administrador",
        "Trabajador",
        "Consultor externo",
        "Compras"
    };

    // Métodos para conversión
    public static String getRoleDisplay(String rol) {
        switch (rol.toLowerCase()) {
            case "administrador":
            case "Administrador":
                return "Administrador";
            case "trabajador":
            case "Trabajador":
                return "Trabajador";
            case "consultor_externo":
            case "Consultor externo":
                return "Consultor externo";
            case "compras":
            case "Compras":
                return "Compras";
            default:
                return rol; // Por si algún día agregas uno nuevo
        }
    }
    public static String getRoleValue(String display) {
        for (int i = 0; i < ROLE_DISPLAY.length; i++)
            if (ROLE_DISPLAY[i].equalsIgnoreCase(display)) return ROLE_VALUES[i];
        return display;
    }

    // --- Clase auxiliar para empleados ---
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

    // ---- Singleton launcher ----
   public static void mostrarVentana(String usuarioAdmin, RolUsuario rolAdmin) {
    if (instanciaUnica == null || !instanciaUnica.isDisplayable()) {
        instanciaUnica = new AltaUsuarios(usuarioAdmin, rolAdmin);
    }
    instanciaUnica.setVisible(true);
    instanciaUnica.toFront();
    instanciaUnica.requestFocus();
}


    private AltaUsuarios(String usuarioAdmin, RolUsuario rolAdmin) {
    this.usuarioAdmin = usuarioAdmin;
    this.rolAdmin = rolAdmin;
        setTitle("Alta de Usuarios del Sistema");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(820, 650);
        setLocationRelativeTo(null);
        setResizable(true);
        initUI();
        cargarEmpleadosSinUsuario();
        cargarUsuariosSistema();
    }

    private void initUI() {
        JPanel main = new JPanel();
        main.setBackground(new Color(42, 44, 55));
        main.setBorder(BorderFactory.createEmptyBorder(18, 32, 12, 32));
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel("Alta y Gestión de Usuarios del Sistema");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setForeground(new Color(80, 180, 255));
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        main.add(lblTitulo);
        main.add(Box.createVerticalStrut(10));

        // --- Panel de campos con borde
        JPanel campos = new JPanel(new GridBagLayout());
        campos.setBackground(new Color(48, 51, 62));
        campos.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 120, 180), 2),
                "Datos de usuario",
                0, 0, new Font("Segoe UI", Font.BOLD, 14), new Color(100, 170, 255)
        ));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 10, 8, 10);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        int y = 0;
        txtBuscarEmpleado = new JTextField(24);
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 1;
        c.weightx = 0;
        campos.add(new JLabel("Buscar empleado:"), c);
        c.gridx = 1;
        c.gridy = y;
        c.gridwidth = 2;
        c.weightx = 1;
        campos.add(txtBuscarEmpleado, c);
        y++;

        comboEmpleado = new JComboBox<>();
        comboEmpleado.setBackground(new Color(61, 65, 78));
        comboEmpleado.setForeground(Color.WHITE);
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 1;
        c.weightx = 0;
        campos.add(new JLabel("Empleado:"), c);
        c.gridx = 1;
        c.gridy = y;
        c.gridwidth = 2;
        c.weightx = 1;
        campos.add(comboEmpleado, c);
        y++;

        txtUsuario = new JTextField(18);
        txtUsuario.setEditable(false);
        txtUsuario.setBackground(new Color(60, 63, 70));
        txtUsuario.setForeground(new Color(200, 255, 255));
        txtUsuario.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtUsuario.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 180, 255), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        btnGenerarUsuario = new JButton("Generar usuario");
        btnGenerarUsuario.setBackground(new Color(24, 175, 255));
        btnGenerarUsuario.setForeground(Color.WHITE);
        btnGenerarUsuario.setFocusPainted(false);

        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 1;
        c.weightx = 0;
        campos.add(new JLabel("Usuario:"), c);
        c.gridx = 1;
        c.gridy = y;
        c.gridwidth = 1;
        c.weightx = 1;
        campos.add(txtUsuario, c);
        c.gridx = 2;
        c.gridy = y;
        c.gridwidth = 1;
        c.weightx = 0;
        campos.add(btnGenerarUsuario, c);
        y++;

        txtPassword = new JPasswordField(18);
        txtPassword.setBackground(new Color(60, 63, 70));
        txtPassword.setForeground(Color.WHITE);
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 180, 255), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 1;
        c.weightx = 0;
        campos.add(new JLabel("Contraseña:"), c);
        c.gridx = 1;
        c.gridy = y;
        c.gridwidth = 2;
        c.weightx = 1;
        campos.add(txtPassword, c);
        y++;

        comboRol = new JComboBox<>(ROLE_DISPLAY); // Mostramos bonitos
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 1;
        c.weightx = 0;
        campos.add(new JLabel("Rol:"), c);
        c.gridx = 1;
        c.gridy = y;
        c.gridwidth = 2;
        c.weightx = 1;
        campos.add(comboRol, c);
        y++;

        main.add(campos);
        main.add(Box.createVerticalStrut(10));

        // --- Tabla usuarios de sistema ---
        modelo = new DefaultTableModel(
                new String[]{"ID", "Empleado", "Usuario", "Rol", "Activo", "Fecha Registro"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaUsuarios = new JTable(modelo);
        tablaUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaUsuarios.setRowHeight(28);

        JScrollPane scroll = new JScrollPane(tablaUsuarios);
        scroll.setPreferredSize(new Dimension(770, 130));
        main.add(scroll);

        // --- Botones abajo ---
        btnGuardar = new JButton("Guardar nuevo");
        btnGuardar.setBackground(new Color(24, 175, 255));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false);

        btnEditar = new JButton("Editar usuario");
        btnEditar.setBackground(new Color(200, 140, 60));
        btnEditar.setForeground(Color.WHITE);

        btnDesactivar = new JButton("Desactivar usuario");
        btnDesactivar.setBackground(new Color(200, 70, 80));
        btnDesactivar.setForeground(Color.WHITE);

        btnActivar = new JButton("Activar usuario");
        btnActivar.setBackground(new Color(40, 180, 80));
        btnActivar.setForeground(Color.WHITE);
        btnActivar.setFocusPainted(false);

        btnCambiarPass = new JButton("Cambiar contraseña");
        btnCambiarPass.setBackground(new Color(60, 130, 230));
        btnCambiarPass.setForeground(Color.WHITE);

        btnEliminar = new JButton("Eliminar usuario");
        btnEliminar.setBackground(new Color(220, 80, 30));
        btnEliminar.setForeground(Color.WHITE);

        btnSalir = new JButton("Salir");
        btnSalir.setBackground(new Color(80, 80, 80));
        btnSalir.setForeground(Color.WHITE);

        JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        pBtns.setBackground(new Color(42, 44, 55));
        pBtns.add(btnGuardar);
        pBtns.add(btnEditar);
        pBtns.add(btnDesactivar);
        pBtns.add(btnActivar);
        pBtns.add(btnCambiarPass);
        pBtns.add(btnEliminar);
        pBtns.add(btnSalir);

        main.add(Box.createVerticalStrut(16));
        main.add(pBtns);

        JScrollPane mainScroll = new JScrollPane(main);
        mainScroll.setBorder(null);
        setContentPane(mainScroll);

        setMinimumSize(new Dimension(820, 650));
        setResizable(true);

        // --- Eventos
        btnGenerarUsuario.addActionListener(e -> generarUsuario());
        btnGuardar.addActionListener(e -> guardarUsuario());
        btnEditar.addActionListener(e -> editarUsuario());
        btnDesactivar.addActionListener(e -> desactivarUsuario());
        btnActivar.addActionListener(e -> activarUsuario());
        btnCambiarPass.addActionListener(e -> cambiarContrasena());
        btnEliminar.addActionListener(e -> eliminarUsuario());
        btnSalir.addActionListener(e -> dispose());
        txtBuscarEmpleado.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrarEmpleados(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrarEmpleados(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrarEmpleados(); }
        });
        tablaUsuarios.getSelectionModel().addListSelectionListener(e -> actualizarBotonesEstado());
        tablaUsuarios.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                llenarCamposDeTabla();
            }
        });
    }

    // --- Cargar empleados SIN usuario activo
    private void cargarEmpleadosSinUsuario() {
        empleadosDisponibles.clear();
        try (Connection con = ConexionDB.conectar()) {
            String sql = "SELECT e.id, e.nombre, e.codigo_empleado FROM empleados e WHERE e.id NOT IN (SELECT empleado_id FROM usuarios WHERE activo=true)";
            try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    empleadosDisponibles.add(new EmpleadoItem(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("codigo_empleado")
                    ));
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar empleados: " + ex.getMessage());
        }
        actualizarComboEmpleado(empleadosDisponibles);
    }

    private void actualizarComboEmpleado(List<EmpleadoItem> lista) {
        comboEmpleado.removeAllItems();
        for (EmpleadoItem e : lista) {
            comboEmpleado.addItem(e);
        }
    }

    private void filtrarEmpleados() {
        String filtro = txtBuscarEmpleado.getText().trim().toLowerCase();
        List<EmpleadoItem> filtrados = empleadosDisponibles.stream()
                .filter(emp
                        -> emp.nombreCompleto.toLowerCase().contains(filtro)
                || emp.codigoEmpleado.toLowerCase().contains(filtro)
                ).collect(Collectors.toList());
        actualizarComboEmpleado(filtrados);
    }

    // --- Autogenerar usuario
    private void generarUsuario() {
        EmpleadoItem emp = (EmpleadoItem) comboEmpleado.getSelectedItem();
        if (emp == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un empleado.");
            return;
        }
        String[] partes = emp.nombreCompleto.trim().split("\\s+");
        if (partes.length < 3) {
            JOptionPane.showMessageDialog(this, "El nombre debe tener al menos 3 palabras.");
            return;
        }
        String primerNombre = partes[0], primerApellido = partes[partes.length - 2], segundoApellido = partes[partes.length - 1];
        String iniciales = ("" +
                Character.toUpperCase(primerApellido.charAt(0)) +
                Character.toUpperCase(segundoApellido.charAt(0)) +
                Character.toUpperCase(primerNombre.charAt(0)));
        String base = iniciales + "_" + capitalizar(primerNombre);
        List<String> malas = Arrays.asList("pene", "culo", "puto", "mierda", "pedo", "puta", "nazi", "gay", "sex", "xxx");
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
        if (str == null || str.isEmpty()) {
            return str;
        }
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

    // --- Guardar usuario (alta) ---
    private void guardarUsuario() {
        EmpleadoItem emp = (EmpleadoItem) comboEmpleado.getSelectedItem();
        String user = txtUsuario.getText().trim();
        String pass = new String(txtPassword.getPassword()).trim();
        String rolDisplay = (String) comboRol.getSelectedItem();
        String rol = getRoleValue(rolDisplay); // Guardamos valor técnico

        if (emp == null || user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.");
            return;
        }
        if (!confirmarAdmin()) {
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
            cargarUsuariosSistema();
            cargarEmpleadosSinUsuario();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al guardar usuario: " + ex.getMessage());
        }
    }

    // --- Editar usuario existente (solo rol) ---
    private void editarUsuario() {
        int row = tablaUsuarios.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario.");
            return;
        }
        int idUsuario = (int) modelo.getValueAt(row, 0);
        String username = modelo.getValueAt(row, 2).toString();
        String currentRoleValue = getRoleValue(modelo.getValueAt(row, 3).toString());
        String newRoleDisplay = (String) JOptionPane.showInputDialog(
            this,
            "Nuevo rol para '" + username + "':",
            "Editar rol",
            JOptionPane.QUESTION_MESSAGE,
            null,
            ROLE_DISPLAY,
            getRoleDisplay(currentRoleValue)
        );
        if (newRoleDisplay == null) return; // Cancelado

        String nuevoRol = getRoleValue(newRoleDisplay);
        if (!confirmarAdmin()) {
            return;
        }
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement("UPDATE usuarios SET rol=? WHERE id=?")) {
            ps.setString(1, nuevoRol);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Rol actualizado.");
            cargarUsuariosSistema();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al actualizar rol: " + ex.getMessage());
        }
    }

    // --- Llenar campos con selección de tabla (opcional para UX) ---
    private void llenarCamposDeTabla() {
        int row = tablaUsuarios.getSelectedRow();
        if (row < 0) {
            return;
        }
        txtUsuario.setText(modelo.getValueAt(row, 2).toString());
        String rolDB = modelo.getValueAt(row, 3).toString();
        comboRol.setSelectedItem(getRoleDisplay(rolDB));
        // Por seguridad, no llenamos contraseña ni comboEmpleado
    }

    // --- Tabla de usuarios de sistema ---
    private void cargarUsuariosSistema() {
        modelo.setRowCount(0);
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(
                "SELECT u.id, e.nombre, u.username, u.rol, u.activo, u.fecha_registro "
                        + "FROM usuarios u LEFT JOIN empleados e ON u.empleado_id = e.id "
                        + "ORDER BY u.fecha_registro DESC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    modelo.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("username"),
                            rs.getString("rol"),
                            rs.getBoolean("activo") ? "SI" : "NO",
                            rs.getTimestamp("fecha_registro")
                    });
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar usuarios: " + e.getMessage());
        }
        actualizarBotonesEstado();
    }

    // --- Desactivar usuario ---
    private void desactivarUsuario() {
        int row = tablaUsuarios.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario.");
            return;
        }
        String username = modelo.getValueAt(row, 2).toString();
        if (username.equals(usuarioAdmin)) {
            JOptionPane.showMessageDialog(this, "No puedes desactivar tu propio usuario.");
            return;
        }
        if (!modelo.getValueAt(row, 4).toString().equals("SI")) {
            JOptionPane.showMessageDialog(this, "El usuario ya está inactivo.");
            return;
        }
        if (!confirmarAdmin()) {
            return;
        }
        int idUsuario = (int) modelo.getValueAt(row, 0);
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement("UPDATE usuarios SET activo=false WHERE id = ?")) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Usuario desactivado.");
            cargarUsuariosSistema();
            cargarEmpleadosSinUsuario();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error desactivando usuario: " + e.getMessage());
        }
    }

    // --- Activar usuario ---
    private void activarUsuario() {
        int row = tablaUsuarios.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario.");
            return;
        }
        String username = modelo.getValueAt(row, 2).toString();
        if (modelo.getValueAt(row, 4).toString().equals("SI")) {
            JOptionPane.showMessageDialog(this, "El usuario ya está activo.");
            return;
        }
        if (!confirmarAdmin()) {
            return;
        }
        int idUsuario = (int) modelo.getValueAt(row, 0);
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement("UPDATE usuarios SET activo=true WHERE id = ?")) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Usuario activado.");
            cargarUsuariosSistema();
            cargarEmpleadosSinUsuario();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error activando usuario: " + e.getMessage());
        }
    }

    // --- Cambiar contraseña ---
    private void cambiarContrasena() {
        int row = tablaUsuarios.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario.");
            return;
        }
        String username = modelo.getValueAt(row, 2).toString();
        JTextField txtPass = new JPasswordField(14);
        JTextField txtPass2 = new JPasswordField(14);
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Nueva contraseña para " + username + ":"));
        panel.add(txtPass);
        panel.add(new JLabel("Repite la nueva contraseña:"));
        panel.add(txtPass2);
        int res = JOptionPane.showConfirmDialog(this, panel, "Cambiar contraseña", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) {
            return;
        }
        String p1 = txtPass.getText().trim(), p2 = txtPass2.getText().trim();
        if (p1.isEmpty() || !p1.equals(p2)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.");
            return;
        }
        if (!confirmarAdmin()) {
            return;
        }
        String hash = BCrypt.hashpw(p1, BCrypt.gensalt());
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement("UPDATE usuarios SET password=? WHERE username=?")) {
            ps.setString(1, hash);
            ps.setString(2, username);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Contraseña cambiada.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error cambiando contraseña: " + e.getMessage());
        }
    }

    // --- Eliminar usuario ---
    private void eliminarUsuario() {
        int row = tablaUsuarios.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario para eliminar.");
            return;
        }
        String username = modelo.getValueAt(row, 2).toString();
        if (username.equals(usuarioAdmin)) {
            JOptionPane.showMessageDialog(this, "No puedes eliminar tu propio usuario.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "¿Estás seguro de eliminar al usuario '" + username + "'?\nEsta acción es irreversible.",
                "Eliminar usuario", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        if (!confirmarAdmin()) {
            return;
        }
        int idUsuario = (int) modelo.getValueAt(row, 0);
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement("DELETE FROM usuarios WHERE id = ?")) {
            ps.setInt(1, idUsuario);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Usuario eliminado correctamente.");
                cargarUsuariosSistema();
                cargarEmpleadosSinUsuario();
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo eliminar el usuario.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error eliminando usuario: " + e.getMessage());
        }
    }

    // --- Confirmación de administrador para cualquier acción crítica
    private boolean confirmarAdmin() {
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Confirma con tu contraseña de administrador:"));
        JPasswordField pwdAdmin = new JPasswordField(12);
        panel.add(pwdAdmin);
        int conf = JOptionPane.showConfirmDialog(this, panel, "Confirmación de administrador", JOptionPane.OK_CANCEL_OPTION);
        if (conf != JOptionPane.OK_OPTION) {
            return false;
        }
        String passAdmin = new String(pwdAdmin.getPassword()).trim();
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(
                "SELECT password FROM usuarios WHERE username = ? AND rol = 'administrador' AND activo = true")) {
            ps.setString(1, usuarioAdmin);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("password");
                if (BCrypt.checkpw(passAdmin, hash)) {
                    return true;
                }
            }
        } catch (Exception e) {}
        JOptionPane.showMessageDialog(this, "Contraseña de administrador incorrecta.");
        return false;
    }

    // --- Habilitar/deshabilitar botones según estado usuario seleccionado ---
    private void actualizarBotonesEstado() {
        int row = tablaUsuarios.getSelectedRow();
        if (row == -1) {
            btnDesactivar.setEnabled(false);
            btnActivar.setEnabled(false);
            btnCambiarPass.setEnabled(false);
            btnEliminar.setEnabled(false);
            btnEditar.setEnabled(false);
        } else {
            boolean activo = modelo.getValueAt(row, 4).toString().equals("SI");
            btnDesactivar.setEnabled(activo);
            btnActivar.setEnabled(!activo);
            btnCambiarPass.setEnabled(true);
            btnEliminar.setEnabled(true);
            btnEditar.setEnabled(true);
        }
    }

    // --- Utilidad: Al eliminar un empleado, desactiva también usuario (llama desde tu módulo de empleados) ---
    public static void desactivarUsuarioPorEmpleado(int idEmpleado) {
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement("UPDATE usuarios SET activo=false WHERE empleado_id = ?")) {
            ps.setInt(1, idEmpleado);
            ps.executeUpdate();
        } catch (Exception e) { }
    }
}
