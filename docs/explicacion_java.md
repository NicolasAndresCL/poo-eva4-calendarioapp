# Explicación del código Java: JDBC, DAO y modelo

Proyecto: `CalendarioApp` (NetBeans, Java with Ant, driver `mysql-connector-j-26.10.0.jar` en `lib/`). Paquetes: `conexion`, `modelo`, `dao` (y `vista`, sección 9).

**Equipo:** Tais Montecinos · Rodolfo Prieto · Nicolás Cano. En la defensa piden "ubicar y explicar el método DAO o la sentencia SQL" (B4) y "explicar el flujo JDBC: Connection, PreparedStatement, ResultSet" (B2). Los códigos L1, C1 a C5 y V1 a V4 son los de la pauta; A4, A5, A6 a A10 son los criterios de la rúbrica.

## 0. El flujo JDBC en una página

JDBC (*Java Database Connectivity*) es la API estándar (`java.sql`) con la que Java habla con una base de datos. El driver (`mysql-connector-j`) traduce las llamadas a lo que entiende MySQL/MariaDB.

| Paso | Objeto | Qué hace |
|---|---|---|
| 1 | `Connection` | Canal abierto con la base de datos. Se obtiene con `DriverManager.getConnection(url, usuario, clave)`. |
| 2 | `PreparedStatement` | Sentencia SQL **precompilada** con marcadores `?`. Se crea con `con.prepareStatement(sql)`. |
| 3 | `setXxx(n, valor)` | Rellena el `?` número `n` (la numeración parte en **1**, no en 0). |
| 4 | `executeQuery()` / `executeUpdate()` | `executeQuery` para `SELECT` (devuelve `ResultSet`); `executeUpdate` para `INSERT`/`UPDATE`/`DELETE` (devuelve un `int`). |
| 5 | `ResultSet` | Cursor sobre las filas del resultado. `next()` avanza una fila; `getXxx("columna")` lee. |
| 6 | Cierre | `try-with-resources` cierra `ResultSet`, `PreparedStatement` y `Connection` solos, en orden inverso. |

Resumen para decir en voz alta: *"abro una `Connection`, preparo un `PreparedStatement` con parámetros, lo ejecuto, recorro el `ResultSet` si es una consulta, y todo se cierra solo gracias al `try-with-resources`"*.

### Mapa de la pauta a los métodos

| Código | Qué pide la pauta | Método | Archivo |
|---|---|---|---|
| A4 | Driver y `Conexion` con parámetros centralizados | `Conexion.conectar()`, `probarConexion()` | `conexion/Conexion.java` |
| A5, V3, V4 | Excepciones con mensajes claros y cierre de recursos | `Conexion.mensajeError()` + `try-with-resources` | `Conexion.java` y los DAO |
| L1 (A6), V1 | Login con `SELECT ... = ? AND ... = ? AND activo = TRUE` | `UsuarioDAO.validar()` | `dao/UsuarioDAO.java` |
| V2 | Usuario inactivo | `UsuarioDAO.estaInactivo()` | `UsuarioDAO.java` |
| C1 (A7) | `INSERT` | `EventoDAO.insertar()` | `dao/EventoDAO.java` |
| C2 (A8) | `SELECT` y llenar la lista | `EventoDAO.listar()` | `EventoDAO.java` |
| C3 (A8) | Buscar por identificador de negocio | `EventoDAO.buscar()` | `EventoDAO.java` |
| C4 (A9) | `UPDATE ... WHERE id = ?` | `EventoDAO.actualizar()` | `EventoDAO.java` |
| C5 (A10) | `DELETE ... WHERE id = ?` | `EventoDAO.eliminar()` | `EventoDAO.java` |

## 1. Clase `Conexion` (paquete `conexion`) · A4, A5, V3, V4

Es la **única** clase que conoce la URL, el usuario y la clave. Ningún DAO las repite.

```java
public class Conexion {
    private static final String URL = "jdbc:mysql://localhost:3306/calendario_app"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Santiago";
    private static final String USUARIO = "root";
    private static final String CLAVE = "";
```

- `private static final`: constantes de clase. `private` = solo esta clase las ve; `static` = pertenecen a la clase (no hace falta crear un objeto); `final` = no se pueden reasignar.
- **Por qué centralizar (criterio A4)**: si cambia el servidor, el puerto, la base o la clave, se edita **un solo lugar**. Si cada DAO tuviera su propia URL, habría que cambiar muchos y se podría olvidar alguno ("conexión repetida en varias clases" = Parcial en la rúbrica).
- **Anatomía de la URL**:
  - `jdbc:mysql://` protocolo y subprotocolo que el `DriverManager` usa para elegir el driver.
  - `localhost:3306` equipo y puerto de MySQL (3306 es el puerto estándar).
  - `/calendario_app` base de datos a la que se conecta (equivale al `USE` del script).
  - `?useSSL=false` sin cifrado SSL (es una base local de desarrollo).
  - `&allowPublicKeyRetrieval=true` permite al driver pedir la clave pública del servidor al autenticarse; sin esto, algunas autenticaciones (como `caching_sha2_password` de MySQL 8) fallan con "Public Key Retrieval is not allowed".
  - `&serverTimezone=America/Santiago` zona horaria para convertir fechas y horas entre Java y el servidor.
- `USUARIO = "root"` y `CLAVE = ""`: el `root` del contenedor de pruebas, que no tiene contraseña. En producción no se usa `root` ni clave vacía (se crea un usuario con permisos mínimos).

```java
public static Connection conectar() throws SQLException {
    return DriverManager.getConnection(URL, USUARIO, CLAVE);
}
```

- `public static Connection conectar()`: método de clase, se llama `Conexion.conectar()` sin crear un objeto.
- `throws SQLException`: declara que puede lanzar `SQLException` (checked exception). Quien llama debe capturarla o declararla también. Aquí **no** se captura: se deja subir para que la ventana la muestre al usuario.
- `DriverManager.getConnection(...)`: es la fábrica de conexiones. Busca entre los drivers registrados uno que acepte el prefijo `jdbc:mysql:` y le pide abrir la conexión. **No hace falta `Class.forName("com.mysql.cj.jdbc.Driver")`**: desde JDBC 4 el driver se registra solo (mecanismo `ServiceLoader`) si el `.jar` está en el classpath, que es lo que hace `lib/mysql-connector-j-...jar`.
- **Devuelve una conexión nueva en cada llamada.** Cada DAO la abre, la usa y la cierra (la cierra su `try-with-resources`). Es simple y adecuado para una app de escritorio pequeña (un servidor real usaría un *pool* de conexiones).

