# Defensa individual — Rodolfo Prieto

**Evaluación 4 · Fundamentos de POO (IEI-085) · CalendarioApp · Defensa: viernes 4 de diciembre de 2026**
**Profesor: Pablo** · Equipo: Tais Montecinos · Rodolfo Prieto · Nicolás Cano

Mi parte: **`Conexion`**, **`UsuarioDAO`** (L1 `validar`, `estaInactivo`) y la **ventana de login** con las validaciones **V1, V2 y V4**. Pero me pueden preguntar **cualquier cosa del proyecto**: el profesor ya dijo que «un 7 es si todo funciona y se defiende, y un 1 si no saben defender». Defender = explicar con mis palabras, sin leer, y poder cambiar el código en vivo.

---

## 0. Cómo usar esta guía

| Criterio (6 pts c/u) | Qué hace el profesor | Dónde prepararlo |
|---|---|---|
| **B1** Demostración | Me pide ejecutar algo y verlo en MySQL sin apoyo | Sección 6 (guion exacto) |
| **B2** Dominio | Pregunta el modelo y el flujo JDBC con lenguaje técnico | Secciones 2, 3, 9, 11 y 15 |
| **B3** Aportes | Revisa la bitácora y me hace explicar *mi* código | Secciones 2 a 5 |
| **B4** Explicación de código | «Muéstreme el método DAO o la sentencia X» | Secciones 3, 8 y 12 |
| **B5** Pregunta técnica al azar | Una pregunta puntual, aplicada al proyecto | Secciones 9, 12, 14 y 17 |

**Orden de estudio sugerido (60 min por día):** 1.º secciones 1 a 5 en voz alta; 2.º los guiones de la sección 11; 3.º las trampas (12); 4.º «modificar en vivo» (13) **escribiéndolo a mano en NetBeans**; 5.º la autoevaluación (17), tapando las respuestas del anexo.

**Calendario de repaso espaciado (defensa: viernes 4 de diciembre; ensayo del equipo: martes 1 de diciembre).**

| Día | Foco |
|---|---|
| Sáb 28 nov | Secciones 1 a 5 en voz alta; ganchos (sección 10) |
| Dom 29 nov | Guiones G1 a G6 (sección 11) y trampas T1 a T9 (sección 12) |
| Lun 30 nov | «Modificar en vivo» (a) a (f) escritos a mano en NetBeans (sección 13); trampas T10 a T18 |
| **Mar 1 dic** | **Ensayo del equipo:** demo completa (sección 6) y preguntas cruzadas entre los tres |
| Mié 2 dic | Errores (sección 14) y autoevaluación (sección 16) sin mirar el anexo; repetir las falladas |
| Jue 3 dic | Solo activar: demo una vez, ganchos, checklist de contenedores arriba; nada nuevo |
| **Vie 4 dic** | **Defensa** |

**Nombres de los contenedores.** En este repo (`docker-compose.yml`) son `calendario-mysql` (MariaDB 10.6) y `calendario-phpmyadmin`. En mi equipo de clase el mismo stack puede llamarse `poo-mysql`: los comandos son idénticos cambiando solo el nombre. Se comprueba con:

```bash
docker ps --format "{{.Names}}  {{.Status}}"
```
- Lista los contenedores en ejecución con su estado. **Esperado:** `calendario-mysql  Up ... (healthy)` y `calendario-phpmyadmin  Up ...`. Si sale vacío o dice `Cannot connect to the Docker daemon`, falta abrir Docker Desktop y correr `docker compose up -d` en la raíz del repo.

(Todas las consultas y el código de esta guía se probaron contra MariaDB 10.6 con el mismo script `sql/calendario_bd.sql`.)

**Alternativa simple a la terminal: phpMyAdmin** en http://localhost:8081 → base `calendario_app` → pestaña **SQL** → pegar la consulta → **Continuar**. Entra solo como `root`, sin clave. Si la terminal falla en vivo, uso esto.

---

## 1. Mi parte en 30 segundos

«Yo hice la puerta de entrada y el puente a la base de datos. La clase `Conexion` guarda en un solo lugar la URL, el usuario y la clave de MySQL y entrega conexiones con `DriverManager.getConnection`; además tiene `probarConexion` y `mensajeError`, que traduce una `SQLException` a un mensaje claro. `UsuarioDAO.validar` busca en la tabla `usuario` con un `PreparedStatement` y devuelve un `Usuario` o `null`; `estaInactivo` distingue al usuario desactivado del que puso mal la clave. En `VentanaLogin` el botón *Ingresar* comprueba en orden: campos vacíos, credenciales, usuario inactivo y, si todo va bien, abre `VentanaEventos`. Si MySQL está caído, el `catch` muestra el mensaje y la aplicación no se cierra.»

---

## 2. `Conexion.java` línea por línea

### Cabecera

```java
package conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
```
- `package conexion;` agrupa la clase en su paquete (capa de acceso a datos, separada de `vista`, `dao` y `modelo`).
- Se importan solo tres tipos de `java.sql`: `Connection` (la sesión abierta con la base), `DriverManager` (la fábrica de conexiones) y `SQLException` (el error de cualquier operación JDBC).

```java
public class Conexion {
```
- Clase de utilidad: todo es `static`, nunca se hace `new Conexion()`.

### Constantes

```java
private static final String URL = "jdbc:mysql://localhost:3306/calendario_app"
        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Santiago";
```
Anatomía de la URL:

| Pieza | Significado |
|---|---|
| `jdbc:mysql:` | Protocolo: le dice a `DriverManager` qué driver usar (el de MySQL). |
| `//localhost:3306` | Servidor y puerto. El contenedor publica el 3306 solo en `127.0.0.1`, por eso `localhost` funciona. |
| `/calendario_app` | Base de datos con la que se trabaja. Si no existe, error 1049. |
| `useSSL=false` | No cifra la conexión. Es entorno local de desarrollo. (Es el nombre antiguo del parámetro; el moderno es `sslMode=DISABLED`, y el driver aún acepta el viejo.) |
| `allowPublicKeyRetrieval=true` | Permite al driver pedir la clave pública del servidor cuando la autenticación es `caching_sha2_password` (MySQL 8) sin SSL; evita el error «Public Key Retrieval is not allowed». **MariaDB no lo necesita**, queda como red de seguridad si el equipo usa MySQL 8. |
| `serverTimezone=America/Santiago` | Zona horaria con que el driver interpreta `DATETIME`/`TIMESTAMP`. En algunos equipos Windows/XAMPP el servidor informa una zona con nombre raro y drivers antiguos fallaban con «The server time zone value ... is unrecognized». |

Dato honesto: **probé conectar con la URL pelada** (`jdbc:mysql://localhost:3306/calendario_app`) contra MariaDB 10.6 y **también conecta**. Las tres opciones son prevención para otros entornos, no una necesidad de este. Si el profesor pregunta «¿y si las saco?», lo digo así.

El `+` solo parte el texto en dos líneas; es un único `String`.

```java
private static final String USUARIO = "root";
private static final String CLAVE = "";
```
- Usuario `root` sin clave: es lo que configuramos en el contenedor de pruebas (`MARIADB_ALLOW_EMPTY_ROOT_PASSWORD`, igual que XAMPP). Solo vale para el laboratorio; en producción iría un usuario con permisos mínimos y la clave fuera del código (variable de entorno o archivo de configuración).

**¿Por qué `private static final`?**
- `private`: nadie fuera de la clase toca los datos de acceso; todos pasan por `conectar()`.
- `static`: pertenecen a la clase, no a una instancia; `conectar()` es `static` y solo puede usar miembros `static`.
- `final`: constante, no puede reasignarse. Si el dato cambia (otro host o clave), se edita **un solo lugar**, no cada DAO.

### `conectar()`

```java
public static Connection conectar() throws SQLException {
    return DriverManager.getConnection(URL, USUARIO, CLAVE);
}
```
- Devuelve una conexión **nueva** cada vez. `DriverManager` mira el prefijo `jdbc:mysql:` de la URL, encuentra el driver registrado y abre el socket con el servidor.
- `throws SQLException`: no la atrapo aquí, la propago; quien llama decide qué mostrar (la ventana).
- No se usa `Class.forName("com.mysql.cj.jdbc.Driver")`: desde JDBC 4.0 (Java 6) el driver se registra solo si el `.jar` está en el classpath; el jar trae el archivo `META-INF/services/java.sql.Driver` con el nombre de la clase y `DriverManager` lo lee con `ServiceLoader`.
- El driver es **`mysql-connector-j-26.10.0.jar`**, en `CalendarioApp/lib/`. En NetBeans se agrega con: clic derecho en el proyecto → **Properties → Libraries → Compile → Add JAR/Folder** → elegir el jar de `lib/`. Sin él: `No suitable driver found for jdbc:mysql://...`. Queda guardado como `file.reference.mysql-connector-j-26.10.0.jar=lib/...` en `nbproject/project.properties`, con ruta relativa para que funcione en cualquier PC.
- Se probó contra MariaDB 10.6 porque es el servidor con que el driver funciona sin problemas en este entorno.

### `probarConexion()`

