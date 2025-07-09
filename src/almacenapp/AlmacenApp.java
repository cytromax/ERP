package almacenapp;

import login.LoginForm;
import com.formdev.flatlaf.FlatLightLaf;    // <--- IMPORTANTE
import javax.swing.UIManager;   
import com.formdev.flatlaf.FlatDarculaLaf;// <--- IMPORTANTE

public class AlmacenApp {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (Exception ex) {
            System.err.println("No se pudo cargar FlatLaf: " + ex.getMessage());
        }
        javax.swing.SwingUtilities.invokeLater(() -> {
            new LoginForm().setVisible(true);
        });
    }
}
