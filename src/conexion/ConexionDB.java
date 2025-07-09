package conexion;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {
    public static Connection conectar() {
        try {
            // 1. Cambia el driver a PostgreSQL
            Class.forName("org.postgresql.Driver");
            // 2. Cambia la URL de conexión para PostgreSQL
            return DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/almacend", // <- Cambia "almacend" si tu base tiene otro nombre
                "postgres",   // Usuario de PostgreSQL
                "luck0" // Tu contraseña
            );
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error de conexion: " + e.toString());
            return null;
        }
    }
}
