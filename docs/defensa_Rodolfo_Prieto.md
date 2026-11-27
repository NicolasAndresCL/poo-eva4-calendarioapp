# Defensa individual — Rodolfo Prieto

**Evaluación 4 · Fundamentos de POO (IEI-085) · CalendarioApp · Defensa: lunes 30 de noviembre de 2026**

Mi parte: **`Conexion`**, **`UsuarioDAO`** (L1 `validar`, `estaInactivo`) y la **ventana de login** con las validaciones **V1, V2 y V4**.

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
| `//localhost:3306` | Servidor y puerto. En el proyecto, MySQL corre en el contenedor `poo-mysql` que comparte red con NetBeans, por eso `localhost` funciona. |
| `/calendario_app` | Base de datos con la que se trabaja. Si no existe, error 1049. |
| `useSSL=false` | No cifra la conexión. Es entorno local de desarrollo; sin esto el driver intenta TLS y puede fallar. |
| `allowPublicKeyRetrieval=true` | Permite al driver pedir la clave pública del servidor para autenticación `caching_sha2_password`/`sha256`; evita el error «Public Key Retrieval is not allowed». |
| `serverTimezone=America/Santiago` | Zona horaria con que se interpretan `DATETIME`/`TIMESTAMP`. Sin ella el driver puede fallar o correr las horas. |

El `+` solo parte el texto en dos líneas; es un único `String`.

```java
private static final String USUARIO = "root";
private static final String CLAVE = "";
```
- Usuario `root` sin clave: es lo que configuramos en el contenedor de pruebas (`MARIADB_ALLOW_EMPTY_ROOT_PASSWORD`). Solo vale para el laboratorio; en producción iría un usuario con permisos mínimos y la clave fuera del código.

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
- No se usa `Class.forName("com.mysql.cj.jdbc.Driver")`: desde JDBC 4 el driver se registra solo si el `.jar` está en el classpath.
- El driver es **`mysql-connector-j-26.10.0.jar`**, en `CalendarioApp/lib/`. En NetBeans se agrega con: clic derecho en el proyecto → **Properties → Libraries → Compile → Add JAR/Folder** → elegir el jar de `lib/`. Sin él: `No suitable driver found for jdbc:mysql://...`.
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
- `con.isValid(2)`: pregunta al servidor si la conexión sirve, esperando como máximo **2 segundos**. Devuelve `true`/`false`.
- Si `conectar()` falla (MySQL caído, clave mala), cae al `catch` y devuelve `false`. No lanza excepción: es una pregunta de sí/no, usada por `ConexionTest`.

### `mensajeError(SQLException e)`

```java
String estado = e.getSQLState();
int codigo = e.getErrorCode();
```
- `getSQLState()`: código estándar de 5 caracteres (SQLState); `getErrorCode()`: código propio de MySQL/MariaDB.

```java
if ((estado != null && estado.startsWith("08")) || codigo == 1045 || codigo == 1049) {
    return "No fue posible conectar con la base de datos.";
}
```
- SQLState **`08xxx`** = familia «error de conexión» (p. ej. `08S01` *Communications link failure*, MySQL apagado). Se revisa `estado != null` para no caer en `NullPointerException`.
- **1045** = *Access denied* (usuario o clave de la base incorrectos). **1049** = *Unknown database*. Para el usuario final los tres significan lo mismo: no se pudo conectar.

```java
if (codigo == 1062) { return "El registro ya existe."; }
```
- **1062** = *Duplicate entry*: viola un `UNIQUE` (p. ej. dos eventos con el mismo título/fecha). Es la V3, parte de Nicolás, pero vive en este método mío.

```java
if (codigo == 4025 || codigo == 3819 || codigo == 3140) {
    return "Los datos no cumplen las reglas de la tabla (prioridad, horas, duracion o JSON).";
}
```
- **4025** = *CONSTRAINT failed* en MariaDB (un `CHECK`). **3819** = el mismo `CHECK` en MySQL 8. **3140** = *JSON inválido*. Se cubren ambos motores.

