package Main.newpackage;

import Main.newpackage.Components.HistorialMovimientosPanel;
import javax.swing.*;
import java.awt.*;

public class HistorialMovimientos extends JFrame {
    public HistorialMovimientos() {
        setTitle("Historial de Movimientos de Productos");
        setSize(1450, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel de pestañas
        JTabbedPane tabs = new JTabbedPane();

        // Cada tab usa el panel reutilizable y solo cambia el tipo
        tabs.addTab("Entradas", new HistorialMovimientosPanel("entrada"));
        tabs.addTab("Salidas",  new HistorialMovimientosPanel("salida"));
        tabs.addTab("Todos",    new HistorialMovimientosPanel("todos"));

        add(tabs, BorderLayout.CENTER);
    }
}