```java
public static boolean probarConexion() {
    try (Connection con = conectar()) {
        return con.isValid(2);
    } catch (SQLException e) {
        return false;
    }
}
```

- Sirve para saber si MySQL responde (utilidad para comprobar la base sin hacer una consulta; `VentanaLogin` no la usa: el login detecta la caída con el `catch (SQLException)`, V4).
- `try (Connection con = conectar())`: **try-with-resources**: la conexión se cierra sola al salir del bloque, ocurra o no una excepción.
- `con.isValid(2)`: verifica que la conexión esté viva, con un tiempo máximo de **2 segundos**. Devuelve `true` o `false`.
- `catch (SQLException e) { return false; }`: si no se pudo conectar (servidor apagado, clave mala, base inexistente) devuelve `false` en vez de propagar el error. **Esto es V4**: la aplicación no se cae, el llamador decide mostrar "No fue posible conectar con la base de datos".

```java
public static String mensajeError(SQLException e) {
    String estado = e.getSQLState();
    int codigo = e.getErrorCode();
    if ((estado != null && estado.startsWith("08")) || codigo == 1045 || codigo == 1049) {
        return "No fue posible conectar con la base de datos.";
    }
    if (codigo == 1062) {
        return "El registro ya existe.";
    }
    if (codigo == 4025 || codigo == 3819 || codigo == 3140) {
        return "Los datos no cumplen las reglas de la tabla (prioridad, horas, duracion o JSON).";
    }
    return "Error de base de datos: " + e.getMessage();
}
```

Traduce una `SQLException` técnica a un mensaje claro para el usuario (V3 y V4, y el criterio A5 "mensajes claros"). Una `SQLException` entrega dos datos:

- `getSQLState()`: código **estándar** de 5 caracteres (X/Open / SQL:2003), igual en todos los motores. La clase `08` (por ejemplo `08001`, `08S01`) significa **error de conexión**. Puede ser `null`, por eso se revisa `estado != null` antes de `startsWith`.
- `getErrorCode()`: código **propio del motor** (MySQL/MariaDB).

| Código | Origen | Significado | Mensaje al usuario |
|---|---|---|---|
| SQLState `08xxx` | Estándar | Fallo de conexión (servidor caído, red) | "No fue posible conectar con la base de datos." |
| 1045 | MySQL | `Access denied for user`: usuario o clave erróneos | idem |
| 1049 | MySQL | `Unknown database`: la base no existe | idem |
| 1062 | MySQL | `Duplicate entry`: viola `UNIQUE` (código repetido) | "El registro ya existe." (V3) |
| 4025 | MariaDB | `CONSTRAINT ... failed`: viola un `CHECK` | "Los datos no cumplen las reglas de la tabla..." |
| 3819 | MySQL 8 | `Check constraint ... is violated` | idem |
| 3140 | MySQL 8 | `Invalid JSON text` (viola el `CHECK` de JSON) | idem |
| otro | cualquiera | no previsto | "Error de base de datos: " + mensaje original |

Los tres últimos códigos de la fila de `CHECK` se revisan juntos porque la **misma falla tiene código distinto según el motor**: el proyecto se probó en MariaDB pero debe funcionar en MySQL. El `return` final es la red de seguridad: ningún error queda sin mensaje (por ejemplo el 1364, campo obligatorio sin valor).

## 2. Clase `Usuario` (paquete `modelo`) · Tabla 4 de la pauta

```java
public class Usuario {
    private int idUsuario;
    private String nombreUsuario;
    private String clave;
    private String nombreCompleto;
    private String rol;
    private boolean activo;
```

Representa **una fila** de la tabla `usuario`: un atributo `private` por columna (encapsulamiento). Mapa de tipos:

| Columna SQL | Atributo Java |
|---|---|
| `id_usuario INT` | `int idUsuario` |
| `nombre_usuario VARCHAR` | `String nombreUsuario` |
| `clave VARCHAR` | `String clave` |
| `nombre_completo VARCHAR` | `String nombreCompleto` |
| `rol VARCHAR` | `String rol` |
| `activo BOOLEAN` | `boolean activo` |

Convención: las columnas van en `snake_case` (SQL) y los atributos en `camelCase` (Java).

- `public Usuario() { }`: **constructor vacío**; permite crear el objeto y rellenarlo con los `setXxx`.
- `public Usuario(int idUsuario, String nombreUsuario, ..., boolean activo)`: **constructor completo**; recibe todos los datos y los asigna con `this.atributo = parámetro`. `this` distingue el atributo del parámetro cuando se llaman igual. El `UsuarioDAO` lo usa para armar el objeto desde el `ResultSet`.
- `getIdUsuario()`, `getNombreUsuario()`, etc.: **getters**, devuelven el valor del atributo privado.
- `setIdUsuario(int)`, etc.: **setters**, lo modifican.
- `isActivo()` en vez de `getActivo()`: por convención JavaBeans, el getter de un `boolean` primitivo se llama `isXxx`.

## 3. Clase `Evento` (paquete `modelo`)

Una fila de la tabla `evento`. Se diferencia de `Usuario` en cuatro cosas:

```java
private Double latitud;
private Double longitud;
private String detalle;
private LocalDate fechaEvento;
private LocalTime horaInicio;
private LocalTime horaFin;
```

- **`Double` (envoltorio), no `double`**: un `double` primitivo no puede ser `null`, y la coordenada **puede faltar** (`ubicacion` es `NULL` en la base). `Double` sí admite `null`.
- **La columna `POINT` se parte en dos atributos** (`latitud` y `longitud`): Java no tiene un tipo para `POINT`; es más simple manejar dos números.
- **`detalle` es `String`**: la columna JSON se maneja como texto. El JSON se valida en la base (`CHECK` + `JSON_VALID`).
- **`LocalDate` y `LocalTime`** (paquete `java.time`): equivalen a `DATE` y `TIME`. Son inmutables y no llevan zona horaria. `LocalDate` = solo fecha; `LocalTime` = solo hora. `horaFin` puede ser `null`.
- No hay atributo para `fecha_creacion`: la llena el servidor y la app no la necesita.

