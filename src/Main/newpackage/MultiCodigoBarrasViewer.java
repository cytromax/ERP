package Main.newpackage;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MultiCodigoBarrasViewer extends JDialog {
    private final String nombre;
    private final String codigo;
    private static final int SCALE = 4;
    private final int labelWpx, labelHpx;
    private BufferedImage barcodeImage;

    public MultiCodigoBarrasViewer(Frame parent, String[] cods, String[] noms) {
        super(parent, "Etiqueta 4×2.5 cm", true);
        this.codigo = cods[0];
        this.nombre = noms[0];

        labelWpx = (int)(4.0 / 2.54 * 72) * SCALE;
        labelHpx = (int)(2.5 / 2.54 * 72) * SCALE;

        try {
            // El código de barras incluye SOLO el número
            barcodeImage = createBarcode(codigo, labelWpx, (int)(labelHpx * 0.6));
        } catch (WriterException ex) {
            JOptionPane.showMessageDialog(this, "Error generando código: " + ex.getMessage());
            barcodeImage = new BufferedImage(labelWpx, (int)(labelHpx * 0.6), BufferedImage.TYPE_INT_RGB);
        }
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Mini-preview reescalada (solo para mostrar, NO para exportar)
        BufferedImage preview = renderEtiqueta();
        Image previewImg = preview.getScaledInstance(labelWpx/SCALE, labelHpx/SCALE, Image.SCALE_SMOOTH);
        panel.add(new JLabel(new ImageIcon(previewImg)));

        // Botonera
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        btns.setBackground(Color.WHITE);
        JButton bPrint = new JButton("Imprimir");
        JButton bSave  = new JButton("Guardar PNG");
        JButton bClose = new JButton("Cerrar");
        bPrint.addActionListener(e -> doPrint());
        bSave .addActionListener(e -> doSave());
        bClose.addActionListener(e -> dispose());
        btns.add(bPrint);
        btns.add(bSave);
        btns.add(bClose);
        panel.add(Box.createVerticalStrut(5));
        panel.add(btns);

        setContentPane(panel);
        pack();
        setResizable(false);
        setLocationRelativeTo(getParent());
    }

    // --- Render Etiqueta ---
    private BufferedImage renderEtiqueta() {
        BufferedImage img = new BufferedImage(labelWpx, labelHpx, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, labelWpx, labelHpx);

        // Nombre (arriba)
        int nameH = labelHpx / 6;
        Font fNombre = new Font("Segoe UI", Font.BOLD, (int)(nameH*0.65));
        g2.setColor(Color.BLACK);
        g2.setFont(fNombre);
        FontMetrics fmN = g2.getFontMetrics();
        int nameW = fmN.stringWidth(nombre);
        g2.drawString(nombre, (labelWpx - nameW) / 2, fmN.getAscent() + (int)(4 * SCALE));

        // Código de barras (centro)
        int barY = nameH + SCALE * 2;
        int barH = labelHpx * 3 / 6;
        g2.drawImage(barcodeImage, 0, barY, labelWpx, barH, null);

        // Solo el número (abajo)
        int codeH = labelHpx - (barY + barH) - 8 * SCALE;
        Font fCode = new Font("Consolas", Font.PLAIN, (int)(codeH*0.8));
        g2.setFont(fCode);
        FontMetrics fmC = g2.getFontMetrics();
        int codeW = fmC.stringWidth(codigo);
        g2.drawString(codigo, (labelWpx - codeW) / 2, barY + barH + fmC.getAscent() + (int)(2 * SCALE));

        g2.dispose();
        return img;
    }

    // --- Generador de Código de Barras ---
    private static BufferedImage createBarcode(String txt, int width, int height) throws WriterException {
        Map<EncodeHintType,Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 10 * SCALE);
        BitMatrix m = new com.google.zxing.oned.Code128Writer()
            .encode(txt, BarcodeFormat.CODE_128, width, height, hints);
        return MatrixToImageWriter.toBufferedImage(m);
    }

    // --- Impresión centrada en la hoja ---
    private void doPrint() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Etiqueta 4×2.5 cm");
        job.setPrintable((Graphics gr, PageFormat pf, int page) -> {
            if (page > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) gr;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Tamaño de etiqueta (a 1x)
            int etqW = labelWpx / SCALE;
            int etqH = labelHpx / SCALE;

            // Área imprimible de la hoja
            double imgX = pf.getImageableX();
            double imgY = pf.getImageableY();
            double areaW = pf.getImageableWidth();
            double areaH = pf.getImageableHeight();

            // Centrar la etiqueta
            int posX = (int)(imgX + (areaW - etqW) / 2);
            int posY = (int)(imgY + (areaH - etqH) / 2);

            BufferedImage etiqueta = renderEtiqueta();
            g2.drawImage(etiqueta, posX, posY, etqW, etqH, null);
            return Printable.PAGE_EXISTS;
        });
        try {
            if (job.printDialog()) job.print();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error imprimir: " + ex.getMessage());
        }
    }

    // --- Guardar etiqueta como PNG ---
    private void doSave() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar etiqueta como PNG");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".png")) f = new File(f.getAbsolutePath() + ".png");
        try {
            BufferedImage out = renderEtiqueta();
            ImageIO.write(out, "PNG", f);
            JOptionPane.showMessageDialog(this, "PNG guardado correctamente.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error guardar PNG: " + ex.getMessage());
        }
    }
}
