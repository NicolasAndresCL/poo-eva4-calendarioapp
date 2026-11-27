# Guía de defensa oral — CalendarioApp (Parte B)

**Evaluación 4 (20 %) · Fundamentos de POO (IEI-085) · Profesor: Pablo · Defensa: viernes 4 de diciembre de 2026**
Equipo: Tais Montecinos · Rodolfo Prieto · Nicolás Cano

La defensa es **individual** (30 puntos, 6 por criterio). El orden y quién responde se definen al azar, así que **cualquiera de los tres debe poder explicar cualquier parte**. Esta guía cubre B1, B2, B4 y B5; B3 (aportes) se respalda con `docs/bitacora.md`.

| Criterio | Qué piden | Dónde está en esta guía |
|---|---|---|
| B1. Demostración | Ejecutar la operación pedida y verificarla en MySQL sin apoyo | Sección 1 |
| B2. Dominio del contenido | Explicar el modelo de datos y el flujo JDBC con lenguaje técnico | Sección 2 |
| B3. Aportes | Aportes en la bitácora y explicar con seguridad el código propio | `docs/bitacora.md` |
| B4. Explicación de código | Ubicar y explicar el método DAO o la sentencia SQL pedida | Sección 3 |
| B5. Pregunta técnica al azar | Responder correctamente y aplicar al proyecto | Sección 4 |

---

## 0. Antes de empezar (checklist de 2 minutos)

1. MySQL arriba: `docker ps` debe mostrar `calendario-mysql` y `calendario-phpmyadmin` (y `calendario-netbeans`). Si no están: `docker compose up -d` desde la carpeta del proyecto POO.
2. Base cargada: `docker exec calendario-mysql mariadb -uroot calendario_app -e "SELECT COUNT(*) FROM evento;"` debe devolver un número (el script deja 5 filas; las pruebas pueden haber agregado más).
3. phpMyAdmin abierto en http://localhost:8081 (base `calendario_app`, usuario `root`, sin clave).
4. NetBeans con el proyecto `CalendarioApp` abierto; el driver `mysql-connector-j-26.10.0.jar` está en `CalendarioApp/lib/` y en las *Libraries* del proyecto.
5. Si hace falta partir de cero: ejecutar `sql/calendario_bd.sql` completo (borra y recrea las tablas).

> Comando de verificación sin phpMyAdmin (desde Git Bash, con `export MSYS_NO_PATHCONV=1` si el comando lleva rutas):
> `docker exec calendario-mysql mariadb -uroot calendario_app -e "SELECT ..."`

---

## 1. B1 — Demostración paso a paso (cada operación + su verificación en MySQL)

Regla de oro: **hacer la operación en la app y enseguida mostrar el SELECT**. Decir en voz alta qué se espera ver antes de ejecutarlo.

### 1.1 Login correcto (L1)
- **En la app:** `VentanaLogin` → usuario `admin`, clave `Admin2026` → *Ingresar*. Se abre `VentanaEventos`.
- **Qué decir:** «`UsuarioDAO.validar()` ejecuta un `SELECT ... WHERE nombre_usuario = ? AND clave = ? AND activo = TRUE` con `executeQuery()`; si el `ResultSet` trae una fila, devuelve un `Usuario`».
- **Verificar en MySQL:**
  ```sql
  SELECT id_usuario, nombre_usuario, rol, activo FROM usuario;
  ```
  Se ven `admin`, `operador1` e `inactivo1` (este con `activo = 0`).

### 1.2 Login incorrecto o vacío (V1)
- **Extra:** `ADMIN2026` (mayúsculas distintas) tampoco entra: la columna `clave` usa `COLLATE utf8mb4_bin`.
- **En la app:** campos vacíos, o `admin` con clave `mala`. Mensaje: «Usuario o contraseña incorrectos.» No entra.
- **Qué decir:** campos vacíos se detectan antes de consultar; clave mala hace que `validar()` devuelva `null`.

### 1.3 Usuario inactivo (V2)
- **En la app:** `inactivo1` / `Inac2026`. Mensaje: «Usuario inactivo.»
- **Verificar:** `SELECT nombre_usuario, activo FROM usuario WHERE nombre_usuario = 'inactivo1';` → `activo = 0`.
- **Qué decir:** `validar()` filtra `activo = TRUE`, por eso devuelve `null`; luego `estaInactivo()` cuenta si existe esa combinación con `activo = FALSE` para dar el mensaje específico.

