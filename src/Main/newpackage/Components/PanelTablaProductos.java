package Main.newpackage.Components;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Vector;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Map;
import java.util.ArrayList;
import Main.newpackage.RolUsuario;

public class PanelTablaProductos extends JPanel {
    private JTable tabla;
    private DefaultTableModel modelo;
    private RolUsuario rol;

    public PanelTablaProductos(RolUsuario rol, DefaultTableModel modelo, JTable tabla) {
        this.rol = rol;
        this.modelo = modelo;
        this.tabla = tabla;

        // Ordenar por prioridad (última columna asumida como prioridad)
        int columnaPrioridad = modelo.getColumnCount() - 1;
        ordenarPorPrioridad(modelo, columnaPrioridad);

        setLayout(new BorderLayout());
        setBackground(new Color(30, 33, 40));

        // Renderizador de color para la columna existencia (índice 2)
        tabla.getColumnModel().getColumn(2).setCellRenderer(new ColorCantidadRenderer());

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 32, 10, 32));
        add(scroll, BorderLayout.CENTER);
    }

    public JTable getTabla() {
        return tabla;
    }

    public DefaultTableModel getModelo() {
        return modelo;
    }

    public RolUsuario getRol() {
        return rol;
    }

    /**
     * Ordena las filas del DefaultTableModel según el valor de prioridad.
     * @param modelo DefaultTableModel a ordenar
     * @param columnaPrioridad Índice de la columna "prioridad" (empezando en 0)
     */
    public static void ordenarPorPrioridad(DefaultTableModel modelo, int columnaPrioridad) {
        Vector<?> dataVectorRaw = modelo.getDataVector(); // Vector "crudo"
        ArrayList<Vector<?>> filas = new ArrayList<>();
        for (Object filaObj : dataVectorRaw) {
            filas.add((Vector<?>) ((Vector<?>) filaObj).clone());
        }

        // Definir el orden de prioridad
        Map<String, Integer> ordenPrioridad = new HashMap<>();
        ordenPrioridad.put("priorizado", 1);
        ordenPrioridad.put("mas_pedidos", 2);
        ordenPrioridad.put("stock_bajo", 3);
        ordenPrioridad.put("stock_comprar", 4);
        ordenPrioridad.put("normal", 5);

        // Ordenar usando la prioridad
        filas.sort(Comparator.comparingInt(fila -> {
            Object valor = ((Vector<?>) fila).get(columnaPrioridad);
            String clave = (valor != null) ? valor.toString() : "normal";
            return ordenPrioridad.getOrDefault(clave, 999);
        }));

        // Limpiar y volver a agregar filas al modelo
        modelo.setRowCount(0);
        for (Vector<?> fila : filas) {
            modelo.addRow(((Vector<?>) fila).toArray());
        }
    }
}
