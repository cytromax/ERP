package Main.newpackage;

import conexion.ConexionDB;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class EditarUsuarios extends JFrame {
    private JTable tabla;
    private DefaultTableModel modelo;
    private JButton btnGuardarCambios, btnEliminarUsuario, btnCerrar;

    public EditarUsuarios() {
        setTitle("Editar Usuarios");
        setSize(900, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
        cargarUsuarios();
    }

    private void initUI() {
        modelo = new DefaultTableModel(new String[]{
            "ID", "Código empleado", "Nombre", "Usuario", "Rol", "Área", "Turno", "Activo"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Solo las columnas 1 a 7 son editables (no ID)
                return column > 0;
            }
        };
        tabla = new JTable(modelo);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(tabla);
        add(scroll, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnGuardarCambios = new JButton("Guardar Cambios");
        btnEliminarUsuario = new JButton("Eliminar Usuario");
        btnCerrar = new JButton("Cerrar");

        btnGuardarCambios.addActionListener(e -> guardarCambios());
        btnEliminarUsuario.addActionListener(e -> eliminarUsuario());
        btnCerrar.addActionListener(e -> dispose());

        panelBotones.add(btnGuardarCambios);
        panelBotones.add(btnEliminarUsuario);
        panelBotones.add(btnCerrar);

        add(panelBotones, BorderLayout.SOUTH);
    }

    private void cargarUsuarios() {
        modelo.setRowCount(0);
        try (Connection con = ConexionDB.conectar()) {
            String sql = "SELECT id, codigo_empleado, nombre, usuario, puesto, area, turno, activo FROM empleados ORDER BY nombre";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("codigo_empleado"),
                    rs.getString("nombre"),
                    rs.getString("usuario"),
                    rs.getString("puesto"),
                    rs.getString("area"),
                    rs.getString("turno"),
                    rs.getBoolean("activo") ? "Sí" : "No"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar usuarios: " + ex.getMessage());
        }
    }

    private void guardarCambios() {
        try (Connection con = ConexionDB.conectar()) {
            for (int i = 0; i < modelo.getRowCount(); i++) {
                int id = Integer.parseInt(modelo.getValueAt(i, 0).toString());
                String codigoEmpleado = modelo.getValueAt(i, 1).toString().trim();
                String nombre = modelo.getValueAt(i, 2).toString().trim();
                String usuario = modelo.getValueAt(i, 3).toString().trim();
                String puesto = modelo.getValueAt(i, 4).toString().trim();
                String area = modelo.getValueAt(i, 5).toString().trim();
                String turno = modelo.getValueAt(i, 6).toString().trim();
                String activoStr = modelo.getValueAt(i, 7).toString();
                boolean activo = activoStr.equalsIgnoreCase("Sí");

                if (codigoEmpleado.isEmpty() || nombre.isEmpty() || usuario.isEmpty() || puesto.isEmpty() || area.isEmpty() || turno.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No puedes dejar campos vacíos. Revisa la fila " + (i + 1));
                    return;
                }

                String sql = "UPDATE empleados SET codigo_empleado=?, nombre=?, usuario=?, puesto=?, area=?, turno=?, activo=? WHERE id=?";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, codigoEmpleado);
                ps.setString(2, nombre);
                ps.setString(3, usuario);
                ps.setString(4, puesto);
                ps.setString(5, area);
                ps.setString(6, turno);
                ps.setBoolean(7, activo);
                ps.setInt(8, id);

                ps.executeUpdate();
                ps.close();
            }
            JOptionPane.showMessageDialog(this, "Cambios guardados correctamente.");
            cargarUsuarios();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al guardar cambios: " + e.getMessage());
        }
    }

    private void eliminarUsuario() {
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario para eliminar.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "¿Seguro que deseas desactivar este usuario? (No se eliminará de la base, solo se marcará como inactivo)", "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        int id = Integer.parseInt(modelo.getValueAt(fila, 0).toString());

        try (Connection con = ConexionDB.conectar()) {
            PreparedStatement ps = con.prepareStatement("UPDATE empleados SET activo = false WHERE id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            modelo.setValueAt("No", fila, 7); // Refleja el cambio en la tabla inmediatamente
            JOptionPane.showMessageDialog(this, "Usuario desactivado.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al desactivar usuario: " + e.getMessage());
        }
    }
}
