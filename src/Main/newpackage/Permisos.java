package Main.newpackage;

/**
 * Permisos del sistema según el RolUsuario.
 * Define qué acciones puede realizar cada rol. Si agregas roles nuevos, no olvides añadirlos aquí.
 */
public class Permisos {

    // --- Permisos Generales ---
    /** Cualquier usuario puede ver productos (incluye consultor externo y compras). */
    public static boolean puedeVerProductos(RolUsuario rol) {
        return true;
    }

    /** Consultor externo solo puede consultar productos (bloquea acciones). */
    public static boolean esSoloConsulta(RolUsuario rol) {
        return rol == RolUsuario.CONSULTOR_EXTERNO;
    }

    // --- Permisos sobre productos ---
    /** Solo administrador puede editar productos. */
    public static boolean puedeEditarProductos(RolUsuario rol) {
        return rol == RolUsuario.ADMINISTRADOR;
    }
    /** Solo administrador puede eliminar productos. */
    public static boolean puedeEliminarProductos(RolUsuario rol) {
        return rol == RolUsuario.ADMINISTRADOR;
    }

    // --- Permisos para solicitudes de productos ---
    /** Almacén (trabajador) puede solicitar productos. */
    public static boolean puedeSolicitarProductos(RolUsuario rol) {
        return rol == RolUsuario.TRABAJADOR;
    }

    /** Compras puede revisar y aprobar/rechazar solicitudes de productos. */
    public static boolean puedeRevisarSolicitudes(RolUsuario rol) {
        return rol == RolUsuario.COMPRAS;
    }

    /** Compras y Administrador pueden exportar las solicitudes de productos. */
    public static boolean puedeExportarSolicitudes(RolUsuario rol) {
        return rol == RolUsuario.COMPRAS || rol == RolUsuario.ADMINISTRADOR;
    }

    /** Compras y Administrador pueden ver productos priorizados. */
    public static boolean puedeVerPrioridad(RolUsuario rol) {
        return rol == RolUsuario.COMPRAS || rol == RolUsuario.ADMINISTRADOR;
    }

    // --- Permisos de acceso a módulos o áreas ---
    /** Solo admin y trabajador pueden entrar al almacén (Compras NO puede). */
    public static boolean puedeEntrarAlmacen(RolUsuario rol) {
        return rol == RolUsuario.ADMINISTRADOR || rol == RolUsuario.TRABAJADOR;
    }
    /** Solo administrador puede acceder a TI. */
    public static boolean puedeEntrarTI(RolUsuario rol) {
        return rol == RolUsuario.ADMINISTRADOR;
    }

    // --- Permisos de acciones de inventario ---
    /** Solo admin puede hacer ajustes críticos de inventario. */
    public static boolean puedeHacerAjustes(RolUsuario rol) {
        return rol == RolUsuario.ADMINISTRADOR;
    }
    /** Solo admin y trabajador pueden escanear productos. */
    public static boolean puedeEscanearProductos(RolUsuario rol) {
        return rol == RolUsuario.ADMINISTRADOR || rol == RolUsuario.TRABAJADOR;
    }

    // --- Acciones de trabajador sobre productos ---
    /** Solo trabajador puede imprimir etiquetas. */
    public static boolean puedeImprimirEtiquetas(RolUsuario rol) {
        return rol == RolUsuario.TRABAJADOR;
    }
    /** Solo trabajador puede entregar productos. */
    public static boolean puedeEntregarProductos(RolUsuario rol) {
        return rol == RolUsuario.TRABAJADOR;
    }
    /** Solo trabajador puede recibir productos. */
    public static boolean puedeRecibirProductos(RolUsuario rol) {
        return rol == RolUsuario.TRABAJADOR;
    }
    /** Solo trabajador puede ver gráficas. */
    public static boolean puedeVerGraficas(RolUsuario rol) {
        return rol == RolUsuario.TRABAJADOR;
    }
    /** Solo trabajador puede imprimir códigos de barras. */
    public static boolean puedeImprimirCodigosBarras(RolUsuario rol) {
        return rol == RolUsuario.TRABAJADOR;
    }

    // --- Puedes agregar más métodos para nuevos permisos/roles aquí ---
}
