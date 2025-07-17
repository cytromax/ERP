package Main.newpackage;

public enum RolUsuario {
    ADMINISTRADOR,
    TRABAJADOR,
    CONSULTOR_EXTERNO,
    COMPRAS; // NUEVO nombre

    public static RolUsuario fromString(String rol) {
        if (rol == null) return TRABAJADOR; // Por default
        switch (rol.trim().toLowerCase()) {
            case "administrador": return ADMINISTRADOR;
            case "trabajador": return TRABAJADOR;
            case "consultor_externo": return CONSULTOR_EXTERNO;
            case "compras": return COMPRAS;
            default: return TRABAJADOR;
        }
    }
}
