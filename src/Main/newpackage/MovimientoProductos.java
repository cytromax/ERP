package Main.newpackage;

import conexion.ConexionDB;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class MovimientoProductos extends JFrame {
    // --- INICIO: Singleton para evitar varias ventanas ---
private static MovimientoProductos instanciaUnica = null;

public static void mostrarVentana(String rol, String usuarioSesion) {
    if (instanciaUnica == null || !instanciaUnica.isDisplayable()) {
        instanciaUnica = new MovimientoProductos(rol, usuarioSesion);
    }
    instanciaUnica.setVisible(true);
    instanciaUnica.toFront();
    instanciaUnica.requestFocus();
}
// --- FIN ---


    private JTextField txtBuscar;
    private JTable tablaBusqueda;
    private DefaultTableModel modeloBusqueda;

    private JTable tablaPendientes;
    private DefaultTableModel modeloPendientes;

    private JRadioButton rbEntrada, rbSalida;
    private JTextField txtEmpleadoScan;

    private JButton btnAceptarTodos;
    private JButton btnLimpiarTodos;

    private Vector<EmpleadoItem> empleadosCache = new Vector<>();

    private String rol;
    private String usuarioSesion;

    // Debounce
    private Timer debounceTimerEmpleadoScan;
    private final int DEBOUNCE_DELAY = 200; // ms

    public MovimientoProductos(String rol, String usuarioSesion) {
        this.rol = rol;
        this.usuarioSesion = usuarioSesion;

        setTitle("Carga masiva de Productos");
        setSize(1100, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        // Panel búsqueda arranque y escaneo
        JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtBuscar = new JTextField(20);
        JLabel lblBuscar = new JLabel("Escanea o ingresa código:");
        panelBusqueda.add(lblBuscar);
        panelBusqueda.add(txtBuscar);
        add(panelBusqueda, BorderLayout.NORTH);

        // DocumentListener para añadir automáticamente a pendientes
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                procesarCodigo();
            }

            public void removeUpdate(DocumentEvent e) {
                /* no-op */ }

            public void changedUpdate(DocumentEvent e) {
                procesarCodigo();
            }

            private void procesarCodigo() {
                String codigo = txtBuscar.getText().trim();
                if (codigo.length() >= 4) {
                    buscarYAgregarPendiente(codigo);
                    txtBuscar.setText("");
                }
            }
        });

        // Tablas en split
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Tabla de búsqueda (sólo último escaneo)
        modeloBusqueda = new DefaultTableModel(new String[]{"ID", "Código", "Modelo", "Existencia"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaBusqueda = new JTable(modeloBusqueda);
        split.setLeftComponent(new JScrollPane(tablaBusqueda));

        // Tabla de movimientos pendientes
        modeloPendientes = new DefaultTableModel(new String[]{
            "ID", "Código", "Modelo", "Existencia", "Cantidad", "Tipo", "Empleado"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Sólo columna 4 (Cantidad) es editable
                return col == 4;
            }
        };
        tablaPendientes = new JTable(modeloPendientes);
        split.setRightComponent(new JScrollPane(tablaPendientes));
        split.setDividerLocation(400);
        add(split, BorderLayout.CENTER);

        // Opciones de movimiento / empleado
        JPanel panelOpciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        rbEntrada = new JRadioButton("Entrada");
        rbSalida = new JRadioButton("Salida");
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbEntrada);
        bg.add(rbSalida);
        rbEntrada.setSelected(true);
        panelOpciones.add(rbEntrada);
        panelOpciones.add(rbSalida);

        // CAMPO PARA ESCANEAR EMPLEADO Y AUTOCOMPLETAR
        panelOpciones.add(new JLabel("Empleado:"));
        txtEmpleadoScan = new JTextField(20);
        panelOpciones.add(txtEmpleadoScan);

        add(panelOpciones, BorderLayout.WEST);

        // Botones para aceptar o limpiar todo
        JPanel panelAccion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        btnLimpiarTodos = new JButton("Limpiar todos");
        btnAceptarTodos = new JButton("Aceptar todos");
        panelAccion.add(btnLimpiarTodos);
        panelAccion.add(btnAceptarTodos);
        add(panelAccion, BorderLayout.SOUTH);

        // listeners
        btnLimpiarTodos.addActionListener(e -> limpiarTodos());
        btnAceptarTodos.addActionListener(e -> aplicarTodosPendientes());

        // inicializar empleados
        cargarEmpleados();

        // ----------- AUTOCOMPLETADO CON DEBOUNCE --------------
        txtEmpleadoScan.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                debounceAutocomplete();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                debounceAutocomplete();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                debounceAutocomplete();
            }

            private void debounceAutocomplete() {
                if (debounceTimerEmpleadoScan != null && debounceTimerEmpleadoScan.isRunning()) {
                    debounceTimerEmpleadoScan.stop();
                }

                debounceTimerEmpleadoScan = new Timer(DEBOUNCE_DELAY, ev -> {
                    autocompletarEmpleado();
                });
                debounceTimerEmpleadoScan.setRepeats(false);
                debounceTimerEmpleadoScan.start();
            }

            private void autocompletarEmpleado() {
                String textoCampo = txtEmpleadoScan.getText().replaceAll("\\s", "");
                if (textoCampo.contains("-")) {
                    return;
                }
                if (textoCampo.length() < 3) {
                    return;
                }

                EmpleadoItem tempEmpleado = null;
                for (EmpleadoItem it : empleadosCache) {
                    if (it.codigo.replaceAll("\\s", "").equalsIgnoreCase(textoCampo)) {
                        tempEmpleado = it;
                        break;
                    }
                }
                // Declara la variable final SOLO aquí para el lambda
                final EmpleadoItem empleadoSeleccionado = tempEmpleado;

                if (empleadoSeleccionado != null) {
                    SwingUtilities.invokeLater(() -> {
                        txtEmpleadoScan.setText(empleadoSeleccionado.codigo + " - " + empleadoSeleccionado.nombre);
                        txtEmpleadoScan.selectAll();
                    });
                }
            }
        });
        // ------------------------------------------------------
    }

    private void cargarEmpleados() {
        empleadosCache.clear();
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(
                "SELECT id,codigo_empleado,nombre FROM empleados ORDER BY nombre"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                empleadosCache.add(new EmpleadoItem(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("codigo_empleado")
                ));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar empleados: " + ex.getMessage());
        }
    }

    private void buscarYAgregarPendiente(String codigo) {
        try (Connection con = ConexionDB.conectar(); PreparedStatement ps = con.prepareStatement(
                "SELECT id,codigo_barras,modelo,existencia FROM productos WHERE codigo_barras = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String codigoEmpleado = obtenerCodigoEmpleado();
                    EmpleadoItem emp = empleadosCache.stream()
                            .filter(it -> it.codigo.replaceAll("\\s", "").equalsIgnoreCase(codigoEmpleado))
                            .findFirst().orElse(null);

                    if (emp == null) {
                        JOptionPane.showMessageDialog(this, "Escanea un empleado válido.");
                        return;
                    }

                    Object[] fila = new Object[7];
                    fila[0] = rs.getInt("id");
                    fila[1] = rs.getString("codigo_barras");
                    fila[2] = rs.getString("modelo");
                    fila[3] = rs.getDouble("existencia");
                    fila[4] = 1.0;
                    fila[5] = rbEntrada.isSelected() ? "entrada" : "salida";
                    fila[6] = emp.codigo + " - " + emp.nombre;
                    modeloPendientes.addRow(fila);
                    modeloBusqueda.setRowCount(0);
                    modeloBusqueda.addRow(new Object[]{fila[0], fila[1], fila[2], fila[3]});
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al buscar producto: " + ex.getMessage());
        }
    }

    private void limpiarTodos() {
        modeloPendientes.setRowCount(0);
    }

    private void aplicarTodosPendientes() {
        if (modeloPendientes.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay movimientos pendientes.");
            return;
        }
        String codigoEmpleado = obtenerCodigoEmpleado();
        EmpleadoItem emp = empleadosCache.stream()
                .filter(it -> it.codigo.replaceAll("\\s", "").equalsIgnoreCase(codigoEmpleado))
                .findFirst().orElse(null);

        if (emp == null) {
            JOptionPane.showMessageDialog(this, "Escanea un empleado válido antes de aplicar los movimientos.");
            return;
        }

        try (Connection con = ConexionDB.conectar()) {
            con.setAutoCommit(false);
            String sqlUpd = "UPDATE productos SET existencia = ? WHERE id = ?";
            String sqlIns = "INSERT INTO movimientos (id_producto,id_empleado,existencia_antes,tipo,cantidad,existencia_despues,fecha,usuario) VALUES (?,?,?,?,?,?,NOW(),?)";
            try (PreparedStatement psUpd = con.prepareStatement(sqlUpd); PreparedStatement psIns = con.prepareStatement(sqlIns)) {
                for (int i = 0; i < modeloPendientes.getRowCount(); i++) {
                    int id = (int) modeloPendientes.getValueAt(i, 0);
                    double antes = (double) modeloPendientes.getValueAt(i, 3);
                    double mov = Double.parseDouble(modeloPendientes.getValueAt(i, 4).toString());
                    String tipo = modeloPendientes.getValueAt(i, 5).toString();
                    double despues = tipo.equals("entrada") ? antes + mov : antes - mov;
                    psUpd.setDouble(1, despues);
                    psUpd.setInt(2, id);
                    psUpd.executeUpdate();
                    psIns.setInt(1, id);
                    psIns.setInt(2, emp.id);
                    psIns.setDouble(3, antes);
                    psIns.setString(4, tipo);
                    psIns.setDouble(5, mov);
                    psIns.setDouble(6, despues);
                    psIns.setString(7, usuarioSesion);
                    psIns.executeUpdate();
                }
                con.commit();
                JOptionPane.showMessageDialog(this, "Movimientos aplicados correctamente.");
                limpiarTodos();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error en transacción: " + ex.getMessage());
        }
    }

    /**
     * Obtiene el código del empleado desde el campo (limpio).
     */
    private String obtenerCodigoEmpleado() {
        String campo = txtEmpleadoScan.getText().trim();
        if (!campo.contains("-")) {
            return campo.replaceAll("\\s", "");
        }
        return campo.split("-", 2)[0].replaceAll("\\s", "");
    }

    private static class EmpleadoItem {

        int id;
        String nombre, codigo;

        EmpleadoItem(int id, String nombre, String codigo) {
            this.id = id;
            this.nombre = nombre;
            this.codigo = codigo;
        }

        @Override
        public String toString() {
            return codigo + " - " + nombre;
        }
    }
}
