package login;

import Main.newpackage.SelectorDeArea;
import Main.newpackage.SistemaPrincipal;
import conexion.ConexionDB;
import org.mindrot.jbcrypt.BCrypt;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginForm extends JFrame {
    private JTextField txtUsuario;
    private JPasswordField txtPassword;

    public LoginForm() {
        setTitle("Iniciar Sesión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(28, 29, 33));

        JPanel loginPanel = new JPanel();
        loginPanel.setBackground(new Color(40, 42, 50));
        loginPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 180, 255), 2, true),
                BorderFactory.createEmptyBorder(20, 40, 20, 40)
        ));
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));

        // Carga y recolor del logo con umbral de luminancia
        JLabel lblLogo = new JLabel();
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        URL logoUrl = getClass().getResource("/images/viveza-textil-logo.png");
        if (logoUrl != null) {
            try {
                BufferedImage img = ImageIO.read(logoUrl);
                int w = img.getWidth(), h = img.getHeight();
                // Umbral de luminancia para considerar negro oscuro
                double threshold = 50.0;
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int pixel = img.getRGB(x, y);
                        int alpha = (pixel >> 24) & 0xFF;
                        int r = (pixel >> 16) & 0xFF;
                        int g = (pixel >> 8) & 0xFF;
                        int b = pixel & 0xFF;
                        // Luminancia perceptual
                        double lum = 0.2126*r + 0.7152*g + 0.0722*b;
                        if (lum < threshold) {
                            // Pinta de blanco manteniendo alfa
                            int white = (alpha << 24) | 0x00FFFFFF;
                            img.setRGB(x, y, white);
                        }
                    }
                }
                // Escalado proporcional
                int maxW = 180, maxH = 80;
                double scale = Math.min((double) maxW / w, (double) maxH / h);
                int newW = (int) (w * scale), newH = (int) (h * scale);
                Image scaled = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
                lblLogo.setIcon(new ImageIcon(scaled));
            } catch (IOException ioe) {
                System.err.println("Error leyendo logo: " + ioe.getMessage());
            }
        } else {
            System.err.println("Logo no encontrado en /images/viveza-textil-logo.png");
        }
        loginPanel.add(lblLogo);
        loginPanel.add(Box.createVerticalStrut(20));

        // Título
        JLabel lblTitulo = new JLabel("Bienvenido");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitulo.setForeground(new Color(90, 195, 255));
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginPanel.add(lblTitulo);
        loginPanel.add(Box.createVerticalStrut(30));

        // Usuario
        JLabel lblUsuario = new JLabel("Usuario");
        styleLabel(lblUsuario);
        loginPanel.add(lblUsuario);
        loginPanel.add(Box.createVerticalStrut(8));
        txtUsuario = (JTextField) createField("Ingresa tu usuario");
        loginPanel.add(txtUsuario);
        loginPanel.add(Box.createVerticalStrut(20));

        // Contraseña
        JLabel lblPass = new JLabel("Contraseña");
        styleLabel(lblPass);
        loginPanel.add(lblPass);
        loginPanel.add(Box.createVerticalStrut(8));
        txtPassword = (JPasswordField) createField("Ingresa tu contraseña");
        loginPanel.add(txtPassword);
        loginPanel.add(Box.createVerticalStrut(30));

        // Botón iniciar
        JButton btnLogin = new JButton("Iniciar Sesión");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnLogin.setBackground(new Color(24, 175, 255));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btnLogin.addActionListener(this::login);
        addHoverEffect(btnLogin);
        loginPanel.add(btnLogin);

        mainPanel.add(loginPanel);
        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null);
    }

    private void styleLabel(JLabel label) {
        label.setForeground(new Color(220, 220, 220));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
    }

    private JTextComponent createField(String placeholder) {
        JTextComponent field = placeholder.toLowerCase().contains("contraseña")
                ? new JPasswordField()
                : new JTextField();
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        field.setBackground(new Color(37, 38, 43));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 180, 255), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        setPlaceholder(field, placeholder);
        return field;
    }

    private void login(ActionEvent e) {
        String usuario = txtUsuario.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();
        if (usuario.equals("arsenal") && password.equals("admin")) {
            JOptionPane.showMessageDialog(this, "Usuario de emergencia");
            openNext("administrador");
            return;
        }
        try (Connection con = ConexionDB.conectar()) {
            PreparedStatement ps = con.prepareStatement(
                    "SELECT password, rol, activo FROM usuarios WHERE username = ?");
            ps.setString(1, usuario);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (!rs.getBoolean("activo")) {
                    JOptionPane.showMessageDialog(this, "Usuario inactivo.");
                    return;
                }
                if (BCrypt.checkpw(password, rs.getString("password"))) {
                    openNext(rs.getString("rol"));
                } else {
                    JOptionPane.showMessageDialog(this, "Contraseña incorrecta.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Usuario no encontrado.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de conexión: " + ex.getMessage());
        }
    }

    private void openNext(String rol) {
        dispose();
        if (rol.equalsIgnoreCase("administrador")) {
            new SelectorDeArea(txtUsuario.getText(), rol).setVisible(true);
        } else {
            new SistemaPrincipal(txtUsuario.getText(), rol, "almacen").setVisible(true);
        }
    }

    private void setPlaceholder(JTextComponent comp, String placeholder) {
        comp.setText(placeholder);
        comp.setForeground(new Color(130, 130, 130));
        if (comp instanceof JPasswordField) ((JPasswordField) comp).setEchoChar((char) 0);
        comp.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (comp.getText().equals(placeholder)) {
                    comp.setText("");
                    comp.setForeground(Color.WHITE);
                    if (comp instanceof JPasswordField)
                        ((JPasswordField) comp).setEchoChar('•');
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (comp.getText().isEmpty()) {
                    comp.setForeground(new Color(130, 130, 130));
                    comp.setText(placeholder);
                    if (comp instanceof JPasswordField)
                        ((JPasswordField) comp).setEchoChar((char) 0);
                }
            }
        });
    }

    private void addHoverEffect(JButton btn) {
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(50, 200, 255));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(24, 175, 255));
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
    }
}

/*
 * Coloca tu logo bajo:
 * src
 *  └─ images
 *       └─ viveza-textil-logo.png
 */