```java
public Evento() {
    categoria = "personal";
    duracionMinutos = 60;
    prioridad = 2;
    activo = true;
}
```

El constructor vacío **replica los `DEFAULT` de la tabla** (`'personal'`, 60, 2, `TRUE`): un evento nuevo parte con los mismos valores que pondría la base.

```java
public boolean tieneUbicacion() {
    return latitud != null && longitud != null;
}
```

Devuelve `true` solo si **ambas** coordenadas existen. El DAO lo usa para decidir si guarda un punto o un `NULL` (un `POINT` necesita las dos). El `&&` evita llamar a nada sobre un `null`.

El resto es el constructor completo (14 parámetros, mismo orden que los atributos y el que usa `leerFila`) y los getters y setters. Nota: `isActivo()` por ser `boolean`; `getLatitud()` y `getLongitud()` devuelven `Double`.

## 4. Clase `UsuarioDAO` (paquete `dao`) · L1, V1, V2, A6

DAO = *Data Access Object*: la clase que **encapsula el acceso a una tabla**. Así el resto del programa (ventanas) no escribe SQL.

### 4.1 `validar` (L1, V1)

```java
public Usuario validar(String nombreUsuario, String clave) throws SQLException {
    String sql = "SELECT id_usuario, nombre_usuario, clave, nombre_completo, rol, activo "
            + "FROM usuario WHERE nombre_usuario = ? AND clave = ? AND activo = TRUE";
    try (Connection con = Conexion.conectar();
            PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setString(1, nombreUsuario);
        ps.setString(2, clave);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new Usuario(
                        rs.getInt("id_usuario"),
                        rs.getString("nombre_usuario"),
                        rs.getString("clave"),
                        rs.getString("nombre_completo"),
                        rs.getString("rol"),
                        rs.getBoolean("activo"));
            }
            return null;
        }
    }
}
```

Línea por línea:

1. `public Usuario validar(String nombreUsuario, String clave) throws SQLException`: recibe lo que el usuario escribió y devuelve un `Usuario` si el login es válido y está activo, o `null` si no. Declara `throws SQLException`: el error de base de datos sube a la ventana, que lo traduce con `Conexion.mensajeError`.
2. `String sql = "SELECT ... WHERE nombre_usuario = ? AND clave = ? AND activo = TRUE"`: la consulta con **dos marcadores `?`**. Va partida en dos líneas con `+` solo por legibilidad; sigue siendo texto fijo. La condición `activo = TRUE` es lo que hace que un usuario inactivo no pueda entrar (criterio A6: "solo ingresan usuarios activos").
3. `try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql))`: **try-with-resources con dos recursos**. Se abren en el orden escrito y se cierran en **orden inverso** (primero `ps`, luego `con`) al terminar el bloque, aunque haya excepción o `return`. `prepareStatement(sql)` envía la plantilla SQL al servidor para que la prepare; aún no tiene valores.
4. `ps.setString(1, nombreUsuario)`: pone el texto en el **primer** `?`. `ps.setString(2, clave)`: en el segundo. El driver escapa el valor correctamente.
5. `try (ResultSet rs = ps.executeQuery())`: ejecuta el `SELECT` y devuelve un `ResultSet`. Se abre en un `try` anidado para que se cierre **antes** que `ps` y `con`.
6. `if (rs.next())`: el cursor del `ResultSet` parte **antes de la primera fila**; `next()` lo mueve a la siguiente fila y devuelve `true` si existe. Si hay una fila, las credenciales y el estado coinciden. Como `nombre_usuario` es `UNIQUE`, hay a lo sumo una.
7. `rs.getInt("id_usuario")`, `getString(...)`, `getBoolean("activo")`: leen la columna por **nombre** (más legible y seguro que por posición). El tipo del `getXxx` corresponde al tipo de la columna. `getBoolean` convierte `TINYINT(1)`: 0 es false, distinto de 0 es true.
8. `return new Usuario(...)`: arma el objeto del modelo con el constructor completo.
9. `return null;`: si `next()` dio `false` (ninguna fila), las credenciales son incorrectas o el usuario está inactivo. Esta es la convención de la pauta: "null si no es válido o está inactivo". La ventana interpreta `null` como V1.

**Por qué `?` y no concatenar (inyección SQL).** Si se escribiera `"... WHERE nombre_usuario = '" + usuario + "'"`, un atacante podría escribir en el campo `' OR '1'='1` y la consulta quedaría `... nombre_usuario = '' OR '1'='1'`, que es verdadera para todas las filas: ingresa sin clave. Con `PreparedStatement`, el valor viaja **como dato**, nunca como parte del código SQL: lo escrito se compara como texto literal y no puede cambiar la estructura de la consulta. Además la sentencia queda precompilada y se puede reutilizar. La pauta lo exige: "No se aceptan consultas concatenadas".

Ojo con una cosa que sí se concatena: en `EventoDAO`, la constante `COLUMNAS` se une al `SELECT`. Es seguro porque es una constante del programa, no un dato del usuario.

### 4.2 `estaInactivo` (V2)

```java
public boolean estaInactivo(String nombreUsuario, String clave) throws SQLException {
    String sql = "SELECT COUNT(*) FROM usuario "
            + "WHERE nombre_usuario = ? AND clave = ? AND activo = FALSE";
    try (Connection con = Conexion.conectar();
            PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setString(1, nombreUsuario);
        ps.setString(2, clave);
        try (ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }
}
```

- Se usa **después** de que `validar` devolvió `null`, para distinguir *por qué* falló: credenciales incorrectas (V1, "Usuario o contraseña incorrectos") o usuario inactivo (V2, "Usuario inactivo").
- `SELECT COUNT(*) ... activo = FALSE`: cuenta las filas con esas credenciales **y** `activo = FALSE`. Un `COUNT(*)` siempre devuelve **exactamente una fila** (con 0 si no hay coincidencias), por eso basta `rs.next();` sin `if`.
- `rs.getInt(1)`: lee por **posición** (la primera columna, empezando en 1), porque `COUNT(*)` no tiene un nombre cómodo. `> 0` convierte el conteo en `boolean`.
- El resto (try-with-resources, `setString`, `executeQuery`) es idéntico a `validar`.

## 5. Clase `EventoDAO` (paquete `dao`) · C1 a C5, A7 a A10

