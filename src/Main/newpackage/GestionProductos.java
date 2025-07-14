package Main.newpackage;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import conexion.ConexionDB;

public class GestionProductos extends JFrame {
    // --- INICIO: Singleton para evitar varias ventanas ---
private static GestionProductos instanciaUnica = null;

public static void mostrarVentana() {
    if (instanciaUnica == null || !instanciaUnica.isDisplayable()) {
        instanciaUnica = new GestionProductos();
    }
    instanciaUnica.setVisible(true);
    instanciaUnica.toFront();
    instanciaUnica.requestFocus();
}
// --- FIN ---

    private JTextField txtCodigo, txtModelo, txtCantidad, txtUnidad;
    private JButton btnAgregar;

    public GestionProductos() {
        setTitle("Agregar Producto");
        setSize(400, 250);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));

        txtCodigo = new JTextField();
        txtModelo = new JTextField();
        txtCantidad = new JTextField();
        txtUnidad = new JTextField();
        btnAgregar = new JButton("Agregar Producto");

        panel.add(new JLabel("Código de barras:"));
        panel.add(txtCodigo);
        panel.add(new JLabel("Modelo:"));
        panel.add(txtModelo);
        panel.add(new JLabel("Cantidad:"));
        panel.add(txtCantidad);
        panel.add(new JLabel("Unidad:"));
        panel.add(txtUnidad);
        panel.add(new JLabel());
        panel.add(btnAgregar);

        btnAgregar.addActionListener(e -> agregarProducto());

        add(panel);
        JButton btnSalir = new JButton("Salir");
        btnSalir.addActionListener(e -> dispose());

        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelSur.add(btnSalir);
        add(panelSur, BorderLayout.SOUTH);
    }

    private void agregarProducto() {
        String codigo = txtCodigo.getText().trim();
        String modelo = txtModelo.getText().trim();
        String cantidadStr = txtCantidad.getText().trim();
        String unidad = txtUnidad.getText().trim();

        if (codigo.isEmpty() || modelo.isEmpty() || cantidadStr.isEmpty() || unidad.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, completa todos los campos obligatorios.");
            return;
        }

        try {
            // Si existencia es NUMERIC (decimal) en tu tabla, usa Double.parseDouble, si es INTEGER, usa Integer.parseInt
            double cantidad = Double.parseDouble(cantidadStr); // cambia a int si tu columna es INTEGER

            Connection con = ConexionDB.conectar();
            if (con == null) {
                JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.");
                return;
            }

            String sql = "INSERT INTO productos (codigo_barras, modelo, existencia, unidad) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, codigo);
            ps.setString(2, modelo);
            ps.setDouble(3, cantidad); // usa setInt si tu columna es INTEGER
            ps.setString(4, unidad);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Producto agregado exitosamente.");

            txtCodigo.setText("");
            txtModelo.setText("");
            txtCantidad.setText("");
            txtUnidad.setText("");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "La cantidad debe ser un número.");
        } catch (SQLException ex) {
            if (ex.getMessage().contains("duplicate key")) {
                JOptionPane.showMessageDialog(this, "El código de barras ya existe.");
            } else {
                JOptionPane.showMessageDialog(this, "Error al agregar producto: " + ex.getMessage());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al agregar producto: " + ex.getMessage());
        }
    }
}
