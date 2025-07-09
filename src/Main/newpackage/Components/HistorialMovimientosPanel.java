package Main.newpackage.Components;

import conexion.ConexionDB;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.io.FileOutputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class HistorialMovimientosPanel extends JPanel {
    private DefaultTableModel modelo;
    private JTable tabla;

    private JTextField txtBuscarProducto;
    private JTextField txtNumeroPedido;
    private JComboBox<String> comboTipo;
    private JSpinner spinnerFechaDesde;
    private JSpinner spinnerFechaHasta;
    private JButton btnFiltrar, btnExportar;

    private String tipoDefault = "todos";

    public HistorialMovimientosPanel(String tipoMovimiento) {
        if (tipoMovimiento != null && !tipoMovimiento.trim().isEmpty()) {
            tipoDefault = tipoMovimiento.toLowerCase();
        }
        setLayout(new BorderLayout());

        // Panel filtros
        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        panelFiltros.add(new JLabel("Buscar producto (código o modelo):"));
        txtBuscarProducto = new JTextField(20);
        panelFiltros.add(txtBuscarProducto);

        panelFiltros.add(new JLabel("Núm. pedido:"));
        txtNumeroPedido = new JTextField(12);
        panelFiltros.add(txtNumeroPedido);

        panelFiltros.add(new JLabel("Tipo de mov:"));
        comboTipo = new JComboBox<>(new String[]{"Todos", "Entrada", "Salida"});
        panelFiltros.add(comboTipo);

        // Establecer tipo por defecto según parámetro
        if (tipoDefault.equals("entrada")) comboTipo.setSelectedItem("Entrada");
        else if (tipoDefault.equals("salida")) comboTipo.setSelectedItem("Salida");
        else comboTipo.setSelectedItem("Todos");

        panelFiltros.add(new JLabel("Fecha desde:"));
        spinnerFechaDesde = new JSpinner(new SpinnerDateModel());
        spinnerFechaDesde.setEditor(new JSpinner.DateEditor(spinnerFechaDesde, "yyyy-MM-dd"));
        panelFiltros.add(spinnerFechaDesde);

        panelFiltros.add(new JLabel("Fecha hasta:"));
        spinnerFechaHasta = new JSpinner(new SpinnerDateModel());
        spinnerFechaHasta.setEditor(new JSpinner.DateEditor(spinnerFechaHasta, "yyyy-MM-dd"));
        panelFiltros.add(spinnerFechaHasta);

        btnFiltrar = new JButton("Filtrar");
        btnFiltrar.addActionListener(e -> cargarDatosFiltrados());
        panelFiltros.add(btnFiltrar);

        add(panelFiltros, BorderLayout.NORTH);

        // Modelo y tabla
        modelo = new DefaultTableModel(
            new String[]{
                "ID Movimiento", "ID Producto", "Código", "Modelo",
                "Existencia antes", "Tipo", "Cantidad", "Existencia después",
                "Empleado", "Proveedor", "Usuario", "Fecha", "Núm. Pedido"
            }, 0
        );
        tabla = new JTable(modelo);
        ajustarColumnas();

        JScrollPane scroll = new JScrollPane(tabla);
        add(scroll, BorderLayout.CENTER);

        // Botón exportar
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnExportar = new JButton("Exportar a Excel");
        btnExportar.addActionListener(e -> exportarAExcel());
        panelBotones.add(btnExportar);
        add(panelBotones, BorderLayout.SOUTH);

        // Cargar datos iniciales
        cargarDatosFiltrados();
    }

    private void ajustarColumnas() {
        TableColumnModel columnModel = tabla.getColumnModel();
        int[] anchos = {80, 80, 120, 250, 100, 70, 70, 120, 180, 180, 100, 130, 100};
        for (int i = 0; i < anchos.length; i++) {
            if (i < columnModel.getColumnCount()) {
                columnModel.getColumn(i).setPreferredWidth(anchos[i]);
            }
        }
    }

    private void cargarDatosFiltrados() {
        String texto = txtBuscarProducto.getText().trim();
        String numeroPedido = txtNumeroPedido.getText().trim();
        String tipo = comboTipo.getSelectedItem().toString();
        Date fechaDesde = (Date) spinnerFechaDesde.getValue();
        Date fechaHasta = (Date) spinnerFechaHasta.getValue();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String fechaDesdeStr = sdf.format(fechaDesde);
        String fechaHastaStr = sdf.format(fechaHasta);

        modelo.setRowCount(0);

        String sql = """
            SELECT 
                m.id, m.id_producto, p.codigo_barras, p.modelo, 
                m.existencia_antes, m.tipo, m.cantidad, 
                m.existencia_despues, 
                COALESCE(e.codigo_empleado || ' - ' || e.nombre, 'N/A') AS empleado,
                COALESCE(m.proveedor, 'N/A') AS proveedor,
                m.usuario, m.fecha,
                COALESCE(m.numero_pedido, '') AS numero_pedido
            FROM movimientos m 
            JOIN productos p ON m.id_producto = p.id 
            LEFT JOIN empleados e ON m.id_empleado = e.id
            WHERE (p.codigo_barras ILIKE ? OR p.modelo ILIKE ?)
              AND (? = '' OR m.numero_pedido ILIKE ?)
              AND (? = 'Todos' OR LOWER(m.tipo) = LOWER(?))
              AND m.fecha BETWEEN ?::timestamp AND ?::timestamp + INTERVAL '1 day'
            ORDER BY m.fecha DESC
        """;

        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String filtro = "%" + texto + "%";
            ps.setString(1, filtro);
            ps.setString(2, filtro);
            ps.setString(3, numeroPedido);
            ps.setString(4, "%" + numeroPedido + "%");
            ps.setString(5, tipo);
            ps.setString(6, tipo);
            ps.setString(7, sdf.format(fechaDesde));
            ps.setString(8, sdf.format(fechaHasta));

            ResultSet rs = ps.executeQuery();

            SimpleDateFormat formatoFecha = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            while (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("fecha");
                String fechaFormateada = timestamp != null ? formatoFecha.format(timestamp) : "N/A";
                modelo.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getInt("id_producto"),
                    rs.getString("codigo_barras"),
                    rs.getString("modelo"),
                    rs.getDouble("existencia_antes"),
                    rs.getString("tipo"),
                    rs.getDouble("cantidad"),
                    rs.getDouble("existencia_despues"),
                    rs.getString("empleado"),
                    rs.getString("proveedor"),
                    rs.getString("usuario"),
                    fechaFormateada,
                    rs.getString("numero_pedido")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar historial filtrado: " + e.getMessage());
        }
    }

    private void exportarAExcel() {
        if (modelo.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay datos para exportar.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar historial como Excel");
        int seleccion = fileChooser.showSaveDialog(this);
        if (seleccion == JFileChooser.APPROVE_OPTION) {
            String ruta = fileChooser.getSelectedFile().getAbsolutePath();
            if (!ruta.endsWith(".xlsx")) ruta += ".xlsx";

            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Historial de Movimientos");
                // Crear fila de encabezado
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < modelo.getColumnCount(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(modelo.getColumnName(i));
                }
                // Agregar filas de datos
                for (int i = 0; i < modelo.getRowCount(); i++) {
                    Row row = sheet.createRow(i + 1);
                    for (int j = 0; j < modelo.getColumnCount(); j++) {
                        Object value = modelo.getValueAt(i, j);
                        Cell cell = row.createCell(j);
                        if (value instanceof Number) {
                            cell.setCellValue(((Number) value).doubleValue());
                        } else {
                            cell.setCellValue(value != null ? value.toString() : "");
                        }
                    }
                }
                // Auto-ajustar tamaño columnas
                for (int i = 0; i < modelo.getColumnCount(); i++) {
                    sheet.autoSizeColumn(i);
                }
                // Guardar archivo
                try (FileOutputStream fileOut = new FileOutputStream(ruta)) {
                    workbook.write(fileOut);
                }
                JOptionPane.showMessageDialog(this, "Historial exportado correctamente.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al exportar a Excel: " + e.getMessage());
            }
        }
    }
}