```java
public static boolean probarConexion() {
    try (Connection con = conectar()) {
        return con.isValid(2);
    } catch (SQLException e) {
        return false;
    }
}
```
- `try (Connection con = conectar())` es **try-with-resources**: al terminar el bloque (con o sin error) Java llama `con.close()` solo. Así no quedan conexiones abiertas.
- `con.isValid(2)`: pregunta al servidor si la conexión sirve, esperando como máximo **2 segundos** (el parámetro son segundos, no milisegundos). Devuelve `true`/`false`.
- Si `conectar()` falla (MySQL caído, clave mala), cae al `catch` y devuelve `false`. No lanza excepción: es una pregunta de sí/no.
- **Honestamente:** hoy solo la usa `ConexionTest.testProbarConexion`. El login **no** la llama; detecta la caída porque `validar` lanza `SQLException` y el `catch` la traduce. Existe como utilidad (por ejemplo, para avisar «MySQL no responde» antes de abrir una ventana) y la cubre una prueba.

### `mensajeError(SQLException e)`

```java
String estado = e.getSQLState();
int codigo = e.getErrorCode();
```
- `getSQLState()`: código estándar de 5 caracteres (SQLState): 2 de **clase** + 3 de subclase; `getErrorCode()`: código propio de MySQL/MariaDB (el número del `ERROR 1045`).

```java
if ((estado != null && estado.startsWith("08")) || codigo == 1045 || codigo == 1049) {
    return "No fue posible conectar con la base de datos.";
}
```
- SQLState **`08xxx`** = clase «excepción de conexión» del estándar SQL (p. ej. `08S01` *Communications link failure*, MySQL apagado; `08001` no se pudo establecer la conexión, p. ej. *No suitable driver*; `08003` la conexión ya está cerrada). Se revisa `estado != null` para no caer en `NullPointerException`.
- **1045** = *Access denied* (usuario o clave de la base incorrectos; su SQLState es `28000`, **no** `08`). **1049** = *Unknown database* (SQLState `42000`). Por eso hay que mirar también el código: el SQLState solo no basta. Para el usuario final los tres significan lo mismo: no se pudo conectar.
- Medido en el proyecto: puerto malo → `08S01`/código 0; clave de la base mala → `28000`/1045; base inexistente → `42000`/1049; sin driver → `08001`/0. Los cuatro terminan en «No fue posible conectar con la base de datos.».

```java
if (codigo == 1062) { return "El registro ya existe."; }
```
- **1062** = *Duplicate entry*: viola un `UNIQUE`. En este proyecto, repetir `codigo_evento` (p. ej. otro `EVT-0001`): `Duplicate entry 'EVT-0001' for key 'codigo_evento'`. Es la V3, parte de Nicolás, pero vive en este método mío.

```java
if (codigo == 4025 || codigo == 3819 || codigo == 3140) {
    return "Los datos no cumplen las reglas de la tabla (prioridad, horas, duracion o JSON).";
}
```
- **4025** = *CONSTRAINT failed* en MariaDB (un `CHECK`, incluido el de `JSON_VALID`). **3819** = el mismo `CHECK` en MySQL 8. **3140** = *Invalid JSON text* en MySQL. Se cubren ambos motores.

```java
return "Error de base de datos: " + e.getMessage();
```
- Respaldo para cualquier otro error: muestra el mensaje original (por ejemplo un 1064 de sintaxis o un 1146 tabla inexistente).

Pruebas que lo respaldan (`ConexionTest`): `testProbarConexion` (MySQL arriba), `testMensajeConexionCaida` (SQLState `08S01`), `testMensajeClaveDeBaseIncorrecta` (1045) y `testMensajeRegistroDuplicado` (1062).

---

## 3. `UsuarioDAO.java` línea por línea

DAO = *Data Access Object*: la clase que concentra el SQL de una tabla, para que la ventana no contenga SQL.

### `validar`

```java
public Usuario validar(String nombreUsuario, String clave) throws SQLException {
```
- Recibe lo que escribió el usuario. Devuelve `Usuario` o `null`. Propaga `SQLException`.

```java
String sql = "SELECT id_usuario, nombre_usuario, clave, nombre_completo, rol, activo "
        + "FROM usuario WHERE nombre_usuario = ? AND clave = ? AND activo = TRUE";
```
- Los **`?`** son parámetros: huecos que se llenan después, **separados del texto SQL**.
- `AND activo = TRUE`: solo entran los activos. Un inactivo devuelve cero filas, igual que una clave mala.
- Como `clave` es `COLLATE utf8mb4_bin`, la comparación distingue mayúsculas: `Admin2026` ≠ `ADMIN2026`.

```java
try (Connection con = Conexion.conectar();
        PreparedStatement ps = con.prepareStatement(sql)) {
```
- try-with-resources con **dos** recursos; se cierran en orden inverso (primero `ps`, luego `con`).
- `prepareStatement(sql)`: crea el objeto con la plantilla que tiene los `?`. (Detalle técnico: por defecto Connector/J arma la sentencia **en el cliente**, `useServerPrepStmts=false`; midiendo `Com_stmt_prepare` del servidor antes y después de un `validar`, siguió en 0. No hace falta decirlo salvo que pregunten; lo importante es que el valor viaja como **dato**.)

```java
ps.setString(1, nombreUsuario);
ps.setString(2, clave);
```
- Llena el primer `?` y el segundo. **Los índices parten en 1**, no en 0. `setString` entrega el valor como **dato**: el driver lo escapa y lo pone como literal de texto; jamás se interpreta como SQL.

```java
try (ResultSet rs = ps.executeQuery()) {
```
- `executeQuery()` ejecuta un `SELECT` y devuelve un `ResultSet` (tabla de resultados con un cursor que empieza *antes* de la primera fila; **nunca devuelve `null`**, a lo más viene vacío). También va en try-with-resources.
- Para `INSERT/UPDATE/DELETE` se usa `executeUpdate()`, que devuelve un `int` (filas afectadas).

```java
if (rs.next()) {
```
- `next()` mueve el cursor a la siguiente fila y devuelve `true` si existe. Como `nombre_usuario` es `UNIQUE`, hay a lo más una fila, así que `if` basta (no `while`).

```java
return new Usuario(
        rs.getInt("id_usuario"),
        rs.getString("nombre_usuario"),
        rs.getString("clave"),
        rs.getString("nombre_completo"),
        rs.getString("rol"),
        rs.getBoolean("activo"));
```
- Lee cada columna por **nombre** con el `getXxx` que corresponde al tipo (`getInt`, `getString`, `getBoolean`) y arma el objeto del modelo. Así la ventana recibe un `Usuario`, no un `ResultSet`. (Limitación: el objeto lleva también la clave; en un sistema real no se carga.)

```java
return null;
```
- Si no hubo fila: credenciales malas **o** usuario inactivo. **¿Por qué `null` y no una excepción?** Una clave equivocada es un resultado normal, no un fallo técnico; las `SQLException` se reservan para problemas reales (conexión, SQL). El que llama pregunta `if (u == null)`.

### `estaInactivo`

```java
String sql = "SELECT COUNT(*) FROM usuario "
        + "WHERE nombre_usuario = ? AND clave = ? AND activo = FALSE";
```
- Cuenta filas con usuario y clave correctos pero `activo = FALSE`. Devuelve 1 si es inactivo, 0 si no existe o la clave es mala.

```java
rs.next();
return rs.getInt(1) > 0;
```
- `COUNT(*)` **siempre devuelve una fila**, por eso basta `rs.next()` sin `if`. `getInt(1)` lee por **posición** (la columna no tiene nombre útil); aquí sí parte en 1. `> 0` lo convierte en `boolean`.
- Se llama **solo después** de que `validar` dio `null`; así no se informa «inactivo» a quien no sabe la clave.

### Por qué `PreparedStatement` evita la inyección SQL

Versión **vulnerable** (concatenando):
```java
String sql = "SELECT ... FROM usuario WHERE nombre_usuario = '" + u + "' AND clave = '" + c + "'";
```
Si el atacante escribe en la clave `' OR '1'='1`, el SQL queda:
```sql
... WHERE nombre_usuario = 'admin' AND clave = '' OR '1'='1'
```
`'1'='1'` siempre es verdadero: devuelve **todas** las filas (lo comprobé: 3, los tres usuarios) y entra sin saber la clave. Con `PreparedStatement` el SQL y el dato viajan separados; lo escrito se trata como **texto literal de la clave**, nunca como sintaxis. Se busca una clave que sea literalmente `' OR '1'='1` y no existe → `null`. Lo comprueba `UsuarioDAOTest` con `validar("admin' -- ", ...)` y `validar("admin", "' OR '1'='1")`, ambos `null`.

---

## 4. Login en `VentanaLogin` (solo eventos)

`initComponents()` lo genera NetBeans desde el `.form`; no se edita a mano.

### Constructor

```java
public VentanaLogin() {
    initComponents();
    getContentPane().setBackground(new java.awt.Color(238, 242, 247));
    usuarioDAO = new UsuarioDAO();
    this.setLocationRelativeTo(null);
}
```
- `initComponents()` arma la interfaz; luego se pinta el fondo, se crea el DAO que usarán los eventos y se centra la ventana (`setLocationRelativeTo(null)`).

### `btnIngresarActionPerformed`

