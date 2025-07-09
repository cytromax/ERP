package Main.newpackage;

import conexion.ConexionDB;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class ImprimirCodigoBarras extends JFrame {

    private JTextField txtFiltro;
    private JTable tabla;
    private DefaultTableModel modelo;

    public ImprimirCodigoBarras() {
        setTitle("Imprimir Código de Barras");
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        // Panel superior: filtro y botón buscar
        JPanel pnlBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        pnlBusqueda.add(new JLabel("Código o Modelo:"));
        txtFiltro = new JTextField(20);
        pnlBusqueda.add(txtFiltro);
        JButton btnBuscar = new JButton("Buscar");
        pnlBusqueda.add(btnBuscar);
        add(pnlBusqueda, BorderLayout.NORTH);

        // Tabla de resultados
        modelo = new DefaultTableModel(new String[]{"ID", "Código","Modelo"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabla = new JTable(modelo);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // Panel inferior: imprimir y salir
        JPanel pnlAccion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        JButton btnImprimir = new JButton("Imprimir Código de Barras");
        JButton btnSalir    = new JButton("Salir");
        pnlAccion.add(btnImprimir);
        pnlAccion.add(btnSalir);
        add(pnlAccion, BorderLayout.SOUTH);

        // Acciones
        btnBuscar.addActionListener(e -> cargarProductos(txtFiltro.getText().trim()));
        btnImprimir.addActionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (fila == -1) {
                JOptionPane.showMessageDialog(this, "Selecciona un producto primero.");
                return;
            }
            String codigo = modelo.getValueAt(fila, 1).toString();
            String modeloProd = modelo.getValueAt(fila, 2).toString();
            // Aquí usas tu visor/impresor de códigos
            new VistaCodigosBarras(codigo, modeloProd).setVisible(true);
        });
        btnSalir.addActionListener(e -> dispose());

        // Enter en txtFiltro lanza búsqueda
        txtFiltro.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    cargarProductos(txtFiltro.getText().trim());
                }
            }
        });
    }

    private void cargarProductos(String filtro) {
        modelo.setRowCount(0);
        if (filtro.isEmpty()) return;
        String sql = "SELECT id, codigo_barras, modelo FROM productos "
                   + "WHERE codigo_barras ILIKE ? OR modelo ILIKE ?";
        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + filtro + "%");
            ps.setString(2, "%" + filtro + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    modelo.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("codigo_barras"),
                        rs.getString("modelo")
                    });
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar productos: " + ex.getMessage());
        }
    }

    // Para lanzar desde cualquier parte de tu app:
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ImprimirCodigoBarras().setVisible(true));
    }
}
