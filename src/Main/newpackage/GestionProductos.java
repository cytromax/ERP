package Main.newpackage;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import conexion.ConexionDB;

public class GestionProductos extends JFrame {
    private static GestionProductos instanciaUnica = null;

    public static void mostrarVentana() {
        if (instanciaUnica == null || !instanciaUnica.isDisplayable()) {
            instanciaUnica = new GestionProductos();
        }
        instanciaUnica.setVisible(true);
        instanciaUnica.toFront();
        instanciaUnica.requestFocus();
    }

    private JTextField txtCodigo, txtModelo, txtCantidad, txtUnidad, txtUbicacion, txtMinima;
    private JComboBox<String> comboPrioridad;
    private static final String[] PRIORIDADES = {"priorizado", "mas_pedidos", "stock_bajo", "stock_comprar", "normal"};
    private JButton btnAgregar;

    public GestionProductos() {
        setTitle("Agregar Producto");
        setSize(400, 340);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridLayout(8, 2, 10, 10));

        txtCodigo = new JTextField();
        txtModelo = new JTextField();
        txtCantidad = new JTextField();
        txtUnidad = new JTextField();
        txtUbicacion = new JTextField();
        txtMinima = new JTextField();
        comboPrioridad = new JComboBox<>(PRIORIDADES);
        btnAgregar = new JButton("Agregar Producto");

        panel.add(new JLabel("Código de barras:"));
        panel.add(txtCodigo);
        panel.add(new JLabel("Descripción:"));
        panel.add(txtModelo);
        panel.add(new JLabel("Cantidad:"));
        panel.add(txtCantidad);
        panel.add(new JLabel("Unidad:"));
        panel.add(txtUnidad);
        panel.add(new JLabel("Ubicación:"));
        panel.add(txtUbicacion);
        panel.add(new JLabel("Existencia Mínima:"));
        panel.add(txtMinima);
        panel.add(new JLabel("Prioridad:"));
        panel.add(comboPrioridad);

        panel.add(new JLabel()); // Espacio vacío
        panel.add(btnAgregar);

        btnAgregar.addActionListener(e -> agregarProducto());

        add(panel, BorderLayout.CENTER);

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
        String ubicacion = txtUbicacion.getText().trim();
        String minimaStr = txtMinima.getText().trim();
        String prioridad = (String) comboPrioridad.getSelectedItem();

        if (codigo.isEmpty() || modelo.isEmpty() || cantidadStr.isEmpty() ||
            unidad.isEmpty() || ubicacion.isEmpty() || minimaStr.isEmpty() || prioridad.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, completa todos los campos.");
            return;
        }

        try {
            double cantidad = Double.parseDouble(cantidadStr);
            double minima = Double.parseDouble(minimaStr);

            Connection con = ConexionDB.conectar();
            if (con == null) {
                JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.");
                return;
            }

            String sql = "INSERT INTO productos (codigo_barras, modelo, existencia, unidad, ubicacion, existencia_minima, prioridad) VALUES (?, ?, ?, ?, ?, ?, ?::prioridad_tipo)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, codigo);
            ps.setString(2, modelo);
            ps.setDouble(3, cantidad);
            ps.setString(4, unidad);
            ps.setString(5, ubicacion);
            ps.setDouble(6, minima);
            ps.setString(7, prioridad);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Producto agregado exitosamente.");

            txtCodigo.setText("");
            txtModelo.setText("");
            txtCantidad.setText("");
            txtUnidad.setText("");
            txtUbicacion.setText("");
            txtMinima.setText("");
            comboPrioridad.setSelectedIndex(PRIORIDADES.length - 1);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "La cantidad y la existencia mínima deben ser números.");
        } catch (SQLException ex) {
            if (ex.getMessage().contains("duplicate key")) {
                JOptionPane.showMessageDialog(this, "El código de barras ya existe.");
            } else {
                JOptionPane.showMessageDialog(this, "Error al agregar producto: " + ex.getMessage());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error inesperado: " + ex.getMessage());
        }
    }
}