```java
return "Error de base de datos: " + e.getMessage();
```
- Respaldo para cualquier otro error: muestra el mensaje original.

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
- `prepareStatement(sql)`: el servidor recibe y compila la plantilla con los `?` **antes** de conocer los datos.

```java
ps.setString(1, nombreUsuario);
ps.setString(2, clave);
```
- Llena el primer `?` y el segundo. **Los índices parten en 1**, no en 0. `setString` entrega el valor como **dato**, escapado.

```java
try (ResultSet rs = ps.executeQuery()) {
```
- `executeQuery()` ejecuta un `SELECT` y devuelve un `ResultSet` (tabla de resultados con un cursor que empieza *antes* de la primera fila). También va en try-with-resources.
- Para `INSERT/UPDATE/DELETE` se usa `executeUpdate()`.

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
- Lee cada columna por **nombre** con el `getXxx` que corresponde al tipo (`getInt`, `getString`, `getBoolean`) y arma el objeto del modelo. Así la ventana recibe un `Usuario`, no un `ResultSet`.

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
`'1'='1'` siempre es verdadero: entra sin saber la clave. Con `PreparedStatement` el SQL ya está compilado con `?`; lo escrito se trata como **texto literal de la clave**, nunca como sintaxis. Se busca una clave que sea literalmente `' OR '1'='1` y no existe → `null`. Lo comprueba `UsuarioDAOTest` con `validar("admin' -- ", ...)` y `validar("admin", "' OR '1'='1")`, ambos `null`.

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
- `trim()` quita espacios sobrantes del usuario. La clave **no** se recorta (un espacio podría ser parte de ella). `JPasswordField.getPassword()` devuelve `char[]` (no `String`, para no dejar la clave en el *pool* de strings); se convierte con `new String(...)`.

```java
if (usuario.isEmpty() || clave.isEmpty()) {
    JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos.", "Acceso",
            JOptionPane.WARNING_MESSAGE);
    txtUsuario.requestFocus();
    return;
}
```
- **1.º vacíos (V1).** Es lo más barato y no necesita base de datos: si falta un campo, ni se consulta MySQL. `return` corta el método. `requestFocus()` deja el cursor listo para escribir.

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
- **4.º éxito.** Se abre `VentanaEventos` pasándole el `Usuario` y se cierra el login con `dispose()` (libera la ventana sin terminar la JVM).

```java
} catch (SQLException e) {
    JOptionPane.showMessageDialog(this, Conexion.mensajeError(e), "Error de base de datos",
            JOptionPane.ERROR_MESSAGE);
}
```
- **V4.** Cualquier `SQLException` se traduce con `Conexion.mensajeError(e)` y se muestra. Al terminar el `catch` el método acaba y la ventana sigue abierta: **la app no se cierra**, se puede reintentar al levantar MySQL.

**Por qué este orden:** de lo más barato a lo más caro y de lo más general a lo más específico: sin consultar (vacíos) → una consulta (validar) → una segunda consulta solo si hace falta (inactivo) → recién ahí entrar.

`btnSalirActionPerformed` solo hace `System.exit(0)`.

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

## 6. Demostración

### En la app (usuarios cargados por el script)

1. `admin` / `Admin2026` → abre `VentanaEventos` (rol ADMIN).
2. `operador1` / `Oper2026` → abre `VentanaEventos` (rol OPERADOR).
3. `inactivo1` / `Inac2026` → «Usuario inactivo.» (V2).
4. `admin` / `clavemala` → «Usuario o contraseña incorrectos.» (V1).
5. Campos vacíos → «Usuario o contraseña incorrectos.» (V1).
6. `admin` / `ADMIN2026` → rechazado (mayúsculas).

### Verificación en MySQL

