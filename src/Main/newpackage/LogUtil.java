package Main.newpackage;

import conexion.ConexionDB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

public class LogUtil {
    // Registra una acción en el log
    public static void registrarLog(String username, String accion, String descripcion, String resultado) {
        try (Connection con = ConexionDB.conectar()) {
            String sql = "INSERT INTO log_actividades (username, accion, descripcion, resultado, fecha) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, accion);
            ps.setString(3, descripcion);
            ps.setString(4, resultado);
            ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        } catch (Exception ex) {
            // Opcional: imprime en consola o ignora
            System.err.println("Error registrando log: " + ex.getMessage());
        }
    }
}