### 1.4 Insertar (C1)
- **En la app:** código `EVT-0100`, título «Demo defensa», lugar, latitud `-33.4489`, longitud `-70.6693`, detalle `{"notas": "demo"}`, fecha `2026-12-15`, hora inicio `10:00` → *Guardar*. El evento aparece en la `JTable`.
- **Verificar en MySQL:**
  ```sql
  SELECT id_evento, codigo_evento, titulo, fecha_evento, hora_inicio, prioridad, activo
  FROM evento WHERE codigo_evento = 'EVT-0100';
  ```
  Y para POINT y JSON:
  ```sql
  SELECT codigo_evento, ST_X(ubicacion) AS longitud, ST_Y(ubicacion) AS latitud,
         JSON_UNQUOTE(JSON_EXTRACT(detalle, '$.notas')) AS notas
  FROM evento WHERE codigo_evento = 'EVT-0100';
  ```
  Esperado: longitud `-70.6693`, latitud `-33.4489`, notas `demo`.
- **Qué decir:** «`EventoDAO.insertar()`: `INSERT ... VALUES (?, ?, ?, POINT(?, ?), ...)` con `executeUpdate()` y `RETURN_GENERATED_KEYS` para leer el `id_evento` que generó `AUTO_INCREMENT`».

### 1.5 Código repetido (V3)
- **En la app:** guardar otro evento con código `EVT-0100`. Mensaje: «El registro ya existe.»
- **Verificar:** `SELECT COUNT(*) FROM evento WHERE codigo_evento = 'EVT-0100';` → sigue en `1`.
- **Qué decir:** MySQL lanza `SQLException` con `getErrorCode() == 1062` por el `UNIQUE`; `Conexion.mensajeError()` lo traduce.

### 1.6 Buscar (C3) y listar (C2)
- **En la app:** escribir `EVT-0002` en el código → *Buscar*: el formulario se llena. Código inexistente: «No existe un evento con ese código.» La `JTable` se carga con `listar()` al abrir la ventana.
- **Verificar:** `SELECT * FROM evento WHERE codigo_evento = 'EVT-0002';` y `SELECT COUNT(*) FROM evento;` (debe coincidir con las filas de la tabla).
- **Qué decir:** `buscar()` usa `WHERE codigo_evento = ?` (identificador de negocio) y devuelve `null` si `rs.next()` es `false`; `listar()` recorre el `ResultSet` con `while (rs.next())`.

### 1.7 Actualizar (C4)
- **En la app:** seleccionar `EVT-0100`, cambiar el título a «Demo defensa (editado)» y la prioridad → *Actualizar*.
- **Verificar:** `SELECT codigo_evento, titulo, prioridad FROM evento WHERE codigo_evento = 'EVT-0100';` → título nuevo.
- **Qué decir:** `UPDATE ... SET ... WHERE id_evento = ?`; `executeUpdate() == 1` confirma que cambió exactamente una fila.

### 1.8 Eliminar con confirmación (C5)
- **En la app:** seleccionar `EVT-0100` → *Eliminar* → responder *Sí* en «¿Está seguro de eliminar el evento…?».
- **Verificar:** `SELECT COUNT(*) FROM evento WHERE codigo_evento = 'EVT-0100';` → `0`.
- **Qué decir:** `DELETE FROM evento WHERE id_evento = ?`; la confirmación es un `JOptionPane.showConfirmDialog` en la ventana, antes de llamar al DAO.

### 1.9 Falla de conexión (V4)
- **Preparación:** `docker stop calendario-mysql` (hay que levantarlo después: `docker start calendario-mysql`; ojo: `calendario-phpmyadmin` comparte su red).
- **En la app:** intentar login o guardar. Mensaje: «No fue posible conectar con la base de datos.» **La aplicación no se cierra.**
- **Qué decir:** `DriverManager.getConnection` lanza `SQLException` con `SQLState` que empieza en `08`; el `catch` la convierte en mensaje y la ventana sigue viva.
- **Importante:** avisar antes de detener MySQL y volver a subirlo al terminar la demo.

