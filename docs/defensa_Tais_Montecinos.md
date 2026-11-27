# Defensa individual — Tais Montecinos

**Evaluación 4 · Fundamentos de POO (IEI-085) · CalendarioApp · Profesor: Pablo · Defensa: viernes 4 de diciembre de 2026 (ensayo del equipo: martes 1 de diciembre) · Tiempo: unos 15 minutos**

Tu parte: **script SQL completo (S1–S5), modelo de datos y las clases `Usuario` y `Evento`.** Esta guía no contradice a `docs/guia_defensa.md`; la complementa con tu parte al detalle y con todo lo que el profesor Pablo puede hacer para comprobar que **tú** entiendes lo que entregaste.

---

## 0. Cómo se juega esta defensa (léelo primero)

El profesor Pablo avisó: **«un 7 si todo funciona y se defiende, y un 1 si no saben defender, porque es sinónimo de usar ChatGPT»**. Ya probó a Nicolás con una versión del script que traía `JSON(255)` y `POINT(150)` (sintaxis inválida) para ver si quien lo presentaba sabía lo que escribía. Como Nicolás ya se defendió bien, **las preguntas más difíciles van a caer en ti y en Rodolfo**. Eso no es mala señal: es la oportunidad de mostrar que entiendes.

Qué evalúa la Parte B de la rúbrica (6 puntos cada criterio):

| Criterio | Qué piden | Dónde lo preparas en esta guía |
|---|---|---|
| B1. Demostración | Ejecutar la operación pedida **y verificarla en MySQL sin apoyo** | Secciones 5 y 13 |
| B2. Dominio del contenido | Explicar el modelo de datos y el flujo JDBC con lenguaje técnico | Secciones 2, 9 y 10 |
| B3. Aportes | Aportes evidenciados en la bitácora; explicar con seguridad tu código | Sección 1 (pendiente de la bitácora), 3 y 9 |
| B4. Explicación de código | Ubicar y explicar el método DAO o la sentencia SQL pedida | Secciones 2, 3, 6 y 11 |
| B5. Pregunta técnica al azar | Responder bien y aplicarlo al proyecto | Secciones 7, 10, 12 y 15 |

Reglas de oro:

1. **Habla con tus palabras, no leas.** Si el profe nota que recitas, desconfía. Usa los guiones de la sección 9 como esqueleto, no como texto.
2. **Si te piden modificar algo, hazlo en vivo y en voz alta** (sección 11): «voy a agregar la columna; `ALTER TABLE` con `ADD COLUMN`; pongo el tipo; `NOT NULL DEFAULT`…». Equivocarte y corregir leyendo el error suma; quedarte congelada resta.
3. **Si no sabes algo, dilo y razona** (sección 14). Inventar es lo que más te cuesta.
4. **Habla de lo que escribiste con la seguridad de quien lo escribió:** cuando el profe señale una línea del script, tú sabes por qué está ahí.

---

## 1. Mi parte en 30 segundos

«Diseñé la base de datos `calendario_app` y escribí el script `sql/calendario_bd.sql` completo: creo la base (S1), la tabla `usuario` (S2), la tabla `evento` con 14 columnas, incluidos un `POINT` y un `JSON` (S3), y cargo 3 usuarios (S4) y 5 eventos de prueba (S5). Las reglas de integridad (código único, prioridad 1 a 3, duración mayor que 0, hora de fin mayor que la de inicio, JSON válido) las dejé **en la base**, con `UNIQUE`, `NOT NULL`, `DEFAULT` y cuatro `CHECK`. En Java escribí el modelo: `Usuario` y `Evento`, una clase por tabla, con atributos `private`, dos constructores y getters/setters.»

| Archivo | Qué contiene |
|---|---|
| `sql/calendario_bd.sql` | S1 a S5, verificaciones y pruebas de restricciones |
| `CalendarioApp/src/modelo/Usuario.java` | Una fila de `usuario` (6 atributos) |
| `CalendarioApp/src/modelo/Evento.java` | Una fila de `evento` (14 atributos con `latitud`/`longitud` en lugar de `ubicacion`) y `tieneUbicacion()` |
| `CalendarioApp/test/modelo/EventoTest.java` | 3 pruebas JUnit 4 del modelo (no necesitan base de datos) |

**Pendiente que depende de ti (B3).** La rúbrica pide «aportes evidenciados en la bitácora». En `docs/bitacora.md` tus tres filas tienen la fecha como `_(fecha)_` y el propio documento dice que es una propuesta a ajustar. Antes del ensayo del martes 1 de diciembre: reemplaza `_(fecha)_` por las fechas reales de tus tres filas (diseño de `evento` y S1–S3, inserción S4–S5 con las pruebas de restricciones, clases `Usuario` y `Evento`) y corrige cualquier detalle que no sea cierto. Lo que la bitácora diga, tienes que poder explicarlo; lo que no hiciste, sácalo.

---

## 2. Script SQL línea por línea

### 2.1 Cabecera (líneas 1 a 9)

```sql
-- Evaluación 4 (20 %) · POO IEI-085 · Unidad III: JDBC y MySQL
-- Base de datos: calendario_app
-- Motor probado: MariaDB 10.6 (contenedor poo-mysql). Compatible con
--                MySQL 8.0.16+ (desde ahí se aplican los CHECK).
```

Son comentarios (`--` hasta el fin de línea); no se ejecutan. Dicen qué motor se probó: **MariaDB 10.6**. (El comentario del archivo nombra el contenedor `poo-mysql`; en el `docker-compose.yml` del repo el contenedor se llama `calendario-mysql`, y es el nombre que usan los comandos de esta guía.) Los `CHECK` solo se aplican de verdad desde MySQL 8.0.16 (antes se aceptaban pero se ignoraban en silencio); MariaDB los aplica desde la 10.2.1.

### 2.2 S1 — Crear y usar la base (líneas 11 a 16)

```sql
CREATE DATABASE IF NOT EXISTS calendario_app
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_spanish_ci;
```

- `IF NOT EXISTS`: si la base ya existe, no da error.
- `CHARACTER SET utf8mb4`: el juego de caracteres completo de UTF-8 (4 bytes por carácter). Permite tildes, la «ñ» y emojis. El `utf8` antiguo de MySQL es de 3 bytes y no sirve para todo.
- `COLLATE utf8mb4_spanish_ci`: la regla de comparación y orden del texto. `spanish` ordena la «ñ» como en español; `ci` = *case insensitive* (no distingue mayúsculas).

```sql
USE calendario_app;
```

Deja `calendario_app` como base activa: todo lo que sigue se crea dentro de ella.

### 2.3 DROP (líneas 18 a 20)

```sql
DROP TABLE IF EXISTS evento;
DROP TABLE IF EXISTS usuario;
```

Borra las tablas si existen para poder **re-ejecutar el script desde cero** (instalación limpia). `IF EXISTS` evita el error cuando todavía no existen. No importa el orden porque las tablas no tienen clave foránea. **Ojo:** `DROP TABLE` borra la estructura **y todos los datos**, sin papelera; por eso el script es para instalación limpia, no para correr sobre una base con datos reales.

### 2.4 S2 — Tabla `usuario` (líneas 22 a 30)

```sql
CREATE TABLE IF NOT EXISTS usuario (
    id_usuario      INT AUTO_INCREMENT PRIMARY KEY,
```

- `INT`: entero de 4 bytes.
- `AUTO_INCREMENT`: la base genera el siguiente número (1, 2, 3…) al insertar; yo no lo escribo.
- `PRIMARY KEY`: identifica la fila; es única y nunca nula. Solo una por tabla.

```sql
    nombre_usuario  VARCHAR(30) NOT NULL UNIQUE,
```

- `VARCHAR(30)`: texto de largo variable, máximo 30.
- `NOT NULL`: el campo es obligatorio.
- `UNIQUE`: no puede repetirse. Es el identificador de negocio del login.

```sql
    clave           VARCHAR(64) NOT NULL COLLATE utf8mb4_bin,  -- _bin: distingue mayúsculas
```

- `COLLATE utf8mb4_bin`: compara **byte a byte**. Con la collation `_ci` de la base, `ADMIN2026` y `Admin2026` serían iguales y el login entraría con mayúsculas cambiadas. Con `_bin` son distintas. Solo la clave lleva `_bin`; el resto sigue con la de la base.
- 64 de largo: deja espacio para un *hash* si más adelante se cifra la clave (un SHA-256 en hexadecimal mide exactamente 64 caracteres).

```sql
    nombre_completo VARCHAR(80) NOT NULL,
    rol             VARCHAR(20) NOT NULL DEFAULT 'OPERADOR',
    activo          BOOLEAN     NOT NULL DEFAULT TRUE
);
```

- `nombre_completo`: obligatorio, hasta 80.
- `rol`: `DEFAULT 'OPERADOR'`; si el `INSERT` no trae rol, toma ese valor.
- `activo BOOLEAN`: en MySQL/MariaDB es un alias de `TINYINT(1)` (1 verdadero, 0 falso). `DEFAULT TRUE` = 1. Con `activo = 0` el usuario no puede entrar.

### 2.5 S3 — Tabla `evento` (líneas 32 a 67)

```sql
CREATE TABLE evento (
    id_evento        INT AUTO_INCREMENT PRIMARY KEY,
```

Clave técnica autogenerada, como en `usuario`. Aquí uso `CREATE TABLE` sin `IF NOT EXISTS` porque el `DROP` de arriba garantiza que no existe.

```sql
    codigo_evento    VARCHAR(20)  NOT NULL UNIQUE,
```

**Identificador de negocio**: lo escribe el usuario (`EVT-0001`) y por él se busca (C3). Obligatorio y único. Es distinto de la PK: la PK es técnica; este código es el que el usuario ve. El `UNIQUE` crea además un índice automáticamente (en `SHOW CREATE TABLE` aparece como `UNIQUE KEY codigo_evento`), así que buscar por código es rápido.

```sql
    titulo           VARCHAR(100) NOT NULL,
    lugar            VARCHAR(150) NULL,
```

`titulo` es obligatorio. `lugar` es el nombre del lugar en texto; `NULL` explícito significa que es opcional.

```sql
    ubicacion        POINT        NULL,
```

Tipo **espacial** `POINT`: guarda una coordenada como un solo dato (X = **longitud**, Y = **latitud**). Opcional: un cumpleaños en casa puede no tener coordenadas. Se escribe con `POINT(lon, lat)` y se lee con `ST_X` y `ST_Y`. **No lleva largo**: es un tipo de tamaño fijo (ver sección 10).

```sql
    detalle          JSON         NULL,
```

Columna `JSON` para datos libres y variables (notas, participantes, cosas que llevar) sin crear más tablas. Opcional. **No lleva largo.** En MariaDB `JSON` es un alias de `LONGTEXT` y la propia base le agrega una validación `json_valid(detalle)` (por eso `SHOW CREATE TABLE` muestra `longtext ... CHECK (json_valid(...))` pegado a la columna); en MySQL 8 es un tipo nativo que valida solo. Además dejé mi `CHECK` explícito de más abajo, para que la regla se vea en el script y valga igual en cualquier motor.

```sql
    categoria        VARCHAR(30)  NOT NULL DEFAULT 'personal',
```

Obligatoria, pero con valor por defecto, así que el `INSERT` puede omitirla.

```sql
    fecha_evento     DATE         NOT NULL,
    hora_inicio      TIME         NOT NULL,
    hora_fin         TIME         NULL,
```

- `DATE`: solo fecha (`2026-11-23`). Es el tipo que exige la pauta.
- `TIME`: solo hora (`09:00:00`).
- `hora_fin` es opcional: un evento de todo el día no la tiene.

