package Main.newpackage;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

public class PanelReportesPDF extends JPanel {
    private JTable tabla;
    private DefaultTableModel modelo;

    private String usuario;
    private RolUsuario rol; // ← Cambiado a Enum

    public PanelReportesPDF(String usuario, RolUsuario rol) { // ← Enum aquí
        this.usuario = usuario;
        this.rol = rol;

        setLayout(new BorderLayout());
        setBackground(new Color(32, 34, 37));

        JLabel titulo = new JLabel("Reportes PDF de TI", SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titulo.setForeground(new Color(90, 195, 255));
        titulo.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));

        JButton btnAgregarPDF = new JButton("Agregar PDF");
        btnAgregarPDF.setFocusPainted(false);
        btnAgregarPDF.setBackground(new Color(50, 150, 200));
        btnAgregarPDF.setForeground(Color.WHITE);
        btnAgregarPDF.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnAgregarPDF.addActionListener(e -> agregarPDF());

        // Ejemplo de permisos: solo admin puede agregar PDF (ajústalo a tus reglas)
        btnAgregarPDF.setEnabled(rol == RolUsuario.ADMINISTRADOR);

        JPanel panelTop = new JPanel(new BorderLayout());
        panelTop.setOpaque(false);
        panelTop.add(titulo, BorderLayout.CENTER);
        panelTop.add(btnAgregarPDF, BorderLayout.EAST);
        add(panelTop, BorderLayout.NORTH);

        modelo = new DefaultTableModel(new String[]{"Archivo", "Abrir"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tabla = new JTable(modelo);
        tabla.setRowHeight(32);
        tabla.setBackground(new Color(44, 45, 52));
        tabla.setForeground(Color.WHITE);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        cargarPDFs();

        JScrollPane scroll = new JScrollPane(tabla);
        add(scroll, BorderLayout.CENTER);

        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int fila = tabla.getSelectedRow();
                    if (fila != -1) {
                        abrirPDF((String) modelo.getValueAt(fila, 0));
                    }
                }
            }
        });
    }

    private void cargarPDFs() {
        modelo.setRowCount(0);
        File carpeta = new File("./reportes_ti");
        if (!carpeta.exists()) carpeta.mkdirs();
        File[] archivos = carpeta.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
        if (archivos != null) {
            for (File archivo : archivos) {
                modelo.addRow(new Object[]{archivo.getName(), "Abrir"});
            }
        }
    }

    private void abrirPDF(String nombreArchivo) {
        File archivo = new File("./reportes_ti/" + nombreArchivo);
        if (archivo.exists()) {
            try {
                Desktop.getDesktop().open(archivo);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "No se pudo abrir el archivo: " + ex.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, "El archivo no existe.");
        }
    }

    private void agregarPDF() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Selecciona un PDF para agregar");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos PDF", "pdf"));
        int res = fileChooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File seleccionado = fileChooser.getSelectedFile();
            File destino = new File("./reportes_ti/" + seleccionado.getName());
            if (destino.exists()) {
                JOptionPane.showMessageDialog(this, "Ese PDF ya existe en el sistema.");
                return;
            }
            try {
                java.nio.file.Files.copy(seleccionado.toPath(), destino.toPath());
                JOptionPane.showMessageDialog(this, "PDF agregado correctamente.");
                cargarPDFs(); // Recarga la tabla
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al copiar PDF: " + ex.getMessage());
            }
        }
    }
}