```java
String usuario = txtUsuario.getText().trim();
String clave = new String(txtClave.getPassword());
```
- `trim()` quita espacios sobrantes del usuario. La clave **no** se recorta (un espacio podría ser parte de ella). `JPasswordField.getPassword()` devuelve `char[]` (la recomendación es no usar `getText()`, deprecado para este campo, y poder borrar el arreglo después); aquí se convierte con `new String(...)` porque el DAO recibe `String`.

```java
if (usuario.isEmpty() || clave.isEmpty()) {
    JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos.", "Acceso",
            JOptionPane.WARNING_MESSAGE);
    txtUsuario.requestFocus();
    return;
}
```
- **1.º vacíos (V1).** Es lo más barato y no necesita base de datos: si falta un campo, ni se consulta MySQL. `return` corta el método. `requestFocus()` deja el cursor listo para escribir. El cuadro se titula «Acceso» con icono de advertencia (`WARNING_MESSAGE`).

```java
try {
    Usuario u = usuarioDAO.validar(usuario, clave);
```
- **2.º validar.** Todo acceso a la base va dentro del `try`, porque el DAO puede lanzar `SQLException`.

```java
if (u == null) {
    if (usuarioDAO.estaInactivo(usuario, clave)) {
        ... "Usuario inactivo." ...
    } else {
        ... "Usuario o contraseña incorrectos." ...
    }
    txtClave.setText("");
    txtUsuario.requestFocus();
    return;
}
```
- **3.º inactivo (V2).** Solo se pregunta si `validar` dio `null`: ahí hay dos causas posibles (clave mala o inactivo) y `estaInactivo` las separa. Se limpia la clave y se devuelve el foco, y `return` impide abrir la siguiente ventana.

```java
new VentanaEventos(u).setVisible(true);
this.dispose();
```
- **4.º éxito.** Se abre `VentanaEventos` pasándole el `Usuario` y se cierra el login con `dispose()` (libera la ventana sin terminar la JVM; el programa sigue vivo porque `VentanaEventos` está visible).

```java
} catch (SQLException e) {
    JOptionPane.showMessageDialog(this, Conexion.mensajeError(e), "Error de base de datos",
            JOptionPane.ERROR_MESSAGE);
}
```
- **V4.** Cualquier `SQLException` se traduce con `Conexion.mensajeError(e)` y se muestra con el título «Error de base de datos» e icono de error. Al terminar el `catch` el método acaba y la ventana sigue abierta: **la app no se cierra**, se puede reintentar al levantar MySQL (cada intento abre una conexión nueva, no hay que reiniciar la aplicación).

**Por qué este orden:** de lo más barato a lo más caro y de lo más general a lo más específico: sin consultar (vacíos) → una consulta (validar) → una segunda consulta solo si hace falta (inactivo) → recién ahí entrar.

`btnSalirActionPerformed` solo hace `System.exit(0)`. La ventana tiene `EXIT_ON_CLOSE`: cerrar con la X también termina el programa.

---

## 5. Reglas de negocio de mi parte

| Regla | Código | Mensaje |
|---|---|---|
| **V1** credenciales vacías | `usuario.isEmpty() \|\| clave.isEmpty()` en `btnIngresarActionPerformed` | «Usuario o contraseña incorrectos.» |
| **V1** credenciales incorrectas | `validar` → `null` y `estaInactivo` → `false` | «Usuario o contraseña incorrectos.» |
| **V2** usuario inactivo | `estaInactivo` (`COUNT(*) ... activo = FALSE`) → `true` | «Usuario inactivo.» |
| **V4** falla de conexión | `catch (SQLException)` + `Conexion.mensajeError` (SQLState `08`, 1045, 1049) | «No fue posible conectar con la base de datos.» |
| Solo activos ingresan | `AND activo = TRUE` en el SQL de `validar` | — |
| La clave distingue mayúsculas | columna `clave ... COLLATE utf8mb4_bin` en la tabla `usuario` | `ADMIN2026` → «Usuario o contraseña incorrectos.» |

Nota: V1 usa el mismo mensaje para vacío, usuario inexistente y clave mala a propósito: no revela cuál dato falló.

---

## 6. Demostración B1 sin apoyo

Regla de oro: **hago la acción en la app, digo en voz alta qué espero ver, y recién ahí ejecuto la consulta**. El login no escribe nada en la base (es un `SELECT`), así que la «verificación» es **correr en MySQL la misma consulta que corre `validar`** y mostrar que da lo mismo que la pantalla.

### 6.1 Guion de los 5 casos (≈ 4 minutos)

**Antes:** `docker ps` muestra `calendario-mysql` `healthy`; phpMyAdmin abierto en http://localhost:8081 con la base `calendario_app` y la pestaña **SQL** lista; `VentanaLogin` abierta.

| # | Hago en la app | Debe pasar | Verifico en phpMyAdmin / terminal | Debe dar |
|---|---|---|---|---|
| 1 | `admin` / `Admin2026` → *Ingresar* | Se abre `VentanaEventos` («Bienvenido/a Administrador (ADMIN)») | `SELECT id_usuario, nombre_usuario, rol, activo FROM usuario WHERE nombre_usuario='admin' AND clave='Admin2026' AND activo=TRUE;` | **1 fila** (`admin`, `ADMIN`, 1) |
| 2 | `admin` / `clavemala` | «Usuario o contraseña incorrectos.» y la clave se borra | La misma consulta con `clave='clavemala'` | **0 filas** |
| 3 | Campos vacíos | «Usuario o contraseña incorrectos.» (no consultó nada) | No hay consulta que verificar: **ni llegó a MySQL** | — (lo digo: es la validación antes de consultar) |
| 4 | `inactivo1` / `Inac2026` | «Usuario inactivo.» | `SELECT nombre_usuario, activo FROM usuario WHERE nombre_usuario='inactivo1' AND clave='Inac2026';` y luego `SELECT COUNT(*) FROM usuario WHERE nombre_usuario='inactivo1' AND clave='Inac2026' AND activo=FALSE;` | `activo = 0`, y el `COUNT` **1** (eso es `estaInactivo`) |
| 5 | Apago MySQL (6.3) y pulso *Ingresar* | «No fue posible conectar con la base de datos.»; la ventana **sigue abierta** | `docker ps` ya no lista `calendario-mysql` | — |

Extra de mayúsculas (si lo piden): `admin` / `ADMIN2026` → rechazado; verificación: `SELECT COUNT(*) FROM usuario WHERE nombre_usuario='admin' AND clave='ADMIN2026';` → `0`.

### 6.2 Consultas por terminal (alternativa a phpMyAdmin)

Comando 1: ver los usuarios y su estado.
```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "SELECT id_usuario, nombre_usuario, rol, activo FROM usuario"
```
- `docker exec calendario-mysql` ejecuta un comando dentro del contenedor; `mariadb -uroot` es el cliente con usuario `root` (sin clave); `calendario_app` es la base; `-e "..."` ejecuta esa sentencia y sale.
- Salida esperada: 3 filas: `admin / ADMIN / 1`, `operador1 / OPERADOR / 1`, `inactivo1 / OPERADOR / 0`. `activo` se ve como 1/0 (BOOLEAN es TINYINT).
- Si sale distinto (error de conexión o tabla inexistente): revisar `docker ps` y que el script `sql/calendario_bd.sql` se haya cargado.

Comando 2: lo que hace `validar`, para el caso correcto.
```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "SELECT id_usuario, nombre_usuario, rol, activo FROM usuario WHERE nombre_usuario='admin' AND clave='Admin2026' AND activo=TRUE"
```
- Salida esperada: 1 fila. Con `clave='clavemala'`: no imprime nada (cero filas).

Comando 3: comprobar la regla de mayúsculas.
```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "SELECT COUNT(*) FROM usuario WHERE nombre_usuario='admin' AND clave='ADMIN2026'"
```
- Salida esperada: `0`. Con `Admin2026` sale `1`. Demuestra que `utf8mb4_bin` distingue mayúsculas.

### 6.3 Mostrar V4 (MySQL caído)

Comando 1: apagar la base.
```bash
docker stop calendario-mysql
```
- Detiene el contenedor. En la app, con la ventana de login abierta, ingresar `admin` / `Admin2026`: aparece «No fue posible conectar con la base de datos.» y la ventana **sigue abierta**. (phpMyAdmin también dejará de responder: es normal, depende de la base.)
- Por dentro: `DriverManager.getConnection` lanza `CommunicationsException` (SQLState `08S01`, *Communications link failure*); `mensajeError` ve el `08` y devuelve el texto.

Comando 2: volver a encenderla.
```bash
docker start calendario-mysql
```
- Reinicia el contenedor (los datos persisten en el volumen `calendario_data`). Esperar unos segundos hasta que `docker ps` diga `(healthy)`; reintentar el login **en la misma ventana** y ahora entra. Si no entra: `docker ps` debe mostrar `calendario-mysql` como `Up`.

(Si el login ya mostró `VentanaEventos`, volver a abrirla ejecutando de nuevo `VentanaLogin`.)

### 6.4 Si algo falla en vivo

Plan B: mostrar el mismo efecto con la consulta en phpMyAdmin y explicar el método del DAO. Mantener la calma vale más que el error; decir «déjeme ver qué mensaje da» y **leer el mensaje** (sección 14).