```sql
    duracion_minutos INT          NOT NULL DEFAULT 60,
    prioridad        INT          NOT NULL DEFAULT 2,   -- 1 = alta, 2 = media, 3 = baja
```

Numéricos `INT` con default. La prioridad usa una convención: 1 alta, 2 media, 3 baja.

```sql
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
```

`activo` es el booleano de la pauta. `fecha_creacion` es de **auditoría**: `CURRENT_TIMESTAMP` la rellena sola con la fecha y hora del servidor al insertar.

```sql
    CONSTRAINT chk_evento_duracion  CHECK (duracion_minutos > 0),
    CONSTRAINT chk_evento_prioridad CHECK (prioridad BETWEEN 1 AND 3),
    CONSTRAINT chk_evento_horas     CHECK (hora_fin IS NULL OR hora_fin > hora_inicio),
    CONSTRAINT chk_evento_detalle   CHECK (detalle IS NULL OR JSON_VALID(detalle))
) ENGINE = InnoDB;
```

Cuatro reglas que **cada fila debe cumplir**; `CONSTRAINT nombre` les pone nombre para que el error diga cuál falló.

1. `duracion_minutos > 0`: no hay eventos de duración cero o negativa.
2. `prioridad BETWEEN 1 AND 3`: solo 1, 2 o 3.
3. `hora_fin IS NULL OR hora_fin > hora_inicio`: si hay hora de fin, tiene que ser posterior. El `IS NULL OR` es necesario: comparar con `NULL` da «desconocido» y la regla debe dejar pasar los eventos sin hora de fin.
4. `detalle IS NULL OR JSON_VALID(detalle)`: si hay detalle, debe ser JSON válido.

`ENGINE = InnoDB`: el motor de almacenamiento transaccional (soporta transacciones y claves foráneas). Es el estándar de MySQL.

### 2.6 S4 — Usuarios de prueba (líneas 69 a 73)

```sql
INSERT INTO usuario (nombre_usuario, clave, nombre_completo, rol, activo) VALUES
('admin',     'Admin2026', 'Administrador',    'ADMIN',    TRUE),
('operador1', 'Oper2026',  'Operador Uno',     'OPERADOR', TRUE),
('inactivo1', 'Inac2026',  'Usuario Inactivo', 'OPERADOR', FALSE);
```

Un solo `INSERT` con tres filas. Se nombran las columnas, así que `id_usuario` queda fuera y lo genera `AUTO_INCREMENT`. Tres casos de prueba: un ADMIN, un OPERADOR y un usuario **inactivo** (`FALSE` = 0) para probar la validación V2. Vienen del script base de la pauta.

### 2.7 S5 — Eventos de prueba (líneas 75 a 96)

```sql
INSERT INTO evento
    (codigo_evento, titulo, lugar, ubicacion, detalle, categoria,
     fecha_evento, hora_inicio, hora_fin, duracion_minutos, prioridad)
VALUES
    ('EVT-0001', 'Reunión de equipo', 'Sala 3, Santo Tomás Santiago',
     POINT(-70.6506, -33.4378),
     '{"notas": "Revisión semanal de avances", "participantes": ["Tais", "Rodolfo", "Nicolás"]}',
     'trabajo', '2026-11-23', '09:00:00', '10:00:00', 60, 1),
```

- La lista de columnas **omite** `id_evento`, `activo` y `fecha_creacion`: toman su valor automático (`AUTO_INCREMENT`, `DEFAULT TRUE`, `CURRENT_TIMESTAMP`).
- `POINT(-70.6506, -33.4378)`: **longitud primero, latitud después** (X, Y). Son las coordenadas de Santiago.
- El JSON va como texto entre comillas simples; las comillas dobles de adentro son las del JSON. El `CHECK` lo valida al insertar.
- Fecha `'2026-11-23'` (formato `AAAA-MM-DD`) y hora `'09:00:00'`.

```sql
    ('EVT-0003', 'Cumpleaños de mamá', 'Casa',
     NULL,
     '{"notas": "Comprar torta"}',
     'personal', '2026-11-28', '00:00:00', NULL, 1440, 2),
```

Caso especial: `ubicacion` y `hora_fin` en `NULL` (evento sin coordenadas y de todo el día: 1440 minutos = 24 horas). Demuestra que las columnas opcionales funcionan.

El script inserta **4 eventos** en este bloque (`EVT-0001`, `0002`, `0003`, `0004`) y un quinto aparte (siguiente bloque), para un total de **5 filas**. Los que tienen ubicación son 0001, 0002 y 0004.

```sql
INSERT INTO evento (codigo_evento, titulo, fecha_evento, hora_inicio)
VALUES ('EVT-0005', 'Pagar cuentas', '2026-12-05', '20:00:00');
```

**Inserción mínima**: solo las 4 columnas obligatorias que no tienen `DEFAULT`. El resto se completa solo: `categoria = 'personal'`, `duracion_minutos = 60`, `prioridad = 2`, `activo = 1`, `fecha_creacion = ahora`, y `lugar`, `ubicacion`, `detalle`, `hora_fin` quedan en `NULL`. Es la prueba de que los `DEFAULT` funcionan.

### 2.8 Verificación (líneas 102 a 117)

```sql
DESCRIBE usuario;
DESCRIBE evento;
```

Muestran las columnas, tipos, si admiten `NULL`, las claves (`PRI`, `UNI`) y los defaults.

```sql
SELECT id_usuario, nombre_usuario, rol, activo FROM usuario;
SELECT id_evento, codigo_evento, titulo, fecha_evento, hora_inicio, prioridad, activo
FROM evento ORDER BY fecha_evento, hora_inicio;
```

Lista las filas; el segundo `SELECT` ordena por fecha y, a igualdad, por hora de inicio.

```sql
SELECT codigo_evento, ST_X(ubicacion) AS longitud, ST_Y(ubicacion) AS latitud
FROM evento WHERE ubicacion IS NOT NULL;
```

`ST_X` devuelve la coordenada X (longitud) y `ST_Y` la Y (latitud). `AS` les da un alias legible. `WHERE ubicacion IS NOT NULL` descarta los eventos sin coordenadas (en SQL se compara con `IS NULL`, no con `= NULL`: `= NULL` nunca da verdadero y devuelve cero filas).

```sql
SELECT codigo_evento, JSON_UNQUOTE(JSON_EXTRACT(detalle, '$.notas')) AS notas
FROM evento WHERE detalle IS NOT NULL;
```

`JSON_EXTRACT(detalle, '$.notas')` saca el campo `notas` (`$` es la raíz del documento) y lo entrega con comillas; `JSON_UNQUOTE` se las quita.

### 2.9 Pruebas de restricciones (líneas 119 a 136)

Están comentadas (`--`) y se descomentan **de una en una**; todas deben fallar:

| Prueba | Error esperado |
|---|---|
| Código repetido `EVT-0001` | 1062 Duplicate entry |
| Falta el título | 1364 Field 'titulo' doesn't have a default value |
| Prioridad 7 | MariaDB 4025 (`chk_evento_prioridad`) / MySQL 3819 |
| JSON inválido `'esto no es json'` | MariaDB 4025 (en MariaDB el nombre que muestra es `evento.detalle`, el de la validación automática de la columna JSON) / MySQL 3140 |

---

## 3. `Usuario.java` y `Evento.java` línea por línea

### 3.1 La idea general

Cada clase del paquete `modelo` representa **una fila de su tabla**. Cada columna es un atributo `private` (encapsulamiento: solo se accede por getters y setters). Los DAO de Rodolfo y Nicolás crean estos objetos al leer de la base y los reciben al escribir. El modelo **no tiene SQL ni JDBC**: solo datos.

### 3.2 `Usuario.java`

```java
package modelo;
```

La clase pertenece al paquete `modelo`.

```java
private int idUsuario;
private String nombreUsuario;
private String clave;
private String nombreCompleto;
private String rol;
private boolean activo;
```

Seis atributos = seis columnas de `usuario`. La correspondencia es directa: `id_usuario` INT → `int`; `VARCHAR` → `String`; `BOOLEAN` → `boolean`. Pasa del estilo SQL `snake_case` al estilo Java `camelCase`. Son `private`.

```java
public Usuario() { }
```

Constructor vacío: permite crear el objeto y rellenarlo después con los setters.

```java
public Usuario(int idUsuario, String nombreUsuario, String clave,
        String nombreCompleto, String rol, boolean activo) {
    this.idUsuario = idUsuario; ...
}
```

Constructor completo: recibe todos los valores. `this.x = x` distingue el atributo (`this.x`) del parámetro (`x`). Lo usa el DAO al leer una fila.

```java
public int getIdUsuario() { return idUsuario; }
public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
```

Getter y setter: la forma controlada de leer y modificar el atributo privado. Se repite el par para cada atributo. La única excepción de nombre es `isActivo()`: por convención JavaBeans, el getter de un `boolean` se llama `isXxx`.

### 3.3 `Evento.java`

```java
import java.time.LocalDate;
import java.time.LocalTime;
```

Tipos de fecha y hora de `java.time` (Java 8+): `LocalDate` = solo fecha (`DATE`), `LocalTime` = solo hora (`TIME`). Son inmutables y más claros que `java.util.Date`. El DAO los convierte a `java.sql.Date`/`Time` con `valueOf` y de vuelta con `toLocalDate()`/`toLocalTime()`.

```java
private int idEvento;
private String codigoEvento;
private String titulo;
private String lugar;
private Double latitud;
private Double longitud;
private String detalle;
private String categoria;
private LocalDate fechaEvento;
private LocalTime horaInicio;
private LocalTime horaFin;
private int duracionMinutos;
private int prioridad;
private boolean activo;
```

Catorce atributos privados. Detalles que hay que saber explicar:

- **`latitud` y `longitud` son `Double` (objeto) y no `double` (primitivo)** porque la columna `ubicacion` puede ser `NULL`. Un `double` primitivo no puede valer `null` (valdría `0.0`, que es una coordenada real: el golfo de Guinea). Con `Double` el `null` significa «sin ubicación». Por la misma razón `hora_fin` es `LocalTime` (objeto) y puede ser `null`.
- **`ubicacion` (POINT) no existe como atributo**: se parte en dos números, `latitud` y `longitud`. Es más simple en Java; el DAO arma el `POINT(lon, lat)` al guardar y usa `ST_X`/`ST_Y` al leer.
- **`detalle` es `String`**: el JSON viaja como texto; la validación la hace la base con `JSON_VALID`.
- **`int` y `boolean` primitivos** en `idEvento`, `duracionMinutos`, `prioridad` y `activo` porque esas columnas son `NOT NULL`: siempre tienen valor.

```java
public Evento() {
    categoria = "personal";
    duracionMinutos = 60;
    prioridad = 2;
    activo = true;
}
```

El constructor vacío **replica los `DEFAULT` de la tabla**: `categoria 'personal'`, `duracion_minutos 60`, `prioridad 2`, `activo TRUE`. Así un `new Evento()` se comporta igual que un `INSERT` mínimo. Lo comprueba `testConstructorVacioUsaLosDefaultDeLaTabla`.

```java
public Evento(int idEvento, String codigoEvento, ... , boolean activo) { ... }
```

Constructor completo de 14 parámetros, en el mismo orden que los atributos; lo usa el DAO al leer una fila (`leerFila`).

```java
public boolean tieneUbicacion() {
    return latitud != null && longitud != null;
}
```

Devuelve `true` solo si **ambas** coordenadas existen. El DAO la usa al guardar: si es `true`, escribe `POINT(?, ?)` con los dos números; si es `false`, guarda `NULL` con `setNull`. Un `POINT` necesita las dos coordenadas; con una sola no se puede armar. Lo comprueba `testTieneUbicacionSoloConLatitudYLongitud` (falso sin nada, falso con solo latitud, verdadero con ambas).

