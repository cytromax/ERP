package Main.newpackage;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.awt.print.*;

public class CodigoBarrasZXing extends JDialog {

    private BufferedImage barcodeImage;

    public CodigoBarrasZXing(Frame parent, String codigo, String titulo) {
        super(parent, "Código de Barras - " + titulo, true);
        setSize(540, 360);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // --- Panel principal ---
        JPanel content = new JPanel(new BorderLayout(14, 14));
        content.setBackground(new Color(32, 37, 45));
        content.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(40, 115, 200), 2, true),
            BorderFactory.createEmptyBorder(20, 28, 20, 28)
        ));

        // --- Etiqueta de título (empleado o producto) con JTextPane centrado ---
        JTextPane lblTitulo = new JTextPane();
        lblTitulo.setText(titulo);
        lblTitulo.setEditable(false);
        lblTitulo.setOpaque(false);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitulo.setForeground(new Color(91, 207, 247));
        lblTitulo.setFocusable(false);
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTitulo.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        StyledDocument doc = lblTitulo.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        content.add(lblTitulo, BorderLayout.NORTH);

        // --- Generar el código de barras ---
        try {
            barcodeImage = generarCodigoBarras(codigo);
        } catch (WriterException e) {
            JLabel error = new JLabel("Error generando código: " + e.getMessage(), SwingConstants.CENTER);
            error.setForeground(Color.RED);
            content.add(error, BorderLayout.CENTER);
        }

        // --- Panel con la imagen y el número ---
        if (barcodeImage != null) {
            JPanel panelImagen = new JPanel(new BorderLayout());
            panelImagen.setOpaque(false);

            JLabel lblImage = new JLabel(new ImageIcon(barcodeImage));
            lblImage.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            lblImage.setHorizontalAlignment(SwingConstants.CENTER);
            panelImagen.add(lblImage, BorderLayout.CENTER);

            JTextArea lblNumero = new JTextArea();
            lblNumero.setText(codigo);
            lblNumero.setLineWrap(true);
            lblNumero.setWrapStyleWord(true);
            lblNumero.setEditable(false);
            lblNumero.setOpaque(false);
            lblNumero.setFont(new Font("Consolas", Font.BOLD, 22));
            lblNumero.setForeground(new Color(200, 200, 200));
            lblNumero.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            lblNumero.setFocusable(false);
            lblNumero.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Centrar texto con StyledDocument
            StyledDocument docNum = new DefaultStyledDocument();
            SimpleAttributeSet centerNum = new SimpleAttributeSet();
            StyleConstants.setAlignment(centerNum, StyleConstants.ALIGN_CENTER);
            try {
                docNum.insertString(0, codigo, null);
                docNum.setParagraphAttributes(0, codigo.length(), centerNum, false);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
            lblNumero.setDocument(docNum);

            panelImagen.add(lblNumero, BorderLayout.SOUTH);

            content.add(panelImagen, BorderLayout.CENTER);
        }

        // --- Botonera ---
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        panelBotones.setOpaque(false);

        JButton btnGuardar = new JButton("Guardar PNG");
        JButton btnImprimir = new JButton("Imprimir");
        JButton btnCerrar = new JButton("Cerrar");

        btnGuardar.setBackground(new Color(35, 155, 245));
        btnGuardar.setForeground(Color.WHITE);
        btnImprimir.setBackground(new Color(44, 130, 201));
        btnImprimir.setForeground(Color.WHITE);
        btnCerrar.setBackground(new Color(66, 66, 66));
        btnCerrar.setForeground(Color.WHITE);

        btnGuardar.addActionListener(this::guardarPNG);
        btnImprimir.addActionListener(this::imprimir);
        btnCerrar.addActionListener(e -> dispose());

        panelBotones.add(btnGuardar);
        panelBotones.add(btnImprimir);
        panelBotones.add(btnCerrar);

        content.add(panelBotones, BorderLayout.SOUTH);

        setContentPane(content);
    }

    /** Genera un BufferedImage con el código de barras CODE_128 */
    public static BufferedImage generarCodigoBarras(String texto) throws WriterException {
        int width = 410, height = 110;
        BitMatrix bitMatrix = new com.google.zxing.oned.Code128Writer()
            .encode(texto, BarcodeFormat.CODE_128, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /** Guarda el BufferedImage actual como PNG */
    private void guardarPNG(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar como PNG");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".png")) {
                f = new File(f.getAbsolutePath() + ".png");
            }
            try {
                ImageIO.write(barcodeImage, "PNG", f);
                JOptionPane.showMessageDialog(this, "¡Guardado exitoso!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage());
            }
        }
    }

    /** Imprime el código de barras centrado en la hoja */
    private void imprimir(ActionEvent e) {
        if (barcodeImage == null) {
            JOptionPane.showMessageDialog(this, "Nada que imprimir.");
            return;
        }
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable((Graphics g, PageFormat pf, int page) -> {
                if (page > 0) return Printable.NO_SUCH_PAGE;

                // Convertir cm a pulgadas (1 pulgada = 2.54 cm)
                double widthInch = 4.0 / 2.54;
                double heightInch = 2.5 / 2.54;

                // Tamaño deseado en puntos (1 pulgada = 72 puntos)
                double printWidth = widthInch * 72;
                double printHeight = heightInch * 72;

                Graphics2D g2 = (Graphics2D) g;
                g2.translate(pf.getImageableX(), pf.getImageableY());

                // Escalar componente al tamaño deseado
                double scaleX = printWidth / barcodeImage.getWidth();
                double scaleY = printHeight / barcodeImage.getHeight();
                double scale = Math.min(scaleX, scaleY);
                g2.scale(scale, scale);

                g2.drawImage(barcodeImage, 0, 0, null);
                return Printable.PAGE_EXISTS;
            });
            if (job.printDialog()) job.print();
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Error al imprimir: " + ex.getMessage());
        }
    }
}
