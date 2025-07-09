package Main.newpackage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class PanelAlmacen extends JPanel {
    private final String usuario;
    private final String rol;

    public PanelAlmacen(String usuario, String rol) {
        this.usuario = usuario;
        this.rol = rol;

        // Configuración principal
        setBackground(new Color(28, 32, 36));
        setLayout(new BorderLayout());

        // --- Header con logo recoloreado y escalado ---
        JPanel panelHeader = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelHeader.setBackground(new Color(28, 32, 36));
        panelHeader.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        URL logoUrl = getClass().getResource("/images/viveza-textil-logo.png");
        if (logoUrl != null) {
            try {
                BufferedImage img = ImageIO.read(logoUrl);
                int w = img.getWidth(), h = img.getHeight();
                
                // Umbral alto para asegurar letras en blanco
                double threshold = 200.0;
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int pixel = img.getRGB(x, y);
                        int alpha = (pixel >> 24) & 0xFF;
                        // Solo recolorea si es visible
                        if (alpha > 0) {
                            int r = (pixel >> 16) & 0xFF;
                            int g = (pixel >> 8) & 0xFF;
                            int b = pixel & 0xFF;
                            double lum = 0.2126*r + 0.7152*g + 0.0722*b;
                            if (lum < threshold) {
                                // pinta de blanco, conserva alfa
                                img.setRGB(x, y, (alpha << 24) | 0x00FFFFFF);
                            }
                        }
                    }
                }
                // Escalado proporcional: ancho máx 200, alto máx 60
                double scale = Math.min(200.0 / w, 60.0 / h);
                int newW = (int) (w * scale), newH = (int) (h * scale);
                Image scaled = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
                JLabel lblLogo = new JLabel(new ImageIcon(scaled));
                panelHeader.add(lblLogo);
            } catch (IOException e) {
                System.err.println("Error al procesar el logo: " + e.getMessage());
            }
        } else {
            System.err.println("Logo no encontrado en /images/viveza-textil-logo.png");
        }
        add(panelHeader, BorderLayout.NORTH);

        // --- Panel de botones central ---
        JPanel panelCentral = new JPanel();
        panelCentral.setOpaque(false);
        panelCentral.setLayout(new BoxLayout(panelCentral, BoxLayout.Y_AXIS));
        panelCentral.setBorder(BorderFactory.createEmptyBorder(20, 60, 20, 60));

        Font font = new Font("Segoe UI", Font.BOLD, 18);
        int espaciado = 24;
        panelCentral.add(Box.createVerticalStrut(espaciado * 2));

        if (rol.equalsIgnoreCase("administrador")) {
            panelCentral.add(crearBotonOscuro("Productos", font,
                e -> new VerProductos(usuario, rol).setVisible(true)));
            panelCentral.add(Box.createVerticalStrut(espaciado));
            panelCentral.add(crearBotonOscuro("Administración de Empleados", font,
                e -> new GestionUsuarios(rol).setVisible(true)));
            panelCentral.add(Box.createVerticalStrut(espaciado));
            panelCentral.add(crearBotonOscuro("Alta de Usuario", font,
                e -> new AltaUsuarios().setVisible(true)));
            panelCentral.add(Box.createVerticalStrut(espaciado));
            panelCentral.add(crearBotonOscuro("Historial", font,
                e -> new HistorialGeneralMovimientos(rol).setVisible(true)));
            panelCentral.add(Box.createVerticalStrut(espaciado));
        } else {
            JLabel label = new JLabel("Sin permisos para ver productos.", SwingConstants.CENTER);
            label.setForeground(new Color(190, 70, 70));
            label.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            panelCentral.add(label);
            panelCentral.add(Box.createVerticalStrut(espaciado * 3));
        }

        JScrollPane scrollPanel = new JScrollPane(panelCentral);
        scrollPanel.setOpaque(false);
        scrollPanel.getViewport().setOpaque(false);
        scrollPanel.setBorder(BorderFactory.createEmptyBorder());
        scrollPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPanel, BorderLayout.CENTER);

        // --- Barra inferior con "Salir" ---
        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.setOpaque(false);
        JButton btnSalir = crearBotonOscuro("Salir", font, e -> salirAlSelector());
        btnSalir.setPreferredSize(new Dimension(130, 38));
        panelInferior.add(btnSalir, BorderLayout.WEST);
        panelInferior.setBorder(BorderFactory.createEmptyBorder(20, 30, 12, 20));
        add(panelInferior, BorderLayout.SOUTH);
    }

    private JButton crearBotonOscuro(String texto, Font font,
                                     java.awt.event.ActionListener onClick) {
        JButton boton = new JButton(texto);
        boton.setFont(font);
        boton.setFocusPainted(false);
        boton.setBackground(new Color(40, 115, 200));
        boton.setForeground(Color.WHITE);
        boton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 130, 220), 1, true),
            BorderFactory.createEmptyBorder(18, 22, 18, 22)
        ));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                boton.setBackground(new Color(24, 175, 255));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                boton.setBackground(new Color(40, 115, 200));
            }
        });
        boton.addActionListener(onClick);
        boton.setAlignmentX(Component.CENTER_ALIGNMENT);
        boton.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            boton.getPreferredSize().height));
        return boton;
    }

    private void salirAlSelector() {
        SwingUtilities.getWindowAncestor(this).dispose();
        new SelectorDeArea(usuario, rol).setVisible(true);
    }
}