Después vienen los getters y setters de los 14 atributos, con el mismo patrón que en `Usuario` (`isActivo` para el booleano).

### 3.4 Las pruebas (`EventoTest`)

Tres pruebas JUnit 4, sin base de datos: (1) el constructor vacío trae los defaults; (2) `tieneUbicacion()` exige latitud y longitud; (3) el constructor completo y los getters devuelven lo que se les pasó. Es **JUnit 4 y no 5** porque en un proyecto Ant de NetBeans `ant test` no ejecuta JUnit 5. En total el proyecto tiene 24 pruebas (`ConexionTest` 4, `UsuarioDAOTest` 6, `EventoDAOTest` 11, `EventoTest` 3).

---

## 4. Reglas de negocio que viven en la base

| Regla | Dónde se implementa | Error si se viola |
|---|---|---|
| Código de evento único | `codigo_evento VARCHAR(20) NOT NULL UNIQUE` | **1062** `Duplicate entry 'EVT-0001' for key 'codigo_evento'` |
| Nombre de usuario único | `nombre_usuario ... UNIQUE` | **1062** |
| Solo usuarios activos entran | Columna `activo` (`DEFAULT TRUE`) + filtro `AND activo = TRUE` en el `SELECT` de `UsuarioDAO.validar()`. La base guarda el dato; **el filtro lo aplica la consulta** | No hay error SQL: la consulta devuelve cero filas y la app dice «Usuario inactivo.» |
| Prioridad entre 1 y 3 | `CHECK (prioridad BETWEEN 1 AND 3)` | MariaDB **4025** `CONSTRAINT chk_evento_prioridad failed` / MySQL **3819** |
| Duración mayor que 0 | `CHECK (duracion_minutos > 0)` | MariaDB **4025** `chk_evento_duracion` / MySQL **3819** |
| `hora_fin` mayor que `hora_inicio` | `CHECK (hora_fin IS NULL OR hora_fin > hora_inicio)` | MariaDB **4025** `chk_evento_horas` / MySQL **3819** |
| `detalle` es JSON válido | `CHECK (detalle IS NULL OR JSON_VALID(detalle))` (más la validación automática de MariaDB para el tipo `JSON`) | MariaDB **4025** `CONSTRAINT evento.detalle failed` / MySQL **3140** `Invalid JSON text` |
| La clave distingue mayúsculas | `clave ... COLLATE utf8mb4_bin` | Sin error: `ADMIN2026` simplemente no coincide y el login falla |
| Campos obligatorios | `NOT NULL` sin `DEFAULT` (`codigo_evento`, `titulo`, `fecha_evento`, `hora_inicio`, y en `usuario` `nombre_usuario`, `clave`, `nombre_completo`) | **1364** `Field 'titulo' doesn't have a default value` (si se omite) o **1048** `Column 'titulo' cannot be null` (si se manda `NULL`) |

Puntos finos:

- Todos los `CHECK` fallidos dan 4025 en MariaDB (la clase `Conexion` lo traduce en `mensajeError` junto con 3819 y 3140 de MySQL, con un único texto: «Los datos no cumplen las reglas de la tabla (prioridad, horas, duracion o JSON).»). Los códigos **1364 y 1048 no tienen traducción propia**: la app muestra «Error de base de datos: » seguido del mensaje original del servidor.
- Desde la ventana, dos de los cuatro `CHECK` ya los previene la interfaz: la prioridad es un combo con 1, 2 y 3, y la duración un spinner de 1 a 1440. Los que sí pueden llegar a la base desde la app son **hora de fin anterior a la de inicio** y **detalle que no es JSON** (en `VentanaEventos.leerFormulario()` no se validan). Sirven para demostrar el `CHECK` en vivo.
- La **ventaja de dejar las reglas en la base** es que valen para cualquier programa que escriba en ella, no solo para esta aplicación.

---

## 5. Demostración en MySQL por consola

Todos los comandos se corren desde Git Bash (o PowerShell) en tu PC, con el contenedor `calendario-mysql` arriba (el de `docker-compose.yml`; compruébalo con `docker ps`). `docker exec calendario-mysql` ejecuta un comando dentro del contenedor; `mariadb` es el cliente de consola; `-uroot` es el usuario (sin clave); `calendario_app` es la base; `-e "..."` ejecuta la sentencia y sale. Si el contenedor no está arriba, el comando dice `No such container`: levantar con `docker compose up -d` desde la carpeta del proyecto.

**Alternativa más simple (recomendada para ti): phpMyAdmin.** Abre http://localhost:8081 (entra solo como `root`, sin clave), haz clic en `calendario_app` en el panel izquierdo, abre la pestaña **SQL**, pega **una sentencia a la vez** (sin el `docker exec ... -e "..."`, solo lo que va entre comillas) y presiona **Continuar**. Todo lo de esta sección y de las secciones 11 y 13 funciona igual allí. En phpMyAdmin la pestaña **Estructura** muestra lo mismo que `DESCRIBE`.

**Estructura de `evento`**

```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "DESCRIBE evento"
```

Esperado: 14 filas. `id_evento` con `PRI` y `auto_increment`; `codigo_evento` con `UNI`; `ubicacion` tipo `point`; `detalle` tipo `longtext` (en MariaDB el alias de JSON) o `json` en MySQL; los defaults `personal`, `60`, `2`, `1` y `current_timestamp()`.

**Estructura de `usuario`**

```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "DESCRIBE usuario"
```

Esperado: 6 filas; `nombre_usuario` con `UNI`; `rol` con default `OPERADOR`.

**Ver las restricciones tal como quedaron**

```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "SHOW CREATE TABLE evento\G"
```

`\G` muestra el resultado en vertical, más legible. Esperado: la definición completa con los cuatro `CONSTRAINT chk_evento_...`, el `PRIMARY KEY (id_evento)` y el `UNIQUE KEY codigo_evento`. Sirve para probar que los `CHECK` están en la tabla. (En phpMyAdmin: sentencia `SHOW CREATE TABLE evento` sin el `\G`.)

**Los datos**

```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "SELECT id_evento, codigo_evento, titulo, fecha_evento, hora_inicio, prioridad, activo FROM evento ORDER BY fecha_evento, hora_inicio"
```

Esperado: al menos 5 filas, de `EVT-0001` (23-nov) a `EVT-0005` (5-dic), ordenadas por fecha. Si se hicieron pruebas desde la app puede haber más.

**Defaults de la inserción mínima**

```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "SELECT codigo_evento, categoria, duracion_minutos, prioridad, activo, fecha_creacion FROM evento WHERE codigo_evento = 'EVT-0005'"
```

Esperado: `personal`, `60`, `2`, `1` y una fecha de creación con la hora del servidor, aunque ese `INSERT` solo trajo 4 campos.

**POINT con ST_X / ST_Y**

```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "SELECT codigo_evento, ST_X(ubicacion) AS longitud, ST_Y(ubicacion) AS latitud FROM evento WHERE ubicacion IS NOT NULL"
```

Esperado: tres filas, `EVT-0001` (longitud `-70.6506`, latitud `-33.4378`), `EVT-0002` (`-70.6483`, `-33.4569`) y `EVT-0004` (igual que el 0001). Los eventos 0003 y 0005 no aparecen (no tienen ubicación). Decir en voz alta: «X es la longitud, Y es la latitud».

**JSON con JSON_EXTRACT**

```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "SELECT codigo_evento, JSON_EXTRACT(detalle, '$.notas') AS con_comillas, JSON_UNQUOTE(JSON_EXTRACT(detalle, '$.notas')) AS notas FROM evento WHERE detalle IS NOT NULL"
```

Esperado: la columna `con_comillas` muestra `"Revisión semanal de avances"` y la columna `notas` el mismo texto sin comillas. Muestra la diferencia entre `JSON_EXTRACT` y `JSON_UNQUOTE`. (El evento 0005 no aparece: su `detalle` es `NULL`.)

**Un elemento de un arreglo JSON**

```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "SELECT JSON_UNQUOTE(JSON_EXTRACT(detalle, '$.participantes[0]')) AS primero FROM evento WHERE codigo_evento = 'EVT-0001'"
```

Esperado: `Tais`. El `[0]` es la primera posición del arreglo (se cuenta desde 0).

**Probar cada restricción.** Todas deben fallar; lo que importa mostrar es el mensaje de error de MariaDB. **Aviso:** un `INSERT` que falla no deja filas, pero el contador `AUTO_INCREMENT` puede haber avanzado, así que el siguiente evento que insertes podría tener un `id_evento` más alto de lo esperado. Es normal (ver pregunta de la sección 10).

UNIQUE (código repetido):

```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "INSERT INTO evento (codigo_evento, titulo, fecha_evento, hora_inicio) VALUES ('EVT-0001', 'Duplicado', '2026-12-10', '10:00:00')"
```

Esperado: `ERROR 1062 (23000): Duplicate entry 'EVT-0001' for key 'codigo_evento'`.

NOT NULL (sin título):

```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "INSERT INTO evento (codigo_evento, fecha_evento, hora_inicio) VALUES ('EVT-0099', '2026-12-10', '10:00:00')"
```

Esperado: `ERROR 1364 (HY000): Field 'titulo' doesn't have a default value`.

CHECK prioridad:

```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "INSERT INTO evento (codigo_evento, titulo, fecha_evento, hora_inicio, prioridad) VALUES ('EVT-0098', 'Mala prioridad', '2026-12-10', '10:00:00', 7)"
```

Esperado: ``ERROR 4025 (23000): CONSTRAINT `chk_evento_prioridad` failed for `calendario_app`.`evento` ``.

CHECK duración:

```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "INSERT INTO evento (codigo_evento, titulo, fecha_evento, hora_inicio, duracion_minutos) VALUES ('EVT-0096', 'Duracion cero', '2026-12-10', '10:00:00', 0)"
```

Esperado: error 4025 con `chk_evento_duracion`.

CHECK horas (fin antes del inicio):

```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "INSERT INTO evento (codigo_evento, titulo, fecha_evento, hora_inicio, hora_fin) VALUES ('EVT-0095', 'Horas al reves', '2026-12-10', '10:00:00', '09:00:00')"
```

Esperado: error 4025 con `chk_evento_horas`.

CHECK JSON inválido:

```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "INSERT INTO evento (codigo_evento, titulo, fecha_evento, hora_inicio, detalle) VALUES ('EVT-0097', 'Mal JSON', '2026-12-10', '10:00:00', 'esto no es json')"
```

Esperado: ``ERROR 4025 (23000): CONSTRAINT `evento.detalle` failed for `calendario_app`.`evento` ``. Ojo: en MariaDB el nombre que sale es `evento.detalle` (la validación automática del tipo `JSON`), **no** `chk_evento_detalle`; esa regla mía existe pero la automática salta primero. Si el profe pregunta, lo explicas tal cual.

**La clave distingue mayúsculas (`_bin`)**

```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "SELECT COUNT(*) FROM usuario WHERE nombre_usuario='admin' AND clave='Admin2026'; SELECT COUNT(*) FROM usuario WHERE nombre_usuario='admin' AND clave='ADMIN2026'"
```

Esperado: `1` y luego `0`. Con `Admin2026` coincide; con `ADMIN2026` no, gracias a `utf8mb4_bin`. (El nombre de usuario, en cambio, no distingue mayúsculas: `ADMIN` también encuentra a `admin`.)

**Después de las pruebas:** ninguna de las inserciones fallidas deja filas, así que el conteo no cambia:

```bash
docker exec calendario-mysql mariadb -uroot calendario_app -e "SELECT COUNT(*) FROM evento"
```