> Plan B si algo falla en vivo: mostrar el mismo efecto con el SELECT en phpMyAdmin y explicar el método del DAO (B4). Mantener la calma vale más que el error.

---

## 2. B2 — Dominio del contenido (explicación de 60 a 90 segundos)

### 2.1 El modelo de datos
- Dos tablas **independientes** (sin clave foránea) en la base `calendario_app`: `usuario` (acceso) y `evento` (CRUD del tema).
- `usuario`: `id_usuario` (PK AUTO_INCREMENT), `nombre_usuario` (UNIQUE), `clave`, `nombre_completo`, `rol` (DEFAULT 'OPERADOR'), `activo` (BOOLEAN DEFAULT TRUE).
- `evento` (14 columnas, la pauta exige 8): `id_evento` PK; `codigo_evento` identificador de negocio (VARCHAR, NOT NULL, UNIQUE); `titulo`; `lugar`; **`ubicacion` POINT**; **`detalle` JSON**; `categoria` (DEFAULT 'personal'); `fecha_evento` DATE; `hora_inicio`/`hora_fin` TIME; `duracion_minutos` (DEFAULT 60); `prioridad` (DEFAULT 2); `activo` BOOLEAN; `fecha_creacion` TIMESTAMP DEFAULT CURRENT_TIMESTAMP.
- Cuatro `CHECK`: `duracion_minutos > 0`, `prioridad BETWEEN 1 AND 3`, `hora_fin IS NULL OR hora_fin > hora_inicio`, `detalle IS NULL OR JSON_VALID(detalle)`.

### 2.2 El flujo JDBC
1. **`Connection`**: la conexión abierta con MySQL. Se obtiene con `Conexion.conectar()` (que llama a `DriverManager.getConnection(URL, USUARIO, CLAVE)`).
2. **`PreparedStatement`**: la sentencia SQL con parámetros `?`, que se rellenan con `setString`, `setInt`, `setDouble`, etc. Se crea con `con.prepareStatement(sql)`.
3. **`ResultSet`**: las filas que devuelve un `SELECT` (`executeQuery()`); se recorre con `next()` y se lee con `getString("col")`, `getInt("col")`...
4. Para `INSERT`/`UPDATE`/`DELETE` se usa `executeUpdate()`, que devuelve las filas afectadas.
5. Los tres se declaran en un **try-with-resources**: se cierran solos aunque haya excepción.

### 2.3 Las capas
`vista` (ventanas, sin SQL) → `dao` (todo el SQL) → `modelo` (Usuario, Evento) y `conexion` (la única clase con URL, usuario y clave).

---

## 3. B4 — Dónde está cada cosa (archivo y método)

Ruta base: `CalendarioApp/src/`

| Código | Qué | Archivo | Método / sentencia |
|---|---|---|---|
| S1–S5 | Script SQL completo | `../sql/calendario_bd.sql` | `CREATE DATABASE`, `CREATE TABLE usuario`, `CREATE TABLE evento`, `INSERT` de usuarios y eventos |
| A4 | Conexión centralizada | `conexion/Conexion.java` | constantes `URL`, `USUARIO`, `CLAVE`; `conectar()`; `probarConexion()` |
| V3/V4 | Traducción de errores SQL | `conexion/Conexion.java` | `mensajeError(SQLException)` (1062, 1045, 1049, estado `08`, 4025/3819/3140) |
| L1 | Login | `dao/UsuarioDAO.java` | `validar(nombreUsuario, clave)`: `SELECT ... WHERE nombre_usuario = ? AND clave = ? AND activo = TRUE` |
| V2 | Usuario inactivo | `dao/UsuarioDAO.java` | `estaInactivo(...)`: `SELECT COUNT(*) ... AND activo = FALSE` |
| V1/V2 en pantalla | Mensajes de login | `vista/VentanaLogin.java` | `btnIngresarActionPerformed` |
| C1 | Insertar | `dao/EventoDAO.java` | `insertar(Evento)`: `INSERT ... POINT(?, ?) ...`, `RETURN_GENERATED_KEYS` |
| C2 | Listar | `dao/EventoDAO.java` | `listar()`: `SELECT ... ORDER BY fecha_evento, hora_inicio` |
| C3 | Buscar | `dao/EventoDAO.java` | `buscar(String codigoEvento)`: `WHERE codigo_evento = ?` |
| C4 | Actualizar | `dao/EventoDAO.java` | `actualizar(Evento)`: `UPDATE ... WHERE id_evento = ?` |
| C5 | Eliminar | `dao/EventoDAO.java` | `eliminar(int idEvento)`: `DELETE ... WHERE id_evento = ?` |
| POINT/JSON | Parámetros y lectura | `dao/EventoDAO.java` | `cargarParametros(...)` (setNull si no hay ubicación), `leerFila(...)` (`wasNull()`), constante `COLUMNAS` con `ST_Y`/`ST_X` |
| Modelo | Una fila de cada tabla | `modelo/Usuario.java`, `modelo/Evento.java` | atributos `private`, constructores, getters/setters, `tieneUbicacion()` |
| Pantalla CRUD | Formulario + `JTable` | `vista/VentanaEventos.java` | `btnGuardar…`, `btnBuscar…`, `btnActualizar…`, `btnEliminar…` (confirmación), `cargarTabla()`, `leerFormulario()` (validaciones de formato) |

