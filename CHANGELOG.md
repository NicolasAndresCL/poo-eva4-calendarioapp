# Changelog

Todos los cambios relevantes de este proyecto se documentan aquí.
El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa
[Versionado Semántico](https://semver.org/lang/es/).

## [Sin publicar]

## [1.0.0] — 2026-11-27

### Añadido

- Script `sql/calendario_bd.sql` (S1–S5): base `calendario_bd`, tablas `usuario` y `evento`, columna
  `POINT` (ubicación) y columna `JSON` (detalle) con `CHECK (JSON_VALID(...))`, datos de prueba y
  consultas de verificación.
- Login con `UsuarioDAO` y `VentanaLogin`: validación de credenciales (L1), clave incorrecta (V1) y
  usuario inactivo (V2). La clave distingue mayúsculas (`COLLATE utf8mb4_bin`).
- CRUD de eventos (C1–C5) con `EventoDAO` y `VentanaEventos` (formulario + `JTable`), con
  `PreparedStatement` en toda sentencia.
- Manejo de errores: registro duplicado (V3, error 1062) y fallo de conexión (V4) traducidos por
  `Conexion.mensajeError()`; `try-with-resources` en todos los DAO.
- 24 pruebas JUnit 4 (modelo, conexión y DAO).
- Ventanas con archivos `.form` abribles en Design de NetBeans.
- Documentación (explicación línea a línea de SQL y Java, guía de defensa, bitácora) y presentación
  de la defensa.
- Integración continua en GitHub Actions: compila con Ant y corre las 24 pruebas contra MariaDB 10.6.
