package Main.newpackage;

import conexion.ConexionDB;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.FileWriter;
import java.sql.*;

public class AjusteInventario extends JFrame {
 private static AjusteInventario instanciaUnica = null;

public static void mostrarVentana(String usuarioActual, String rolActual) {
    if (instanciaUnica == null || !instanciaUnica.isDisplayable()) {
        instanciaUnica = new AjusteInventario(usuarioActual, rolActual);
    }
    instanciaUnica.setVisible(true);
    instanciaUnica.toFront();
    instanciaUnica.requestFocus();
}


    private JTextField txtCodigo, txtCantidad;
    private JButton btnAplicar, btnHistorial, btnExportarTodo, btnExportarSeleccion, btnEliminar, btnSalir;
    private JTable tablaHistorial;
    private DefaultTableModel modeloHistorial;

    private String usuarioActual;
    private String rolActual;

    public AjusteInventario(String usuarioActual, String rolActual) {
        this.usuarioActual = usuarioActual;
        this.rolActual = rolActual;

        setTitle("Ajuste de Inventario");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
        cargarHistorialAjustes();
    }

    private void initUI() {
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Formulario
        JPanel panelFormulario = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Código o Nombre
        c.gridx = 0; c.gridy = 0;
        panelFormulario.add(new JLabel("Código de barras o Nombre:"), c);
        txtCodigo = new JTextField();
        c.gridx = 1; c.gridy = 0;
        c.weightx = 1.0;
        panelFormulario.add(txtCodigo, c);

        // Cantidad
        c.gridx = 0; c.gridy = 1;
        c.weightx = 0;
        panelFormulario.add(new JLabel("Cantidad (positiva o negativa):"), c);
        txtCantidad = new JTextField();
        c.gridx = 1; c.gridy = 1;
        c.weightx = 1.0;
        panelFormulario.add(txtCantidad, c);

        // Botones
        btnAplicar = new JButton("Aplicar ajuste");
        btnAplicar.addActionListener(e -> aplicarAjuste());

        btnHistorial = new JButton("Historial de Ajustes");
        btnHistorial.addActionListener(e -> {
            HistorialAjustes historial = new HistorialAjustes();
            historial.setVisible(true);
        });

        btnExportarTodo = new JButton("Exportar todo a CSV");
        btnExportarTodo.addActionListener(e -> exportarCSV(true));

        btnExportarSeleccion = new JButton("Exportar selección a CSV");
        btnExportarSeleccion.addActionListener(e -> exportarCSV(false));

        btnEliminar = new JButton("Eliminar seleccionados");
        btnEliminar.addActionListener(e -> eliminarSeleccionados());

        btnSalir = new JButton("Salir");
        btnSalir.addActionListener(e -> dispose());

        JPanel panelBotones = new JPanel(new GridLayout(1, 6, 10, 0));
        panelBotones.add(btnAplicar);
        panelBotones.add(btnHistorial);
        panelBotones.add(btnExportarTodo);
        panelBotones.add(btnExportarSeleccion);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnSalir);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.weightx = 1.0;
        panelFormulario.add(panelBotones, c);

        // Tabla historial
        modeloHistorial = new DefaultTableModel(
            new String[]{"ID", "Código", "Descripción", "Existencia antes", "Cantidad", "Existencia después", "Fecha", "Usuario"},
            0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaHistorial = new JTable(modeloHistorial);
        JScrollPane scrollHistorial = new JScrollPane(tablaHistorial);

        panelPrincipal.add(panelFormulario, BorderLayout.NORTH);
        panelPrincipal.add(scrollHistorial, BorderLayout.CENTER);

        add(panelPrincipal);
    }