Comando 1: ver los usuarios y su estado.
```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "SELECT id_usuario, nombre_usuario, rol, activo FROM usuario"
```
- `docker exec poo-mysql` ejecuta un comando dentro del contenedor; `mariadb -uroot` es el cliente con usuario `root` (sin clave); `calendario_app` es la base; `-e "..."` ejecuta esa sentencia y sale.
- Salida esperada: 3 filas: `admin / ADMIN / 1`, `operador1 / OPERADOR / 1`, `inactivo1 / OPERADOR / 0`. `activo` se ve como 1/0 (BOOLEAN es TINYINT).
- Si sale distinto (error de conexión o tabla inexistente): revisar `docker ps` y que el script `sql/calendario_bd.sql` se haya cargado.

Comando 2: comprobar la regla de mayúsculas.
```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "SELECT COUNT(*) FROM usuario WHERE nombre_usuario='admin' AND clave='ADMIN2026'"
```
- Salida esperada: `0`. Con `Admin2026` sale `1`. Demuestra que `utf8mb4_bin` distingue mayúsculas.

### Mostrar V4 (MySQL caído)

Comando 1: apagar la base.
```bash
docker stop poo-mysql
```
- Detiene el contenedor. En la app, con la ventana de login abierta, ingresar `admin` / `Admin2026`: aparece «No fue posible conectar con la base de datos.» y la ventana **sigue abierta**.

Comando 2: volver a encenderla.
```bash
docker start poo-mysql
```
- Reinicia el contenedor (los datos persisten en el volumen). Esperar unos segundos; reintentar el login y ahora entra. Si no entra: `docker ps` debe mostrar `poo-mysql` como `Up`.

(Si el login ya mostró VentanaEventos, volver a abrirla ejecutando de nuevo `VentanaLogin`.)

---

## 7. Diagrama de secuencia del login (texto)

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
Al salir del `try-with-resources` se cierran `ResultSet`, `PreparedStatement` y `Connection` en ese orden.

---

## 8. Lo mínimo de las partes de Tais y de Nicolás

**Tais (script SQL y modelo)**
- `sql/calendario_bd.sql` crea la base y las tablas `usuario` y `evento`, con PK autoincremental, `UNIQUE`, `CHECK` y `COLLATE utf8mb4_bin` en la clave; inserta 3 usuarios y 5 eventos de prueba.
- `evento` tiene columnas del tema: ubicación `POINT`, un campo `JSON` (detalle), prioridad, horas y duración con restricciones.
- El paquete `modelo` (`Usuario`, `Evento`): clases con atributos privados, constructor, getters y setters; el DAO las llena con lo que lee del `ResultSet`.
- Es quien sabe explicar por qué hay restricciones en la tabla (integridad en la base, no solo en la interfaz).

**Nicolás (`EventoDAO`, CRUD, POINT/JSON)**
- `EventoDAO` hace el CRUD de eventos con `PreparedStatement`: `INSERT`, `SELECT`, `UPDATE`, `DELETE` (`executeUpdate` para los que modifican).
- `POINT(?, ?)` se escribe con (longitud, latitud) y se lee con `ST_X`/`ST_Y`.
- El `JSON` viaja como texto válido; si no lo es, error 3140, que `mensajeError` traduce.
- `VentanaEventos` es la pantalla del CRUD, con la tabla y los formularios; recibe el `Usuario` que yo valido.
- V3 (registro repetido, error 1062) y las restricciones 4025/3819 se traducen en mi `mensajeError`.

---

## 9. Quince preguntas probables

