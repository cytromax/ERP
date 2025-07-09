package Main.newpackage;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;

public class VistaCodigosBarras extends JFrame {
    public VistaCodigosBarras(String codigo, String modelo) {
        setTitle("Código de Barras");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.DARK_GRAY);

        try {
            BitMatrix matrix = new MultiFormatWriter().encode(codigo, BarcodeFormat.CODE_128, 300, 100);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);

            JLabel lblCodigo = new JLabel(codigo, SwingConstants.CENTER);
            lblCodigo.setForeground(Color.WHITE);
            lblCodigo.setAlignmentX(Component.CENTER_ALIGNMENT);
            lblCodigo.setFont(new Font("Segoe UI", Font.BOLD, 20));

            JLabel lblImagen = new JLabel(new ImageIcon(image));
            lblImagen.setAlignmentX(Component.CENTER_ALIGNMENT);

            // JTextArea para modelo, multilinea, sin guiones ni truncado
            JTextArea txtModelo = new JTextArea();
            txtModelo.setText(modelo);
            txtModelo.setLineWrap(true);
            txtModelo.setWrapStyleWord(true);
            txtModelo.setEditable(false);
            txtModelo.setOpaque(false);
            txtModelo.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            txtModelo.setForeground(Color.WHITE);
            txtModelo.setFocusable(false);
            txtModelo.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            txtModelo.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Centramos el texto
            StyledDocument doc = new DefaultStyledDocument();
            SimpleAttributeSet center = new SimpleAttributeSet();
            StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
            try {
                doc.insertString(0, modelo, null);
                doc.setParagraphAttributes(0, modelo.length(), center, false);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
            txtModelo.setDocument(doc);

            panel.add(lblCodigo);
            panel.add(lblImagen);
            panel.add(txtModelo);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al generar el código de barras: " + e.getMessage());
        }

        JButton btnImprimir = new JButton("Imprimir");
        btnImprimir.addActionListener(e -> printComponent(panel));

        add(panel, BorderLayout.CENTER);
        add(btnImprimir, BorderLayout.SOUTH);
    }

    private void printComponent(Component component) {
        PrinterJob pj = PrinterJob.getPrinterJob();
        pj.setJobName("Código de Barras");

        pj.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            // Convertir cm a pulgadas (1 pulgada = 2.54 cm)
            double widthInch = 4.0 / 2.54;
            double heightInch = 2.5 / 2.54;

            // Tamaño deseado en puntos (1 pulgada = 72 puntos)
            double printWidth = widthInch * 72;
            double printHeight = heightInch * 72;

            // Escalar componente al tamaño deseado
            double scaleX = printWidth / component.getWidth();
            double scaleY = printHeight / component.getHeight();
            double scale = Math.min(scaleX, scaleY);
            g2.scale(scale, scale);

            component.paint(g2);
            return Printable.PAGE_EXISTS;
        });

        if (pj.printDialog()) {
            try {
                pj.print();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Error al imprimir: " + e.getMessage());
            }
        }
    }
}