---

## 7. Diagramas del login

### 7.1 Secuencia (texto)

```
Usuario      VentanaLogin        UsuarioDAO         Conexion        PreparedStatement     ResultSet      Usuario
  | clic Ingresar |                  |                 |                   |                 |             |
  |-------------->| (¿vacíos?  sí -> mensaje V1, fin)  |                   |                 |             |
  |               |--validar(u,c)--->|                 |                   |                 |             |
  |               |                  |--conectar()---->|                   |                 |             |
  |               |                  |<--Connection----|                   |                 |             |
  |               |                  |--prepareStatement(sql)------------->|                 |             |
  |               |                  |--setString(1,u), setString(2,c)---->|                 |             |
  |               |                  |--executeQuery()-------------------->|---------------->|             |
  |               |                  |<---------------------------------------- ResultSet ---|             |
  |               |                  |--next()---------------------------------------------->|             |
  |               |                  |   fila  --> new Usuario(getInt, getString...)---------------------->|
  |               |<----Usuario------|   sin fila --> null                 |                 |             |
  |               | (null -> estaInactivo -> V2 o V1;  Usuario -> VentanaEventos(u))        |             |
  |               | (SQLException -> Conexion.mensajeError -> V4, la app sigue abierta)     |             |
```
Revisado contra el código: la secuencia calza con `validar`. Al salir del `try-with-resources` se cierran `ResultSet`, `PreparedStatement` y `Connection` en ese orden. Si `validar` da `null`, `estaInactivo` repite el patrón completo (otra conexión, otro `PreparedStatement`) y devuelve un `boolean`.

### 7.2 Decisión del botón *Ingresar* (de arriba hacia abajo)

```
 clic en Ingresar
        |
 ¿usuario o clave vacíos? --sí--> "Usuario o contraseña incorrectos." (V1) --> FIN
        | no
 validar(usuario, clave)  --SQLException--> mensajeError --> "No fue posible conectar..." (V4) --> FIN
        |
 ¿devolvió Usuario?  --sí--> new VentanaEventos(u) + dispose() --> ENTRA
        | no (null)
 estaInactivo(usuario, clave)
        |-- true  --> "Usuario inactivo."                      (V2) --> limpia clave, FIN
        |-- false --> "Usuario o contraseña incorrectos."      (V1) --> limpia clave, FIN
```

### 7.3 Para dibujar en la pizarra: el flujo JDBC en 5 cajas

Lo dibujo de izquierda a derecha y voy diciendo el nombre del método **de mi código** en cada caja:

```
+------------+   +-----------------+   +--------------+   +----------------+   +-----------+
| 1 Connection|-->| 2 Prepared-     |-->| 3 setString  |-->| 4 executeQuery |-->| 5 close   |
| Conexion.   |   | Statement       |   | (1, usuario) |   | -> ResultSet   |   | (try-with-|
| conectar()  |   | prepareStatement|   | setString    |   | rs.next()      |   | resources)|
|             |   | (sql con ?)     |   | (2, clave)   |   | rs.getXxx(...) |   |           |
+------------+   +-----------------+   +--------------+   +----------------+   +-----------+
   la sesión        la plantilla          los datos           las filas         se cierra solo
   con MySQL        con huecos ?          (índice desde 1)    (cursor)          en orden inverso
```
Y debajo, la arquitectura en capas (4 cajas verticales):

```
VentanaLogin (vista)  --usa-->  UsuarioDAO (dao)  --usa-->  Conexion (conexion)  --JDBC-->  MariaDB
      sin SQL                   todo el SQL de usuario        URL, usuario, clave          calendario_app
                                   |
                                   +--devuelve--> Usuario (modelo)
```

---

## 8. Lo mínimo de las partes de Tais y de Nicolás

**Tais (script SQL y modelo)**
- `sql/calendario_bd.sql` crea la base y las tablas `usuario` y `evento`, con PK autoincremental, `UNIQUE`, `CHECK` y `COLLATE utf8mb4_bin` en la clave; inserta 3 usuarios y 5 eventos de prueba.
- `evento` tiene 14 columnas, entre ellas la ubicación `POINT`, un campo `JSON` (detalle), prioridad, horas y duración con restricciones.
- El paquete `modelo` (`Usuario`, `Evento`): clases con atributos privados, constructor, getters y setters; el DAO las llena con lo que lee del `ResultSet`.
- Es quien sabe explicar por qué hay restricciones en la tabla (integridad en la base, no solo en la interfaz).

**Nicolás (`EventoDAO`, CRUD, POINT/JSON)**
- `EventoDAO` hace el CRUD de eventos con `PreparedStatement`: `INSERT`, `SELECT`, `UPDATE`, `DELETE` (`executeUpdate` para los que modifican).
- `POINT(?, ?)` se escribe con (longitud, latitud) y se lee con `ST_X`/`ST_Y`.
- El `JSON` viaja como texto válido; si no lo es, el `CHECK (JSON_VALID(detalle))` falla (4025 en MariaDB, 3140/3819 en MySQL), que `mensajeError` traduce.
- `VentanaEventos` es la pantalla del CRUD, con la tabla y los formularios; recibe el `Usuario` que yo valido y lo muestra en la bienvenida («Bienvenido/a Administrador (ADMIN)»).
- V3 (registro repetido, error 1062) y las restricciones 4025/3819 se traducen en mi `mensajeError`.

**Chuleta de `EventoDAO` por si me preguntan (todos usan try-with-resources y `PreparedStatement`):**

| Código | Método | SQL | Ejecuta | Devuelve |
|---|---|---|---|---|
| C1 | `insertar(Evento)` | `INSERT ... VALUES (?, ..., POINT(?, ?), ...)` con `RETURN_GENERATED_KEYS` | `executeUpdate()` + `getGeneratedKeys()` | el `id` autogenerado (`int`) |
| C2 | `listar()` | `SELECT ... ORDER BY fecha_evento, hora_inicio` | `executeQuery()` + `while (rs.next())` | `ArrayList<Evento>` |
| C3 | `buscar(String)` | `SELECT ... WHERE codigo_evento = ?` | `executeQuery()` + `if (rs.next())` | `Evento` o `null` |
| C4 | `actualizar(Evento)` | `UPDATE ... WHERE id_evento = ?` (parámetro 14) | `executeUpdate() == 1` | `boolean` |
| C5 | `eliminar(int)` | `DELETE FROM evento WHERE id_evento = ?` | `executeUpdate() == 1` | `boolean` |

---

## 9. Quince preguntas probables

1. **¿Qué es JDBC?** API de Java (`java.sql`) para hablar con bases de datos relacionales; el driver traduce las llamadas al protocolo de MySQL.
2. **Explique el flujo JDBC.** Se abre una `Connection`, se prepara un `PreparedStatement` con el SQL, se asignan parámetros, se ejecuta (`executeQuery`) y se recorre el `ResultSet` con `next()`; al final se cierra todo.
3. **¿Qué es `Connection`?** La sesión abierta con la base. Se obtiene con `DriverManager.getConnection(URL, usuario, clave)` y debe cerrarse.
4. **¿Por qué `PreparedStatement` y no `Statement`?** Evita inyección SQL (los datos van separados del SQL y el driver los escapa como valor) y permite reutilizar la plantilla; además no hay que armar a mano comillas ni formatos de fecha.
5. **¿Qué pasa con `' OR '1'='1`?** Con concatenación entraría sin clave; con `?` se toma como texto de la clave y no coincide con nada → `null`.
6. **¿Qué hace `rs.next()`?** Avanza a la siguiente fila y devuelve `true` si existe. El cursor parte antes de la primera.
7. **¿Por qué `validar` devuelve `null`?** Una clave equivocada es un resultado esperado, no un error técnico; las excepciones se reservan para fallos de conexión o SQL.
8. **¿Para qué sirve `estaInactivo`?** Separa «inactivo» de «clave incorrecta» (V2 vs V1); se llama solo si `validar` dio `null`.
9. **¿Por qué `COUNT(*)` y `rs.next()` sin `if`?** Porque `COUNT(*)` siempre devuelve exactamente una fila.
10. **¿Qué es try-with-resources?** `try (recurso) {...}` cierra automáticamente lo que implementa `AutoCloseable`, incluso si hay excepción; evita fugas de conexiones.
11. **¿Por qué las constantes son `static final`?** Un solo lugar para editar, no se pueden alterar en ejecución y sirven desde un método `static`.
12. **¿Qué hacen `useSSL=false`, `allowPublicKeyRetrieval` y `serverTimezone`?** Sin cifrado (entorno local), permite obtener la clave pública para autenticarse con MySQL 8, y fija la zona horaria `America/Santiago`. Con MariaDB local la conexión también funciona sin ellas.
13. **¿Qué significan 1045, 1049, 1062 y 4025?** Acceso denegado, base desconocida, entrada duplicada (UNIQUE) y restricción CHECK fallida en MariaDB. SQLState `08` = error de conexión.
14. **¿Qué pasa si MySQL está caído?** `getConnection` lanza `SQLException` (SQLState `08S01`); el `catch` muestra «No fue posible conectar con la base de datos.» y la app sigue abierta (V4).
15. **¿Cómo agrega el driver en NetBeans?** Properties → Libraries → Compile → Add JAR/Folder → `lib/mysql-connector-j-26.10.0.jar`. Sin él: `No suitable driver found`.

