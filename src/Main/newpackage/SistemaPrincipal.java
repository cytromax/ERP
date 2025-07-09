package Main.newpackage;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import login.LoginForm;
import conexion.ConexionDB;

public class SistemaPrincipal extends JFrame {
    private JPanel panelContenido;
    private CardLayout layoutCards;

    public SistemaPrincipal(String usuario, String rol, String areaInicial) {
        setTitle("Sistema - Área: " + areaInicial + " - " + usuario);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        layoutCards = new CardLayout();
        panelContenido = new JPanel(layoutCards);

        Connection con = null;
        if (areaInicial.equalsIgnoreCase("ti")) {
            con = ConexionDB.conectar();
        }

        panelContenido.add(new PanelAlmacen(usuario, rol), "almacen");

        if (con != null) {
            panelContenido.add(new PanelTI(con, usuario, rol), "ti");
        } else {
            JPanel panelError = new JPanel(new BorderLayout());
            JLabel lblError = new JLabel("No se pudo conectar a la base de datos TI", SwingConstants.CENTER);
            lblError.setForeground(Color.RED);
            panelError.add(lblError, BorderLayout.CENTER);
            panelContenido.add(panelError, "ti");
        }

        add(panelContenido, BorderLayout.CENTER);
        layoutCards.show(panelContenido, areaInicial);

        JButton btnCerrarSesion = new JButton("Cerrar sesión");
        btnCerrarSesion.addActionListener(e -> {
            dispose();
            new LoginForm().setVisible(true);
        });

        JPanel panelSalir = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelSalir.add(btnCerrarSesion);
        add(panelSalir, BorderLayout.SOUTH);
    }
}