Esperado: el mismo número que antes de probar (5 en una base recién cargada). Si algo falla en vivo, el plan B es hacer lo mismo en phpMyAdmin.

---

## 6. Lo mínimo que debes saber de las partes de Rodolfo y Nicolás

**Rodolfo — `Conexion`, `UsuarioDAO`, login**

- `Conexion` es la única clase con la URL, el usuario y la clave de la base; `conectar()` llama a `DriverManager.getConnection`. Si cambia algo, se toca un solo archivo.
- `Conexion.mensajeError(SQLException)` traduce los códigos de error (1062 duplicado, 1045 clave mala, 1049 base inexistente, estado `08` conexión caída, 4025/3819/3140 `CHECK`) a mensajes en español. De mi tabla de la sección 4, los que se traducen ahí son 1062 y los `CHECK`; 1364 y 1048 salen como «Error de base de datos: …» con el texto original.
- `UsuarioDAO.validar(nombre, clave)` ejecuta `SELECT ... WHERE nombre_usuario = ? AND clave = ? AND activo = TRUE` con `executeQuery()`. Si el `ResultSet` trae fila, devuelve un `Usuario`; si no, `null`.
- `estaInactivo(...)` cuenta si existe esa combinación con `activo = FALSE` para dar el mensaje «Usuario inactivo.» (V2).
- Se usa `PreparedStatement` con `?` para evitar la inyección SQL; todo va en try-with-resources.

**Nicolás — `EventoDAO`, CRUD, POINT/JSON desde Java**

- `EventoDAO` tiene `insertar`, `listar`, `buscar` (por `codigo_evento`), `actualizar` (por `id_evento`) y `eliminar` (por `id_evento`). Los de lectura (`listar` y `buscar`) usan `executeQuery()`; los de escritura (`insertar`, `actualizar` y `eliminar`) usan `executeUpdate()`.
- `insertar` usa `RETURN_GENERATED_KEYS` para leer el `id_evento` que generó `AUTO_INCREMENT`.
- El `POINT` se escribe con `POINT(?, ?)` pasando **longitud y luego latitud** (`setDouble`); si `tieneUbicacion()` es falso, se usa `setNull(..., Types.DOUBLE)` en los dos parámetros (y `POINT(NULL, NULL)` da `NULL`). Aquí entra mi método.
- Al leer, la consulta trae `ST_X(ubicacion)` y `ST_Y(ubicacion)` y `rs.wasNull()` distingue un `NULL` de un `0.0` real (por eso mis atributos son `Double`).
- El JSON se guarda con `setString` y se lee con `getString`; la base lo valida con `JSON_VALID`.
- Fechas y horas: `Date.valueOf(LocalDate)` / `Time.valueOf(LocalTime)` al escribir y `toLocalDate()` / `toLocalTime()` al leer.
- Las ventanas (`vista`) no tienen SQL: `VentanaEventos` llama al DAO; `Eliminar` pide confirmación con `JOptionPane`.

---

## 7. Quince preguntas probables sobre tu parte

1. **¿Qué es una clave primaria y cuál es la de `evento`?** La columna que identifica cada fila: única y nunca nula; solo una por tabla. Es `id_evento` (`INT AUTO_INCREMENT PRIMARY KEY`).
2. **¿Por qué `id_evento` y además `codigo_evento`?** `id_evento` es una clave técnica que genera la base. `codigo_evento` (`EVT-0001`) es el identificador de negocio que escribe el usuario y por el que se busca; es `UNIQUE` y `NOT NULL`.
3. **¿Diferencia entre `PRIMARY KEY` y `UNIQUE`?** La PK es una por tabla y no admite `NULL`; `UNIQUE` puede haber varios por tabla y evita repetidos. En `evento`: 1 PK y 1 `UNIQUE`.
4. **¿Qué hace `AUTO_INCREMENT`?** Genera el siguiente entero al insertar, así el `INSERT` no trae el id. El DAO lo recupera con `RETURN_GENERATED_KEYS`.
5. **¿Qué hace `DEFAULT` y dónde lo demuestra?** Da un valor cuando el `INSERT` no lo trae. `EVT-0005` se insertó con 4 campos y tomó `personal`, 60, 2 y `TRUE`.
6. **¿Qué es un `CHECK` y cuántos tiene?** Una regla que cada fila debe cumplir. Cuatro: duración > 0, prioridad entre 1 y 3, `hora_fin` > `hora_inicio` y JSON válido. Falla con error 4025 (MariaDB) o 3819/3140 (MySQL).
7. **¿Por qué el `CHECK` de las horas lleva `hora_fin IS NULL OR`?** Porque `hora_fin` es opcional. Comparar `NULL > hora_inicio` da «desconocido», y sin ese `OR` rechazaría los eventos sin hora de fin.
8. **¿Para qué `COLLATE utf8mb4_bin` en `clave`?** La collation de la base (`_ci`) no distingue mayúsculas, y `ADMIN2026` entraba como `Admin2026`. `_bin` compara bytes exactos, así que la clave sí distingue mayúsculas y minúsculas.
9. **¿Por qué `utf8mb4` y no `utf8`?** `utf8mb4` es UTF-8 completo de 4 bytes (tildes, «ñ», emojis); el `utf8` de MySQL es de 3 bytes y está obsoleto.
10. **¿Por qué una columna `POINT` y no dos columnas decimales?** Guarda la ubicación como un solo dato geoespacial y permite funciones espaciales. Se escribe `POINT(longitud, latitud)` y se lee con `ST_X` (longitud) y `ST_Y` (latitud).
11. **¿Por qué `POINT(lon, lat)` en ese orden?** X es el eje horizontal (longitud) e Y el vertical (latitud). En `POINT(-70.6506, -33.4378)` la latitud es -33.4378.
12. **¿Por qué una columna `JSON` y cómo se valida?** Para datos libres y variables sin crear tablas extra. Se valida con `CHECK (detalle IS NULL OR JSON_VALID(detalle))`; y se lee con `JSON_EXTRACT(detalle, '$.notas')`.
13. **¿Por qué `latitud` y `longitud` son `Double` y no `double`?** Porque la columna `ubicacion` puede ser `NULL`; un `double` primitivo no puede ser `null` y valdría `0.0`, que es una coordenada real. Con `Double`, `null` significa «sin ubicación».
14. **¿Para qué sirve `tieneUbicacion()` y qué pasa si falta una coordenada?** Devuelve `true` solo si `latitud` y `longitud` no son `null`. El DAO la usa para decidir entre `POINT(?, ?)` y `NULL`. Con una sola coordenada no se puede formar el `POINT`, así que se guarda `NULL`.
15. **¿Por qué el constructor vacío de `Evento` asigna valores?** Para que `new Evento()` tenga los mismos defaults que la tabla (`personal`, 60, 2, `true`). Lo comprueba `testConstructorVacioUsaLosDefaultDeLaTabla`.

Extras por si caen: *¿Qué es `BOOLEAN`?* Alias de `TINYINT(1)`; en consola se ve 1/0 y en Java es `boolean`. *¿Por qué `DROP TABLE IF EXISTS`?* Para re-ejecutar el script desde cero sin error. *¿Por qué las tablas no tienen clave foránea?* La pauta las pide independientes. *¿Por qué la clave está en texto plano?* Porque la pauta entrega el script así; en un sistema real se guardaría un *hash*.

---

## 8. Tres ganchos mnemotécnicos con autoevaluación

Cada gancho lleva al término técnico exacto. Primero lo dices, luego respondes sin mirar.

### 8.1 Restricciones del script — «**P**edro **U**sa **N**ombres **D**istintos y **C**orrectos»

**P**RIMARY KEY · **U**NIQUE · **N**OT NULL · **D**EFAULT · **C**HECK. Las cinco restricciones del script.

- *Autoevaluación:* ¿Qué columna de `evento` ilustra cada una? (R: `id_evento`, `codigo_evento`, `titulo`, `categoria`, `prioridad`). ¿Cuál restricción rechaza `prioridad = 7` y con qué error? (R: `CHECK`, 4025 en MariaDB).

### 8.2 POINT — «**X** cruza de lado a lado: **LON**; **Y** sube al cielo: **LAT**»

Imagina una cruz en el mapa: la **X** se estira de lado a lado (longitud) y la **Y** apunta hacia arriba (latitud). Orden: **X antes que Y, lon antes que lat**. Se escribe `POINT(lon, lat)` y se lee `ST_X` = lon, `ST_Y` = lat.

- *Autoevaluación:* En `POINT(-70.6506, -33.4378)`, ¿cuál es la latitud y con qué función se lee? (R: -33.4378, con `ST_Y`).

### 8.3 `Double` vs `double` — «**D**oble con **D** mayúscula puede ser **D**esconocido»

`Double` (objeto) admite `null` = desconocido = columna `NULL`; `double` (primitivo) nunca: si falta, vale `0.0` y miente. Mayúscula = objeto = puede ser nulo.

- *Autoevaluación:* ¿Por qué `latitud` es `Double`? ¿Qué método de `Evento` revisa que no sean `null`? (R: porque `ubicacion` es opcional; `tieneUbicacion()`).

### 8.4 Tipos sin largo — «**J**uan y **P**ablo no miden: **J**SON y **P**OINT»

`JSON` y `POINT` **no llevan paréntesis con número**; el largo es solo para `VARCHAR` (y `CHAR`). Si ves `JSON(255)` o `POINT(150)`, el script está mal escrito.

- *Autoevaluación:* ¿Qué tipos de `evento` llevan largo entre paréntesis? (R: solo los `VARCHAR`: `codigo_evento(20)`, `titulo(100)`, `lugar(150)`, `categoria(30)`).

El calendario de repaso está en la sección 16.

---

## 9. Explícalo con tus palabras (guiones de 30 a 60 segundos)

Son un **esqueleto**, no un libreto. Léelos una vez, tápalos y dilo con tus palabras mirando a alguien. Cada uno dura entre 30 y 60 segundos hablado con calma. Si te sale distinto pero con las mismas ideas, está perfecto.

### 9.1 El modelo de datos

«Mi base se llama `calendario_app` y tiene dos tablas. `usuario` guarda quién puede entrar: nombre de usuario, clave, nombre completo, rol y si está activo. `evento` es el corazón del tema, una agenda: cada fila es un evento con su código, título, lugar, fecha, hora de inicio y de fin, duración, prioridad, categoría y si está activo. Además tiene dos tipos especiales que pidió el profesor: `POINT` para la ubicación en el mapa y `JSON` para datos libres como notas o participantes. Las reglas importantes las dejé dentro de la base con restricciones, para que se cumplan sin importar desde dónde se escriba.»

### 9.2 Por qué dos tablas sin clave foránea

«Las dos tablas son independientes a propósito. `usuario` solo sirve para el acceso: valida el login y nada más. `evento` es el CRUD del tema. Un evento no pertenece a un usuario en este diseño, la pauta pedía tablas separadas, así que no hay nada que relacionar y una clave foránea no tendría sentido. Si más adelante quisiéramos saber quién creó cada evento, agregaríamos una columna `id_usuario` en `evento` con una clave foránea hacia `usuario`. Hoy no es necesario, y por eso el `DROP TABLE` puede borrar en cualquier orden.»

### 9.3 PK, UNIQUE, NOT NULL, DEFAULT y CHECK