### 5.1 Constante `COLUMNAS`

```java
private static final String COLUMNAS = "id_evento, codigo_evento, titulo, lugar, "
        + "ST_Y(ubicacion) AS latitud, ST_X(ubicacion) AS longitud, detalle, categoria, "
        + "fecha_evento, hora_inicio, hora_fin, duracion_minutos, prioridad, activo";
```

Lista de columnas que usan `listar` y `buscar`, escrita **una vez** (no se repite el mismo texto). Dos detalles:

- No se hace `SELECT *`: `ubicacion` es un `POINT` binario que Java no sabe leer; se extrae con `ST_Y(ubicacion) AS latitud` y `ST_X(ubicacion) AS longitud` (recuerda: Y = latitud, X = longitud). Los **alias** son los nombres con que `leerFila` lee el `ResultSet`.
- No incluye `fecha_creacion`, que la aplicación no necesita.

(La Tabla 5 de la pauta escribe `SELECT *` como ejemplo; aquí se listan columnas porque hay un `POINT`.)

### 5.2 `insertar` (C1, A7)

```java
public int insertar(Evento e) throws SQLException {
    String sql = "INSERT INTO evento (codigo_evento, titulo, lugar, ubicacion, detalle, "
            + "categoria, fecha_evento, hora_inicio, hora_fin, duracion_minutos, prioridad, activo) "
            + "VALUES (?, ?, ?, POINT(?, ?), ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Connection con = Conexion.conectar();
            PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
        cargarParametros(ps, e);
        ps.executeUpdate();
        try (ResultSet claves = ps.getGeneratedKeys()) {
            if (claves.next()) {
                e.setIdEvento(claves.getInt(1));
            }
        }
        return e.getIdEvento();
    }
}
```

1. **El SQL**: 12 columnas, pero **13 marcadores `?`**, porque `ubicacion` usa `POINT(?, ?)` (dos parámetros: longitud y latitud). No aparecen `id_evento` (lo genera `AUTO_INCREMENT`) ni `fecha_creacion` (`DEFAULT CURRENT_TIMESTAMP`).
2. `prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)`: segundo argumento = "devuélveme las claves que genere la base". Así Java puede saber qué `id_evento` asignó `AUTO_INCREMENT` sin hacer otra consulta.
3. `cargarParametros(ps, e)`: método privado (5.7) que pone los 13 valores.
4. `ps.executeUpdate()`: ejecuta el `INSERT`. **`executeUpdate` devuelve un `int`**: la cantidad de filas afectadas (aquí 1). Esta vez no se guarda porque no se necesita.
5. `ps.getGeneratedKeys()`: devuelve un `ResultSet` con la clave generada (una fila, una columna). `claves.next()` se posiciona en esa fila y `claves.getInt(1)` lee el id (columna 1).
6. `e.setIdEvento(...)`: **deja el id en el objeto** que recibió. Así la ventana y la `JTable` conocen el id nuevo sin recargar.
7. `return e.getIdEvento()`: devuelve el id (o 0 si no hubo clave).
8. Si el código ya existe, `executeUpdate()` lanza `SQLException` con el código **1062**: la sentencia **no inserta** y el error sube a la ventana, que muestra "El registro ya existe" (V3).

### 5.3 `listar` (C2, A8)

```java
public ArrayList<Evento> listar() throws SQLException {
    String sql = "SELECT " + COLUMNAS + " FROM evento ORDER BY fecha_evento, hora_inicio";
    ArrayList<Evento> lista = new ArrayList<>();
    try (Connection con = Conexion.conectar();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            lista.add(leerFila(rs));
        }
    }
    return lista;
}
```

- `ORDER BY fecha_evento, hora_inicio`: ordena cronológicamente (primero la fecha y, si coincide, la hora).
- **Tres recursos en el mismo `try`**: `Connection`, `PreparedStatement` y `ResultSet` (este último puede ir ahí porque `executeQuery()` no necesita `setXxx` antes: no hay `?`). Se cierran **al revés**: `rs`, `ps`, `con`. Este es el criterio A5 (cierre de recursos).
- `while (rs.next())`: recorre **todas** las filas (con `if` leería solo una). En cada vuelta, `leerFila(rs)` construye un `Evento` y se agrega al `ArrayList`.
- `return lista;` va **después** del `try`: los recursos ya se cerraron, y la lista es de objetos Java comunes que siguen existiendo. Con cero eventos devuelve una lista **vacía**, no `null`.
- La ventana recorre la lista para llenar la `JTable` (en el paquete `vista`).

### 5.4 `buscar` (C3, A8)

```java
public Evento buscar(String codigoEvento) throws SQLException {
    String sql = "SELECT " + COLUMNAS + " FROM evento WHERE codigo_evento = ?";
    try (Connection con = Conexion.conectar();
            PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setString(1, codigoEvento);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return leerFila(rs);
            }
            return null;
        }
    }
}
```

Busca por el **identificador de negocio** (`codigo_evento`, `UNIQUE`), como pide la pauta. Como el código es único, hay a lo sumo una fila: se usa `if (rs.next())`, no `while`. Devuelve el `Evento`, o `null` si no existe ("mensaje si no existe" lo muestra la ventana). Misma estructura que `validar`.

### 5.5 `actualizar` (C4, A9)

```java
public boolean actualizar(Evento e) throws SQLException {
    String sql = "UPDATE evento SET codigo_evento = ?, titulo = ?, lugar = ?, "
            + "ubicacion = POINT(?, ?), detalle = ?, categoria = ?, "
            + "fecha_evento = ?, hora_inicio = ?, hora_fin = ?, "
            + "duracion_minutos = ?, prioridad = ?, activo = ? "
            + "WHERE id_evento = ?";
    try (Connection con = Conexion.conectar();
            PreparedStatement ps = con.prepareStatement(sql)) {
        cargarParametros(ps, e);
        ps.setInt(14, e.getIdEvento());
        return ps.executeUpdate() == 1;
    }
}
```

