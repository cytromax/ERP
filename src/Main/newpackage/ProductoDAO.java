// Main.newpackage.ProductoDAO.java
package Main.newpackage;

import conexion.ConexionDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    public List<Producto> buscarProductos(String texto) {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT id, codigo_barras, modelo, existencia, unidad FROM productos WHERE codigo_barras LIKE ? OR modelo LIKE ?";
        try (Connection con = ConexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + texto + "%");
            ps.setString(2, "%" + texto + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new Producto(
                        rs.getInt("id"),
                        rs.getString("codigo_barras"),
                        rs.getString("modelo"),
                        rs.getDouble("existencia"),
                        rs.getString("unidad")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // Puedes agregar más métodos: agregar, editar, eliminar, etc.
}
