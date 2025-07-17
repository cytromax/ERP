package Main.newpackage;

import conexion.ConexionDB;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class GestionUsuarios extends JFrame {
    private static GestionUsuarios instanciaUnica = null;
    private JTextField txtCodigo, txtNombre, txtDepartamento, txtPuesto, txtBuscar;
    private JTable tabla;
    private DefaultTableModel modelo;
    private String rolActual;
    private JLabel lblTotalEmp, lblTotalBec;
    private JComboBox<String> comboTipo;

    private GestionUsuarios(String rolActual) {
        this.rolActual = rolActual;
        setTitle("Gestión de Empleados y Becarios");
        setSize(950, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
        cargarEmpleados("");
    }

    private void initUI() {
        // FORMULARIO arriba
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;

        txtCodigo       = new JTextField(10);
        txtNombre       = new JTextField(16);
        txtDepartamento = new JTextField(12);
        txtPuesto       = new JTextField(12);

        int y=0;
        c.gridx=0; c.gridy=y; form.add(new JLabel("Código:"), c);
        c.gridx=1;           form.add(txtCodigo, c);
        y++;
        c.gridx=0; c.gridy=y; form.add(new JLabel("Nombre:"), c);
        c.gridx=1;           form.add(txtNombre, c);
        y++;
        c.gridx=0; c.gridy=y; form.add(new JLabel("Depto:"), c);
        c.gridx=1;           form.add(txtDepartamento, c);
        y++;
        c.gridx=0; c.gridy=y; form.add(new JLabel("Puesto:"), c);
        c.gridx=1;           form.add(txtPuesto, c);
        y++;
        c.gridx=0; c.gridy=y; form.add(new JLabel("Tipo:"), c);
        comboTipo = new JComboBox<>(new String[]{"empleado", "becario"});
        c.gridx=1;           form.add(comboTipo, c);

        // TOTALES
        lblTotalEmp = new JLabel("Empleados: 0");
        lblTotalBec = new JLabel("Becarios: 0");
        JPanel pTot = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        pTot.add(lblTotalEmp); pTot.add(lblTotalBec);

        // BUSCADOR
        JPanel pBuscar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        txtBuscar = new JTextField(30);
        pBuscar.add(new JLabel("🔎 Buscar (nombre/código):")); pBuscar.add(txtBuscar);

        // BOTONES CRUD
        JButton btnAgregar  = new JButton("Agregar");
        JButton btnEditar   = new JButton(" Guardar");
        JButton btnEliminar = new JButton("Eliminar");
        JButton btnLimpiar  = new JButton("Limpiar");
        JButton btnImprimir = new JButton("Imprimir Etiqueta");
        JButton btnSalir    = new JButton("Salir");

        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pBtn.add(btnAgregar); pBtn.add(btnEditar); pBtn.add(btnEliminar);
        pBtn.add(btnLimpiar); pBtn.add(btnImprimir); pBtn.add(btnSalir);

        // TABLA
        modelo = new DefaultTableModel(
            new String[]{"ID","Código","Nombre","Depto","Puesto","Tipo"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        tabla = new JTable(modelo);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(tabla);

        // LAYOUT PRINCIPAL
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(pTot);
        top.add(form);
        top.add(pBtn);

        JPanel centro = new JPanel();
        centro.setLayout(new BorderLayout(0,0));
        centro.add(pBuscar, BorderLayout.NORTH);
        centro.add(scroll, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);

        // PERMISOS
        boolean admin = rolActual.equalsIgnoreCase("administrador");
        btnAgregar.setEnabled(admin);
        btnEditar.setEnabled(admin);
        btnEliminar.setEnabled(admin);

        // LISTENERS
        btnAgregar.addActionListener(e -> agregar());
        btnEditar.addActionListener(e -> editar());
        btnEliminar.addActionListener(e -> eliminar());
        btnLimpiar.addActionListener(e -> limpiar());
        btnImprimir.addActionListener(e -> imprimirSeleccionado());
        btnSalir.addActionListener(e -> dispose());

        tabla.getSelectionModel().addListSelectionListener(e -> llenarCampos());
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { buscar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { buscar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { buscar(); }
        });

        // ESC para cerrar
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void buscar() {
        String texto = txtBuscar.getText().trim();
        cargarEmpleados(texto);
    }

    private void cargarEmpleados(String filtro) {
        modelo.setRowCount(0);
        int cntE=0, cntB=0;
        try(Connection con=ConexionDB.conectar()){
            String sql = "SELECT id,codigo_empleado,nombre,departamento,puesto,tipo FROM empleados "
                       + (filtro==null||filtro.isEmpty()?"":"WHERE codigo_empleado ILIKE ? OR nombre ILIKE ? ")
                       + "ORDER BY nombre";
            try(var ps=con.prepareStatement(sql)){
                if(filtro!=null && !filtro.isEmpty()){
                    String f="%"+filtro+"%";
                    ps.setString(1, f);
                    ps.setString(2, f);
                }
                try(var rs=ps.executeQuery()){
                    while(rs.next()){
                        String tipo = rs.getString("tipo");
                        modelo.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("codigo_empleado"),
                            rs.getString("nombre"),
                            rs.getString("departamento"),
                            rs.getString("puesto"),
                            tipo
                        });
                        if(tipo.equalsIgnoreCase("empleado")) cntE++;
                        else cntB++;
                    }
                }
            }
            lblTotalEmp.setText("Empleados: " + cntE);
            lblTotalBec.setText("Becarios: " + cntB);
        } catch(Exception ex){
            JOptionPane.showMessageDialog(this,"Error al cargar: "+ex.getMessage());
        }
    }

    // -------- CRUD --------
    private void agregar() {
        String cod = txtCodigo.getText().trim(),
               nom = txtNombre.getText().trim(),
               dep = txtDepartamento.getText().trim(),
               pue = txtPuesto.getText().trim(),
               tipo = (String) comboTipo.getSelectedItem();
        if(nom.isEmpty()||dep.isEmpty()||pue.isEmpty()){
            JOptionPane.showMessageDialog(this,"Completa todos los campos."); return;
        }
        if(cod.isEmpty()){
            cod = generarCodigo();
            txtCodigo.setText(cod);
        }
        try(Connection con=ConexionDB.conectar()){
            try(var ps1=con.prepareStatement(
                "SELECT 1 FROM empleados WHERE codigo_empleado=?")) {
                ps1.setString(1,cod);
                if(ps1.executeQuery().next()){
                    JOptionPane.showMessageDialog(this,"Código repetido."); return;
                }
            }
            try(var ps2=con.prepareStatement(
                "INSERT INTO empleados(codigo_empleado,nombre,departamento,puesto,tipo) VALUES(?,?,?,?,?)")){
                ps2.setString(1,cod);
                ps2.setString(2,nom);
                ps2.setString(3,dep);
                ps2.setString(4,pue);
                ps2.setString(5,tipo);
                ps2.executeUpdate();
            }
            limpiar();
            cargarEmpleados(txtBuscar.getText().trim());
        }catch(Exception ex){
            JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage());
        }
    }

    private String generarCodigo(){
        String base="EMP"; int n=1;
        try(Connection con=ConexionDB.conectar()){
            try(var ps=con.prepareStatement(
                "SELECT codigo_empleado FROM empleados WHERE codigo_empleado LIKE 'EMP%' "
               + "ORDER BY codigo_empleado DESC LIMIT 1");
                var rs=ps.executeQuery()){
                if(rs.next()){
                    n = Integer.parseInt(rs.getString(1).replace("EMP","")) + 1;
                }
            }
        }catch(Exception ignored){}
        return String.format("%s%04d",base,n);
    }

    private void editar() {
        int f = tabla.getSelectedRow();
        if(f < 0){ JOptionPane.showMessageDialog(this,"Selecciona uno."); return; }
        int id = (int) modelo.getValueAt(f,0);
        String cod = txtCodigo.getText().trim(),
               nom = txtNombre.getText().trim(),
               dep = txtDepartamento.getText().trim(),
               pue = txtPuesto.getText().trim(),
               tipo = (String) comboTipo.getSelectedItem();
        if(cod.isEmpty()||nom.isEmpty()||dep.isEmpty()||pue.isEmpty()){
            JOptionPane.showMessageDialog(this,"Completa todos los campos."); return;
        }
        try(Connection con=ConexionDB.conectar()){
            try(var ps1=con.prepareStatement(
                "SELECT 1 FROM empleados WHERE codigo_empleado=? AND id<>?")){
                ps1.setString(1,cod);
                ps1.setInt(2,id);
                if(ps1.executeQuery().next()){
                    JOptionPane.showMessageDialog(this,"Código repetido."); return;
                }
            }
            try(var ps2=con.prepareStatement(
                "UPDATE empleados SET codigo_empleado=?,nombre=?,departamento=?,puesto=?,tipo=? WHERE id=?")){
                ps2.setString(1,cod);
                ps2.setString(2,nom);
                ps2.setString(3,dep);
                ps2.setString(4,pue);
                ps2.setString(5,tipo);
                ps2.setInt(6,id);
                ps2.executeUpdate();
            }
            limpiar();
            cargarEmpleados(txtBuscar.getText().trim());
        }catch(Exception ex){
            JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage());
        }
    }

    // --- AJUSTE: Eliminar empleado también elimina usuario del sistema ---
    private void eliminar() {
        int f = tabla.getSelectedRow();
        if(f < 0){
            JOptionPane.showMessageDialog(this,"Selecciona uno."); return;
        }
        int id = (int) modelo.getValueAt(f,0);
        if(JOptionPane.showConfirmDialog(
                this,
                "¿Eliminar empleado?\nEsto también eliminará el usuario del sistema asociado (si existe).",
                "Confirma eliminación",
                JOptionPane.YES_NO_OPTION
            ) != JOptionPane.YES_OPTION) return;

        try(Connection con=ConexionDB.conectar()){
            con.setAutoCommit(false); // Para rollback en error

            // 1. Elimina usuario de sistema (si existe)
            try(var psUsuario = con.prepareStatement(
                "DELETE FROM usuarios WHERE empleado_id = ?"
            )) {
                psUsuario.setInt(1, id);
                psUsuario.executeUpdate();
            }

            // 2. Elimina empleado
            try(var ps = con.prepareStatement("DELETE FROM empleados WHERE id=?")){
                ps.setInt(1,id);
                ps.executeUpdate();
            }

            con.commit();
            limpiar();
            cargarEmpleados(txtBuscar.getText().trim());
        }catch(Exception ex){
            JOptionPane.showMessageDialog(this,"Error al eliminar: "+ex.getMessage());
        }
    }

    private void limpiar() {
        txtCodigo.setText("");
        txtNombre.setText("");
        txtDepartamento.setText("");
        txtPuesto.setText("");
        comboTipo.setSelectedIndex(0);
        tabla.clearSelection();
        txtBuscar.setText(""); // <-- Esto limpia el campo de búsqueda también
    }

    private void llenarCampos() {
        int f = tabla.getSelectedRow();
        if(f<0) return;
        txtCodigo      .setText(modelo.getValueAt(f,1).toString());
        txtNombre      .setText(modelo.getValueAt(f,2).toString());
        txtDepartamento.setText(modelo.getValueAt(f,3).toString());
        txtPuesto      .setText(modelo.getValueAt(f,4).toString());
        comboTipo      .setSelectedItem(modelo.getValueAt(f,5).toString());
    }

    private void imprimirSeleccionado() {
        int f = tabla.getSelectedRow();
        if(f<0){
            JOptionPane.showMessageDialog(this,"Selecciona uno."); return;
        }
        String[] cods = { modelo.getValueAt(f,1).toString() };
        String[] noms = { modelo.getValueAt(f,2).toString() };
        new MultiCodigoBarrasViewer(this, cods, noms).setVisible(true);
    }

    // Singleton launcher
    public static void mostrarVentana(String rolActual) {
        if (instanciaUnica == null || !instanciaUnica.isDisplayable()) {
            instanciaUnica = new GestionUsuarios(rolActual);
        }
        instanciaUnica.setVisible(true);
        instanciaUnica.toFront();
        instanciaUnica.requestFocus();
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(()-> new GestionUsuarios("administrador").setVisible(true));
    }
}
