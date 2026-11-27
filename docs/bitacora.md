# Bitácora del equipo — CalendarioApp

**Evaluación 4 (20 %) · Fundamentos de POO (IEI-085) · Universidad Santo Tomás**
Integrantes: Tais Montecinos · Rodolfo Prieto · Nicolás Cano

> **PROPUESTA A AJUSTAR POR EL EQUIPO.** Este reparto es un borrador armado a partir del código existente y de lo conversado. Antes de la defensa, cada integrante debe revisar sus filas, corregir fechas y horas reales, y agregar lo que realmente hizo. La rúbrica B3 pide «aportes evidenciados en la bitácora»: lo que no esté aquí no se puede demostrar.

## 1. Reparto de aportes

| Integrante | Responsabilidad principal | Entregables (códigos de la pauta) |
|---|---|---|
| **Tais Montecinos** | Script SQL y modelo de datos | S1 (crear base), S2 (`usuario`), S3 (`evento` con PK, UNIQUE, DEFAULT, CHECK, POINT y JSON), S4 (3 usuarios), S5 (eventos de prueba); clases `Usuario` y `Evento` del paquete `modelo` |
| **Rodolfo Prieto** | Conexión, autenticación y validaciones | Clase `Conexion` (A4); `UsuarioDAO` (`validar`, `estaInactivo`); L1; validaciones V1, V2 y V4 en `VentanaLogin` |
| **Nicolás Cano** | CRUD, tipos especiales y pruebas | `EventoDAO` (C1 a C5); V3 (código repetido, error 1062); manejo de POINT (`POINT(?, ?)`, `ST_X`/`ST_Y`) y JSON (`JSON_VALID`); 24 pruebas JUnit 4 (`CalendarioApp/test/`) |
| **Los tres** | Interfaz e integración | `VentanaLogin` y `VentanaEventos` (formulario + `JTable`); integración de capas; pruebas manuales; presentación y guía de defensa |

## 2. Registro de trabajo

> Completar con las fechas y horas reales. Las filas siguientes son una propuesta de cómo registrarlo.

| Fecha | Integrante | Actividad | Resultado / evidencia |
|---|---|---|---|
| _(fecha)_ | Todos | Elección del tema (agenda de eventos), lectura de la pauta y definición de campos | Tema aprobado: `usuario` + `evento` con POINT y JSON |
| _(fecha)_ | Tais | Diseño de la tabla `evento` (14 campos) y escritura de S1 a S3 | `sql/calendario_bd.sql` se ejecuta sin errores; `DESCRIBE` coincide con la pauta |
| _(fecha)_ | Tais | Inserción de 3 usuarios y 5 eventos de prueba (S4, S5); pruebas de UNIQUE, NOT NULL y CHECK | `SELECT` muestra los datos; errores 1062, 1364 y 4025 comprobados |
| _(fecha)_ | Tais | Clases `Usuario` y `Evento` (atributos `private`, constructores, getters/setters) | Paquete `modelo` |
| _(fecha)_ | Rodolfo | Integración del driver `mysql-connector-j 26.10.0` y clase `Conexion` | `probarConexion()` devuelve `true` |
| _(fecha)_ | Rodolfo | `UsuarioDAO.validar()` y `estaInactivo()` con `PreparedStatement` | Ingresan `admin` y `operador1`; `inactivo1` es rechazado |
| _(fecha)_ | Rodolfo | Validaciones V1 (credenciales/campos vacíos), V2 (inactivo) y V4 (conexión caída) en el login | Mensajes correctos; la app no se cierra con MySQL detenido |
| _(fecha)_ | Nicolás | `EventoDAO.insertar()` y `listar()` (C1, C2) con `POINT(?, ?)` y JSON | Evento visible en MySQL y en la `JTable` |
| _(fecha)_ | Nicolás | `EventoDAO.buscar()`, `actualizar()` y `eliminar()` (C3, C4, C5) | Cambios reflejados en MySQL; eliminación con confirmación |
| _(fecha)_ | Nicolás | V3 (código repetido, error 1062) y `Conexion.mensajeError()` | «El registro ya existe.» y no inserta |
| _(fecha)_ | Nicolás | Pruebas JUnit de los DAO y verificación con SELECT | 24 pruebas en verde (ConexionTest 4, UsuarioDAOTest 6, EventoDAOTest 11, EventoTest 3) con `ant test` |
| _(fecha)_ | Todos | `VentanaLogin` y `VentanaEventos` (diseño, eventos, `JTable`) e integración | Flujo login → CRUD funcionando de punta a punta |
| _(fecha)_ | Todos | Presentación (15 diapositivas) y ensayo de la defensa | `presentacion/Presentacion_Evaluacion4_CalendarioApp.pptx` |

## 3. Decisiones técnicas del equipo

- **MariaDB 10.6 en contenedor** (`calendario-mysql`, red compartida con NetBeans, phpMyAdmin en http://localhost:8081). El script es compatible con MySQL 8.0.16+ porque los `CHECK` se aplican desde esa versión.
- **Una sola clase `Conexion`** con URL, usuario y clave en constantes; los DAO no repiten datos de conexión (criterio A4).
- **`PreparedStatement` en todas las sentencias**, sin concatenar datos: evita la inyección SQL (A6 a A10).
- **Capas separadas** (`conexion`, `modelo`, `dao`, `vista`): la ventana nunca escribe SQL.
- **POINT y JSON** por pedido del profesor: `POINT(?, ?)` con orden (longitud, latitud); JSON guardado como texto y validado con `CHECK (JSON_VALID(detalle))`.
- **`clave` con `COLLATE utf8mb4_bin`** para que la contraseña distinga mayúsculas (con la collation `_ci` de la base, `ADMIN2026` entraba como `Admin2026`).
- **Claves en texto plano** porque la pauta entrega el script de `usuario` así; en un sistema real irían con hash.

## 4. Pendientes antes de la defensa (viernes 4 de diciembre; ensayo el martes 1)

- [ ] Cada integrante completa fechas y detalles de sus filas (B3).
- [ ] Ensayar la demo completa de `docs/guia_defensa.md` (sección 1) al menos dos veces.
- [ ] Confirmar que el reparto de la sección 1 refleja lo que realmente hizo cada uno.
- [ ] Tomar las capturas de la app y ponerlas en la diapositiva de demostración.
- [ ] Llevar la presentación y el proyecto en un respaldo (pendrive o nube).