«Una **clave primaria** es la columna que identifica cada fila: única y nunca nula; en `evento` es `id_evento` y la genera la base con `AUTO_INCREMENT`. `UNIQUE` evita que un valor se repita pero puede haber varios por tabla; lo usé en `codigo_evento` porque dos eventos no pueden tener el mismo código. `NOT NULL` obliga a que el campo tenga valor, como el título. `DEFAULT` da un valor automático cuando el `INSERT` no lo trae: la prioridad parte en 2 y la categoría en `personal`. Y `CHECK` es una regla que cada fila debe cumplir, por ejemplo que la prioridad esté entre 1 y 3. Si algo se viola, la base rechaza la fila con un error.»

### 9.4 Por qué `POINT` en este tema

«Un evento de agenda pasa en un lugar, y el lugar se puede mostrar en un mapa. Guardar la ubicación como `POINT` me da la coordenada completa en un solo dato, en vez de dos decimales sueltos que podrían quedar desordenados o incompletos, y además la base trae funciones para trabajar con ella, como `ST_X` y `ST_Y` para leerla o `ST_Distance_Sphere` para medir distancias entre dos eventos. Lo importante es el orden: primero la longitud y después la latitud, `POINT(-70.6506, -33.4378)`. Es opcional porque un cumpleaños en la casa puede no tener coordenadas.»

### 9.5 Por qué `JSON` en este tema

«Los eventos tienen datos que cambian según el evento: una reunión tiene participantes, un control médico tiene cosas que llevar, un cumpleaños solo una nota. Si hiciera una columna para cada cosa, tendría muchas vacías, o tendría que crear tablas extra para los participantes. Con una columna `JSON` guardo esos datos variables como un documento dentro de la misma fila, y los saco con `JSON_EXTRACT`, por ejemplo `'$.participantes[0]'` para el primero. La base valida que sea JSON de verdad, así que no entra texto cualquiera. Es opcional, porque algunos eventos no necesitan detalle.»

### 9.6 El flujo JDBC en tres pasos

«Para hablar con la base desde Java se hace siempre lo mismo, en tres pasos. Primero la **`Connection`**: abro la conexión con `Conexion.conectar()`, que usa el driver. Segundo el **`PreparedStatement`**: escribo el SQL con signos `?` y relleno cada uno con `setString`, `setInt`, `setDouble`; así los datos viajan separados del código y no hay inyección SQL. Y tercero ejecuto: si es un `SELECT` uso `executeQuery()` y obtengo un **`ResultSet`** que recorro con `next()`; si es `INSERT`, `UPDATE` o `DELETE` uso `executeUpdate()`, que me devuelve cuántas filas cambiaron. Todo va en un try-with-resources para que se cierre solo.»

### 9.7 Mi clase `Evento` en 40 segundos

«`Evento` representa una fila de la tabla: tiene un atributo privado por cada columna, 14 en total, salvo `ubicacion`, que la separé en `latitud` y `longitud` para que en Java sea más simple. Esos dos y la hora de fin son objetos, `Double` y `LocalTime`, porque pueden ser nulos; el resto de números son primitivos porque sus columnas son obligatorias. Tiene un constructor vacío que copia los valores por defecto de la tabla, uno completo que usa el DAO al leer, getters y setters, y un método `tieneUbicacion()` que dice si hay las dos coordenadas. No tiene nada de SQL: solo datos.»

### 9.8 Qué hace mi script de punta a punta

«El script crea la base con soporte completo de tildes y la `ñ`, borra las tablas si existían para poder repetirlo, crea `usuario` y `evento` con sus restricciones, carga tres usuarios, uno de ellos inactivo para probar esa validación, y cinco eventos de prueba, uno con ubicación y JSON, uno sin ubicación y de todo el día, y uno mínimo con solo los cuatro campos obligatorios para demostrar que los `DEFAULT` funcionan. Al final tiene consultas de verificación y pruebas de restricciones comentadas para mostrar que cada regla rechaza datos malos.»

---

## 10. Las trampas del profe: `JSON(255)`, `POINT(150)` y otras

### 10.1 La historia del `JSON(255)` y `POINT(150)`

El profesor le entregó a Nicolás una versión del script que tenía:

```sql
detalle   JSON(255)  NULL,     -- inválido
ubicacion POINT(150) NULL,     -- inválido
```

**Por qué está mal:** el número entre paréntesis es un **largo** y solo tiene sentido en tipos de texto de largo variable como `VARCHAR(100)` («hasta 100 caracteres»). `JSON` no tiene un tamaño que elegir: guarda un documento de cualquier largo (en MariaDB es un `LONGTEXT`). `POINT` tampoco: es una coordenada de **tamaño fijo** (unos 25 bytes), siempre dos números. Por eso estos tipos se escriben **sin paréntesis**.

**Qué error da** (probado en MariaDB 10.6, que es el motor del proyecto):

```text
CREATE TABLE t (id INT, detalle JSON(255))
ERROR 1064 (42000): You have an error in your SQL syntax; check the manual that corresponds
to your MariaDB server version for the right syntax to use near '(255))' at line 1
```

- `1064` es el error de **sintaxis**: la sentencia está mal escrita, no es un problema de datos. El mensaje señala dónde se rompió: `near '(255))'`, justo en el paréntesis.
- Con `POINT(150)`: en MySQL 8 es el mismo error 1064. **Ojo, dato fino que sí probé:** MariaDB 10.6 **acepta** `POINT(150)` en silencio e ignora el número (la tabla queda con `point` a secas), pero `JSON(255)` sí lo rechaza. Aun así `POINT(150)` está mal: es un descuido que otro motor no perdona.

**Cómo se corrigió** (así está en `sql/calendario_bd.sql`):

```sql
ubicacion        POINT        NULL,
detalle          JSON         NULL,
```

**Cómo lo explicas si el profe te lo muestra** (en voz alta, sin apuro):

> «Esa versión está mal: `JSON` y `POINT` no llevan largo entre paréntesis. El número es para `VARCHAR`, que es texto de largo variable. `JSON` guarda un documento completo y `POINT` es una coordenada de tamaño fijo, así que no hay nada que dimensionar. Si se ejecuta, `JSON(255)` da el error 1064 de sintaxis. Lo correcto es `ubicacion POINT NULL` y `detalle JSON NULL`. Mi script lo tiene así y puedo mostrar que corre sin errores.»

Y para cerrarlo con evidencia, ejecuta el script o muestra `DESCRIBE evento` (los tipos salen `point` y `longtext`/`json`, sin números).

### 10.2 Otras trampas probables de SQL

Para cada una: lo que el profe puede preguntar o mostrar, y una respuesta corta y correcta. Todo lo marcado como «probado» se ejecutó en la base real.

| Pregunta o trampa del profe | Respuesta modelo |
|---|---|
| «¿Y si escribo `nombre VARCHAR` sin el número?» | Error 1064: `VARCHAR` **necesita** su largo, porque es texto de largo variable con un máximo (`VARCHAR(100)`). Es al revés que `JSON` y `POINT`, que **no** lo llevan. (Probado.) |
| «¿`BOOLEAN` es un tipo de verdad?» | No: en MySQL y MariaDB es un alias de `TINYINT(1)`; `TRUE` es 1 y `FALSE` es 0. Por eso en consola `activo` se ve como 1 o 0. Ojo: la base **no impide** guardar un 5 (probado: `activo = 5` se acepta); si quisiera blindarlo agregaría `CHECK (activo IN (0, 1))`. En Java sí es `boolean` (`getBoolean`/`setBoolean`). |
| «¿Qué pasa si inserto `'Sala 3'` en el `POINT`?» | Error 1416: `Cannot get geometry object from data you send to the GEOMETRY field`. Un `POINT` solo acepta una coordenada, `POINT(lon, lat)`. El nombre del lugar va en la columna `lugar`, que es texto. (Probado.) |
| «¿Y si el JSON lleva comillas simples adentro, como `{'notas': 'hola'}`?» | Se rechaza (4025): el JSON exige **comillas dobles** en claves y textos. Como el valor completo va entre comillas simples en el `INSERT`, las de adentro tienen que ser dobles: `'{"notas": "hola"}'`. (Probado.) |
| «¿Qué hace `DROP TABLE`? ¿Se pierden los datos?» | Sí: borra la tabla **con todos sus datos** y no se puede deshacer. Por eso el script es de instalación limpia. Distinto de `DELETE` (borra filas, deja la tabla) y de `TRUNCATE` (vacía la tabla). Y ojo: `docker compose down -v` también borra el volumen con la base; el script de carga solo corre con el volumen vacío. |
| «¿Por qué `utf8mb4_bin` en la clave?» | Para que la comparación sea byte a byte y distinga mayúsculas: con la collation `_ci` de la base, `ADMIN2026` entraba como `Admin2026`. Lo comprobé con dos `SELECT`: uno devuelve 1 y el otro 0. |
| «¿Y si el servidor es un MySQL viejo, anterior a 8.0.16?» | Los `CHECK` se **aceptan pero se ignoran**: la tabla se crea sin error y las reglas no se aplican. Por eso en el script dejo anotado que necesita MySQL 8.0.16 o superior, o MariaDB 10.2.1+ (el motor probado: MariaDB 10.6). |
| «¿Diferencia entre `DATE`, `TIME` y `TIMESTAMP`?» | `DATE` es solo la fecha (`2026-11-30`). `TIME` es solo la hora (`09:00:00`). `TIMESTAMP` es fecha **y** hora juntas (`2026-11-27 09:59:23`), se guarda en UTC y cubre de 1970 a 2038; lo usé en `fecha_creacion` para que la base la complete sola con `CURRENT_TIMESTAMP`. Para la fecha del evento usé `DATE`, que es el tipo que exige la pauta. |
| «¿Qué pasa si inserto un código repetido?» | Error 1062 `Duplicate entry 'EVT-0001' for key 'codigo_evento'`, por el `UNIQUE`. No se inserta nada. La app lo muestra como «El registro ya existe.» |
| «¿Por qué el id saltó del 5 al 7?» | Porque `AUTO_INCREMENT` **nunca reutiliza ni devuelve** un número: si un `INSERT` falla (por ejemplo, un código repetido), el número que había reservado se pierde. Es normal y no es un error. (Probado.) |
| «¿Por qué guardas fecha y hora en columnas separadas y no un `DATETIME`?» | La pauta pide `DATE` para la fecha; además la hora de fin es opcional y un evento de todo el día no la tiene. Mantenerlas separadas permite filtrar por día sin cálculos. |
| «¿Y si un evento termina después de medianoche?» | Mi `CHECK` (`hora_fin > hora_inicio`) lo rechazaría: es una limitación conocida del diseño. Se resolvería con una columna `fecha_fin` o guardando inicio y fin como `DATETIME`. |
| «¿Para qué `duracion_minutos` si ya hay `hora_fin`?» | Es un dato numérico que pide la pauta y sirve para eventos sin hora de fin (el cumpleaños de todo el día vale 1440). Reconozco que se puede solapar con `hora_fin`; no hay un `CHECK` que los obligue a coincidir. |
| «¿Por qué `WHERE ubicacion = NULL` no devuelve nada?» | Porque comparar con `NULL` da «desconocido», nunca verdadero. Se usa `IS NULL` o `IS NOT NULL`. (Probado: con `= NULL` devuelve cero filas.) |
| «¿Qué es `INT(11)` en el `DESCRIBE`?» | El 11 es solo el ancho de visualización, no limita el valor: un `INT` siempre es de 4 bytes. No es un largo como el de `VARCHAR`. |
| «¿Por qué no hay un índice en `codigo_evento`?» | Sí lo hay: `UNIQUE` crea un índice automáticamente (se ve en `SHOW CREATE TABLE` como `UNIQUE KEY codigo_evento`). |

---

## 11. Modificar en vivo (por si el profe quiere descartar el uso de IA)