- (En el archivo real las líneas del `String` están cortadas en otros puntos; aquí se reacomodaron solo para que quepan en la página.)
- `UPDATE ... SET columna = ?, ... WHERE id_evento = ?`: modifica **solo** la fila con ese id. **Sin el `WHERE` se modificarían todas las filas de la tabla**; es el error clásico, por eso el `WHERE id_evento = ?` es lo que más miran.
- Usa el **mismo `cargarParametros`** que `insertar`: los parámetros **1 a 13** coinciden en orden con el `INSERT`. El **14** es el `id_evento` del `WHERE`, que se agrega con `ps.setInt(14, ...)`. Se actualiza por el id (clave primaria, estable) y no por el código: así el usuario también puede **cambiar el código** del evento (`codigo_evento = ?` está en el `SET`).
- `ps.executeUpdate() == 1`: `executeUpdate` devuelve las filas afectadas; si es **1**, el cambio se aplicó y el método devuelve `true`; si es 0 (no existe ese id), `false`.
- Con el Connector/J, por defecto ese conteo incluye las filas que **coinciden** aunque los valores no cambien.
- Si el nuevo código ya lo tiene otro evento, lanza `SQLException` 1062 (V3 también en la edición).

### 5.6 `eliminar` (C5, A10)

```java
public boolean eliminar(int idEvento) throws SQLException {
    String sql = "DELETE FROM evento WHERE id_evento = ?";
    try (Connection con = Conexion.conectar();
            PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, idEvento);
        return ps.executeUpdate() == 1;
    }
}
```

`DELETE FROM evento WHERE id_evento = ?` borra la fila con ese id (sin `WHERE` vaciaría la tabla). `setInt(1, ...)` porque el id es `INT`. Devuelve `true` si borró exactamente una fila. La **confirmación** que pide la pauta ("previa confirmación") la hace la ventana con un `JOptionPane` antes de llamar a este método; el DAO solo borra.

### 5.7 `cargarParametros` (método privado compartido)

```java
private void cargarParametros(PreparedStatement ps, Evento e) throws SQLException {
    ps.setString(1, e.getCodigoEvento());
    ps.setString(2, e.getTitulo());
    ps.setString(3, e.getLugar());
    if (e.tieneUbicacion()) {
        ps.setDouble(4, e.getLongitud());
        ps.setDouble(5, e.getLatitud());
    } else {
        ps.setNull(4, Types.DOUBLE);
        ps.setNull(5, Types.DOUBLE);
    }
    ps.setString(6, e.getDetalle());
    ps.setString(7, e.getCategoria());
    ps.setDate(8, Date.valueOf(e.getFechaEvento()));
    ps.setTime(9, Time.valueOf(e.getHoraInicio()));
    if (e.getHoraFin() != null) {
        ps.setTime(10, Time.valueOf(e.getHoraFin()));
    } else {
        ps.setNull(10, Types.TIME);
    }
    ps.setInt(11, e.getDuracionMinutos());
    ps.setInt(12, e.getPrioridad());
    ps.setBoolean(13, e.isActivo());
}
```

Evita escribir dos veces lo mismo (el `INSERT` y el `UPDATE` comparten el orden de parámetros). Correspondencia entre número de `?`, columna y método:

| `?` | Columna | Método `setXxx` |
|---|---|---|
| 1 | `codigo_evento` | `setString` |
| 2 | `titulo` | `setString` |
| 3 | `lugar` | `setString` |
| 4 | `ubicacion` (X = longitud) | `setDouble` o `setNull` |
| 5 | `ubicacion` (Y = latitud) | `setDouble` o `setNull` |
| 6 | `detalle` (JSON) | `setString` |
| 7 | `categoria` | `setString` |
| 8 | `fecha_evento` | `setDate` |
| 9 | `hora_inicio` | `setTime` |
| 10 | `hora_fin` | `setTime` o `setNull` |
| 11 | `duracion_minutos` | `setInt` |
| 12 | `prioridad` | `setInt` |
| 13 | `activo` | `setBoolean` |

Los métodos `setXxx` de `PreparedStatement`:

- `setString(n, texto)`: para `VARCHAR`, `JSON` y cualquier texto. Si el `String` es `null`, el driver envía `NULL` (así `lugar` y `detalle` pueden quedar vacíos).
- `setInt(n, entero)` para `INT`; `setBoolean(n, bool)` para `BOOLEAN` (se envía 1 o 0); `setDouble(n, valor)` para decimales.
- `setDate(n, java.sql.Date)` y `setTime(n, java.sql.Time)`: ver la conversión abajo.
- `setNull(n, Types.XXX)`: pone **`NULL`** explícito; el segundo argumento (`java.sql.Types`) le dice al driver de qué tipo SQL es la columna. Un `double` primitivo no puede ser `null`, por eso se pregunta antes con `tieneUbicacion()` / `getHoraFin() != null`.
- **POINT con NULL**: si no hay ubicación se mandan dos `NULL` a `POINT(?, ?)`, y la función `POINT` con argumentos `NULL` devuelve `NULL`, que es lo que se guarda en `ubicacion`. Y el orden `setDouble(4, longitud)` y `setDouble(5, latitud)` respeta `POINT(longitud, latitud)`.

**Conversión `LocalDate`/`LocalTime` y `java.sql.Date`/`Time`.** JDBC trabaja con `java.sql.Date` y `java.sql.Time` (clases más antiguas); el modelo usa `java.time` (más moderno y limpio). Se convierte en los bordes:

| Dirección | Fecha | Hora |
|---|---|---|
| Java a SQL (al guardar) | `Date.valueOf(LocalDate)` | `Time.valueOf(LocalTime)` |
| SQL a Java (al leer) | `rs.getDate(...).toLocalDate()` | `rs.getTime(...).toLocalTime()` |

Por eso hay `import java.sql.Date` e `import java.sql.Time` en `EventoDAO`.

### 5.8 `leerFila` (método privado compartido)

```java
private Evento leerFila(ResultSet rs) throws SQLException {
    Double latitud = rs.getDouble("latitud");
    if (rs.wasNull()) {
        latitud = null;
    }
    Double longitud = rs.getDouble("longitud");
    if (rs.wasNull()) {
        longitud = null;
    }
    Time fin = rs.getTime("hora_fin");
    return new Evento(
            rs.getInt("id_evento"),
            ...
            rs.getDate("fecha_evento").toLocalDate(),
            rs.getTime("hora_inicio").toLocalTime(),
            fin == null ? null : fin.toLocalTime(),
            ...
            rs.getBoolean("activo"));
}
```

