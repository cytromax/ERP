package Main.newpackage;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;

public class PanelTI extends JPanel {
    public PanelTI(Connection con, String usuario, String rol) {
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();

        // Pasamos conexión, usuario y rol a PanelTickets para control de permisos y consultas
        tabs.addTab("Tickets", new PanelTickets(con, usuario, rol));

        // PanelReportesPDF solo necesita usuario y rol
        tabs.addTab("Reportes PDF", new PanelReportesPDF(usuario, rol));

        add(tabs, BorderLayout.CENTER);
    }
}