**Cómo explicar un método del DAO (plantilla de 4 frases):** (1) qué sentencia SQL ejecuta; (2) qué parámetros `?` rellena y con qué `setXxx`; (3) si usa `executeQuery()` o `executeUpdate()` y qué devuelve; (4) cómo se cierran los recursos (try-with-resources).

Ejemplo con `buscar()`: «Es el C3. Ejecuta `SELECT ... WHERE codigo_evento = ?`. Con `ps.setString(1, codigoEvento)` pone el código en el parámetro. Usa `executeQuery()`, que devuelve un `ResultSet`; si `rs.next()` es verdadero construyo el `Evento` con `leerFila`, y si no devuelvo `null`. Connection, PreparedStatement y ResultSet están en try-with-resources, así que se cierran solos.»

---

## 4. B5 — Preguntas técnicas probables (con respuesta breve, aplicada al proyecto)

1. **¿Qué es JDBC?** El API de Java para hablar con bases de datos relacionales (`java.sql`). En el proyecto, `Connection`, `PreparedStatement` y `ResultSet` de `java.sql` más el driver `mysql-connector-j`.
2. **¿Qué es el driver y dónde está?** La implementación de JDBC para MySQL. Es `mysql-connector-j-26.10.0.jar` en `CalendarioApp/lib/`, agregado a las *Libraries* del proyecto; `DriverManager` lo encuentra solo.
3. **Explique la URL de conexión.** `jdbc:mysql://localhost:3306/calendario_app?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Santiago`: protocolo, servidor, puerto 3306, base de datos y parámetros (sin SSL en local, autenticación y zona horaria).
4. **¿Para qué sirve la clase `Conexion`?** Centraliza URL, usuario y clave en constantes; los DAO solo llaman `Conexion.conectar()`. Si cambia la clave se edita un solo archivo.
5. **¿`Statement` o `PreparedStatement`?** `PreparedStatement`: la sentencia va precompilada con `?` y los valores se envían aparte. `Statement` concatena texto y es vulnerable. La pauta exige `PreparedStatement` en todo.
6. **¿Qué es una inyección SQL y cómo la evitan?** Meter SQL en un dato (p. ej. clave `' OR '1'='1`). Con `?` el dato siempre se trata como valor, nunca como código. Se ve en `UsuarioDAO.validar()`.
7. **`executeQuery()` vs `executeUpdate()`.** `executeQuery()` es para `SELECT` y devuelve `ResultSet` (L1, C2, C3). `executeUpdate()` es para `INSERT`/`UPDATE`/`DELETE` y devuelve las filas afectadas (C1, C4, C5).
8. **¿Qué es un `ResultSet` y para qué sirve `next()`?** Un cursor sobre las filas del resultado. Parte antes de la primera fila; `next()` avanza y devuelve `false` cuando no hay más. En `listar()` es `while (rs.next())`; en `buscar()` es `if (rs.next())`.
9. **¿Qué es try-with-resources y por qué se usa?** Un `try (recurso)` que cierra los recursos `AutoCloseable` al salir, aunque haya excepción. Evita fugas de conexiones y reemplaza al `finally`. Criterio A5.
10. **¿Qué pasa si MySQL está caído?** `getConnection` lanza `SQLException` (SQLState `08…`). El `catch` muestra «No fue posible conectar con la base de datos.» y la app no se cierra (V4).
11. **¿Para qué sirve `SQLException`?** Es la excepción de todo error JDBC; trae `getMessage()`, `getSQLState()` y `getErrorCode()`. Con el código identificamos 1062 (duplicado) o el error de conexión.
12. **PRIMARY KEY vs UNIQUE.** La PK identifica la fila: una por tabla, nunca nula (aquí `id_evento`). `UNIQUE` evita repetidos pero puede haber varios por tabla (aquí `codigo_evento` y `nombre_usuario`).
13. **¿Por qué `id_evento` y además `codigo_evento`?** `id_evento` es clave técnica autogenerada; `codigo_evento` (p. ej. `EVT-0001`) es el identificador de negocio que el usuario escribe y por el que se busca (C3).
14. **¿Qué hace `AUTO_INCREMENT`?** La base genera el siguiente entero al insertar. En `insertar()` se recupera con `RETURN_GENERATED_KEYS` y `getGeneratedKeys()`.
15. **¿Qué hace `DEFAULT`?** Da un valor cuando el `INSERT` no lo trae. El evento `EVT-0005` se insertó solo con 4 campos y tomó `categoria = 'personal'`, `duracion_minutos = 60`, `prioridad = 2`, `activo = TRUE`.
16. **¿Qué hace `NOT NULL`?** Obliga a que el campo tenga valor. Si falta, error 1364 (p. ej. sin `titulo`).
17. **¿Qué es un `CHECK`?** Una regla que cada fila debe cumplir (`prioridad BETWEEN 1 AND 3`, `hora_fin > hora_inicio`). Se aplica desde MySQL 8.0.16 y en MariaDB 10.2+; falla con error 4025 (MariaDB) o 3819 (MySQL).
18. **¿Por qué una columna `POINT`?** Guarda la ubicación como un solo dato geoespacial (longitud, latitud) en vez de dos columnas sueltas, y permite funciones espaciales. Se escribe con `POINT(?, ?)` y se lee con `ST_X` (longitud) y `ST_Y` (latitud).
19. **¿Por qué `POINT(longitud, latitud)` y no al revés?** Porque X es el eje horizontal = longitud e Y el vertical = latitud. Por eso `cargarParametros` hace `setDouble(4, longitud)` y `setDouble(5, latitud)`.
20. **¿Por qué una columna `JSON`?** Para datos libres y variables (notas, participantes, cosas a llevar) sin crear tablas extra. El `CHECK (JSON_VALID(detalle))` rechaza texto inválido; en Java se guarda con `setString` y se lee con `getString`.
21. **¿Cómo se lee un dato dentro del JSON?** `JSON_EXTRACT(detalle, '$.notas')`, o `JSON_UNQUOTE(...)` para quitar las comillas.
22. **¿Cómo se guarda un evento sin ubicación?** Con `ps.setNull(4, Types.DOUBLE)` y `ps.setNull(5, Types.DOUBLE)`; al leer, `rs.wasNull()` distingue un 0 real de un `NULL`.
23. **¿Qué es `BOOLEAN` en MySQL?** Un alias de `TINYINT(1)`: 1 es verdadero, 0 falso. Por eso en consola `activo` se ve como 1 o 0, pero en Java se usa `getBoolean`/`setBoolean`.
24. **¿Por qué se usa `DATE` y `TIME` y cómo pasan a Java?** `fecha_evento` es `DATE` y las horas `TIME`. Se convierten con `Date.valueOf(LocalDate)` y `Time.valueOf(LocalTime)` al escribir, y `getDate().toLocalDate()` / `getTime().toLocalTime()` al leer.
25. **¿Qué es un DAO y por qué existe?** *Data Access Object*: la clase que concentra el SQL de una tabla. La ventana no sabe SQL; solo llama `eventoDAO.insertar(e)`. Separa responsabilidades y facilita cambiar la base.
26. **¿Qué hace `probarConexion()`?** Abre una conexión y devuelve `con.isValid(2)`; `false` si hay `SQLException`. Sirve para saber si MySQL responde.
27. **¿Por qué la clave está en texto plano?** Porque la pauta entrega el script con claves como `'Admin2026'`. En un sistema real se guardaría un *hash* (p. ej. bcrypt). Reconocer la limitación suma.
28. **¿Qué significa `RETURN_GENERATED_KEYS`?** Le pide al driver devolver los valores de columnas autogeneradas tras el `INSERT`; así `insertar()` deja el `id_evento` en el objeto.
29. **¿Qué diferencia hay entre `CREATE TABLE` y `CREATE TABLE IF NOT EXISTS`?** El segundo no falla si la tabla ya existe. S1 y S2 lo usan; el script además hace `DROP TABLE IF EXISTS` para re-ejecutarse desde cero.
30. **¿Por qué dos tablas sin clave foránea?** La pauta pide tablas independientes: `usuario` solo controla el acceso y `evento` es el CRUD del tema.

