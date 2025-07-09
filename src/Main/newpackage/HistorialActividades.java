package Main.newpackage;

import conexion.ConexionDB;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.sql.*;

public class HistorialActividades extends JFrame {
    private JTable tabla;
    private DefaultTableModel modelo;
    private String rolActual;

    public HistorialActividades(String rolActual) {
        this.rolActual = rolActual;
        setTitle("Historial General de Actividades");
        setSize(1000, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
        if (rolActual.equalsIgnoreCase("administrador")) {
            cargarHistorial();
        } else {
            JOptionPane.showMessageDialog(this, "Acceso solo para administradores.");
            dispose();
        }
    }

    private void initUI() {
        modelo = new DefaultTableModel(new String[]{
            "ID", "Usuario", "Acción", "Descripción", "Resultado", "Fecha"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tabla = new JTable(modelo);
        JScrollPane scroll = new JScrollPane(tabla);
        add(scroll, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnExportar = new JButton("Exportar a CSV");
        btnExportar.addActionListener(e -> exportarCSV());
        panelBotones.add(btnExportar);

        JButton btnSalir = new JButton("Salir");
        btnSalir.addActionListener(e -> dispose());
        panelBotones.add(btnSalir);

        add(panelBotones, BorderLayout.SOUTH);
    }

    private void cargarHistorial() {
        modelo.setRowCount(0);
        try (Connection con = ConexionDB.conectar()) {
            String sql = "SELECT id, username, accion, descripcion, resultado, fecha FROM log_actividades ORDER BY fecha DESC";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("accion"),
                    rs.getString("descripcion"),
                    rs.getString("resultado"),
                    rs.getTimestamp("fecha")
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar historial: " + ex.getMessage());
        }
    }

    private void exportarCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar historial como CSV");
        int seleccion = fileChooser.showSaveDialog(this);
        if (seleccion == JFileChooser.APPROVE_OPTION) {
            String ruta = fileChooser.getSelectedFile().getAbsolutePath();
            if (!ruta.endsWith(".csv")) ruta += ".csv";
            try (FileWriter fw = new FileWriter(ruta)) {
                for (int i = 0; i < modelo.getColumnCount(); i++) {
                    fw.write(modelo.getColumnName(i));
                    if (i < modelo.getColumnCount() - 1) fw.write(",");
                }
                fw.write("\n");
                for (int i = 0; i < modelo.getRowCount(); i++) {
                    for (int j = 0; j < modelo.getColumnCount(); j++) {
                        fw.write(String.valueOf(modelo.getValueAt(i, j)));
                        if (j < modelo.getColumnCount() - 1) fw.write(",");
                    }
                    fw.write("\n");
                }
                JOptionPane.showMessageDialog(this, "Historial exportado correctamente.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al exportar: " + ex.getMessage());
            }
        }
    }
}
