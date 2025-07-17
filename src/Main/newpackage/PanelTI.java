package Main.newpackage;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import Main.newpackage.RolUsuario;


public class PanelTI extends JPanel {
    private final RolUsuario rolEnum;
    private final String usuario;
    private RolUsuario rolActual;


    public PanelTI(Connection con, String usuario, RolUsuario rol) {
        this.usuario = usuario;
        this.rolEnum = rol;
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();

        // Ahora pasamos el RolUsuario tipado a los paneles internos:
        tabs.addTab("Tickets", new PanelTickets(con, usuario, rolEnum));
        tabs.addTab("Reportes PDF", new PanelReportesPDF(usuario, rolEnum));

        add(tabs, BorderLayout.CENTER);
    }
}