Convierte la **fila actual** del `ResultSet` en un `Evento`. Lo usan `listar` y `buscar` (así la lectura está en un solo lugar). **No** llama a `rs.next()`: eso lo hace el que la llama.

- **`wasNull()`**: los `getInt`, `getDouble`, `getBoolean` devuelven un valor primitivo y, si la columna era `NULL`, devuelven **0 / 0.0 / false**, que se confunde con un valor real. `rs.wasNull()` dice si **la última columna leída** fue `NULL`. Por eso se llama **justo después** de cada `getDouble`. Si fue `NULL`, se asigna `null` al `Double`. Así un evento sin ubicación queda con `latitud = null` y `longitud = null` y no con las coordenadas (0, 0), que existen en el mapa (golfo de Guinea).
- `rs.getTime("hora_fin")` devuelve un objeto (`Time`), y para un objeto `NULL` es `null`: se compara directo con `fin == null` (no hace falta `wasNull`). `fin == null ? null : fin.toLocalTime()` es el operador ternario: si es `null`, `null`; si no, lo convierte. Llamar `toLocalTime()` sobre `null` lanzaría `NullPointerException`.
- `rs.getDate("fecha_evento").toLocalDate()` y `rs.getTime("hora_inicio").toLocalTime()` no se protegen porque esas columnas son `NOT NULL`.
- `rs.getString("detalle")`: el JSON se lee como texto.
- Los nombres `"latitud"` y `"longitud"` son los **alias** definidos en `COLUMNAS` (`ST_Y(...) AS latitud`).

## 6. `executeQuery` vs `executeUpdate`

| | `executeQuery()` | `executeUpdate()` |
|---|---|---|
| Para | `SELECT` | `INSERT`, `UPDATE`, `DELETE` (y DDL) |
| Devuelve | `ResultSet` (las filas) | `int` (filas afectadas) |
| Se usa en | `validar`, `estaInactivo`, `listar`, `buscar` | `insertar`, `actualizar`, `eliminar` |
| Si se usa el equivocado | `executeQuery` con un `INSERT` lanza `SQLException` | `executeUpdate` con un `SELECT` lanza `SQLException` |

## 7. try-with-resources: qué cierra y por qué (A5)

```java
try (Connection con = Conexion.conectar();
        PreparedStatement ps = con.prepareStatement(sql)) {
    ...
}
```

- Cualquier objeto que implemente `AutoCloseable` (`Connection`, `PreparedStatement`, `ResultSet` lo hacen) declarado **entre los paréntesis** del `try` se cierra automáticamente al salir del bloque: normalmente, por `return` o por excepción.
- Se cierran en **orden inverso al de declaración**: primero el `ResultSet`, luego el `PreparedStatement`, al final la `Connection`.
- Sin esto habría **fugas de conexiones**: MySQL tiene un máximo de conexiones simultáneas y, si no se cierran, el servidor termina rechazando nuevas. Antes se escribía un `finally` con `close()` en cada recurso (y su propio `try/catch`); `try-with-resources` lo reemplaza.
- Los DAO **no usan `catch`**: declaran `throws SQLException` y dejan que la excepción suba hasta la ventana, que muestra `Conexion.mensajeError(e)` en un `JOptionPane`. La app no se cae (V4). Los recursos ya se cerraron antes de que la excepción llegue arriba.
- Solo `Conexion.probarConexion()` captura la excepción (devuelve `false`).

## 8. Diagrama de secuencia: flujo de login

```
VentanaLogin      UsuarioDAO         Conexion       PreparedStatement      MySQL      ResultSet      Usuario
    |                 |                  |                  |                |            |            |
    | (clic "Ingresar": lee usuario y clave; si hay vacíos -> V1 y termina)   |            |            |
    |-- validar(u,c) ->|                  |                  |                |            |            |
    |                 |-- conectar() --->|                  |                |            |            |
    |                 |                  |-- DriverManager.getConnection ---->|            |            |
    |                 |<-- Connection ---|                  |                |            |            |
    |                 |-- prepareStatement(sql) ----------->|                |            |            |
    |                 |-- setString(1,u) / setString(2,c) ->|                |            |            |
    |                 |-- executeQuery() ------------------>|-- SELECT ... ->|            |            |
    |                 |<-------------------------------------- ResultSet ----|            |            |
    |                 |-- rs.next() ---------------------------------------------------->|            |
    |                 |       (true)  -- new Usuario(getInt/getString/getBoolean) -------------------->|
    |                 |       (false) -- return null                                     |            |
    |                 |-- cierra rs, ps, con (try-with-resources) ------------------------|            |
    |<-- Usuario / null|                  |                  |                |            |            |
    |  Usuario != null -> abre VentanaEventos                                              |            |
    |  null  -> estaInactivo(u,c): true  -> "Usuario inactivo" (V2)                        |            |
    |                              false -> "Usuario o contraseña incorrectos" (V1)        |            |
    |  SQLException -> JOptionPane con Conexion.mensajeError(e) (V4)                       |            |
```

Lectura en una línea: **Ventana, DAO, `Conexion`, `PreparedStatement`, `ResultSet` y de vuelta un objeto `Usuario` (o `null`)**. La ventana nunca ve SQL ni `ResultSet`: solo recibe un `Usuario` o `null`.

El código real de la ventana está en la sección 9.1 (`VentanaLogin.btnIngresarActionPerformed`).

## 9. Paquete `vista` (ventanas Swing)

Archivos: `vista/VentanaLogin.java` y `vista/VentanaEventos.java` (más sus `.form`). **Qué es `initComponents()`:** método **generado por NetBeans** desde el archivo `.form` del diseñador gráfico (crea y posiciona los componentes con `GroupLayout`). Está entre las marcas `//GEN-BEGIN` y `//GEN-END`, no se edita a mano y no se explica línea por línea; lo que se defiende son los métodos de eventos y los auxiliares que sí escribió el equipo. Las ventanas **no contienen SQL**: llaman a `UsuarioDAO` / `EventoDAO` y traducen los errores con `Conexion.mensajeError`. `EventoDAO` y `Conexion` no cambiaron por la vista.

### 9.1 `VentanaLogin` · L1, V1, V2, V4

```java
public VentanaLogin() {
    initComponents();
    getContentPane().setBackground(new java.awt.Color(238, 242, 247));
    usuarioDAO = new UsuarioDAO();
    this.setLocationRelativeTo(null);
}
```