31. **¿Qué es una collation y por qué `clave` usa `utf8mb4_bin`?** Es la regla con que MySQL compara y ordena texto. Las `_ci` (*case insensitive*, como `utf8mb4_spanish_ci` de la base) no distinguen mayúsculas, así que `ADMIN2026` entraba como `Admin2026`. `_bin` compara los bytes exactos: distingue mayúsculas y minúsculas. Por eso la columna es `clave VARCHAR(64) NOT NULL COLLATE utf8mb4_bin`; `nombre_usuario` y los demás textos siguen con la collation `_ci` de la base.
32. **¿Cómo comprueban que las pruebas pasan?** 24 pruebas JUnit 4 en `CalendarioApp/test/` (`ConexionTest` 4, `UsuarioDAOTest` 6, `EventoDAOTest` 11, `EventoTest` 3), todas en verde con Alt+F6 (`ant test`). JUnit 4 y no 5 porque en un proyecto Ant de NetBeans `ant test` no ejecuta JUnit 5. Hay además un driver de interfaz con 23 de 23 comprobaciones.

---

## 5. Ganchos mnemotécnicos (con autoevaluación)

Cada gancho lleva al **término técnico exacto**. Primero se dice el gancho, luego se responde la pregunta sin mirar.

### 5.1 El flujo JDBC — frase-ancla «**C**ada **P**izza **R**ica»
**C**onnection → **P**reparedStatement → **R**esultSet (en ese orden: línea → sentencia → resultados).
- *Autoevaluación:* ¿Cuáles son los tres objetos del flujo JDBC y en qué orden se usan? ¿Cuál de los tres no existe en un `INSERT`? (R: el `ResultSet`; el `INSERT` usa `executeUpdate()`).

