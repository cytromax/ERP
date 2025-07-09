// Main.newpackage.Producto.java
package Main.newpackage;

public class Producto {
    private int id;
    private String codigoBarras;
    private String modelo;
    private double existencia;
    private String unidad;

    // Constructor, getters y setters
    public Producto(int id, String codigoBarras, String modelo, double existencia, String unidad) {
        this.id = id;
        this.codigoBarras = codigoBarras;
        this.modelo = modelo;
        this.existencia = existencia;
        this.unidad = unidad;
    }
    // Getters y Setters (puedes generarlos con NetBeans)
    public int getId() { return id; }
    public String getCodigoBarras() { return codigoBarras; }
    public String getModelo() { return modelo; }
    public double getExistencia() { return existencia; }
    public String getUnidad() { return unidad; }

    public void setId(int id) { this.id = id; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public void setExistencia(double existencia) { this.existencia = existencia; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
}
