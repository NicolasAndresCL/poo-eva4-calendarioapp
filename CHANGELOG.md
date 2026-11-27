# Changelog

Todos los cambios relevantes de este proyecto se documentan aquí.
El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa
[Versionado Semántico](https://semver.org/lang/es/).

## [Sin publicar]

## [1.1.0] — 2026-11-27

### Añadido

- Entorno completo en Docker para el equipo: `Dockerfile` (escritorio Ubuntu en el navegador con
  NetBeans 31 y JDK 21) y `docker-compose.yml` con tres servicios: `netbeans` (http://localhost:3000),
  `mysql` (MariaDB 10.6 en la red de NetBeans, carga `sql/calendario_bd.sql` la primera vez) y
  `phpmyadmin` (http://localhost:8081). Basta `git clone` + `docker compose up -d --build`.
- `docs/guia_principiantes.md`: Git, GitHub, Docker y NetBeans paso a paso, para quien nunca los usó.
- `docs/ensayo_martes.md`: plan de ensayo de la defensa con banco de preguntas y ejercicios en vivo.
- CI: job `imagen` que valida `docker-compose.yml` y construye la imagen de NetBeans.

### Cambiado

- Defensas de Tais y Rodolfo revisadas línea por línea contra el código y ampliadas (guiones con sus
  palabras, preguntas trampa, modificaciones en vivo, lectura de errores, autoevaluación).
- Los comandos de la documentación usan los contenedores del repo (`calendario-*`).
- Profesor del ramo: Pablo (la pauta es de la profesora Rosa Rao, jefa de carrera).

### Corregido

- NetBeans 31 se descarga de `archive.apache.org` y se verifica su SHA-512: en `dlcdn.apache.org`
  ya da 404 (ahí solo queda la versión 32), así que una imagen nueva no se podía construir.
- CHANGELOG: la base se llama `calendario_app` (no `calendario_bd`).

## [1.0.0] — 2026-11-27

### Añadido

- Script `sql/calendario_bd.sql` (S1–S5): base `calendario_app`, tablas `usuario` y `evento`, columna
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