El profe puede decir: «agrégame esta columna», «escríbeme esta consulta». Qué hacer: **narra mientras escribes**, escribe despacio, y **comprueba el resultado con un `SELECT`**. Si te equivocas, lee el mensaje de error (sección 12) y corrige. Para practicar sin tocar la tabla real, haz antes una copia y trabaja en ella:

```sql
CREATE TABLE evento_prueba LIKE evento;
INSERT INTO evento_prueba SELECT * FROM evento;
-- ... practicas aquí ...
DROP TABLE evento_prueba;
```

`CREATE TABLE ... LIKE` copia la estructura (incluidos los `CHECK`); `INSERT ... SELECT` copia las filas. Todos los comandos de esta sección están probados contra el esquema real (MariaDB 10.6).

### 11.1 (a) Agregar la columna `color VARCHAR(20) NOT NULL DEFAULT 'azul'`

**En SQL:**

```sql
ALTER TABLE evento ADD COLUMN color VARCHAR(20) NOT NULL DEFAULT 'azul';
```

- `ALTER TABLE evento`: modifico la tabla existente (sin borrarla).
- `ADD COLUMN color`: agrego una columna nueva, al final.
- `VARCHAR(20)`: texto de hasta 20 caracteres (el largo es obligatorio en `VARCHAR`).
- `NOT NULL DEFAULT 'azul'`: es obligatoria y, si no se da valor, toma `azul`.

Qué pasa con las filas que ya existían: como la columna es `NOT NULL`, la base les pone el `DEFAULT` a todas. Compruébalo:

```sql
SELECT codigo_evento, color FROM evento;
```

Esperado: los 5 eventos con `azul`. Un evento nuevo insertado sin `color` también queda `azul`. Para dejar la base como antes: `ALTER TABLE evento DROP COLUMN color;`.

Variante si te piden que quede junto a otra columna: `ADD COLUMN color VARCHAR(20) NOT NULL DEFAULT 'azul' AFTER categoria;` (probado: la deja justo después de `categoria`).

**En `Evento.java`** (paquete `modelo`): lo mínimo, sin tocar el resto:

```java
// 1) atributo, junto a los demás
private String color;

// 2) en el constructor vacío, para replicar el DEFAULT de la tabla
public Evento() {
    categoria = "personal";
    duracionMinutos = 60;
    prioridad = 2;
    activo = true;
    color = "azul";
}

// 3) getter y setter
public String getColor() {
    return color;
}

public void setColor(String color) {
    this.color = color;
}
```

Cómo lo explicas: «agrego el atributo privado, el getter y el setter, y en el constructor vacío le pongo `azul`, igual que el `DEFAULT` de la tabla, para que `new Evento()` siga comportándose como un `INSERT` mínimo. **No toco el constructor completo de 14 parámetros**: si le sumara un parámetro, tendría que cambiar también la llamada en `EventoDAO.leerFila` y la prueba `testConstructorCompletoYGetters`, porque dejarían de compilar. Si el profe pide sumarlo al constructor completo, lo agrego **al final de la lista** y corrijo esas dos llamadas.»

Lo que **no** cambia con este ejercicio: `EventoDAO`. El `INSERT` del DAO nombra sus columnas, así que no menciona `color` y la base le pone `azul`; la app sigue funcionando igual. Para que la aplicación realmente guarde y lea el color habría que agregar `color` al `INSERT`, al `UPDATE`, a la constante `COLUMNAS` y a `leerFila` (y un campo en la ventana). Si te lo preguntan, lo dices: «con esto el modelo y la base quedan listos; falta enchufarlo en el DAO y la ventana».

Prueba opcional que puedes agregar en `EventoTest`: `assertEquals("azul", new Evento().getColor());`.

### 11.2 (b) Eventos de prioridad alta de diciembre

Prioridad alta es `1`. Dos formas correctas:

```sql
SELECT codigo_evento, titulo, fecha_evento, prioridad
FROM evento
WHERE prioridad = 1
  AND fecha_evento BETWEEN '2026-12-01' AND '2026-12-31';
```

```sql
SELECT codigo_evento, titulo, fecha_evento, prioridad
FROM evento
WHERE prioridad = 1
  AND MONTH(fecha_evento) = 12 AND YEAR(fecha_evento) = 2026;
```

- `BETWEEN a AND b` incluye ambos extremos. `MONTH()` y `YEAR()` sacan el mes y el año de un `DATE`.
- **Con los datos de prueba la respuesta es vacía** (0 filas): el único evento de diciembre es `EVT-0005` y es de prioridad 2. Que salga vacío **no es un error**: dilo, y demuestra que la consulta funciona cambiando a `prioridad = 2`: devuelve `EVT-0005 Pagar cuentas`. Para ver filas con prioridad 1 usa noviembre (`MONTH(fecha_evento) = 11`): `EVT-0001`, `EVT-0002` y `EVT-0004`.

### 11.3 (c) `JSON_EXTRACT` del primer participante

```sql
SELECT codigo_evento,
       JSON_UNQUOTE(JSON_EXTRACT(detalle, '$.participantes[0]')) AS primer_participante
FROM evento
WHERE JSON_EXTRACT(detalle, '$.participantes') IS NOT NULL;
```

Esperado: una fila, `EVT-0001` con `Tais`.

- `$` es la raíz del documento; `.participantes` entra al campo; `[0]` es la primera posición (se cuenta desde 0).
- Sin `JSON_UNQUOTE` sale `"Tais"` con comillas.
- El `WHERE` deja solo los eventos que tienen el campo `participantes` (en los demás da `NULL`).
- Variantes: `JSON_EXTRACT(detalle, '$.participantes')` devuelve el arreglo completo `["Tais", "Rodolfo", "Nicolás"]`, y `JSON_LENGTH(detalle, '$.participantes')` cuenta los participantes (3). Evita el operador `->` : lo entiende MySQL 8 pero **MariaDB da error 1064**.

### 11.4 (d) Distancia entre dos eventos con `ST_X`/`ST_Y` o `ST_Distance_Sphere`

Ver las coordenadas de dos eventos:

```sql
SELECT codigo_evento, ST_X(ubicacion) AS longitud, ST_Y(ubicacion) AS latitud
FROM evento
WHERE codigo_evento IN ('EVT-0001', 'EVT-0002');
```

Distancia real en metros entre `EVT-0001` y `EVT-0002`:

```sql
SELECT ROUND(ST_Distance_Sphere(a.ubicacion, b.ubicacion)) AS metros
FROM evento a, evento b
WHERE a.codigo_evento = 'EVT-0001' AND b.codigo_evento = 'EVT-0002';
```

Esperado: unos **2135** metros (≈ 2,1 km).

- `evento a, evento b`: uso la tabla dos veces con alias para comparar dos filas.
- `ST_Distance_Sphere` mide la distancia sobre una esfera (la Tierra) en **metros**. Funciona con dos `POINT`; si algún evento no tiene ubicación, el resultado es `NULL`.
- Alternativa con `ST_X`/`ST_Y` (el teorema de Pitágoras sobre las coordenadas): sale `0.0192`, pero en **grados**, no en metros; sirve para explicar pero no mide kilómetros. Por eso conviene `ST_Distance_Sphere`.
- Variante útil: distancia de un evento a un punto fijo: `ST_Distance_Sphere(ubicacion, POINT(-70.6693, -33.4489))` (longitud primero).

### 11.5 (e) Desactivar un evento por código

```sql
UPDATE evento SET activo = 0 WHERE codigo_evento = 'EVT-0003';
```

- `UPDATE ... SET` cambia el valor; **el `WHERE` es vital**: sin él se desactivan *todos* los eventos. Dilo en voz alta antes de escribirlo.
- Es un «borrado lógico»: el evento sigue en la tabla pero marcado como inactivo. `TRUE` y `1` son lo mismo (`activo = TRUE` también sirve).
- Comprobación: `SELECT codigo_evento, activo FROM evento WHERE activo = 0;` → `EVT-0003`. La base informa «1 row affected» (en consola: `ROW_COUNT()` da 1); con un código que no existe sería 0 filas y **no es un error**.
- Para practicar sin dejar cambios: `START TRANSACTION;` antes, y al terminar `ROLLBACK;` (deshace) en lugar de `COMMIT;`. Si el profe te pide dejarlo, lo vuelves atrás con `UPDATE evento SET activo = 1 WHERE codigo_evento = 'EVT-0003';`.

### 11.6 (f) Contar eventos por categoría

```sql
SELECT categoria, COUNT(*) AS total
FROM evento
GROUP BY categoria
ORDER BY total DESC, categoria;
```

Esperado con los datos de prueba: `personal` 2, y `estudio`, `salud` y `trabajo` con 1 cada una.

- `GROUP BY categoria` agrupa las filas con la misma categoría; `COUNT(*)` cuenta cuántas hay en cada grupo.
- `ORDER BY total DESC, categoria`: primero el mayor y, en empate, por orden alfabético.
- Variante con filtro sobre el resultado: `... GROUP BY prioridad HAVING COUNT(*) > 1` (`HAVING` filtra grupos; `WHERE` filtra filas antes de agrupar).

### 11.7 Otros pedidos probables (todos probados)

| Pedido | Sentencia |
|---|---|
| Buscar eventos cuyo título contenga una palabra | `SELECT codigo_evento, titulo FROM evento WHERE titulo LIKE '%Defensa%';` |
| Eventos de la semana del 23 al 29 de noviembre | `SELECT codigo_evento, fecha_evento FROM evento WHERE fecha_evento BETWEEN '2026-11-23' AND '2026-11-29' ORDER BY fecha_evento;` |
| Cuánto dura cada evento con hora de fin | `SELECT codigo_evento, TIMEDIFF(hora_fin, hora_inicio) FROM evento WHERE hora_fin IS NOT NULL;` |
| Eventos sin ubicación | `SELECT codigo_evento FROM evento WHERE ubicacion IS NULL;` |
| Ver el punto como texto | `SELECT ST_AsText(ubicacion) FROM evento WHERE codigo_evento = 'EVT-0001';` (sale `POINT(-70.6506 -33.4378)`) |
| Borrar un evento por código | `DELETE FROM evento WHERE codigo_evento = 'EVT-0100';` (otra vez, no olvidar el `WHERE`) |

---

## 12. Leer un error

Un error de la base **no es un fracaso**: es información. El método: (1) lee el **número**, (2) lee el **mensaje** (casi siempre nombra la columna o la tabla), (3) di en voz alta qué regla se violó, (4) corrige. Estos mensajes salieron de la base real (MariaDB 10.6):