### 5.2 Query lee, Update escribe — contraste en espejo
`executeQ`uery = **Q**uiero **ver** filas (SELECT, devuelve `ResultSet`) · `executeU`pdate = **U**sa **c**ambios (INSERT, UPDATE, DELETE, devuelve un `int`).
- *Autoevaluación:* ¿Qué método usa C2? ¿Y C5? ¿Qué devuelve cada uno?

### 5.3 CRUD y sus códigos — «**I**nés **L**ee **B**uenos **A**vances **E**ditados»
C1 **I**nsertar · C2 **L**istar · C3 **B**uscar · C4 **A**ctualizar · C5 **E**liminar. Y los verbos SQL en el mismo orden: INSERT, SELECT, SELECT, UPDATE, DELETE.
- *Autoevaluación:* ¿Qué código es «Buscar» y por qué columna busca? (R: C3, por `codigo_evento`, el identificador de negocio).

### 5.4 Validaciones V1–V4 — «**V**acío, **I**nactivo, **R**epetido, **R**oto»
V1 **V**acío o incorrecto (login) · V2 **I**nactivo (`activo = FALSE`) · V3 **R**epetido (`UNIQUE`, error 1062) · V4 **R**oto (conexión caída, SQLState `08`).
- *Autoevaluación:* ¿Qué excepción y qué código dispara V3? ¿Se cierra la app en V4?

### 5.5 `?` es la puerta cerrada — imagen absurda
Imagina el dato del usuario como un ladrón con un papelito que dice `' OR '1'='1`. Con **concatenación** le abres la puerta y el papelito entra como código; con **`?`** el ladrón queda en el pasillo: es solo un valor. *Concatenar = Colarse; `?` = Cerrado.*
- *Autoevaluación:* ¿Por qué `PreparedStatement` evita la inyección SQL?