(Extra por si preguntan: guardar la clave en texto plano es una limitación académica; en un sistema real se guardaría un hash con sal, por ejemplo bcrypt, y se compararía el hash. Detalle en la trampa T9.)

---

## 10. Tres ganchos mnemotécnicos

**1. El orden del flujo JDBC: «C-P-S-R-C» → «Con Pan Sale Rico Café»**
**C**onnection → **P**reparedStatement → **S**et (setString) → **R**esultSet (executeQuery + next) → **C**lose (try-with-resources).
- *Autoevaluación:* ¿Qué método llena el primer `?` y desde qué índice parte? (`setString(1, ...)`, desde 1.)

**2. El orden de las comprobaciones del login: «V-V-I-A» → «Vacío, Validar, Inactivo, Abrir»**
Imagina un guardia de discoteca: mira si traes algo (vacío), revisa tu cédula (validar), mira si estás vetado (inactivo) y recién ahí abre la puerta (abrir `VentanaEventos`).
- *Autoevaluación:* ¿Por qué `estaInactivo` va después de `validar` y no antes? (Solo hay que separar clave mala de inactivo cuando `validar` dio `null`; así no se revela nada a quien no sabe la clave.)

**3. Los códigos de `mensajeError`: «08 = Se Cayó el cable; 1045 = Puerta con clave; 1049 = Casa que no existe; 1062 = Gemelo repetido»**
Imagen absurda: un cable (08) que se desconecta, una puerta con candado (1045), una casa sin dirección (1049) y dos gemelos idénticos en la fila (1062, duplicado).
- *Autoevaluación:* ¿Cuál de estos códigos corresponde a «Unknown database» y cuál a una violación de `UNIQUE`? (1049 y 1062.)

---

## 11. «Explícalo con tus palabras» (guiones de 30 a 60 segundos)

Se dicen **sin leer**, mirando a quien pregunta. Cada guion tiene una frase de arranque, el cuerpo y un ejemplo **del proyecto**. Los practico en voz alta con cronómetro: ~110 palabras = 45 s.

### G1. ¿Qué es JDBC y qué es un driver?
«JDBC es la API estándar de Java, el paquete `java.sql`, para hablar con bases de datos relacionales. Define *interfaces*: `Connection`, `PreparedStatement`, `ResultSet`. Pero una interfaz no sabe hablar con ninguna base en particular. Eso lo hace el **driver**: una librería, en nuestro caso `mysql-connector-j-26.10.0.jar`, que implementa esas interfaces y traduce mis llamadas al protocolo de red de MySQL/MariaDB. Por eso mi código solo usa tipos de `java.sql` y el único lugar que sabe que es MySQL es la URL `jdbc:mysql://...` en `Conexion`. Si cambiara de motor, cambiaría el jar y la URL. El jar está en `lib/` y en las *Libraries* del proyecto; sin él sale `No suitable driver`.»

### G2. Connection → PreparedStatement → ResultSet
«Es una cadena de tres objetos. Primero la `Connection`: la sesión abierta con la base, que me da `Conexion.conectar()`. Con ella creo un `PreparedStatement` pasándole el SQL con signos `?` donde irán los datos. Luego relleno cada `?` con `setString(1, ...)`, `setString(2, ...)`; el índice parte en 1. Después ejecuto: `executeQuery()` si es un `SELECT`, y me devuelve un `ResultSet`, que es un cursor que parte antes de la primera fila. Con `rs.next()` avanzo y con `getInt`, `getString` leo las columnas. Para `INSERT`, `UPDATE` o `DELETE` uso `executeUpdate()`, que devuelve cuántas filas cambió. Y todo se cierra solo con try-with-resources.»

### G3. ¿Por qué la conexión está centralizada en `Conexion`?
«Porque los datos de acceso, URL, usuario y clave, deben estar en **un solo lugar**. Si estuvieran repetidos en `UsuarioDAO` y en `EventoDAO`, al cambiar el servidor o la clave tendría que acordarme de editar cada uno, y es fácil dejar uno desactualizado. Con `Conexion`, los DAO solo hacen `Conexion.conectar()` y no saben nada de la URL. Las constantes son `private static final`: nadie las toca desde afuera. Además ahí también quedó `mensajeError`, que traduce cualquier `SQLException` a un mensaje para el usuario, así las ventanas no repiten ese `if` de códigos. Es separación de responsabilidades: la ventana no escribe SQL, el DAO no sabe de la URL.»

### G4. ¿Qué hace try-with-resources?
«Es un `try` con paréntesis donde declaro recursos que implementan `AutoCloseable`: `Connection`, `PreparedStatement`, `ResultSet`. Cuando el bloque termina, **sea por salida normal, por `return` o por excepción**, Java llama `close()` en cada uno, en orden inverso a como se abrieron: primero el `ResultSet`, luego el `PreparedStatement` y al final la `Connection`. Reemplaza al `finally` con `close()` a mano, que es largo y fácil de olvidar. Si olvidara cerrar, cada login dejaría una conexión abierta en el servidor hasta agotar `max_connections`, que en MariaDB es 151. En `validar` lo ven dos veces: uno para `con` y `ps`, otro anidado para `rs`.»

### G5. ¿Por qué `PreparedStatement` y no concatenar?
«Por seguridad y por orden. Si concateno el usuario y la clave dentro del texto SQL, quien escribe `' OR '1'='1` en la clave cambia la **estructura** de la consulta y entra sin saber nada: eso es inyección SQL. Con `PreparedStatement` el SQL lleva `?` y los valores se pasan aparte con `setString`: el driver los trata como **datos**, los escapa, y nunca se interpretan como código. Lo probé: con concatenación esa clave devuelve las tres filas de `usuario`; con `?` devuelve cero. También evita errores de sintaxis si el dato trae una comilla, como en `O'Brien`. Y me evita armar fechas y números como texto. Está comprobado en `UsuarioDAOTest.testInyeccionSqlNoEntra`.»

### G6. El login de punta a punta
«El usuario escribe y pulsa *Ingresar*. En `btnIngresarActionPerformed` leo el usuario con `trim()` y la clave del `JPasswordField`. **Primero** reviso que no estén vacíos: si lo están, muestro el mensaje de V1 y ni consulto la base. **Segundo**, llamo a `usuarioDAO.validar`, que abre una conexión, prepara el `SELECT ... WHERE nombre_usuario = ? AND clave = ? AND activo = TRUE`, y devuelve un `Usuario` si hay fila o `null` si no. **Tercero**, si fue `null`, llamo a `estaInactivo` para saber si el motivo era que el usuario está desactivado: mensaje de V2; si no, clave incorrecta: V1. **Cuarto**, si vino un `Usuario`, abro `VentanaEventos` pasándoselo y cierro el login con `dispose()`. Todo está en un `try`: si hay `SQLException`, `mensajeError` la traduce, la muestro y la aplicación sigue abierta: eso es V4.»

---

## 12. Preguntas trampa probables, con respuesta modelo

Primero **respondo directo** (1 frase), luego **lo ligo al código**. Marco con ⚠ lo que conviene reconocer con honestidad.

**T1. ¿Por qué no usó `Class.forName("com.mysql.cj.jdbc.Driver")`?**
Porque desde JDBC 4.0 no hace falta: el jar del driver trae `META-INF/services/java.sql.Driver` con el nombre de su clase y `DriverManager` lo registra solo al arrancar (mecanismo `ServiceLoader`). `Class.forName` también funcionaría, y lo probé con el jar presente, pero sería redundante. Si el jar no está en el classpath, `Class.forName` lanzaría `ClassNotFoundException` y `getConnection` daría `No suitable driver found`; el problema es el jar, no esa línea.

**T2. ¿Qué pasa si olvido cerrar la conexión?**
Queda abierta en el servidor (**fuga**). Cada `validar` abre una; si no se cierran, se acumulan hasta `max_connections` (151 en este MariaDB) y las siguientes fallan con error 1040 *Too many connections*; mientras tanto consumen memoria y se pueden perder cambios sin `commit`. Por eso todo va en try-with-resources, que cierra aunque haya excepción.

**T3. ¿`executeQuery` vs `executeUpdate`: qué devuelve cada uno?**
`executeQuery()` es para `SELECT` y devuelve un `ResultSet` (nunca `null`, puede venir vacío). `executeUpdate()` es para `INSERT`, `UPDATE`, `DELETE` (y DDL) y devuelve un `int`: las filas afectadas (0 en DDL). Si uso el equivocado, el driver lanza `SQLException`. Existe un tercero, `execute()`, que devuelve `boolean` (true si el resultado es un `ResultSet`). En mi parte solo uso `executeQuery`, tanto en `validar` como en `estaInactivo`.

