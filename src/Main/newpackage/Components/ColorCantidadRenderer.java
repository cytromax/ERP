package Main.newpackage.Components;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class ColorCantidadRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        try {
            double existencia = Double.parseDouble(value.toString());

            // Obtener existencia mínima desde la columna correspondiente
            double existenciaMinima;
            try {
                // ⚠️ Ajusta el índice si la columna cambia de posición
                existenciaMinima = Double.parseDouble(table.getValueAt(row, 6).toString()); // columna 6: existencia mínima
            } catch (Exception ex) {
                existenciaMinima = 10; // Valor por defecto si no está en tabla
            }

            if (existencia < 0) {
                c.setBackground(new Color(138, 43, 226)); // 🟪 Morado (negativo)
                c.setForeground(Color.WHITE);
            } else if (existencia == 0) {
                c.setBackground(new Color(235, 87, 87)); // 🟥 Rojo
                c.setForeground(Color.WHITE);
            } else if (existencia <= existenciaMinima) {
                c.setBackground(new Color(240, 220, 80)); // 🟨 Amarillo
                c.setForeground(Color.BLACK);
            } else {
                c.setBackground(new Color(65, 180, 90)); // 🟩 Verde
                c.setForeground(Color.BLACK);
            }

        } catch (Exception ex) {
            c.setBackground(Color.LIGHT_GRAY);
            c.setForeground(Color.BLACK);
        }

        return c;
    }
}