### 5.6 PK vs UNIQUE — contraste «**Una** y **nunca nula** / **Varias** y **no repetidas**»
PK: **U**na por tabla, jamás `NULL` (`id_evento`). UNIQUE: puede haber **v**arias, no se repite el valor (`codigo_evento`, `nombre_usuario`).
- *Autoevaluación:* ¿Cuántas PK y cuántos UNIQUE tiene `evento`? (R: 1 PK, 1 UNIQUE; `usuario` también 1 y 1).

### 5.7 POINT(X, Y) — «**X** cruza de lado a lado: **LON**; **Y** sube al cielo: **LAT**»
X = **lon**gitud (horizontal), Y = **lat**itud (vertical). Se escribe `POINT(lon, lat)`; se lee `ST_X` (lon) y `ST_Y` (lat). *«X antes que Y, lon antes que lat»*.
- *Autoevaluación:* En `POINT(-70.6506, -33.4378)`, ¿cuál es la latitud? (R: -33.4378, la segunda).

### 5.8 Las restricciones del script — acrónimo «**P-U-N-D-C**»
**P**K · **U**NIQUE · **N**OT NULL · **D**EFAULT · **C**HECK («**P**edro **U**sa **N**ombres **D**istintos y **C**orrectos»). Los cinco que se ven en `evento`.
- *Autoevaluación:* Nombra una columna de `evento` con cada restricción. (R: `id_evento`, `codigo_evento`, `titulo`, `categoria`, `prioridad`).

### 5.9 try-with-resources — «**Abre en el paréntesis, se cierra solo**»
Lo que abres dentro de `try ( … )` se cierra automáticamente, incluso si hay error: ni `finally`, ni `close()`.
- *Autoevaluación:* ¿Qué tres recursos abre `buscar()` en el `try` y cuál es el orden de cierre? (R: `Connection`, `PreparedStatement`, `ResultSet`; se cierran al revés: el último abierto se cierra primero).

### 5.10 JSON y su CHECK — «**JSON** viene **V**álido»
`JSON_VALID` es el guardia del CHECK: si el texto no es JSON, error 4025 (MariaDB) / 3140 (MySQL).
- *Autoevaluación:* ¿Qué función SQL extrae el campo `notas` del JSON? (R: `JSON_EXTRACT(detalle, '$.notas')`).

---

## 6. Calendario de repaso espaciado

| Fecha | Foco | Qué hacer (45 a 60 min) |
|---|---|---|
| **Viernes 27 nov** | Entender | Leer secciones 2 y 3. Abrir `Conexion`, `UsuarioDAO` y `EventoDAO` y explicar cada método en voz alta con la plantilla de 4 frases. Memorizar los ganchos 5.1 a 5.4. |
| **Sábado 28 nov** | Recuperar y demostrar | Sin mirar: responder las autoevaluaciones de la sección 5. Hacer la demo completa (sección 1) dos veces, cronometrada, verificando cada paso con el SELECT. Preguntas 1 a 15 de la sección 4. |
| **Domingo 29 nov** | Estrés y huecos | Preguntas 16 a 32 al azar (que otro integrante las lea). Simular el azar de la pauta: «explica `actualizar()`», «¿qué es un `ResultSet`?». Practicar el plan B si la app falla. Repasar ganchos 5.5 a 5.10. |
| **Martes 1 dic** | Ensayo en equipo | Ensayo completo guiado por Nicolás: ver `docs/ensayo_martes.pdf` (presentación cronometrada, rondas B1–B5, modificaciones en vivo y simulacro). Anotar los huecos de cada uno. |
| **Jueves 3 dic** | Cerrar huecos | Repasar solo lo que falló el martes. Repetir la demo completa sin mirar y las preguntas trampa de la defensa individual. |
| **Viernes 4 dic (mañana)** | Activar | 20 minutos: checklist de la sección 0 (contenedores arriba, base con datos, NetBeans abierto). Recitar C-P-R, V-I-R-R, I-L-B-A-E y POINT(lon, lat). Una pasada rápida por la tabla de la sección 3. No aprender nada nuevo. |

**Criterio de éxito:** poder explicar cualquier método sin leer, nombrar el archivo donde está y mostrar su efecto en MySQL.