Constructor: arma la ventana (`initComponents`), pinta el fondo, crea **un** `UsuarioDAO` que reutiliza y centra la ventana (`setLocationRelativeTo(null)` = centro de la pantalla).

```java
private void btnIngresarActionPerformed(java.awt.event.ActionEvent evt) {
    String usuario = txtUsuario.getText().trim();
    String clave = new String(txtClave.getPassword());
    if (usuario.isEmpty() || clave.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos.", "Acceso",
                JOptionPane.WARNING_MESSAGE);
        txtUsuario.requestFocus();
        return;
    }
    try {
        Usuario u = usuarioDAO.validar(usuario, clave);
        if (u == null) {
            if (usuarioDAO.estaInactivo(usuario, clave)) {
                JOptionPane.showMessageDialog(this, "Usuario inactivo.", ...);
            } else {
                JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos.", ...);
            }
            txtClave.setText("");
            txtUsuario.requestFocus();
            return;
        }
        new VentanaEventos(u).setVisible(true);
        this.dispose();
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, Conexion.mensajeError(e), "Error de base de datos",
                JOptionPane.ERROR_MESSAGE);
    }
}
```

- Es el manejador del botón **Ingresar** (`addActionListener(this::btnIngresarActionPerformed)` en `initComponents`).
- `txtUsuario.getText().trim()`: texto sin espacios en los extremos. `txtClave` es un `JPasswordField`: su `getPassword()` devuelve un `char[]` (no un `String`), que se convierte con `new String(...)`.
- **V1 (campos vacíos)**: si falta alguno, muestra "Usuario o contraseña incorrectos.", devuelve el foco y hace `return` **sin tocar la base**.
- `usuarioDAO.validar(usuario, clave)`: **L1**. Si devuelve un `Usuario`, se abre `VentanaEventos(u)` pasándole el usuario (para el saludo) y se cierra el login con `dispose()`.
- Si devuelve `null` hay dos causas, y `estaInactivo` las distingue: **V2** "Usuario inactivo." o **V1** "Usuario o contraseña incorrectos.". Se borra la clave escrita y se vuelve al campo de usuario.
- `catch (SQLException e)`: **V4** (y A5). Cualquier error de base (MySQL apagado, clave del `root` mala, base inexistente) se muestra con `Conexion.mensajeError(e)` en un `JOptionPane` y **la aplicación sigue abierta**.
- `btnSalirActionPerformed` hace `System.exit(0)`.
- `main`: elige el aspecto *Nimbus* y abre la ventana con `EventQueue.invokeLater` (las ventanas Swing se crean en el hilo de eventos).

### 9.2 `VentanaEventos` · C1 a C5, V3, V4

Atributos propios: `usuario` (quién ingresó), `eventoDAO` e **`idSeleccionado`**: id del evento cargado en el formulario (0 = ninguno); lo usan Actualizar y Eliminar para el `WHERE id_evento = ?`.

El constructor recibe el `Usuario`, crea el `EventoDAO`, escribe el saludo (nombre completo y rol), deja por defecto `activo` marcado y prioridad media, registra el `MouseAdapter` de la tabla y llama a **`cargarTabla()`**, de modo que la tabla muestra lo que hay en MySQL apenas abre.

**`cargarTabla()` (C2).**

```java
DefaultTableModel modelo = (DefaultTableModel) tblEventos.getModel();
modelo.setRowCount(0);
try {
    ArrayList<Evento> lista = eventoDAO.listar();
    for (int i = 0; i < lista.size(); i++) {
        Evento e = lista.get(i);
        modelo.addRow(new Object[]{e.getIdEvento(), e.getCodigoEvento(), ...});
    }
} catch (SQLException e) { mostrarErrorBD(e); }
```

`setRowCount(0)` vacía la `JTable`; `eventoDAO.listar()` trae los datos **desde MySQL**; cada `Evento` se agrega como fila (`addRow` con un arreglo, un valor por columna; `activo` se muestra como "Sí"/"No"). Así la tabla refleja la base (la pauta no acepta datos que existan solo en la `JTable`). Se vuelve a llamar tras guardar, actualizar y eliminar.

**`leerFormulario()` (validaciones y paso de formulario a `Evento`).** Devuelve un `Evento` armado, o `null` si algo está mal (y ya avisó con `mostrarError`):

- `codigo` y `titulo` no pueden quedar vacíos (son `NOT NULL`).
- `LocalDate.parse(...)` y `LocalTime.parse(...)` convierten el texto; si el formato no es `yyyy-MM-dd` / `HH:mm` lanzan `DateTimeParseException`, que se captura y se avisa. `hora_fin` es opcional (vacío = `null`).
- Latitud y longitud: o vienen **las dos** o ninguna (porque `tieneUbicacion()` exige ambas). `Double.valueOf` lanza `NumberFormatException` si no es número.
- Textos opcionales vacíos (`lugar`, `detalle`) se guardan como `null` (el DAO envía `NULL`).
- Prioridad: `Integer.parseInt((String) cboPrioridad.getSelectedItem())`; duración: `(Integer) spnDuracion.getValue()`; activo: `chkActivo.isSelected()`.
- No valida el JSON ni que `hora_fin > hora_inicio`: eso lo rechaza la **base** con los `CHECK` (error 4025, 3819 o 3140) y `mensajeError` lo traduce.

**`btnGuardarActionPerformed` (C1).**

```java
Evento e = leerFormulario();
if (e == null) { return; }
try {
    eventoDAO.insertar(e);
    idSeleccionado = e.getIdEvento();
    cargarTabla();
    JOptionPane.showMessageDialog(this, "Evento " + e.getCodigoEvento() + " guardado.");
} catch (SQLException ex) { mostrarErrorBD(ex); }
```

Valida, inserta (el DAO deja el id generado en `e`), recuerda ese id en `idSeleccionado`, **recarga la tabla desde la base** y avisa. Si el código está repetido, `insertar` lanza `SQLException` 1062: **no inserta** y se muestra "El registro ya existe." (**V3**).

**`btnBuscarActionPerformed` (C3).** Lee el código; si está vacío avisa. `eventoDAO.buscar(codigo)`: si devuelve `null`, "No existe un evento con ese código."; si no, `cargarFormulario(e)` rellena los campos y guarda el `idSeleccionado` (los `null` de lugar, coordenadas y hora fin se muestran como texto vacío).

