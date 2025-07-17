package Main.newpackage;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

public class SelectorDeArea extends JFrame {
    public SelectorDeArea(String usuario, RolUsuario rol) {  // <--- Recibe el Enum, no String
        setTitle("Selecciona un área – " + usuario);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(new Color(32, 35, 40));
        setLayout(new BorderLayout(10, 10));

        // Header con logo
        JPanel panelHeader = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelHeader.setBackground(new Color(32, 35, 40));
        panelHeader.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        URL logoUrl = getClass().getResource("/images/viveza-textil-logo.png");
        if (logoUrl != null) {
            try {
                BufferedImage img = ImageIO.read(logoUrl);
                int w = img.getWidth(), h = img.getHeight();
                for (int y = 0; y < h; y++)
                    for (int x = 0; x < w; x++) {
                        int pixel = img.getRGB(x, y);
                        int alpha = (pixel >>> 24) & 0xFF;
                        if (alpha != 0)
                            img.setRGB(x, y, (alpha << 24) | 0x00FFFFFF);
                    }
                double scale = Math.min(150.0 / w, 50.0 / h);
                int newW = (int) (w * scale), newH = (int) (h * scale);
                Image scaled = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
                panelHeader.add(new JLabel(new ImageIcon(scaled)));
            } catch (IOException e) {
                System.err.println("Error procesando logo: " + e.getMessage());
            }
        }
        add(panelHeader, BorderLayout.NORTH);

        // Centro: texto y botones según permisos
        JPanel panelCenter = new JPanel();
        panelCenter.setBackground(new Color(32, 35, 40));
        panelCenter.setLayout(new BoxLayout(panelCenter, BoxLayout.Y_AXIS));
        panelCenter.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));

        JLabel lblTexto = new JLabel("Selecciona un área para continuar:", SwingConstants.CENTER);
        lblTexto.setForeground(Color.WHITE);
        lblTexto.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTexto.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelCenter.add(lblTexto);
        panelCenter.add(Box.createVerticalStrut(15));

        List<JButton> botonesVisibles = new ArrayList<>();

        // Área Almacén: visible para admin y trabajador
        if (Permisos.puedeEntrarAlmacen(rol)) {
            JButton btnAlmacen = new JButton("Almacén");
            btnAlmacen.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnAlmacen.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnAlmacen.addActionListener(e -> {
                dispose();
                new SistemaPrincipal(usuario, rol, "almacen").setVisible(true);  // <--- Pasa el Enum aquí también
            });
            panelCenter.add(btnAlmacen);
            botonesVisibles.add(btnAlmacen);
            panelCenter.add(Box.createVerticalStrut(10));
        }

        // Área TI: solo para admin
        if (Permisos.puedeEntrarTI(rol)) {
            JButton btnTI = new JButton("TI");
            btnTI.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnTI.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnTI.addActionListener(e -> {
                dispose();
                new SistemaPrincipal(usuario, rol, "ti").setVisible(true); // <--- Enum aquí también
            });
            panelCenter.add(btnTI);
            botonesVisibles.add(btnTI);
            panelCenter.add(Box.createVerticalStrut(10));
        }

        add(panelCenter, BorderLayout.CENTER);

        // Click automático si solo hay un área
        SwingUtilities.invokeLater(() -> {
            if (botonesVisibles.size() == 1) {
                botonesVisibles.get(0).doClick();
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
            new SelectorDeArea("usuarioEjemplo", RolUsuario.ADMINISTRADOR).setVisible(true)
        );
    }
}
