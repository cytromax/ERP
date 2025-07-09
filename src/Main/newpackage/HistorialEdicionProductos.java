package Main.newpackage;

import conexion.ConexionDB;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.io.FileOutputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HistorialEdicionProductos extends JFrame {
    private JTable tabla;
    private DefaultTableModel modelo;

    private JTextField txtBuscarProducto;
    private JSpinner spinnerFechaDesde;
    private JSpinner spinnerFechaHasta;
    private JButton btnFiltrar, btnExportarTodo, btnExportarSeleccion, btnEliminarSeleccion, btnSalir;

    private String rolActual;

    public HistorialEdicionProductos(String rolActual) {
        this.rolActual = rolActual;

        setTitle("Historial de Edición de Productos");
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel filtros
        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panelFiltros.add(new JLabel("Buscar producto (código o modelo):"));
        txtBuscarProducto = new JTextField(20);
        panelFiltros.add(txtBuscarProducto);

        panelFiltros.add(new JLabel("Fecha desde:"));
        spinnerFechaDesde = new JSpinner(new SpinnerDateModel());
        spinnerFechaDesde.setEditor(new JSpinner.DateEditor(spinnerFechaDesde, "yyyy-MM-dd"));
        panelFiltros.add(spinnerFechaDesde);

        panelFiltros.add(new JLabel("Fecha hasta:"));
        spinnerFechaHasta = new JSpinner(new SpinnerDateModel());
        spinnerFechaHasta.setEditor(new JSpinner.DateEditor(spinnerFechaHasta, "yyyy-MM-dd"));
        panelFiltros.add(spinnerFechaHasta);

        btnFiltrar = new JButton("Filtrar");
        btnFiltrar.addActionListener(e -> cargarHistorialFiltrado());
        panelFiltros.add(btnFiltrar);

        add(panelFiltros, BorderLayout.NORTH);

        // Tabla
        modelo = new DefaultTableModel(
            new String[]{
                "ID Edición", "ID Producto", "Código", "Modelo",
                "Campo Modificado", "Valor Anterior", "Valor Nuevo",
                "Usuario", "Fecha"
            }, 0);
        tabla = new JTable(modelo);
        tabla.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        ajustarColumnas();

        JScrollPane scroll = new JScrollPane(tabla);
        add(scroll, BorderLayout.CENTER);

        // Panel botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnExportarTodo = new JButton("Exportar todo a Excel");
        btnExportarSeleccion = new JButton("Exportar selección a Excel");
        btnEliminarSeleccion = new JButton("Eliminar seleccionados");
        btnSalir = new JButton("Salir");

        panelBotones.add(btnExportarTodo);
        panelBotones.add(btnExportarSeleccion);
        panelBotones.add(btnEliminarSeleccion);
        panelBotones.add(btnSalir);

        add(panelBotones, BorderLayout.SOUTH);

        // Listeners
        btnExportarTodo.addActionListener(e -> exportarAExcel(false));
        btnExportarSeleccion.addActionListener(e -> exportarAExcel(true));
        btnEliminarSeleccion.addActionListener(e -> eliminarSeleccionados());
        btnSalir.addActionListener(e -> dispose());

        // Carga inicial sin filtro
        cargarHistorialFiltrado();
    }

    private void ajustarColumnas() {
        TableColumnModel columnModel = tabla.getColumnModel();
        int[] anchos = {80, 80, 120, 250, 120, 120, 120, 100, 130};
        for (int i = 0; i < anchos.length; i++) {
            if (i < columnModel.getColumnCount()) {
                columnModel.getColumn(i).setPreferredWidth(anchos[i]);
            }
        }
    }

    private void cargarHistorialFiltrado() {
        String texto = txtBuscarProducto.getText().trim();
        Date fechaDesde = (Date) spinnerFechaDesde.getValue();
        Date fechaHasta = (Date) spinnerFechaHasta.getValue();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String fechaDesdeStr = sdf.format(fechaDesde);
        String fechaHastaStr = sdf.format(fechaHasta);

        modelo.setRowCount(0);

        String sql = """
            SELECT 
                he.id, he.id_producto, p.codigo_barras, p.modelo,
                he.campo_modificado, he.valor_anterior, he.valor_nuevo,
                he.usuario, he.fecha
            FROM historial_edicion he
            JOIN productos p ON he.id_producto = p.id
            WHERE (p.codigo_barras ILIKE ? OR p.modelo ILIKE ?)
              AND he.fecha BETWEEN ?::timestamp AND ?::timestamp + INTERVAL '1 day'
            ORDER BY he.fecha DESC
        """;

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String filtro = "%" + texto + "%";
            ps.setString(1, filtro);
            ps.setString(2, filtro);
            ps.setString(3, fechaDesdeStr);
            ps.setString(4, fechaHastaStr);

            ResultSet rs = ps.executeQuery();

            SimpleDateFormat formatoFecha = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getInt("id_producto"),
                    rs.getString("codigo_barras"),
                    rs.getString("modelo"),
                    rs.getString("campo_modificado"),
                    rs.getString("valor_anterior"),
                    rs.getString("valor_nuevo"),
                    rs.getString("usuario"),
                    formatoFecha.format(rs.getTimestamp("fecha"))
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar historial filtrado: " + e.getMessage());
        }
    }

    private void exportarAExcel(boolean soloSeleccion) {
        if (soloSeleccion && tabla.getSelectedRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Selecciona al menos una fila para exportar.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar archivo Excel");
        int opcion = chooser.showSaveDialog(this);
        if (opcion != JFileChooser.APPROVE_OPTION) return;

        String rutaArchivo = chooser.getSelectedFile().getAbsolutePath();
        if (!rutaArchivo.toLowerCase().endsWith(".xlsx")) {
            rutaArchivo += ".xlsx";
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Historial Edición Productos");

            // Crear fila cabecera
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < modelo.getColumnCount(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(modelo.getColumnName(i));
            }

            int filaExcel = 1;
            if (soloSeleccion) {
                int[] filasSeleccionadas = tabla.getSelectedRows();
                for (int filaTabla : filasSeleccionadas) {
                    Row fila = sheet.createRow(filaExcel++);
                    for (int col = 0; col < modelo.getColumnCount(); col++) {
                        Cell cell = fila.createCell(col);
                        Object valor = modelo.getValueAt(filaTabla, col);
                        setCellValue(cell, valor);
                    }
                }
            } else {
                for (int filaTabla = 0; filaTabla < modelo.getRowCount(); filaTabla++) {
                    Row fila = sheet.createRow(filaExcel++);
                    for (int col = 0; col < modelo.getColumnCount(); col++) {
                        Cell cell = fila.createCell(col);
                        Object valor = modelo.getValueAt(filaTabla, col);
                        setCellValue(cell, valor);
                    }
                }
            }

            // Ajustar ancho columnas
            for (int i = 0; i < modelo.getColumnCount(); i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(rutaArchivo)) {
                workbook.write(fos);
            }
            JOptionPane.showMessageDialog(this, "Archivo Excel guardado correctamente.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al exportar a Excel: " + e.getMessage());
        }
    }

    private void setCellValue(Cell cell, Object valor) {
        if (valor == null) {
            cell.setCellValue("");
        } else if (valor instanceof Number) {
            cell.setCellValue(((Number) valor).doubleValue());
        } else {
            cell.setCellValue(valor.toString());
        }
    }

    private void eliminarSeleccionados() {
        if (!"administrador".equalsIgnoreCase(rolActual)) {
            JOptionPane.showMessageDialog(this, "Solo los administradores pueden eliminar registros.");
            return;
        }

        int[] filas = tabla.getSelectedRows();
        if (filas.length == 0) {
            JOptionPane.showMessageDialog(this, "Selecciona al menos una fila para eliminar.");
            return;
        }

        int confirmar = JOptionPane.showConfirmDialog(this,
            "¿Estás seguro de eliminar los registros seleccionados? Esta acción no se puede deshacer.",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION);

        if (confirmar != JOptionPane.YES_OPTION) return;

        try (Connection con = ConexionDB.conectar()) {
            con.setAutoCommit(false);

            String sqlDelete = "DELETE FROM historial_edicion WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlDelete)) {
                for (int fila : filas) {
                    int id = (int) modelo.getValueAt(fila, 0);
                    ps.setInt(1, id);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            con.commit();

            cargarHistorialFiltrado();

            JOptionPane.showMessageDialog(this, "Registros eliminados correctamente.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al eliminar registros: " + e.getMessage());
        }
    }
}