1. **¿Qué es JDBC?** API de Java (`java.sql`) para hablar con bases de datos relacionales; el driver traduce las llamadas al protocolo de MySQL.
2. **Explique el flujo JDBC.** Se abre una `Connection`, se prepara un `PreparedStatement` con el SQL, se asignan parámetros, se ejecuta (`executeQuery`) y se recorre el `ResultSet` con `next()`; al final se cierra todo.
3. **¿Qué es `Connection`?** La sesión abierta con la base. Se obtiene con `DriverManager.getConnection(URL, usuario, clave)` y debe cerrarse.
4. **¿Por qué `PreparedStatement` y no `Statement`?** Evita inyección SQL (los datos van separados del SQL) y el servidor reutiliza la sentencia compilada.
5. **¿Qué pasa con `' OR '1'='1`?** Con concatenación entraría sin clave; con `?` se toma como texto de la clave y no coincide con nada → `null`.
6. **¿Qué hace `rs.next()`?** Avanza a la siguiente fila y devuelve `true` si existe. El cursor parte antes de la primera.
7. **¿Por qué `validar` devuelve `null`?** Una clave equivocada es un resultado esperado, no un error técnico; las excepciones se reservan para fallos de conexión o SQL.
8. **¿Para qué sirve `estaInactivo`?** Separa «inactivo» de «clave incorrecta» (V2 vs V1); se llama solo si `validar` dio `null`.
9. **¿Por qué `COUNT(*)` y `rs.next()` sin `if`?** Porque `COUNT(*)` siempre devuelve exactamente una fila.
10. **¿Qué es try-with-resources?** `try (recurso) {...}` cierra automáticamente lo que implementa `AutoCloseable`, incluso si hay excepción; evita fugas de conexiones.
11. **¿Por qué las constantes son `static final`?** Un solo lugar para editar, no se pueden alterar en ejecución y sirven desde un método `static`.
12. **¿Qué hacen `useSSL=false`, `allowPublicKeyRetrieval` y `serverTimezone`?** Sin cifrado (entorno local), permite obtener la clave pública para autenticarse, y fija la zona horaria `America/Santiago`.
13. **¿Qué significan 1045, 1049, 1062 y 4025?** Acceso denegado, base desconocida, entrada duplicada (UNIQUE) y restricción CHECK fallida en MariaDB. SQLState `08` = error de conexión.
14. **¿Qué pasa si MySQL está caído?** `getConnection` lanza `SQLException` (SQLState `08S01`); el `catch` muestra «No fue posible conectar con la base de datos.» y la app sigue abierta (V4).
15. **¿Cómo agrega el driver en NetBeans?** Properties → Libraries → Compile → Add JAR/Folder → `lib/mysql-connector-j-26.10.0.jar`. Sin él: `No suitable driver found`.

(Extra por si preguntan: guardar la clave en texto plano es una limitación académica; en un sistema real se guardaría un hash con sal, por ejemplo bcrypt, y se compararía el hash.)

### Tres ganchos mnemotécnicos

**1. El orden del flujo JDBC: «C-P-S-R-C» → «Con Pan Sale Rico Café»**
**C**onnection → **P**reparedStatement → **S**et (setString) → **R**esultSet (executeQuery + next) → **C**lose (try-with-resources).
- *Autoevaluación:* ¿Qué método llena el primer `?` y desde qué índice parte? (`setString(1, ...)`, desde 1.)

**2. El orden de las comprobaciones del login: «V-V-I-A» → «Vacío, Validar, Inactivo, Abrir»**
Imagina un guardia de discoteca: mira si traes algo (vacío), revisa tu cédula (validar), mira si estás vetado (inactivo) y recién ahí abre la puerta (abrir `VentanaEventos`).
- *Autoevaluación:* ¿Por qué `estaInactivo` va después de `validar` y no antes? (Solo hay que separar clave mala de inactivo cuando `validar` dio `null`; así no se revela nada a quien no sabe la clave.)

**3. Los códigos de `mensajeError`: «08 = Se Cayó el cable; 1045 = Puerta con clave; 1049 = Casa que no existe; 1062 = Gemelo repetido»**
Imagen absurda: un cable (08) que se desconecta, una puerta con candado (1045), una casa sin dirección (1049) y dos gemelos idénticos en la fila (1062, duplicado).
- *Autoevaluación:* ¿Cuál de estos códigos corresponde a «Unknown database» y cuál a una violación de `UNIQUE`? (1049 y 1062.)
