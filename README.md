# AlmacenApp

Gestor de inventario de equipos y accesorios de TI.

## 📦 Descripción

**AlmacenApp** es una aplicación de escritorio desarrollada en Java y NetBeans para la gestión de equipos, accesorios y componentes de TI en una empresa u organización. Permite llevar el control de inventario, historial de movimientos, usuarios con diferentes roles, y reportes exportables.

## 🚀 Características principales

* Registro, consulta y edición de equipos y accesorios
* Control de stock y notificaciones por bajo inventario
* Historial de movimientos (entradas y salidas)
* Usuarios con roles (administrador, editor, lector)
* Exportación de reportes a Excel y PDF
* Autenticación de usuarios y control de acceso
* Soporte para adjuntar fotos a los productos
* Filtros avanzados y buscador en las tablas

## 📂 Estructura del proyecto

```
AlmacenApp/
├── src/                   # Código fuente Java
├── dist/                  # Archivos compilados (binarios)
├── docs/                  # Documentación técnica y diagramas
├── lib/                   # Librerías externas (.jar)
├── README.md              # Este archivo
├── .gitignore             # Archivos a ignorar por Git
└── build.xml              # Script de construcción (NetBeans)
```

## 🛠️ Requisitos

* **Java 11** o superior
* **NetBeans 12** o superior
* **PostgreSQL 12** o superior

## ⚡ Instalación y ejecución

1. **Clona este repositorio**

   ```bash
   git clone https://github.com/cytromax/AlmacenApp.git
   ```
2. **Abre el proyecto en NetBeans**

   * Menú: File > Open Project > Selecciona la carpeta `AlmacenApp`
3. **Configura la base de datos**

   * Crea la base de datos y las tablas usando el script proporcionado en `/docs`
   * Actualiza la configuración de conexión en el archivo `config.properties`
4. **Compila y ejecuta**

   * Usa el botón “Run” en NetBeans para iniciar la aplicación

## 👤 Roles de usuario

* **Administrador:** Control total, puede gestionar usuarios y ver todo el historial
* **Editor:** Puede registrar, editar o eliminar productos
* **Lector:** Solo consulta información, sin permisos de edición

## 📈 Funcionalidades avanzadas

* Soporte para exportar reportes en PDF y Excel
* Búsqueda y filtrado por departamento, estatus, ubicación, etc.
* Indicadores visuales de stock (colores)
* Gestión de turnos, ubicaciones y responsables de los equipos

## ❓ FAQ / Preguntas frecuentes

**¿Cómo cambio la configuración de la base de datos?**
Edita el archivo `config.properties` en la raíz del proyecto con tus credenciales de conexión.

**¿Dónde están los scripts de la base de datos?**
En la carpeta `/docs` encontrarás los scripts `.sql` para crear la estructura y datos de ejemplo.

## 🤝 Contribuir

1. Haz un fork del proyecto
2. Crea una rama (`git checkout -b nueva-funcionalidad`)
3. Realiza tus cambios y haz commit (`git commit -am 'Agrego nueva funcionalidad'`)
4. Haz push a tu rama (`git push origin nueva-funcionalidad`)
5. Abre un Pull Request

## 📄 Licencia

MIT

---

Desarrollado por Cytromax & Equipo 🚀