**T4. ¿Qué es SQLState y por qué `08xxx` es conexión?**
Es un código de 5 caracteres del **estándar SQL**, independiente del motor: los 2 primeros son la *clase* y los 3 siguientes la *subclase*. La clase `08` es «connection exception»: `08001` no se pudo abrir la conexión, `08003` la conexión está cerrada, `08S01` falló el enlace de comunicación (la `S` indica subclase definida por el fabricante). Por eso en `mensajeError` uso `startsWith("08")` y cubro toda la familia. ⚠ El 1045 tiene SQLState `28000` y el 1049 `42000`, no `08`: por eso además comparo el **código** propio (`getErrorCode()`).

**T5. ¿Qué pasa si la clave tiene una comilla `'`?**
Con mi código, nada especial: el `?` hace que el driver escape la comilla y la trate como un carácter más; la clave `O'Brien` se busca tal cual. Con concatenación, la comilla cerraría el texto antes de tiempo y el servidor respondería `ERROR 1064 (42000): You have an error in your SQL syntax ... near 'b'' at line 1` (lo reproduje con la clave `a'b`). Y peor: con una comilla bien puesta se puede inyectar SQL.

**T6. ¿Por qué `validar` devuelve `null` y no lanza una excepción?**
Porque una clave incorrecta **no es un error del sistema**, es un resultado esperado del negocio; usar excepciones para el flujo normal es mala práctica. Las `SQLException` quedan para lo realmente excepcional (base caída, SQL mal escrito). La ventana distingue los dos casos: `null` → mensaje de credenciales; `catch` → mensaje de conexión. ⚠ Contrapartida: el que llama **debe** comprobar `null`; si no lo hace, aparece un `NullPointerException` (ver sección 14).

**T7. ¿Por qué existe `estaInactivo` aparte?**
Porque `validar` filtra `activo = TRUE` y por eso un inactivo se ve igual que una clave mala: cero filas. Para dar el mensaje específico de V2 («Usuario inactivo.») hace falta preguntar por separado. Se llama **después** y solo si `validar` dio `null`, para no decirle «está inactivo» a alguien que no conoce la clave. ⚠ Cuesta una segunda consulta (y una segunda conexión) solo en el caso de fallo; es un trade-off aceptable. Otra forma habría sido un solo `SELECT` sin el filtro `activo` y decidir en Java, pero se pidió dejar L1 y V2 como métodos distintos.

**T8. ¿Por qué `ADMIN2026` no entra? (collation)**
Porque la columna `clave` se declaró `COLLATE utf8mb4_bin`: compara **byte a byte** y distingue mayúsculas de minúsculas. Las collations `_ci` (*case insensitive*), como `utf8mb4_spanish_ci` de la tabla, no las distinguen, y con ellas `ADMIN2026` entraba como `Admin2026` (lo detectamos y se corrigió). ⚠ Dos detalles que **medí** y conviene saber: (1) `nombre_usuario` sigue con la collation `_ci` de la tabla, así que `ADMIN` **sí** encuentra al usuario `admin` (la clave sigue exigiendo mayúsculas exactas); (2) MariaDB 10.6 ignora los espacios finales al comparar, incluso con `_bin` (`PAD SPACE`): `Admin2026 ` con un espacio al final también entra. Es un detalle del motor, no del código; si quisiéramos exactitud total, se agregaría `COLLATE utf8mb4_bin` a `nombre_usuario` y se compararía `BINARY`/longitud.

**T9. ¿Por qué la clave está en texto plano y qué haría en producción?**
Porque la pauta entrega el script con las claves así (`'Admin2026'`) y el alcance es académico. En producción **nunca** se guarda la clave: se guarda un **hash con sal** generado por un algoritmo lento y pensado para claves (BCrypt, Argon2). Al registrar: `hash = BCrypt.hashpw(clave, BCrypt.gensalt())`. Al entrar: el `SELECT` busca solo por `nombre_usuario` (ya no puede comparar `clave = ?` en SQL), se lee el hash y en Java se verifica con `BCrypt.checkpw(claveEscrita, hash)`. Así, aunque roben la base, no ven las claves. Además la columna sería más larga (un hash BCrypt mide 60 caracteres) y se agregaría control de intentos. Es una librería externa (p. ej. jBCrypt), que no usamos aquí.

**T10. ¿Qué es `useSSL=false` y qué es `serverTimezone`?**
`useSSL=false`: la conexión viaja **sin cifrar**; aceptable en `localhost` (el puerto 3306 solo escucha en `127.0.0.1`), inaceptable por Internet. `serverTimezone=America/Santiago`: le dice al driver qué zona horaria usar para convertir `DATETIME`/`TIMESTAMP`; sin ella, con algunos servidores el driver falla (`The server time zone value ... is unrecognized`) o corre las horas. ⚠ Lo probé: con MariaDB local la URL **sin** esas opciones conecta igual; las dejamos como prevención.

**T11. ¿Dónde está el driver y cómo se agrega en NetBeans?**
En `CalendarioApp/lib/mysql-connector-j-26.10.0.jar`. En NetBeans: clic derecho al proyecto → Properties → Libraries → Compile → *Add JAR/Folder* → el jar. Queda en `nbproject/project.properties` como `javac.classpath=${file.reference.mysql-connector-j-26.10.0.jar}` con ruta relativa `lib/...`. Si falta, el síntoma es `No suitable driver found for jdbc:mysql://...` y la clase no compila si la referencia está rota.

**T12. ¿`probarConexion` se usa?**
⚠ Honestamente: existe y la cubre `ConexionTest.testProbarConexion`, pero el login **no** la llama. Detecta la caída porque `validar` lanza `SQLException` y el `catch` la traduce con `mensajeError`. Se dejó como utilidad por si una ventana quisiera avisar antes de intentar. No la defiendo como parte del flujo: la explico como utilidad probada.

**T13. ¿Por qué cada método abre su propia conexión? ¿No es lento?**
Es simple y seguro: cada operación abre y cierra, no hay conexiones colgadas ni estado compartido. En una app de escritorio con un usuario es suficiente. En un sistema con muchos usuarios se usaría un **pool de conexiones** (p. ej. HikariCP) que las reutiliza.

**T14. ¿Dónde se captura la excepción y por qué ahí?**
En la ventana, no en el DAO. El DAO declara `throws SQLException` y la propaga; la ventana es la que sabe **qué mostrarle al usuario** (un `JOptionPane`). Si el DAO la tragara, la ventana no podría reaccionar.

**T15. ¿Qué pasa con la pantalla mientras consulta?**
La consulta corre en el hilo de eventos de Swing (EDT), así que la ventana se congela lo que tarde la base. Con la base local son milisegundos; en una app grande se usaría `SwingWorker` en segundo plano.

**T16. ¿MySQL o MariaDB? ¿Por qué `jdbc:mysql`?**
El servidor del contenedor es **MariaDB 10.6**, que habla el protocolo de MySQL y es compatible con el driver `mysql-connector-j`; por eso la URL empieza con `jdbc:mysql:`. El script también corre en MySQL 8.0.16+ (los `CHECK` se aplican desde esa versión). Con MySQL 8 la autenticación es `caching_sha2_password`, que es donde entra `allowPublicKeyRetrieval=true`.

**T17. ¿Qué pasa si hubiera dos usuarios con el mismo nombre?**
No puede: `nombre_usuario` es `UNIQUE` (el segundo `INSERT` daría error 1062). Por eso `validar` usa `if (rs.next())` y no `while`.

**T18. ¿Por qué `getInt(1)` en `estaInactivo` y `getInt("id_usuario")` en `validar`?**
Porque `COUNT(*)` no tiene un nombre cómodo de columna, así que leo por **posición** (parte en 1); en `validar` hay nombres claros y leer por nombre es más legible y no se rompe si cambia el orden de las columnas.

---

## 13. «Modificar en vivo»: soluciones exactas

El profesor puede pedirme cambiar el código en vivo. Regla: **leer el pedido, ubicar el método, cambiar lo mínimo, guardar y probar**. Las seis soluciones de abajo las **compilé** (JDK 21) y las **probé en ejecución** contra MariaDB sobre una copia del proyecto, sin tocar el original. En NetBeans: `F6` ejecuta; si el archivo es el de login, ejecutar `VentanaLogin` (clic derecho → *Run File*, `Shift+F6`).

Importante: **nunca edito `initComponents()`** (es código generado, fondo gris). Todo va en `btnIngresarActionPerformed`, en el constructor o en los campos de la clase.

### (a) Cambiar el mensaje de V1

**Dónde:** `VentanaLogin.java`, `btnIngresarActionPerformed`. El texto aparece **dos veces** (campos vacíos y credenciales incorrectas): `Ctrl+F` buscar `incorrectos` y cambiar donde corresponda. Para el pedido «cambiar el mensaje de V1» cambio **las dos**, para que sea coherente:

```java
JOptionPane.showMessageDialog(this, "Credenciales inválidas.", "Acceso",
        JOptionPane.WARNING_MESSAGE);
```
Si solo piden el de campos vacíos, puede ser uno distinto (lo probé así):
```java
JOptionPane.showMessageDialog(this, "Debe ingresar usuario y contraseña.", "Acceso",
        JOptionPane.WARNING_MESSAGE);
```
**Resultado:** con campos vacíos aparece el texto nuevo. **Qué decir:** «es un `String` literal pasado a `JOptionPane.showMessageDialog`; el segundo argumento después de la ventana es el mensaje, el tercero el título y el cuarto el icono». Aviso: al cambiarlo, la fila de V1 de la sección 5 y la prueba manual cambian de texto.