| Error y mensaje real | Qué significa | Qué dirías |
|---|---|---|
| **1062** `Duplicate entry 'EVT-0001' for key 'codigo_evento'` | Se violó un `UNIQUE`: ese valor ya existe en la columna. | «El código `EVT-0001` ya está registrado y `codigo_evento` es único. Uso otro código, o actualizo el existente en vez de insertar.» En la app se ve «El registro ya existe.» |
| **1364** `Field 'titulo' doesn't have a default value` | El `INSERT` **omitió** una columna `NOT NULL` que no tiene `DEFAULT`. | «Falta el título: la columna es obligatoria y no tiene valor por defecto. Lo agrego a la lista de columnas y de valores.» |
| **4025** `CONSTRAINT chk_evento_prioridad failed for calendario_app.evento` | Se violó un `CHECK` (MariaDB). El nombre dice cuál. En MySQL sería el 3819. | «La prioridad 7 no cumple `BETWEEN 1 AND 3`. Uso 1, 2 o 3.» Si el nombre es `evento.detalle`: «el detalle no es JSON válido». |
| **1048** `Column 'titulo' cannot be null` | Se mandó `NULL` explícito a una columna `NOT NULL`. | «Le pasé `NULL` al título, que es obligatorio. La diferencia con el 1364 es que acá sí se mencionó la columna, pero con valor nulo.» |
| **1146** `Table 'calendario_app.evento_pruba' doesn't exist` | La tabla nombrada no existe (error de tipeo o base equivocada). | «Escribí mal el nombre de la tabla, o estoy en otra base. Reviso con `SHOW TABLES` y corrijo.» |
| **1054** `Unknown column 'colorx' in 'SELECT'` | La columna no existe en esa tabla. | «Ese nombre de columna no existe. Miro `DESCRIBE evento`; si aún no hice el `ALTER TABLE ... ADD COLUMN`, no puede estar.» |
| **1064** `You have an error in your SQL syntax; ... near 'SELEC * FROM evento'` | Error de **sintaxis**: la sentencia está mal escrita. Lo importante es lo que viene después de `near`: ahí se rompió. | «Un error de sintaxis: escribí `SELEC` en vez de `SELECT`. El mensaje me señala el punto donde se rompe.» (Es el mismo error de `JSON(255)`.) |
| **Communications link failure** (Connector/J, `SQLState 08S01`; típico: *«The last packet sent successfully to the server was 0 milliseconds ago…»*) | La aplicación no logra comunicarse con el servidor: la base está apagada, mal el puerto o el host. | «MySQL no está respondiendo: el contenedor está detenido. Lo levanto con `docker start` o `docker compose up -d`. La app lo maneja: `mensajeError` ve el `SQLState` que empieza con `08` y muestra “No fue posible conectar con la base de datos.” sin cerrarse.» |

Otros que pueden aparecer (también reproducidos):

| Error | Cuándo |
|---|---|
| **1045** `Access denied for user 'root'@'localhost'` | Clave o usuario incorrectos al conectar. La app lo traduce como «No fue posible conectar…». |
| **1049** `Unknown database 'nada'` | La base de la URL no existe (aún no se ejecutó el script). Mismo mensaje de conexión. |
| **1292** `Incorrect date value: '15-12-2026'` | La fecha no está en formato `AAAA-MM-DD` (o es inválida, como `2026-13-45`). |
| **1406** `Data too long for column 'codigo_evento'` | El texto excede el largo del `VARCHAR`. |
| **1416** `Cannot get geometry object from data you send to the GEOMETRY field` | Se intentó guardar texto (p. ej. `'Sala 3'`) en una columna `POINT`. |
| **1050** `Table 't' already exists` | Se hizo `CREATE TABLE` de una tabla que ya existe (sin `IF NOT EXISTS`). |

---

## 13. Demostración B1 sin apoyo (guion exacto)