**`btnActualizarActionPerformed` (C4).** Si `idSeleccionado == 0` pide primero buscar o seleccionar. Si no: `leerFormulario()`, `e.setIdEvento(idSeleccionado)` (el `WHERE id_evento = ?` del DAO usa ese id) y `eventoDAO.actualizar(e)`: si devuelve `true` recarga la tabla y avisa; si `false`, "No se actualizó ningún evento.".

**`btnEliminarActionPerformed` (C5).** Sin selección, avisa. Luego **pide confirmación** con `JOptionPane.showConfirmDialog(..., YES_NO_OPTION)`; si la respuesta no es `YES_OPTION`, `return` sin borrar (lo que exige la pauta, A10). Si confirma: `eventoDAO.eliminar(idSeleccionado)`; si borró, `limpiarFormulario()`, `cargarTabla()` y avisa.

**`tblEventosClicked()` (MouseAdapter de la tabla).** En el constructor, `tblEventos.addMouseListener(new MouseAdapter() { mouseClicked ... })` llama a este método. Toma la fila seleccionada (`getSelectedRow()`; si es menor que 0, no hace nada), lee el código de la columna 1 con `getValueAt(fila, 1)`, llama a `eventoDAO.buscar(codigo)` y carga el formulario: así se edita o elimina el evento seleccionado.

**Auxiliares.** `limpiarFormulario()` vacía los campos y deja `idSeleccionado = 0` (duración 60, prioridad 2, activo); `mostrarError(mensaje, campo)` muestra el aviso y devuelve el foco al campo; `mostrarErrorBD(SQLException)` muestra `Conexion.mensajeError(e)`; `btnCerrarSesionActionPerformed` abre un `VentanaLogin` nuevo y cierra esta ventana.

**Patrón de todos los botones:** validar, llamar al DAO dentro de `try`, refrescar la `JTable` desde la base y, en el `catch (SQLException)`, mostrar el mensaje traducido sin cerrar la aplicación (V3 y V4).

## 10. Pruebas JUnit (`CalendarioApp/test/`, 24 pruebas)

Una prueba automática llama al método real y compara el resultado con el esperado (`assertEquals`, `assertNull`, `assertTrue`...). Son JUnit 4 y, salvo `EventoTest`, **necesitan MySQL arriba** con el script cargado (usan la base real).

| Clase de test | Pruebas | Qué comprueba |
|---|---|---|
| `conexion/ConexionTest` | 4 | `probarConexion()` devuelve `true` con MySQL arriba. `mensajeError` con SQLState `08S01` o código 1045 devuelve "No fue posible conectar con la base de datos." (V4) y con 1062 devuelve "El registro ya existe." (V3). Las `SQLException` se arman a mano, sin base. |
| `dao/UsuarioDAOTest` | 6 | Login (L1, V1, V2): `admin`/`Admin2026` entra con rol ADMIN; clave incorrecta devuelve `null` y `estaInactivo` da `false`; **`testClaveDistingueMayusculas`**: `ADMIN2026` y `admin2026` no entran (la columna `clave` es `utf8mb4_bin`); campos vacíos devuelven `null`; `inactivo1` no entra pero `estaInactivo` da `true`; y **la inyección SQL** (`admin' -- ` y `' OR '1'='1`) no entra, lo que demuestra el `PreparedStatement`. |
| `dao/EventoDAOTest` | 11 | CRUD (C1 a C5) y V3 con códigos `TEST-JU-*` que se borran en `@After`: `listar` trae 5 o más; `insertar` + `buscar` devuelve id mayor que 0 y datos correctos; el `POINT` se guarda y se lee (latitud y longitud, con tolerancia); sin ubicación ni hora fin quedan `null`; el JSON se guarda y se lee; código repetido lanza `SQLException` y `mensajeError` dice "El registro ya existe."; JSON inválido y prioridad 7 los rechaza la base (`expected = SQLException.class`); `buscar` de un código inexistente devuelve `null`; `actualizar` cambia título y prioridad y mantiene el id; `eliminar` borra, `buscar` da `null` y un segundo `eliminar` da `false`. |
| `modelo/EventoTest` | 3 | Sin base de datos: el constructor vacío usa los `DEFAULT` de la tabla; `tieneUbicacion()` solo es `true` con latitud y longitud; el constructor completo y los getters. |

## 11. Preguntas de defensa que salen de este código

1. **¿Qué hace `DriverManager.getConnection`?** Busca el driver registrado que acepte la URL `jdbc:mysql:` y abre una `Connection` con usuario y clave. Lanza `SQLException` si no puede.
2. **¿Qué es un `PreparedStatement` y por qué usarlo?** Una sentencia precompilada con `?`; los valores viajan como datos, lo que evita la inyección SQL y permite reutilizar la sentencia.
3. **¿Qué diferencia hay entre `executeQuery` y `executeUpdate`?** Ver la sección 6.
4. **¿Qué hace `rs.next()`?** Avanza el cursor a la siguiente fila y devuelve `false` cuando no quedan.
5. **¿Para qué sirve `wasNull()`?** Para saber si el último valor leído era `NULL` (los `getInt`/`getDouble` devuelven 0).
6. **¿Cómo obtiene Java el id generado?** Con `RETURN_GENERATED_KEYS` y `ps.getGeneratedKeys()`.
7. **¿Por qué `setNull(4, Types.DOUBLE)`?** Porque un `double` primitivo no puede ser `null`; `setNull` envía `NULL` indicando el tipo SQL.
8. **¿Qué pasa si se repite un código?** `executeUpdate` lanza `SQLException` con código 1062; `mensajeError` la convierte en "El registro ya existe".
9. **¿Qué se cierra y cuándo?** `ResultSet`, `PreparedStatement` y `Connection`, en orden inverso, al terminar el bloque `try`.
10. **¿Por qué la lectura de `ubicacion` usa `ST_X` y `ST_Y`?** El `POINT` es binario; con `ST_X` (longitud) y `ST_Y` (latitud) se obtienen números.
11. **¿Qué código de error es "base de datos no existe"?** 1049. **¿"Acceso denegado"?** 1045. **¿Falla de conexión?** SQLState que empieza con `08`.
12. **¿Dónde está la contraseña de la base?** Solo en `Conexion` (constante `CLAVE`); ningún otro archivo la repite.