### (b) Validar un largo máximo del usuario en el login

El tope sale del script: `nombre_usuario VARCHAR(30)`. Va **después** de la comprobación de vacíos y **antes** del `try` (no necesita base de datos):

```java
if (usuario.length() > 30) {
    JOptionPane.showMessageDialog(this, "El usuario no puede superar los 30 caracteres.", "Acceso",
            JOptionPane.WARNING_MESSAGE);
    txtUsuario.requestFocus();
    return;
}
```
**Probar:** escribir 31 letras en usuario y cualquier clave → aparece el mensaje y no se consulta la base (lo comprobé: da ese mensaje y el botón sigue habilitado). **Qué decir:** «`length()` cuenta caracteres; lo valido antes de ir a la base porque es barato, igual que los vacíos; coincide con el `VARCHAR(30)` de la tabla».

### (c) Contar intentos fallidos y bloquear tras 3 (solo en memoria)

Paso 1: agregar el campo junto a `usuarioDAO` (fuera del código generado):
```java
private UsuarioDAO usuarioDAO;
private int intentosFallidos = 0;
```
Paso 2: en `btnIngresarActionPerformed`, dentro de `if (u == null) { ... }`, **después** de `txtClave.setText("");` y **antes** de `txtUsuario.requestFocus();`:
```java
txtClave.setText("");
intentosFallidos++;
if (intentosFallidos >= 3) {
    JOptionPane.showMessageDialog(this, "Superó los 3 intentos. Acceso bloqueado.", "Acceso",
            JOptionPane.ERROR_MESSAGE);
    btnIngresar.setEnabled(false);
    txtUsuario.setEnabled(false);
    txtClave.setEnabled(false);
    return;
}
txtUsuario.requestFocus();
return;
```
**Probar:** tres veces `admin` / `mala` → al tercero sale el mensaje y los campos y el botón quedan deshabilitados (lo comprobé: el tercer intento muestra primero «Usuario o contraseña incorrectos.» y luego el bloqueo, con el botón deshabilitado). **Qué decir:** «el contador es un campo de la ventana, vive solo en memoria: si cierran y abren la ventana, se reinicia. En un sistema real el bloqueo se guarda en la base (columna de intentos o `bloqueado_hasta`) porque en memoria se puede saltar reabriendo la app». Cuenta también los intentos con un usuario inactivo porque están dentro del mismo `if (u == null)`; si no lo quieren, se mueve el contador al `else`.

### (d) Escribir desde cero `UsuarioDAO.contarActivos()`

Al final de la clase `UsuarioDAO` (antes de la última llave):
```java
public int contarActivos() throws SQLException {
    String sql = "SELECT COUNT(*) FROM usuario WHERE activo = ?";
    try (Connection con = Conexion.conectar();
            PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setBoolean(1, true);
        try (ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
```
**Probar:** `System.out.println(new UsuarioDAO().contarActivos());` imprime **2**. Verificación en MySQL: `SELECT COUNT(*) FROM usuario WHERE activo = TRUE;` → `2`. Si el profesor prefiere sin parámetro, vale `... WHERE activo = TRUE` y se omite el `setBoolean`: el `PreparedStatement` sin `?` también es válido. **Mi plantilla de 4 frases:** (1) qué SQL: cuenta los activos; (2) un `?` relleno con `setBoolean(1, true)`; (3) `executeQuery()` porque es `SELECT`, devuelve `ResultSet` con una sola fila, por eso `rs.next()` sin `if`; (4) los tres recursos se cierran solos. Los imports ya existen en el archivo (`Connection`, `PreparedStatement`, `ResultSet`, `SQLException`).

### (e) Cambiar la clave de la BD en `Conexion` y qué se ve si es incorrecta

En `Conexion.java`:
```java
private static final String CLAVE = "otra";
```
**Qué pasa:** el servidor responde `Access denied for user 'root'@'127.0.0.1' (using password: YES)` (error **1045**, SQLState `28000`) → `mensajeError` devuelve «No fue posible conectar con la base de datos.» (V4) y el título del cuadro es «Error de base de datos». También `probarConexion()` devuelve `false` y `ConexionTest.testProbarConexion` fallaría. (Lo probé con `"otra"`.) **Después, restaurar `CLAVE = ""`** (la del contenedor, sin clave) y volver a ejecutar. **Qué decir:** «la clave de `Conexion` es la del usuario **root de la base**, no la del login de la aplicación; por eso el error es de conexión y no de credenciales de la ventana». Si en cambio cambian el nombre de la base en la URL (p. ej. `calendario_bd`), el error es **1049** *Unknown database* y el mensaje es el mismo.

### (f) Mostrar el rol en el título de la ventana

El rol llega en el objeto `Usuario` que recibe `VentanaEventos`. En su constructor, **después** de `this.usuario = usuario;`:
```java
this.usuario = usuario;
setTitle("Calendario de eventos - " + usuario.getRol());
```
**Probar:** entrar como `operador1` / `Oper2026` → la barra de título dice «Calendario de eventos - OPERADOR» (lo comprobé en ejecución). Va en el constructor y no en `initComponents()` porque ahí NetBeans ya pone `setTitle("Calendario de eventos")` y el constructor corre después y lo sobrescribe. **Qué decir:** «`getRol()` es el getter del modelo `Usuario`; el login se lo pasa por el constructor». Si piden el título en el **login**, no se puede: en ese momento aún no se sabe el rol.

---

## 14. «Leer un error»: qué significa y cómo lo resuelvo

Cómo leer una traza: la **primera línea** dice la excepción y el mensaje; el primer `at` de **mis** paquetes (`dao`, `vista`, `conexion`) dice dónde; la línea `Caused by:` dice la **causa raíz** (la que importa).

### 14.1 Los ocho mensajes que tengo que reconocer

| # | Mensaje (como lo ve el programador) | Qué significa | Cómo lo resuelvo |
|---|---|---|---|
| 1 | `CommunicationsException: Communications link failure` (SQLState `08S01`; causa: `ConnectException: Connection refused`) | El driver no logró hablar con el servidor: MySQL apagado, puerto 3306 equivocado o bloqueado. Es el V4. | `docker ps` → si no está, `docker start calendario-mysql`; revisar el puerto en `URL`; que no lo ocupe XAMPP. |
| 2 | `Access denied for user 'root'@'127.0.0.1' (using password: YES)` (**1045**, SQLState `28000`) | El servidor está vivo pero rechazó el usuario o la clave **de la base**. | Revisar `USUARIO` y `CLAVE` en `Conexion` (aquí `root` y `""`). |
| 3 | `Unknown database 'calendario_bd'` (**1049**, SQLState `42000`) | Conectó, pero la base de la URL no existe (nombre mal escrito o script sin cargar). | Corregir el nombre en `URL` (`calendario_app`) o ejecutar `sql/calendario_bd.sql`. |
| 4 | `No suitable driver found for jdbc:mysql://localhost:3306/...` (SQLState `08001`) | `DriverManager` no encontró ningún driver para ese prefijo: falta el `.jar` en el classpath o la URL está mal escrita. | Agregar `lib/mysql-connector-j-26.10.0.jar` en Libraries; revisar que la URL empiece con `jdbc:mysql:`. |
| 5 | `Duplicate entry 'EVT-0001' for key 'codigo_evento'` (**1062**, SQLState `23000`) | Violación de `UNIQUE`: ya existe una fila con ese valor. Es la V3. | Usar otro código o actualizar la fila existente; `mensajeError` muestra «El registro ya existe.». |
| 6 | `NullPointerException: Cannot invoke "modelo.Usuario.getNombreCompleto()" because "usuario" is null` en `VentanaEventos.<init>` | Se usó una variable que vale `null`. En el login pasa si alguien quita el `if (u == null)` y abre `VentanaEventos(u)` con un `u` nulo (clave mala). | Subir por la traza hasta mi línea; comprobar `null` **antes** de usar el objeto, como hace `btnIngresarActionPerformed`. (Si fuera `"this.usuarioDAO" is null`: faltó `usuarioDAO = new UsuarioDAO();` en el constructor.) |
| 7 | `java.lang.ClassNotFoundException: com.mysql.cj.jdbc.Driver` | Se pidió la clase del driver por nombre (`Class.forName`) y no está en el classpath. | Mismo remedio que el 4: agregar el jar a Libraries; y no hace falta `Class.forName`. |
| 8 | `Public Key Retrieval is not allowed` | **MySQL 8** con `caching_sha2_password` y sin SSL: el driver no puede pedir la clave pública. | Agregar `allowPublicKeyRetrieval=true` a la URL (ya está), o usar SSL. ⚠ No lo pude reproducir: MariaDB, que es mi servidor, no lo produce; lo explico desde la causa. |

(Los mensajes 1 a 5 y el 7 los obtuve ejecutando el código contra el contenedor; el 6 con el formato de mensajes de NPE de Java 21; el 8 es teórico.)

### 14.2 Cinco más, por si acaso