Para cada operación: qué haces en la app, qué dices, y qué consulta escribes **tú**, de memoria, en phpMyAdmin (http://localhost:8081 → base `calendario_app` → pestaña **SQL**). Regla de oro: **di qué esperas ver antes de ejecutarlo**. No necesitas leer esta guía en vivo: practica hasta hacerlo de memoria antes del ensayo del martes.

**Antes de partir** (2 minutos): contenedores arriba (`docker ps`), phpMyAdmin abierto con la pestaña SQL lista, la app en `VentanaLogin`.

### 13.1 Login (L1, V1, V2)

1. **En la app:** usuario `admin`, contraseña `Admin2026` → **Ingresar**. Se abre la ventana de eventos con «Bienvenido/a Administrador (ADMIN)».
2. **Qué dices:** «La ventana llama a `UsuarioDAO.validar()`, que ejecuta un `SELECT` con `nombre_usuario = ?`, `clave = ?` y `activo = TRUE` mediante `executeQuery()`. Si el `ResultSet` trae una fila, devuelve el usuario.»
3. **En phpMyAdmin, escribes:**

```sql
SELECT id_usuario, nombre_usuario, rol, activo FROM usuario;
```

   Esperado: tres filas; `inactivo1` con `activo = 0`.
4. **Y la misma consulta que usa el DAO**, para mostrar que coincide con lo que ocurre:

```sql
SELECT * FROM usuario WHERE nombre_usuario = 'admin' AND clave = 'Admin2026' AND activo = TRUE;
```

   Esperado: una fila. Con `clave = 'ADMIN2026'` sale vacío (por `utf8mb4_bin`); con `inactivo1` / `Inac2026` también sale vacío, y por eso la app dice «Usuario inactivo.» (lo detecta `estaInactivo`).
5. **Si pide un login fallido:** campos vacíos o contraseña mala → «Usuario o contraseña incorrectos.»; `inactivo1` → «Usuario inactivo.».

### 13.2 Insertar (C1)

1. **En la app (con el login ya hecho):** código `EVT-0100`, título `Demo defensa`, lugar `Sala 101`, latitud `-33.4489`, longitud `-70.6693`, detalle `{"notas": "demo"}`, categoría `estudio`, fecha `2026-12-15`, hora de inicio `10:00`, hora de fin `11:00`, duración y prioridad por defecto → **Guardar**. Mensaje: «Evento EVT-0100 guardado.» y aparece en la tabla.
2. **Qué dices:** «`EventoDAO.insertar()` ejecuta un `INSERT` con `POINT(?, ?)` para la ubicación (primero longitud, luego latitud), `executeUpdate()` y `RETURN_GENERATED_KEYS` para leer el `id_evento` que generó `AUTO_INCREMENT`.»
3. **En phpMyAdmin, escribes** (una consulta a la vez):

```sql
SELECT id_evento, codigo_evento, titulo, fecha_evento, hora_inicio, prioridad, activo
FROM evento WHERE codigo_evento = 'EVT-0100';
```

```sql
SELECT codigo_evento, ST_X(ubicacion) AS longitud, ST_Y(ubicacion) AS latitud,
       JSON_UNQUOTE(JSON_EXTRACT(detalle, '$.notas')) AS notas
FROM evento WHERE codigo_evento = 'EVT-0100';
```

   Esperado: la fila con los datos; longitud `-70.6693`, latitud `-33.4489`, notas `demo`. **Di en voz alta:** «la longitud es la X y la latitud es la Y».
4. **Extras que suman:** (a) el mismo código otra vez → «El registro ya existe.» (error 1062; `SELECT COUNT(*) FROM evento WHERE codigo_evento = 'EVT-0100'` sigue en 1). (b) Hora de fin `09:00` con inicio `10:00` → «Los datos no cumplen las reglas de la tabla (prioridad, horas, duracion o JSON).» (el `CHECK`). (c) Detalle `esto no es json` → el mismo mensaje.

### 13.3 Buscar (C3)

1. **En la app:** escribe `EVT-0002` en el código → **Buscar**: el formulario se llena con «Control médico». Con un código que no existe, «No existe un evento con ese código.»
2. **Qué dices:** «`buscar()` ejecuta `SELECT ... WHERE codigo_evento = ?` por el identificador de negocio; si `rs.next()` es verdadero arma el `Evento` con `leerFila`, y si no devuelve `null`.»
3. **En phpMyAdmin:**

```sql
SELECT codigo_evento, titulo, lugar, fecha_evento, hora_inicio, prioridad
FROM evento WHERE codigo_evento = 'EVT-0002';
```

   Esperado: una fila que coincide con lo que mostró el formulario.

### 13.4 Actualizar (C4)

1. **En la app:** busca o selecciona `EVT-0100` en la tabla, cambia el título a `Demo defensa (editado)` y la prioridad a 1 → **Actualizar**. Mensaje: «Evento EVT-0100 actualizado.»
2. **Qué dices:** «`actualizar()` ejecuta `UPDATE ... SET ... WHERE id_evento = ?`; `executeUpdate()` devuelve las filas afectadas y comparo con 1 para saber que cambió exactamente una.»
3. **En phpMyAdmin:**

```sql
SELECT codigo_evento, titulo, prioridad FROM evento WHERE codigo_evento = 'EVT-0100';
```

   Esperado: título `Demo defensa (editado)` y prioridad `1`.

### 13.5 Eliminar (C5)

1. **En la app:** selecciona `EVT-0100` → **Eliminar** → en «¿Está seguro de eliminar el evento EVT-0100?» responde **Sí**. Mensaje: «Evento eliminado.»
2. **Qué dices:** «La confirmación es un `JOptionPane.showConfirmDialog` en la ventana; solo si responde Sí se llama a `eliminar()`, que ejecuta `DELETE ... WHERE id_evento = ?`.»
3. **En phpMyAdmin:**

```sql
SELECT COUNT(*) FROM evento WHERE codigo_evento = 'EVT-0100';
```

   Esperado: `0`. Y para dejar todo como estaba: `SELECT COUNT(*) FROM evento;` debe volver a **5**.

**Si algo se desordena:** si quedó un evento de prueba, `DELETE FROM evento WHERE codigo_evento = 'EVT-0100';` lo borra. Si el próximo `id_evento` es 7 u 8 en vez de 6, es normal (el contador de `AUTO_INCREMENT` no retrocede).

**Si algo falla en vivo:** respira, di «déjeme revisar el mensaje», léelo, y muestra el mismo efecto con el `SELECT` en phpMyAdmin explicando el método del DAO (B4). Mantener la calma vale más que el error.

---

## 14. Si no sabes algo (cómo responder bien)

Equivocarse o no saber no es lo que te baja la nota: **inventar** sí lo hace, y también quedarte en blanco. Un método de cuatro pasos:

1. **Pausa de 3 segundos** y repite la pregunta con tus palabras: «¿me pregunta qué hace X en este código?». Te da tiempo y confirma que entendiste.
2. **Di lo que sí sabes, cerca del tema:** «eso lo escribió Rodolfo / Nicolás, pero sé que...».
3. **Razona en voz alta con el código a la vista**, sin inventar: apunta la línea, lee el nombre, el tipo, lo que recibe y lo que devuelve.
4. **Propón comprobarlo:** «no recuerdo el detalle exacto, pero lo puedo probar acá en la consola y lo veo». Es lo que haría un programador de verdad.

**Frases que sirven:** «No lo tengo memorizado, pero por cómo está escrito, entiendo que...» · «Esa parte la hizo mi compañero, pero lo que veo en el código es que...» · «No estoy segura de la palabra exacta; lo que hace es...» · «Déjeme probarlo y se lo muestro.»

**Frases que NO debes decir:** «eso lo hizo Nicolás, yo no sé», «no sé, no lo miré», «lo sacamos de internet / de una IA». Aunque la parte sea de otro, el criterio B2 pide dominio del proyecto completo.

Ejemplos ligados al código real:

| Te preguntan | Una respuesta honesta que razona |
|---|---|
| «¿Qué hace `rs.wasNull()` en `leerFila`?» (no lo recuerdas) | «Lo veo justo después de `rs.getDouble("latitud")`. Un `getDouble` no puede devolver `null`, devuelve 0.0 cuando la columna es `NULL`. Entonces `wasNull()` pregunta si el último valor leído era nulo, y por eso después asignan `latitud = null`. Eso conecta con mis atributos `Double`.» |
| «¿Qué es `Types.DOUBLE` en `setNull`?» | «Le dice al driver de qué tipo es el `NULL` que está mandando, porque un `NULL` no tiene tipo por sí mismo. Se usa en `cargarParametros` cuando `tieneUbicacion()` es falso.» |
| «¿Para qué sirve `ST_Distance_Sphere`?» (no lo usaste) | «No está en mi script, pero por el nombre mide la distancia sobre una esfera entre dos puntos. Con dos `POINT` me daría la distancia en metros. Lo puedo probar acá y verificarlo.» |
| «¿Qué es un SRID?» | «No lo usé: en mis `POINT` no especifiqué sistema de referencia, así que son coordenadas planas (X, Y). Para medir distancias reales usaría `ST_Distance_Sphere`. Si quisiera un mapa exacto tendría que investigar qué SRID corresponde.» |
| «¿Por qué `executeUpdate() == 1` en el DAO?» | «`executeUpdate` devuelve cuántas filas cambió. Como se actualiza por `id_evento`, que es la clave primaria, solo puede cambiar 0 o 1 fila; comparar con 1 confirma que sí lo encontró.» |
| «¿Por qué `getGeneratedKeys()` devuelve un `ResultSet`?» | «No recuerdo la razón de diseño exacta, pero lo uso así: es una tabla con las columnas autogeneradas, en este caso el `id_evento`, y con `claves.next()` leo la primera fila.» |
| Una sentencia SQL que nunca viste | «No la conozco, pero separándola: ya veo el `SELECT`, el `FROM` y el `WHERE`; lo que no reconozco es esta función. La ejecutaría sola para ver qué devuelve.» |

---

## 15. Autoevaluación final (25 preguntas sin respuesta visible)

Responde en voz alta, **sin mirar**, como si el profe te preguntara. Para cada una, cronométrate: 20 a 40 segundos. Las respuestas están al final, en el **Anexo A**: ahí corriges y marcas cuáles repasar. Haz la prueba el fin de semana y repítela antes del ensayo; las que falles dos veces, vuelves a su sección.

1. ¿Cuántas columnas tiene `evento` y cuáles son `NOT NULL` sin `DEFAULT`?
2. ¿Por qué `JSON(255)` es un error de sintaxis y qué número de error da?
3. ¿Qué devuelve `ST_X` en `POINT(-70.6506, -33.4378)` y por qué es la longitud?
4. ¿Qué error da insertar `EVT-0001` por segunda vez y qué restricción lo provoca?
5. ¿Qué diferencia hay entre el error 1364 y el 1048?
6. ¿Por qué el `CHECK` de las horas lleva `hora_fin IS NULL OR`?
7. ¿Qué es en realidad un `BOOLEAN` en MySQL/MariaDB y qué valores acepta la base?
8. Al ejecutar `ALTER TABLE evento ADD COLUMN color VARCHAR(20) NOT NULL DEFAULT 'azul'`, ¿qué valor tienen los 5 eventos que ya existían?
9. ¿Qué tres cosas agregas a `Evento.java` para el atributo `color`, y por qué no tocas el constructor de 14 parámetros?
10. ¿Por qué `latitud` es `Double` y no `double`?
11. Escribe el `SELECT` de los eventos de prioridad 1 de noviembre de 2026.
12. ¿Cómo sacas el primer participante de `EVT-0001`?
13. ¿Qué diferencia hay entre `JSON_EXTRACT` y `JSON_UNQUOTE`?
14. ¿Cómo desactivas el evento `EVT-0003` sin borrarlo? ¿Qué pasa si te olvidas del `WHERE`?
15. ¿Qué hace `GROUP BY categoria` con `COUNT(*)`?
16. ¿Para qué sirve `COLLATE utf8mb4_bin` en la columna `clave`?
17. ¿Qué diferencia hay entre `DATE`, `TIME` y `TIMESTAMP`, y cuál usaste para la fecha del evento?
18. ¿Por qué el orden de los `DROP TABLE IF EXISTS` no importa?
19. ¿Qué es `AUTO_INCREMENT` y por qué el id puede saltar números?
20. ¿Qué pasa si insertas `'Sala 3'` en la columna `ubicacion`? ¿Dónde va el nombre del lugar?
21. Si desde la app guardas un evento con hora de fin anterior a la de inicio, ¿qué error SQL ocurre y qué mensaje ve el usuario?
22. Nombra el flujo JDBC en tres pasos y qué método usa cada tipo de sentencia.
23. ¿Por qué `PreparedStatement` y no `Statement`?
24. ¿Por qué desde la ventana no se puede disparar el `CHECK` de prioridad ni el de duración, pero desde SQL sí?
25. ¿Para qué sirve `tieneUbicacion()` y cuándo la usa el DAO?

---

## 16. Calendario de repaso espaciado (45 min cada vez)

| Fecha | Qué hacer |
|---|---|
| **Viernes 27 nov** | Leer secciones 0, 2 y 3 en voz alta; recitar los ganchos 8.1 a 8.4. Completar tu bitácora (sección 1). Leer los guiones de la sección 9 una vez. |
| **Sábado 28 y domingo 29 nov** | Sección 5: correr cada comando y predecir la salida antes de ejecutarlo. Sección 13 (demo B1) completa, dos veces, cronometrada. Preguntas 1 a 8 de la sección 7. Autoevaluación de la sección 15 (preguntas 1 a 12). |
| **Lunes 30 nov** | Sección 11: haz cada ejercicio **sin mirar** en una copia (`evento_prueba`) y comprueba. Sección 10 y 12 tapando las respuestas. Autoevaluación completa (25 preguntas) y corrige con el anexo. Preguntas 9 a 15 al azar (que alguien las lea). Sección 14 en voz alta. |
| **Martes 1 dic** | **Ensayo del equipo:** demo completa (sección 13) y preguntas al azar entre los tres, como si fuera la defensa. Anota lo que falló. |
| **Miércoles 2 y jueves 3 dic** | Repasar solo lo que falló en el ensayo; repetir la autoevaluación (sección 15). No aprender nada nuevo. |
| **Viernes 4 dic (mañana)** | Checklist de `guia_defensa.md` sección 0 (contenedores arriba, base con datos, phpMyAdmin abierto). Recitar los ganchos. Dormir bien la noche anterior importa más que una hora extra de estudio. |

**Criterio de éxito:** poder explicar cualquier línea de tu script y de `Evento`/`Usuario` sin leer, escribir en vivo los seis ejercicios de la sección 11, y mostrar el efecto de cada operación de la app en la base.

---

## Anexo A. Respuestas de la autoevaluación (sección 15)

1. **14 columnas.** `NOT NULL` sin `DEFAULT`: `codigo_evento`, `titulo`, `fecha_evento` y `hora_inicio` (más `id_evento`, que es `AUTO_INCREMENT`). Son justo los cuatro campos de la inserción mínima `EVT-0005`.
2. Porque `JSON` **no admite largo**: el paréntesis con número es solo para tipos de texto como `VARCHAR(100)`. Da el error **1064** (sintaxis), con `near '(255))'`. Lo correcto es `detalle JSON NULL`.
3. `-70.6506`. `ST_X` lee la coordenada X, y X es el eje horizontal = **longitud**; `ST_Y` lee la latitud. En `POINT(x, y)` va primero la longitud.
4. **1062** `Duplicate entry 'EVT-0001' for key 'codigo_evento'`, por la restricción `UNIQUE` de `codigo_evento`. No se inserta la fila.
5. **1364**: el `INSERT` **omitió** una columna `NOT NULL` sin `DEFAULT` (no se mencionó). **1048**: se mandó `NULL` **explícito** a una columna `NOT NULL`. En los dos, falta un valor obligatorio.
6. Porque `hora_fin` es opcional y comparar `NULL > hora_inicio` da «desconocido»; sin el `IS NULL OR`, la regla rechazaría los eventos sin hora de fin (como el cumpleaños de todo el día).
7. Un alias de `TINYINT(1)`: `TRUE` = 1, `FALSE` = 0. La base acepta cualquier entero del rango (probado: acepta un 5); para limitarlo a 0 y 1 habría que agregar `CHECK (activo IN (0, 1))`. En Java es `boolean`.
8. Todos quedan con `azul`, porque la columna es `NOT NULL` y la base completa las filas existentes con el `DEFAULT`.
9. Atributo `private String color;`, getter y setter (`getColor`/`setColor`), y `color = "azul";` en el constructor vacío (para replicar el `DEFAULT`). No toco el de 14 parámetros porque al sumarle uno dejarían de compilar `EventoDAO.leerFila` y `EventoTest.testConstructorCompletoYGetters`; solo lo haría si me lo piden, agregándolo al final y corrigiendo esas dos llamadas.
10. Porque `ubicacion` puede ser `NULL` y un `double` primitivo no puede ser `null` (valdría `0.0`, que es una coordenada real). `Double` admite `null` = «sin ubicación».
11. `SELECT codigo_evento, titulo, fecha_evento, prioridad FROM evento WHERE prioridad = 1 AND MONTH(fecha_evento) = 11 AND YEAR(fecha_evento) = 2026;` → `EVT-0001`, `EVT-0002`, `EVT-0004`. (También sirve `BETWEEN '2026-11-01' AND '2026-11-30'`.)
12. `SELECT JSON_UNQUOTE(JSON_EXTRACT(detalle, '$.participantes[0]')) FROM evento WHERE codigo_evento = 'EVT-0001';` → `Tais`. El `[0]` es la primera posición.
13. `JSON_EXTRACT` saca el valor **con comillas** (`"Tais"`, formato JSON); `JSON_UNQUOTE` se las quita (`Tais`, texto plano).
14. `UPDATE evento SET activo = 0 WHERE codigo_evento = 'EVT-0003';`. Sin el `WHERE` se desactivarían **todos** los eventos de la tabla.
15. Agrupa las filas con la misma categoría y cuenta cuántas hay en cada grupo: `personal` 2, `estudio` 1, `salud` 1, `trabajo` 1.
16. Para que la comparación sea byte a byte y **distinga mayúsculas**: con la collation `_ci` de la base, `ADMIN2026` entraba como `Admin2026`. Con `_bin` el segundo `SELECT` de la prueba devuelve 0.
17. `DATE` solo fecha (`2026-11-30`); `TIME` solo hora (`09:00:00`); `TIMESTAMP` fecha y hora (con rango 1970–2038, se guarda en UTC). Para `fecha_evento` usé `DATE` (lo exige la pauta); `TIMESTAMP` solo en `fecha_creacion`.
18. Porque las tablas no tienen clave foránea: ninguna depende de la otra, así que se pueden borrar en cualquier orden.
19. La base genera el siguiente entero al insertar (1, 2, 3…). Nunca reutiliza un número: si un `INSERT` falla (p. ej. código repetido), el número que reservó se pierde, y el siguiente id queda más alto. Es normal.
20. Error **1416** `Cannot get geometry object from data you send to the GEOMETRY field`: un `POINT` solo acepta `POINT(lon, lat)`. El nombre del lugar va en la columna `lugar` (texto).
21. El `CHECK chk_evento_horas`: error **4025** en MariaDB (3819 en MySQL). `Conexion.mensajeError` lo traduce a «Los datos no cumplen las reglas de la tabla (prioridad, horas, duracion o JSON).» Se muestra en un cuadro «Error de base de datos».
22. (1) `Connection` (`Conexion.conectar()`), (2) `PreparedStatement` con `?` y los `setXxx`, (3) ejecución: `executeQuery()` para `SELECT` → `ResultSet` que se recorre con `next()`; `executeUpdate()` para `INSERT`/`UPDATE`/`DELETE` → filas afectadas. Todo en try-with-resources.
23. Porque los datos viajan separados del SQL (parámetros `?`), así no hay inyección SQL; `Statement` concatena texto y es vulnerable. La pauta lo exige.
24. Porque la interfaz ya las previene: la prioridad es un combo con 1, 2 y 3, y la duración un spinner de 1 a 1440. Desde SQL directo (consola o phpMyAdmin) no hay esa barrera y el `CHECK` es la última defensa. Lo que sí llega a la base desde la app: hora de fin anterior al inicio y detalle que no es JSON.
25. Devuelve `true` solo si `latitud` y `longitud` no son `null`. El DAO la usa en `cargarParametros` (al insertar y actualizar): si es `true` escribe `POINT(?, ?)` con longitud y latitud; si es `false` guarda `NULL` con `setNull(..., Types.DOUBLE)`.