    private void aplicarAjuste() {
        if (!"administrador".equalsIgnoreCase(rolActual)) {
            JOptionPane.showMessageDialog(this, "Solo los administradores pueden hacer ajustes de inventario.");
            return;
        }

        String codigo = txtCodigo.getText().trim();
        String cantidadStr = txtCantidad.getText().trim();

        if (codigo.isEmpty() || cantidadStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Completa todos los campos.");
            return;
        }

        double cantidad;
        try {
            cantidad = Double.parseDouble(cantidadStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida.");
            return;
        }

        try (Connection con = ConexionDB.conectar()) {
            con.setAutoCommit(false);

            String sqlProducto = "SELECT id, codigo_barras, modelo, existencia FROM productos WHERE codigo_barras = ? OR modelo ILIKE ?";
            try (PreparedStatement psProducto = con.prepareStatement(sqlProducto)) {
                psProducto.setString(1, codigo);
                psProducto.setString(2, "%" + codigo + "%");
                ResultSet rs = psProducto.executeQuery();

                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "Producto no encontrado.");
                    con.rollback();
                    return;
                }

                int idProducto = rs.getInt("id");
                double existenciaActual = rs.getDouble("existencia");
                double existenciaNueva = existenciaActual + cantidad;

                if (existenciaNueva < 0) {
                    JOptionPane.showMessageDialog(this, "El ajuste dejaría existencia negativa.");
                    con.rollback();
                    return;
                }

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
                cargarHistorialAjustes();

            } catch (Exception ex) {
                con.rollback();
                throw ex;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error en ajuste: " + e.getMessage());
        }
    }

    private void cargarHistorialAjustes() {
        modeloHistorial.setRowCount(0);
        String sql = """
            SELECT m.id, p.codigo_barras, p.modelo, m.existencia_antes, m.cantidad, m.existencia_despues, m.fecha, m.usuario
            FROM movimientos m
            JOIN productos p ON m.id_producto = p.id
            WHERE m.tipo = 'ajuste'
            ORDER BY m.fecha DESC
        """;

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                modeloHistorial.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("codigo_barras"),
                    rs.getString("modelo"),
                    rs.getDouble("existencia_antes"),
                    rs.getDouble("cantidad"),
                    rs.getDouble("existencia_despues"),
                    rs.getTimestamp("fecha"),
                    rs.getString("usuario")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error cargando historial: " + e.getMessage());
        }
    }

    private void exportarCSV(boolean todo) {
        if (modeloHistorial.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay datos para exportar.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar archivo CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("Archivo CSV", "csv"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        String path = chooser.getSelectedFile().getAbsolutePath();
        if (!path.toLowerCase().endsWith(".csv")) path += ".csv";

        try (FileWriter fw = new FileWriter(path)) {
            TableModel model = modeloHistorial;

            // Cabeceras
            for (int i = 0; i < model.getColumnCount(); i++) {
                fw.write(model.getColumnName(i));
                if (i < model.getColumnCount() - 1) fw.write(",");
            }
            fw.write("\n");

            // Filas
            if (todo) {
                for (int i = 0; i < model.getRowCount(); i++) {
                    for (int j = 0; j < model.getColumnCount(); j++) {
                        fw.write(String.valueOf(model.getValueAt(i, j)));
                        if (j < model.getColumnCount() - 1) fw.write(",");
                    }
                    fw.write("\n");
                }
            } else {
                int[] selectedRows = tablaHistorial.getSelectedRows();
                for (int row : selectedRows) {
                    for (int j = 0; j < model.getColumnCount(); j++) {
                        fw.write(String.valueOf(model.getValueAt(row, j)));
                        if (j < model.getColumnCount() - 1) fw.write(",");
                    }
                    fw.write("\n");
                }
            }

            JOptionPane.showMessageDialog(this, "Exportación exitosa.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error exportando CSV: " + e.getMessage());
        }
    }

    private void eliminarSeleccionados() {
        if (!"administrador".equalsIgnoreCase(rolActual)) {
            JOptionPane.showMessageDialog(this, "Solo los administradores pueden eliminar registros.");
            return;
        }

        int[] selectedRows = tablaHistorial.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Selecciona al menos un registro para eliminar.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Estás seguro de eliminar los registros seleccionados?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection con = ConexionDB.conectar()) {
            String sql = "DELETE FROM movimientos WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (int row : selectedRows) {
                    int id = (int) modeloHistorial.getValueAt(row, 0);
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
            }
            JOptionPane.showMessageDialog(this, "Registros eliminados.");
            cargarHistorialAjustes();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error eliminando registros: " + e.getMessage());
        }
    }
}