| Mensaje | Qué significa | Cómo lo resuelvo |
|---|---|---|
| `You have an error in your SQL syntax; ... near 'FORM usuario' at line 1` (**1064**, `42000`) | SQL mal escrito (aquí `FORM` por `FROM`). Es lo que el profesor le puso a Nicolás. | El mensaje trae el **fragmento justo después del error** (`near '...'`): ahí está la falla. Revisar la cadena `sql`. |
| `Table 'calendario_app.usuarios' doesn't exist` (**1146**, `42S02`) | Nombre de tabla mal escrito o script no cargado. | Comparar con `SHOW TABLES;`. |
| `No value specified for parameter 1` (`07001`) | Hay un `?` al que nunca se le hizo `setXxx`. | Contar los `?` y que haya un `setXxx` por cada índice (desde 1). |
| `Column 'nombre' not found.` (`42S22`) | Se leyó con `getString("nombre")` una columna que el `SELECT` no trae. | Usar el nombre exacto (`nombre_usuario`) o revisar la lista del `SELECT`. |
| `Before start of result set` | Se leyó el `ResultSet` sin llamar antes a `rs.next()` (el cursor parte antes de la primera fila). | Poner `rs.next()` antes del primer `getXxx`. |

Relacionado: `No operations allowed after connection closed.` (`08003`) = se usó una `Connection` ya cerrada (p. ej. fuera del try-with-resources).

---

## 15. «Si no sé algo»: cómo razono en voz alta sin inventar

Un 7 no exige saberlo todo: exige **no inventar** y mostrar que entiendo mi código. Estrategia en 5 pasos:

1. **Ganar tiempo con la pregunta:** repetirla con mis palabras («entonces me pregunta qué pasa si…»). Si no la entendí, pedir que la reformule: no es debilidad.
2. **Ubicarla en el código:** «lo más cercano que tengo es `validar`; ahí el SQL es…, el `?` se llena con…». Abrir el archivo y **mostrar la línea**.
3. **Razonar desde lo que sé:** «no recuerdo el nombre exacto, pero sé que el cursor del `ResultSet` parte antes de la primera fila, así que el método que lo avanza tiene que ser `next()`». Separar claramente **lo que sé**, **lo que deduzco** y **lo que no sé**.
4. **Decir qué haría para averiguarlo:** «lo probaría con una consulta en phpMyAdmin / miraría el mensaje de la excepción / lo buscaría en la documentación de `java.sql`». Eso es lo que hace un programador de verdad.
5. **Cerrar honesto:** «eso no lo tengo seguro; lo que sí puedo asegurar es…». **Nunca** inventar un nombre de método o un código de error: el profesor busca justamente eso (es lo que lo hace sospechar de ChatGPT).

Frases útiles:
- «Esa parte la hizo Tais/Nicolás, pero por lo que veo en el código, el método hace…»
- «No me acuerdo del número exacto del error; sé que es de la familia de conexión porque empieza con 08.»
- «Si me permite ejecutarlo, lo compruebo y le muestro.»

Si no puedo ejecutar o la app falla: **leo el mensaje en voz alta** (sección 14), digo qué significa y qué revisaría. Una falla explicada vale más que una demo muda.

---

## 16. Autoevaluación final (25 preguntas cortas)

Las contesto **en voz alta, sin mirar**; recién después reviso el anexo de respuestas. Marco las que fallé y las repito al día siguiente (repaso espaciado).

1. ¿Qué es JDBC y qué es un driver?
2. ¿Cuáles son los tres objetos del flujo JDBC y en qué orden se usan?
3. ¿Qué devuelve `DriverManager.getConnection`?
4. ¿Qué significa cada pieza de la URL `jdbc:mysql://localhost:3306/calendario_app?...`?
5. ¿Por qué las constantes de `Conexion` son `private static final`?
6. ¿Qué hace `probarConexion` y quién la usa?
7. ¿Qué diferencia hay entre `getSQLState()` y `getErrorCode()`?
8. ¿Qué mensaje da `mensajeError` para los códigos 1045, 1049 y SQLState `08S01`?
9. ¿Qué error es 1062 y en qué columna del proyecto ocurre?
10. ¿Por qué `validar` usa `PreparedStatement`?
11. ¿Desde qué número parten los índices de `setString`?
12. ¿Qué devuelve `executeQuery()` y qué devuelve `executeUpdate()`?
13. ¿Por qué `validar` usa `if (rs.next())` y no `while`?
14. ¿Por qué `validar` devuelve `null` y no lanza excepción?
15. ¿Qué hace `estaInactivo` y cuándo se llama?
16. ¿Por qué `estaInactivo` hace `rs.next()` sin `if`?
17. ¿Qué es try-with-resources y en qué orden cierra?
18. ¿Qué pasa si olvido cerrar la conexión?
19. ¿Cuál es el orden de las comprobaciones en `btnIngresarActionPerformed` y por qué?
20. ¿Por qué `ADMIN2026` no entra con la clave `Admin2026`?
21. ¿Qué pasa en el login si MySQL está caído y por qué la app no se cierra?
22. ¿Por qué `JPasswordField.getPassword()` devuelve `char[]` y no `String`?
23. ¿Qué clave y qué usuario usa `Conexion` y por qué es aceptable solo en el laboratorio?
24. ¿Qué haría en producción con las claves de la tabla `usuario`?
25. ¿Qué error obtendría con `CLAVE = "otra"` y qué mensaje ve el usuario?

---

## Anexo: respuestas de la autoevaluación

1. **JDBC** es la API de Java (`java.sql`) para bases relacionales. El **driver** (`mysql-connector-j-26.10.0.jar`) implementa esas interfaces y traduce las llamadas al protocolo de MySQL/MariaDB.
2. `Connection` → `PreparedStatement` → `ResultSet` (con el `setXxx` en medio y el cierre al final: C-P-S-R-C).
3. Una `Connection`: la sesión abierta con la base. Hay que cerrarla.
4. `jdbc:mysql:` = protocolo/driver; `localhost:3306` = servidor y puerto; `/calendario_app` = base; `useSSL=false` = sin cifrado (local); `allowPublicKeyRetrieval=true` = permite pedir la clave pública (MySQL 8); `serverTimezone=America/Santiago` = zona horaria para las fechas.
5. `private` para que nadie toque los datos de acceso; `static` para usarlas desde `conectar()` sin instanciar; `final` para que sean constantes y se cambien en un solo lugar.
6. Abre una conexión y devuelve `con.isValid(2)`; `false` si hay `SQLException`. Solo la usa `ConexionTest`; el login no la llama.
7. `getSQLState()` es el código estándar de 5 caracteres (p. ej. `08S01`); `getErrorCode()` es el número propio del motor (p. ej. 1045).
8. Los tres dan «No fue posible conectar con la base de datos.» (1045 por el código, 1049 por el código y `08S01` por empezar con `08`).
9. *Duplicate entry*, violación de `UNIQUE`; en `codigo_evento` (p. ej. repetir `EVT-0001`) o en `nombre_usuario`. Mensaje: «El registro ya existe.».
10. Para que los datos viajen separados del SQL y no se interpreten como código: evita la inyección SQL (`' OR '1'='1`).
11. Desde **1**.
12. `executeQuery()` → `ResultSet` (para `SELECT`); `executeUpdate()` → `int` con las filas afectadas (para `INSERT/UPDATE/DELETE`).
13. Porque `nombre_usuario` es `UNIQUE`: hay a lo más una fila.
14. Porque una clave equivocada es un resultado normal del negocio, no un fallo técnico; las excepciones se reservan para fallos reales.
15. Cuenta filas con usuario y clave correctos y `activo = FALSE`; se llama **solo si `validar` dio `null`**, para dar el mensaje «Usuario inactivo.».
16. Porque `COUNT(*)` siempre devuelve exactamente una fila.
17. Un `try (recursos) {...}` que cierra los `AutoCloseable` al salir, aunque haya excepción; los cierra en **orden inverso** a como se abrieron (`ResultSet`, `PreparedStatement`, `Connection`).
18. Queda una conexión abierta en el servidor (fuga); al acumularse se llega a `max_connections` (151) y fallan las siguientes con «Too many connections» (1040).
19. Vacíos → `validar` → `estaInactivo` (si fue `null`) → abrir `VentanaEventos`. Va de lo más barato a lo más caro y de lo general a lo específico (V-V-I-A).
20. Porque `clave` es `COLLATE utf8mb4_bin`: compara byte a byte y distingue mayúsculas.
21. `getConnection` lanza `SQLException` (SQLState `08S01`); el `catch` muestra el mensaje de `mensajeError` y el método termina; la ventana sigue abierta y se puede reintentar al levantar MySQL.
22. Porque un `char[]` se puede borrar de la memoria después de usarlo; un `String` es inmutable y queda hasta que lo recoja el *garbage collector*. (Aquí se convierte a `String` porque el DAO lo recibe así.)
23. `root` y clave vacía (`""`), como en el contenedor de pruebas. En producción se usaría un usuario con permisos mínimos y la clave fuera del código.
24. Guardar un **hash con sal** (BCrypt/Argon2), comparar con `checkpw` en Java y no guardar nunca la clave en texto plano.
25. El servidor responde `Access denied for user 'root'@... (using password: YES)` (error 1045, SQLState `28000`); el usuario ve «No fue posible conectar con la base de datos.» con el título «Error de base de datos».
