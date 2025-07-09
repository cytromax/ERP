// src/Main/newpackage/GestionUsuarios.java
package Main.newpackage;

import conexion.ConexionDB;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class GestionUsuarios extends JFrame {
    private JTextField txtCodigo, txtNombre, txtDepartamento, txtPuesto;
    private JTable tabla;
    private DefaultTableModel modelo;
    private String rolActual;
    private JLabel lblTotalEmp, lblTotalBec;

    public GestionUsuarios(String rolActual) {
        this.rolActual = rolActual;
        setTitle("Gestión de Empleados y Becarios");
        setSize(900, 440);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
        cargarEmpleados();
    }

    private void initUI() {
        // —— Formulario —— 
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;

        txtCodigo       = new JTextField(12);
        txtNombre       = new JTextField(18);
        txtDepartamento = new JTextField(14);
        txtPuesto       = new JTextField(14);

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

        // combo Tipo
        c.gridx=0; c.gridy=y; form.add(new JLabel("Tipo:"), c);
        JComboBox<String> comboTipo = new JComboBox<>(new String[]{"empleado","becario"});
        c.gridx=1;           form.add(comboTipo, c);
        y++;

        // totales
        lblTotalEmp = new JLabel("Empleados: 0");
        lblTotalBec = new JLabel("Becarios: 0");
        JPanel pTot = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pTot.add(lblTotalEmp);
        pTot.add(Box.createHorizontalStrut(20));
        pTot.add(lblTotalBec);

        // botones CRUD + Imprimir
        JButton btnAgregar = new JButton("Agregar"),
                btnEditar  = new JButton("Editar"),
                btnEliminar= new JButton("Eliminar"),
                btnLimpiar = new JButton("Limpiar"),
                btnImprimir= new JButton("Imprimir Etiqueta");

        JPanel pBtn = new JPanel(new FlowLayout());
        pBtn.add(btnAgregar);
        pBtn.add(btnEditar);
        pBtn.add(btnEliminar);
        pBtn.add(btnLimpiar);
        pBtn.add(btnImprimir);

        // tabla
        modelo = new DefaultTableModel(
            new String[]{"ID","Código","Nombre","Depto","Puesto","Tipo"}, 0
        ) {
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        tabla = new JTable(modelo);
        tabla.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scroll = new JScrollPane(tabla);

        // permisos
        boolean admin = rolActual.equalsIgnoreCase("administrador");
        btnAgregar .setEnabled(admin);
        btnEditar  .setEnabled(admin);
        btnEliminar.setEnabled(admin);

        // listeners
        btnAgregar .addActionListener(e->agregar(comboTipo));
        btnEditar  .addActionListener(e->editar(comboTipo));
        btnEliminar.addActionListener(e->eliminar());
        btnLimpiar .addActionListener(e->limpiar(comboTipo));
        btnImprimir.addActionListener(e->imprimirSeleccionados());
        tabla.getSelectionModel().addListSelectionListener(e->llenarCampos(comboTipo));

        // layout
        JPanel top = new JPanel(new BorderLayout());
        top.add(pTot, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        top.add(pBtn, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    private void cargarEmpleados() {
        modelo.setRowCount(0);
        int cntE=0, cntB=0;
        try(Connection con=ConexionDB.conectar()){
            String sql="SELECT id,codigo_empleado,nombre,departamento,puesto,tipo "
                     + "FROM empleados ORDER BY nombre";
            try(var ps=con.prepareStatement(sql);
                var rs=ps.executeQuery()){
                while(rs.next()){
                    String tipo=rs.getString("tipo");
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
            lblTotalEmp.setText("Empleados: "+cntE);
            lblTotalBec.setText ("Becarios: "+cntB);
        } catch(Exception ex){
            JOptionPane.showMessageDialog(this,"Error al cargar: "+ex.getMessage());
        }
    }

    private void agregar(JComboBox<String> comboTipo){
        String cod = txtCodigo.getText().trim(),
               nom = txtNombre.getText().trim(),
               dep = txtDepartamento.getText().trim(),
               pue = txtPuesto.getText().trim(),
               tipo= (String)comboTipo.getSelectedItem();

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
                "INSERT INTO empleados(codigo_empleado,nombre,departamento,puesto,tipo) "
               + "VALUES(?,?,?,?,?)")){
                ps2.setString(1,cod);
                ps2.setString(2,nom);
                ps2.setString(3,dep);
                ps2.setString(4,pue);
                ps2.setString(5,tipo);
                ps2.executeUpdate();
            }
            limpiar(comboTipo);
            cargarEmpleados();
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

    private void editar(JComboBox<String> comboTipo){
        int f=tabla.getSelectedRow();
        if(f<0){ JOptionPane.showMessageDialog(this,"Selecciona uno."); return; }
        int id=(int)modelo.getValueAt(f,0);
        String cod = txtCodigo.getText().trim(),
               nom = txtNombre.getText().trim(),
               dep = txtDepartamento.getText().trim(),
               pue = txtPuesto.getText().trim(),
               tipo= (String)comboTipo.getSelectedItem();
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
                "UPDATE empleados SET codigo_empleado=?,nombre=?,departamento=?,puesto=?,tipo=? WHERE id=?"
            )){
                ps2.setString(1,cod);
                ps2.setString(2,nom);
                ps2.setString(3,dep);
                ps2.setString(4,pue);
                ps2.setString(5,tipo);
                ps2.setInt(6,id);
                ps2.executeUpdate();
            }
            limpiar(comboTipo);
            cargarEmpleados();
        }catch(Exception ex){
            JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage());
        }
    }

    private void eliminar(){
        int f=tabla.getSelectedRow();
        if(f<0){ JOptionPane.showMessageDialog(this,"Selecciona uno."); return; }
        int id=(int)modelo.getValueAt(f,0);
        if(JOptionPane.showConfirmDialog(this,"¿Eliminar?","Confirma",JOptionPane.YES_NO_OPTION)
           !=JOptionPane.YES_OPTION) return;
        try(Connection con=ConexionDB.conectar()){
            try(var ps=con.prepareStatement("DELETE FROM empleados WHERE id=?")){
                ps.setInt(1,id);
                ps.executeUpdate();
            }
            limpiar(null);
            cargarEmpleados();
        }catch(Exception ex){
            JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage());
        }
    }

    private void limpiar(JComboBox<String> comboTipo){
        txtCodigo.setText("");
        txtNombre.setText("");
        txtDepartamento.setText("");
        txtPuesto.setText("");
        if(comboTipo!=null) comboTipo.setSelectedIndex(0);
        tabla.clearSelection();
    }

    private void llenarCampos(JComboBox<String> comboTipo){
        int f = tabla.getSelectedRow();
        if(f<0) return;
        txtCodigo      .setText(modelo.getValueAt(f,1).toString());
        txtNombre      .setText(modelo.getValueAt(f,2).toString());
        txtDepartamento.setText(modelo.getValueAt(f,3).toString());
        txtPuesto      .setText(modelo.getValueAt(f,4).toString());
        if(comboTipo!=null)
            comboTipo.setSelectedItem(modelo.getValueAt(f,5).toString());
    }

    private void imprimirSeleccionados(){
        int[] filas = tabla.getSelectedRows();
        if(filas.length==0){
            JOptionPane.showMessageDialog(this,"Selecciona al menos uno."); return;
        }
        // tomamos solo el primero
        String[] cods    = { modelo.getValueAt(filas[0],1).toString() };
        String[] noms    = { modelo.getValueAt(filas[0],2).toString() };
        new MultiCodigoBarrasViewer(this, cods, noms).setVisible(true);
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(()-> new GestionUsuarios("administrador").setVisible(true));
    }
}
