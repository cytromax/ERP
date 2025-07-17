package Main.newpackage;

public class SessionManager {
    private static String usuarioActual;
    private static String rolActual;

    public static void setUsuario(String usuario, String rol) {
        usuarioActual = usuario;
        rolActual = rol;
    }

    public static String getUsuario() {
        return usuarioActual;
    }

    public static String getRol() {
        return rolActual;
    }

    public static void cerrarSesion() {
        usuarioActual = null;
        rolActual = null;
    }
}
