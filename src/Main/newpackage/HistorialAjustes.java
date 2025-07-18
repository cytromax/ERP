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

public class HistorialAjustes extends JFrame {
    private static HistorialAjustes instanciaUnica = null;

    public static void mostrarVentana() {
        if (instanciaUnica == null || !instanciaUnica.isDisplayable()) {
            instanciaUnica = new HistorialAjustes();
        }
        instanciaUnica.setVisible(true);
        instanciaUnica.toFront();
        instanciaUnica.requestFocus();
    }

    private JTable tabla;
    private DefaultTableModel modelo;

    private JTextField txtBuscarProducto;
    private JSpinner spinnerFechaDesde;
    private JSpinner spinnerFechaHasta;
    private JButton btnFiltrar, btnExportarTodo, btnExportarSeleccion, btnSalir;

    public HistorialAjustes() {
        setTitle("Historial de Ajustes de Inventario");
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
        spinnerFechaDesde = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        spinnerFechaDesde.setEditor(new JSpinner.DateEditor(spinnerFechaDesde, "dd-MM-yyyy"));
        panelFiltros.add(spinnerFechaDesde);

        panelFiltros.add(new JLabel("Fecha hasta:"));
        spinnerFechaHasta = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        spinnerFechaHasta.setEditor(new JSpinner.DateEditor(spinnerFechaHasta, "dd-MM-yyyy"));
        panelFiltros.add(spinnerFechaHasta);

        btnFiltrar = new JButton("Filtrar");
        btnFiltrar.addActionListener(e -> cargarHistorialFiltrado());
        panelFiltros.add(btnFiltrar);

        add(panelFiltros, BorderLayout.NORTH);

        // Tabla
        modelo = new DefaultTableModel(
            new String[]{"ID Ajuste", "ID Producto", "Código", "Modelo", "Existencia antes", "Cantidad", "Existencia después", "Usuario", "Fecha"},
            0
        );
        tabla = new JTable(modelo);
        tabla.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // --- OCULTAR las dos primeras columnas (ID Ajuste y ID Producto) ---
        TableColumnModel columnModel = tabla.getColumnModel();
        // Oculta columna 0 (ID Ajuste)
        columnModel.getColumn(0).setMinWidth(0);
        columnModel.getColumn(0).setMaxWidth(0);
        columnModel.getColumn(0).setPreferredWidth(0);
        // Oculta columna 1 (ID Producto)
        columnModel.getColumn(1).setMinWidth(0);
        columnModel.getColumn(1).setMaxWidth(0);
        columnModel.getColumn(1).setPreferredWidth(0);

        ajustarColumnas();

        JScrollPane scroll = new JScrollPane(tabla);
        add(scroll, BorderLayout.CENTER);

        // Panel botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnExportarTodo = new JButton("Exportar todo a Excel");
        btnExportarSeleccion = new JButton("Exportar selección a Excel");
        btnSalir = new JButton("Salir");

        panelBotones.add(btnExportarTodo);
        panelBotones.add(btnExportarSeleccion);
        panelBotones.add(btnSalir);

        add(panelBotones, BorderLayout.SOUTH);

        // Listeners
        btnExportarTodo.addActionListener(e -> exportarAExcel(false));
        btnExportarSeleccion.addActionListener(e -> exportarAExcel(true));
        btnSalir.addActionListener(e -> dispose());

        // Carga inicial sin filtro
        cargarHistorialFiltrado();
    }

    private void ajustarColumnas() {
        TableColumnModel columnModel = tabla.getColumnModel();
        // Ajustar solo desde la columna 2 en adelante porque 0 y 1 están ocultas
        int[] anchos = {0, 0, 120, 250, 100, 70, 120, 100, 130};
        for (int i = 2; i < anchos.length; i++) {
            if (i < columnModel.getColumnCount()) {
                columnModel.getColumn(i).setPreferredWidth(anchos[i]);
            }
        }
    }

    private void cargarHistorialFiltrado() {
        String texto = txtBuscarProducto.getText().trim();
        Date fechaDesde = (Date) spinnerFechaDesde.getValue();
        Date fechaHasta = (Date) spinnerFechaHasta.getValue();

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        String fechaDesdeStr = sdf.format(fechaDesde);
        String fechaHastaStr = sdf.format(fechaHasta);

        modelo.setRowCount(0);

        String sql = """
            SELECT m.id, m.id_producto, p.codigo_barras, p.modelo,
                   m.existencia_antes, m.cantidad, m.existencia_despues,
                   m.usuario, m.fecha
            FROM movimientos m
            JOIN productos p ON m.id_producto = p.id
            WHERE m.tipo = 'ajuste'
              AND (p.codigo_barras ILIKE ? OR p.modelo ILIKE ?)
              AND m.fecha BETWEEN ?::timestamp AND ?::timestamp + INTERVAL '1 day'
            ORDER BY m.fecha DESC
        """;

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String filtro = "%" + texto + "%";
            ps.setString(1, filtro);
            ps.setString(2, filtro);
            ps.setString(3, fechaDesdeStr);
            ps.setString(4, fechaHastaStr);

            // Fecha en formato DÍA/MES/AÑO y hora
            SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("fecha");
                String fechaFormateada = timestamp != null ? formatoFecha.format(timestamp) : "N/A";

                modelo.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getInt("id_producto"),
                    rs.getString("codigo_barras"),
                    rs.getString("modelo"),
                    rs.getDouble("existencia_antes"),
                    rs.getDouble("cantidad"),
                    rs.getDouble("existencia_despues"),
                    rs.getString("usuario"),
                    fechaFormateada
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
            Sheet sheet = workbook.createSheet("Historial Ajustes");

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
}
