package Main.newpackage.Components;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

public class EmpleadoScanField extends JPanel {
    private JTextField textField;
    private Timer debounceTimer;
    private final int DEBOUNCE_DELAY = 200; // ms
    private List<EmpleadoItem> empleados;

    public EmpleadoScanField() {
        setLayout(new BorderLayout());
        JLabel label = new JLabel("Empleado:");
        textField = new JTextField(20);
        add(label, BorderLayout.WEST);
        add(textField, BorderLayout.CENTER);

        // Debounce DocumentListener
        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { debounceAutocomplete(); }
            public void removeUpdate(DocumentEvent e) { debounceAutocomplete(); }
            public void changedUpdate(DocumentEvent e) { debounceAutocomplete(); }

            private void debounceAutocomplete() {
                if (debounceTimer != null && debounceTimer.isRunning())
                    debounceTimer.stop();
                debounceTimer = new Timer(DEBOUNCE_DELAY, ev -> autocompletar());
                debounceTimer.setRepeats(false);
                debounceTimer.start();
            }
        });
    }

    // Llama esto después de crear el componente y cargar empleados
    public void setEmpleados(List<EmpleadoItem> empleados) {
        this.empleados = empleados;
    }

    private void autocompletar() {
        if (empleados == null) return;
        String textoCampo = textField.getText().replaceAll("\\s", "");
        if (textoCampo.contains("-")) return;
        if (textoCampo.length() < 3) return;

        EmpleadoItem emp = null;
        for (EmpleadoItem it : empleados) {
            if (it.codigo.replaceAll("\\s", "").equalsIgnoreCase(textoCampo)) {
                emp = it;
                break;
            }
        }
        final EmpleadoItem empleadoSeleccionado = emp;
        if (empleadoSeleccionado != null) {
            SwingUtilities.invokeLater(() -> {
                textField.setText(empleadoSeleccionado.codigo + " - " + empleadoSeleccionado.nombre);
                textField.selectAll();
            });
        }
    }

    // Devuelve el código de empleado limpio
    public String getCodigoEmpleado() {
        String campo = textField.getText().trim();
        if (!campo.contains("-")) return campo.replaceAll("\\s", "");
        return campo.split("-", 2)[0].replaceAll("\\s", "");
    }

    // Devuelve el objeto EmpleadoItem seleccionado, o null si no hay match
    public EmpleadoItem getEmpleadoSeleccionado() {
        String codigo = getCodigoEmpleado();
        if (empleados == null) return null;
        for (EmpleadoItem it : empleados) {
            if (it.codigo.replaceAll("\\s", "").equalsIgnoreCase(codigo)) {
                return it;
            }
        }
        return null;
    }

    // Si quieres exponer el campo para limpiar, por ejemplo:
    public void limpiar() {
        textField.setText("");
    }

    // Puedes agregar un método para setear texto directo (útil si quieres resetear)
    public void setText(String text) {
        textField.setText(text);
    }
    
    public String getText() {
        return textField.getText();
    }

    // Clase para EmpleadoItem
    public static class EmpleadoItem {
        public int id;
        public String nombre, codigo;
        public EmpleadoItem(int id, String nombre, String codigo) {
            this.id = id; this.nombre = nombre; this.codigo = codigo;
        }
        @Override
        public String toString() { return codigo + " - " + nombre; }
    }
}
