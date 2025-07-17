package Main.newpackage;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.format.DateTimeFormatter;

public class PanelTickets extends JPanel {
    private final Connection connection;
    private final String usuario;
    private final RolUsuario rol; // <- Cambiado a Enum

    private final JTable tabla;
    private final DefaultTableModel modelo;

    private JTextField filtroTitulo;
    private JComboBox<String> filtroEstado;
    private JComboBox<String> filtroPrioridad;
    private JTextField filtroUsuario;
    private JTextField filtroAsignado;

    private JButton btnCrear;
    private JButton btnEditar;
    private JButton btnEliminar;
    private JButton btnSalir;

    public PanelTickets(Connection connection, String usuario, RolUsuario rol) { // <- Enum aquí
        this.connection = connection;
        this.usuario = usuario;
        this.rol = rol;

        setLayout(new BorderLayout());
        setBackground(new Color(32, 34, 37));

        JLabel titulo = new JLabel("Sistema de Tickets", SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titulo.setForeground(new Color(90, 195, 255));
        titulo.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        add(titulo, BorderLayout.NORTH);

        // Panel filtros arriba
        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelFiltros.setBackground(new Color(40, 42, 50));

        filtroTitulo = new JTextField(10);
        filtroEstado = new JComboBox<>(new String[]{"", "Abierto", "En Progreso", "Cerrado"});
        filtroPrioridad = new JComboBox<>(new String[]{"", "Alta", "Media", "Baja"});
        filtroUsuario = new JTextField(8);
        filtroAsignado = new JTextField(8);

        panelFiltros.add(new JLabel("Título:"));
        panelFiltros.add(filtroTitulo);
        panelFiltros.add(new JLabel("Estado:"));
        panelFiltros.add(filtroEstado);
        panelFiltros.add(new JLabel("Prioridad:"));
        panelFiltros.add(filtroPrioridad);
        panelFiltros.add(new JLabel("Usuario:"));
        panelFiltros.add(filtroUsuario);
        panelFiltros.add(new JLabel("Asignado:"));
        panelFiltros.add(filtroAsignado);

        JButton btnBuscar = new JButton("Buscar");
        panelFiltros.add(btnBuscar);

        add(panelFiltros, BorderLayout.PAGE_START);

        // Tabla
        modelo = new DefaultTableModel(
            new String[]{"ID", "Título", "Estado", "Prioridad", "Usuario", "Asignado", "Fecha Creación"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tabla = new JTable(modelo);
        tabla.setRowHeight(32);
        tabla.setBackground(new Color(44, 45, 52));
        tabla.setForeground(Color.WHITE);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // Panel botones abajo
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotones.setOpaque(false);

        btnCrear = new JButton("Crear Ticket");
        btnEditar = new JButton("Editar Ticket");
        btnEliminar = new JButton("Eliminar Ticket");
        btnSalir = new JButton("Salir");

        panelBotones.add(btnCrear);
        panelBotones.add(btnEditar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnSalir);

        add(panelBotones, BorderLayout.SOUTH);

        // Control de roles con Enum
        boolean esAdmin = rol == RolUsuario.ADMINISTRADOR;
        btnEditar.setEnabled(esAdmin);
        btnEliminar.setEnabled(esAdmin);
        btnCrear.setEnabled(esAdmin || rol == RolUsuario.TRABAJADOR); // Puedes agregar más roles si quieres

        // Eventos
        btnBuscar.addActionListener(e -> cargarTickets());
        btnCrear.addActionListener(e -> abrirFormulario(null));
        btnEditar.addActionListener(e -> editarTicket());
        btnEliminar.addActionListener(e -> eliminarTicketSeleccionado());
        btnSalir.addActionListener(e -> salir());

        cargarTickets();
    }

    private void cargarTickets() {
        modelo.setRowCount(0);
        String sql = "SELECT id, titulo, estado, prioridad, usuario, asignado, fecha_creacion FROM tickets WHERE 1=1";

        if (!filtroTitulo.getText().trim().isEmpty())
            sql += " AND LOWER(titulo) LIKE LOWER(?)";
        if (filtroEstado.getSelectedIndex() > 0)
            sql += " AND estado = ?";
        if (filtroPrioridad.getSelectedIndex() > 0)
            sql += " AND prioridad = ?";
        if (!filtroUsuario.getText().trim().isEmpty())
            sql += " AND LOWER(usuario) LIKE LOWER(?)";
        if (!filtroAsignado.getText().trim().isEmpty())
            sql += " AND LOWER(asignado) LIKE LOWER(?)";

        sql += " ORDER BY fecha_creacion DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int paramIndex = 1;

            if (!filtroTitulo.getText().trim().isEmpty())
                ps.setString(paramIndex++, "%" + filtroTitulo.getText().trim() + "%");
            if (filtroEstado.getSelectedIndex() > 0)
                ps.setString(paramIndex++, (String) filtroEstado.getSelectedItem());
            if (filtroPrioridad.getSelectedIndex() > 0)
                ps.setString(paramIndex++, (String) filtroPrioridad.getSelectedItem());
            if (!filtroUsuario.getText().trim().isEmpty())
                ps.setString(paramIndex++, "%" + filtroUsuario.getText().trim() + "%");
            if (!filtroAsignado.getText().trim().isEmpty())
                ps.setString(paramIndex++, "%" + filtroAsignado.getText().trim() + "%");

            ResultSet rs = ps.executeQuery();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("titulo"),
                    rs.getString("estado"),
                    rs.getString("prioridad"),
                    rs.getString("usuario"),
                    rs.getString("asignado"),
                    rs.getTimestamp("fecha_creacion").toLocalDateTime().format(fmt)
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar tickets: " + ex.getMessage());
        }
    }

    private void abrirFormulario(Ticket ticket) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), true);
        dialog.setTitle(ticket == null ? "Crear Ticket" : "Editar Ticket");
        dialog.setSize(400, 450);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(40, 42, 50));

        JTextField txtTitulo = new JTextField();
        JTextArea txtDescripcion = new JTextArea(5, 20);
        JComboBox<String> cmbEstado = new JComboBox<>(new String[]{"Abierto", "En Progreso", "Cerrado"});
        JComboBox<String> cmbPrioridad = new JComboBox<>(new String[]{"Alta", "Media", "Baja"});
        JTextField txtUsuario = new JTextField();
        JTextField txtAsignado = new JTextField();

        txtTitulo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        txtDescripcion.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtDescripcion.setBackground(new Color(37, 38, 43));
        txtDescripcion.setForeground(Color.WHITE);
        txtDescripcion.setCaretColor(Color.WHITE);

        txtUsuario.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        txtAsignado.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        panel.add(new JLabel("Título:"));
        panel.add(txtTitulo);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JLabel("Descripción:"));
        panel.add(new JScrollPane(txtDescripcion));
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JLabel("Estado:"));
        panel.add(cmbEstado);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JLabel("Prioridad:"));
        panel.add(cmbPrioridad);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JLabel("Usuario:"));
        panel.add(txtUsuario);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JLabel("Asignado a:"));
        panel.add(txtAsignado);
        panel.add(Box.createVerticalStrut(20));

        JButton btnGuardar = new JButton("Guardar");
        btnGuardar.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(btnGuardar);

        if (ticket != null) {
            txtTitulo.setText(ticket.getTitulo());
            txtDescripcion.setText(ticket.getDescripcion());
            cmbEstado.setSelectedItem(ticket.getEstado());
            cmbPrioridad.setSelectedItem(ticket.getPrioridad());
            txtUsuario.setText(ticket.getUsuario());
            txtAsignado.setText(ticket.getAsignado());
        }

        btnGuardar.addActionListener(e -> {
            try {
                // Validar permisos: solo admin puede editar, trabajadores solo crear
                if (ticket != null && rol != RolUsuario.ADMINISTRADOR) {
                    JOptionPane.showMessageDialog(dialog, "No tienes permisos para editar tickets.");
                    return;
                }

                if (txtTitulo.getText().trim().isEmpty() ||
                    txtDescripcion.getText().trim().isEmpty() ||
                    txtUsuario.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Título, descripción y usuario son obligatorios.");
                    return;
                }

                if (ticket == null) {
                    String sql = "INSERT INTO tickets (titulo, descripcion, estado, prioridad, usuario, asignado, fecha_creacion, fecha_actualizacion) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        ps.setString(1, txtTitulo.getText().trim());
                        ps.setString(2, txtDescripcion.getText().trim());
                        ps.setString(3, (String) cmbEstado.getSelectedItem());
                        ps.setString(4, (String) cmbPrioridad.getSelectedItem());
                        ps.setString(5, txtUsuario.getText().trim());
                        ps.setString(6, txtAsignado.getText().trim());
                        ps.executeUpdate();
                    }
                } else {
                    String sql = "UPDATE tickets SET titulo=?, descripcion=?, estado=?, prioridad=?, usuario=?, asignado=?, fecha_actualizacion=CURRENT_TIMESTAMP WHERE id=?";
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        ps.setString(1, txtTitulo.getText().trim());
                        ps.setString(2, txtDescripcion.getText().trim());
                        ps.setString(3, (String) cmbEstado.getSelectedItem());
                        ps.setString(4, (String) cmbPrioridad.getSelectedItem());
                        ps.setString(5, txtUsuario.getText().trim());
                        ps.setString(6, txtAsignado.getText().trim());
                        ps.setInt(7, ticket.getId());
                        ps.executeUpdate();
                    }
                }

                dialog.dispose();
                cargarTickets();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error al guardar ticket: " + ex.getMessage());
            }
        });

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void editarTicket() {
        if (rol != RolUsuario.ADMINISTRADOR) {
            JOptionPane.showMessageDialog(this, "No tienes permisos para editar tickets.");
            return;
        }
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un ticket para editar.");
            return;
        }
        int id = (int) modelo.getValueAt(fila, 0);
        Ticket t = cargarTicketPorId(id);
        if (t != null) abrirFormulario(t);
    }

    private void eliminarTicketSeleccionado() {
        if (rol != RolUsuario.ADMINISTRADOR) {
            JOptionPane.showMessageDialog(this, "No tienes permisos para eliminar tickets.");
            return;
        }
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un ticket para eliminar.");
            return;
        }
        int id = (int) modelo.getValueAt(fila, 0);
        int resp = JOptionPane.showConfirmDialog(this, "¿Seguro que quieres eliminar el ticket ID " + id + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (resp == JOptionPane.YES_OPTION) {
            eliminarTicket(id);
        }
    }

    private Ticket cargarTicketPorId(int id) {
        String sql = "SELECT * FROM tickets WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Ticket t = new Ticket();
                    t.setId(rs.getInt("id"));
                    t.setTitulo(rs.getString("titulo"));
                    t.setDescripcion(rs.getString("descripcion"));
                    t.setEstado(rs.getString("estado"));
                    t.setPrioridad(rs.getString("prioridad"));
                    t.setUsuario(rs.getString("usuario"));
                    t.setAsignado(rs.getString("asignado"));
                    t.setFechaCreacion(rs.getTimestamp("fecha_creacion").toLocalDateTime());
                    t.setFechaActualizacion(rs.getTimestamp("fecha_actualizacion").toLocalDateTime());
                    return t;
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar ticket: " + ex.getMessage());
        }
        return null;
    }

    private void eliminarTicket(int id) {
        String sql = "DELETE FROM tickets WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            int r = ps.executeUpdate();
            if (r > 0) {
                JOptionPane.showMessageDialog(this, "Ticket eliminado correctamente.");
                cargarTickets();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al eliminar ticket: " + ex.getMessage());
        }
    }

    private void salir() {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        frame.dispose();
        new SelectorDeArea(usuario, rol).setVisible(true); // Enum directo
    }

    public static class Ticket {
        private int id;
        private String titulo;
        private String descripcion;
        private String estado;
        private String prioridad;
        private String usuario;
        private String asignado;
        private java.time.LocalDateTime fechaCreacion;
        private java.time.LocalDateTime fechaActualizacion;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getTitulo() { return titulo; }
        public void setTitulo(String titulo) { this.titulo = titulo; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
        public String getPrioridad() { return prioridad; }
        public void setPrioridad(String prioridad) { this.prioridad = prioridad; }
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public String getAsignado() { return asignado; }
        public void setAsignado(String asignado) { this.asignado = asignado; }
        public java.time.LocalDateTime getFechaCreacion() { return fechaCreacion; }
        public void setFechaCreacion(java.time.LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
        public java.time.LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
        public void setFechaActualizacion(java.time.LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
    }
}
