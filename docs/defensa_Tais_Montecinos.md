# Defensa individual — Tais Montecinos

**Evaluación 4 · Fundamentos de POO (IEI-085) · CalendarioApp · Defensa: lunes 30 de noviembre de 2026**

Tu parte: **script SQL completo (S1–S5), modelo de datos y las clases `Usuario` y `Evento`.** Esta guía no contradice a `docs/guia_defensa.md`; la complementa con tu parte al detalle.

---

## 1. Mi parte en 30 segundos

«Diseñé la base de datos `calendario_app` y escribí el script `sql/calendario_bd.sql` completo: creo la base (S1), la tabla `usuario` (S2), la tabla `evento` con 14 columnas, incluidos un `POINT` y un `JSON` (S3), y cargo 3 usuarios (S4) y 5 eventos de prueba (S5). Las reglas de integridad (código único, prioridad 1 a 3, duración mayor que 0, hora de fin mayor que la de inicio, JSON válido) las dejé **en la base**, con `UNIQUE`, `NOT NULL`, `DEFAULT` y cuatro `CHECK`. En Java escribí el modelo: `Usuario` y `Evento`, una clase por tabla, con atributos `private`, dos constructores y getters/setters.»

| Archivo | Qué contiene |
|---|---|
| `sql/calendario_bd.sql` | S1 a S5, verificaciones y pruebas de restricciones |
| `CalendarioApp/src/modelo/Usuario.java` | Una fila de `usuario` (6 atributos) |
| `CalendarioApp/src/modelo/Evento.java` | Una fila de `evento` (14 atributos con `latitud`/`longitud` en lugar de `ubicacion`) y `tieneUbicacion()` |
| `CalendarioApp/test/modelo/EventoTest.java` | 3 pruebas JUnit 4 del modelo (no necesitan base de datos) |

---

## 2. Script SQL línea por línea

### 2.1 Cabecera (líneas 1 a 9)

```sql
-- Evaluación 4 (20 %) · POO IEI-085 · Unidad III: JDBC y MySQL
-- Base de datos: calendario_app
-- Motor probado: MariaDB 10.6 (contenedor poo-mysql). Compatible con
--                MySQL 8.0.16+ (desde ahí se aplican los CHECK).
```

Son comentarios (`--` hasta el fin de línea); no se ejecutan. Dicen qué motor se probó: **MariaDB 10.6**. Los `CHECK` solo se aplican de verdad desde MySQL 8.0.16 (antes se aceptaban pero se ignoraban en silencio).

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

Borra las tablas si existen para poder **re-ejecutar el script desde cero** (instalación limpia). `IF EXISTS` evita el error cuando todavía no existen. No importa el orden porque las tablas no tienen clave foránea.

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
- 64 de largo: deja espacio para un *hash* si más adelante se cifra la clave.

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

**Identificador de negocio**: lo escribe el usuario (`EVT-0001`) y por él se busca (C3). Obligatorio y único. Es distinto de la PK: la PK es técnica; este código es el que el usuario ve.

```sql
    titulo           VARCHAR(100) NOT NULL,
    lugar            VARCHAR(150) NULL,
```

`titulo` es obligatorio. `lugar` es el nombre del lugar en texto; `NULL` explícito significa que es opcional.

```sql
    ubicacion        POINT        NULL,
```

Tipo **espacial** `POINT`: guarda una coordenada como un solo dato (X = **longitud**, Y = **latitud**). Opcional: un cumpleaños en casa puede no tener coordenadas. Se escribe con `POINT(lon, lat)` y se lee con `ST_X` y `ST_Y`.

```sql
    detalle          JSON         NULL,
```

Columna `JSON` para datos libres y variables (notas, participantes, cosas que llevar) sin crear más tablas. Opcional. Se valida con el `CHECK` de más abajo. En MariaDB `JSON` es un alias de `LONGTEXT`, por eso el `CHECK` es necesario; en MySQL 8 es un tipo nativo.

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
    ('EVT-0003', 'Cumpleaños de mamá', 'Casa', NULL,
     '{"notas": "Comprar torta"}',
     'personal', '2026-11-28', '00:00:00', NULL, 1440, 2),
```

Caso especial: `ubicacion` y `hora_fin` en `NULL` (evento sin coordenadas y de todo el día: 1440 minutos = 24 horas). Demuestra que las columnas opcionales funcionan.

El script inserta **4 eventos** en este bloque (`EVT-0001`, `0002`, `0003`, `0004`) y un quinto aparte (siguiente bloque), para un total de **5 filas**.

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

`ST_X` devuelve la coordenada X (longitud) y `ST_Y` la Y (latitud). `AS` les da un alias legible. `WHERE ubicacion IS NOT NULL` descarta los eventos sin coordenadas (en SQL se compara con `IS NULL`, no con `= NULL`).

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
| Prioridad 7 | MariaDB 4025 / MySQL 3819 (CHECK) |
| JSON inválido `'esto no es json'` | MariaDB 4025 / MySQL 3140 |

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

Constructor completo de 14 parámetros, en el mismo orden que los atributos; lo usa el DAO al leer una fila.

```java
public boolean tieneUbicacion() {
    return latitud != null && longitud != null;
}
```

Devuelve `true` solo si **ambas** coordenadas existen. El DAO la usa al guardar: si es `true`, escribe `POINT(?, ?)` con los dos números; si es `false`, guarda `NULL` con `setNull`. Un `POINT` necesita las dos coordenadas; con una sola no se puede armar. Lo comprueba `testTieneUbicacionSoloConLatitudYLongitud` (falso sin nada, falso con solo latitud, verdadero con ambas).

Después vienen los getters y setters de los 14 atributos, con el mismo patrón que en `Usuario` (`isActivo` para el booleano).

### 3.4 Las pruebas (`EventoTest`)

Tres pruebas JUnit 4, sin base de datos: (1) el constructor vacío trae los defaults; (2) `tieneUbicacion()` exige latitud y longitud; (3) el constructor completo y los getters devuelven lo que se les pasó. Es **JUnit 4 y no 5** porque en un proyecto Ant de NetBeans `ant test` no ejecuta JUnit 5.

---

## 4. Reglas de negocio que viven en la base

| Regla | Dónde se implementa | Error si se viola |
|---|---|---|
| Código de evento único | `codigo_evento VARCHAR(20) NOT NULL UNIQUE` | **1062** `Duplicate entry 'EVT-0001' for key 'codigo_evento'` |
| Nombre de usuario único | `nombre_usuario ... UNIQUE` | **1062** |
| Solo usuarios activos entran | Columna `activo` (`DEFAULT TRUE`) + filtro `AND activo = TRUE` en el `SELECT` de `UsuarioDAO.validar()`. La base guarda el dato; **el filtro lo aplica la consulta** | No hay error SQL: la consulta devuelve cero filas y la app dice «Usuario inactivo.» |
| Prioridad entre 1 y 3 | `CHECK (prioridad BETWEEN 1 AND 3)` | MariaDB **4025** `CONSTRAINT chk_evento_prioridad failed` / MySQL **3819** |
| Duración mayor que 0 | `CHECK (duracion_minutos > 0)` | MariaDB **4025** / MySQL **3819** |
| `hora_fin` mayor que `hora_inicio` | `CHECK (hora_fin IS NULL OR hora_fin > hora_inicio)` | MariaDB **4025** / MySQL **3819** |
| `detalle` es JSON válido | `CHECK (detalle IS NULL OR JSON_VALID(detalle))` | MariaDB **4025** / MySQL **3140** `Invalid JSON text` |
| La clave distingue mayúsculas | `clave ... COLLATE utf8mb4_bin` | Sin error: `ADMIN2026` simplemente no coincide y el login falla |
| Campos obligatorios | `NOT NULL` sin `DEFAULT` (`codigo_evento`, `titulo`, `fecha_evento`, `hora_inicio`, y en `usuario` `nombre_usuario`, `clave`, `nombre_completo`) | **1364** `Field 'titulo' doesn't have a default value` (si se omite) o **1048** `Column cannot be null` (si se manda `NULL`) |

Punto fino: todos los `CHECK` fallidos dan 4025 en MariaDB (el código lo traduce `Conexion.mensajeError`). La **ventaja de dejar las reglas en la base** es que valen para cualquier programa que escriba en ella, no solo para esta aplicación.

---

## 5. Demostración en MySQL

Todos los comandos se corren desde Git Bash (o PowerShell) en tu PC, con el contenedor `poo-mysql` arriba. `docker exec poo-mysql` ejecuta un comando dentro del contenedor; `mariadb` es el cliente de consola; `-uroot` es el usuario (sin clave); `calendario_app` es la base; `-e "..."` ejecuta la sentencia y sale. Si el contenedor no está arriba, el comando dice `No such container`: levantar con `docker compose up -d`.

**Estructura de `evento`**

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "DESCRIBE evento"
```

Esperado: 14 filas. `id_evento` con `PRI` y `auto_increment`; `codigo_evento` con `UNI`; `ubicacion` tipo `point`; `detalle` tipo `longtext` (en MariaDB el alias de JSON) o `json` en MySQL; los defaults `personal`, `60`, `2`, `1` y `current_timestamp()`.

**Estructura de `usuario`**

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "DESCRIBE usuario"
```

Esperado: 6 filas; `nombre_usuario` con `UNI`; `rol` con default `OPERADOR`.

**Ver las restricciones tal como quedaron**

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "SHOW CREATE TABLE evento\G"
```

`\G` muestra el resultado en vertical, más legible. Esperado: la definición completa con los cuatro `CONSTRAINT chk_evento_...`. Sirve para probar que los `CHECK` están en la tabla.

**Los datos**

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "SELECT id_evento, codigo_evento, titulo, fecha_evento, hora_inicio, prioridad, activo FROM evento ORDER BY fecha_evento, hora_inicio"
```

Esperado: al menos 5 filas, de `EVT-0001` (23-nov) a `EVT-0005` (5-dic), ordenadas por fecha. Si se hicieron pruebas desde la app puede haber más.

**Defaults de la inserción mínima**

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "SELECT codigo_evento, categoria, duracion_minutos, prioridad, activo, fecha_creacion FROM evento WHERE codigo_evento = 'EVT-0005'"
```

Esperado: `personal`, `60`, `2`, `1` y una fecha de creación con la hora del servidor, aunque ese `INSERT` solo trajo 4 campos.

**POINT con ST_X / ST_Y**

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "SELECT codigo_evento, ST_X(ubicacion) AS longitud, ST_Y(ubicacion) AS latitud FROM evento WHERE ubicacion IS NOT NULL"
```

Esperado: `EVT-0001` con longitud `-70.6506` y latitud `-33.4378`. Los eventos 0003 y 0005 no aparecen (no tienen ubicación). Decir en voz alta: «X es la longitud, Y es la latitud».

**JSON con JSON_EXTRACT**

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "SELECT codigo_evento, JSON_EXTRACT(detalle, '$.notas') AS con_comillas, JSON_UNQUOTE(JSON_EXTRACT(detalle, '$.notas')) AS notas FROM evento WHERE detalle IS NOT NULL"
```

Esperado: la columna `con_comillas` muestra `"Revisión semanal de avances"` y la columna `notas` el mismo texto sin comillas. Muestra la diferencia entre `JSON_EXTRACT` y `JSON_UNQUOTE`.

**Un elemento de un arreglo JSON**

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "SELECT JSON_UNQUOTE(JSON_EXTRACT(detalle, '$.participantes[0]')) AS primero FROM evento WHERE codigo_evento = 'EVT-0001'"
```

Esperado: `Tais`. El `[0]` es la primera posición del arreglo (se cuenta desde 0).

**Probar cada restricción.** Todas deben fallar. Se usa `||` con `echo` para avisar si el comando falló; el mensaje de error de MariaDB es lo que interesa mostrar.

UNIQUE (código repetido):

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "INSERT INTO evento (codigo_evento, titulo, fecha_evento, hora_inicio) VALUES ('EVT-0001', 'Duplicado', '2026-12-10', '10:00:00')"
```

Esperado: `ERROR 1062 (23000): Duplicate entry 'EVT-0001' for key 'codigo_evento'`.

NOT NULL (sin título):

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "INSERT INTO evento (codigo_evento, fecha_evento, hora_inicio) VALUES ('EVT-0099', '2026-12-10', '10:00:00')"
```

Esperado: `ERROR 1364 (HY000): Field 'titulo' doesn't have a default value`.

CHECK prioridad:

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "INSERT INTO evento (codigo_evento, titulo, fecha_evento, hora_inicio, prioridad) VALUES ('EVT-0098', 'Mala prioridad', '2026-12-10', '10:00:00', 7)"
```

Esperado: `ERROR 4025 (23000): CONSTRAINT chk_evento_prioridad failed for calendario_app.evento`.

CHECK duración:

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "INSERT INTO evento (codigo_evento, titulo, fecha_evento, hora_inicio, duracion_minutos) VALUES ('EVT-0096', 'Duracion cero', '2026-12-10', '10:00:00', 0)"
```

Esperado: error 4025 con `chk_evento_duracion`.

CHECK horas (fin antes del inicio):

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "INSERT INTO evento (codigo_evento, titulo, fecha_evento, hora_inicio, hora_fin) VALUES ('EVT-0095', 'Horas al reves', '2026-12-10', '10:00:00', '09:00:00')"
```

Esperado: error 4025 con `chk_evento_horas`.

CHECK JSON inválido:

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "INSERT INTO evento (codigo_evento, titulo, fecha_evento, hora_inicio, detalle) VALUES ('EVT-0097', 'Mal JSON', '2026-12-10', '10:00:00', 'esto no es json')"
```

Esperado: error 4025 con `chk_evento_detalle`.

**La clave distingue mayúsculas (`_bin`)**

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "SELECT COUNT(*) FROM usuario WHERE nombre_usuario='admin' AND clave='Admin2026'; SELECT COUNT(*) FROM usuario WHERE nombre_usuario='admin' AND clave='ADMIN2026'"
```

Esperado: `1` y luego `0`. Con `Admin2026` coincide; con `ADMIN2026` no, gracias a `utf8mb4_bin`.

**Después de las pruebas:** ninguna de las inserciones fallidas deja filas, así que el conteo no cambia:

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "SELECT COUNT(*) FROM evento"
```

Esperado: el mismo número que antes de probar (5 en una base recién cargada). Si algo falla en vivo, el plan B es hacer lo mismo en phpMyAdmin (http://localhost:8081, usuario `root`, sin clave).

---

## 6. Lo mínimo que debes saber de las partes de Rodolfo y Nicolás

**Rodolfo — `Conexion`, `UsuarioDAO`, login**

- `Conexion` es la única clase con la URL, el usuario y la clave de la base; `conectar()` llama a `DriverManager.getConnection`. Si cambia algo, se toca un solo archivo.
- `Conexion.mensajeError(SQLException)` traduce los códigos de error (1062 duplicado, 1045 clave mala, 1049 base inexistente, estado `08` conexión caída, 4025/3819/3140 `CHECK`) a mensajes en español. Los códigos de mi tabla de la sección 4 son los que se traducen ahí.
- `UsuarioDAO.validar(nombre, clave)` ejecuta `SELECT ... WHERE nombre_usuario = ? AND clave = ? AND activo = TRUE` con `executeQuery()`. Si el `ResultSet` trae fila, devuelve un `Usuario`; si no, `null`.
- `estaInactivo(...)` cuenta si existe esa combinación con `activo = FALSE` para dar el mensaje «Usuario inactivo.» (V2).
- Se usa `PreparedStatement` con `?` para evitar la inyección SQL; todo va en try-with-resources.

**Nicolás — `EventoDAO`, CRUD, POINT/JSON desde Java**

- `EventoDAO` tiene `insertar`, `listar`, `buscar` (por `codigo_evento`), `actualizar` (por `id_evento`) y `eliminar` (por `id_evento`); los tres últimos de escritura usan `executeUpdate()`, los de lectura `executeQuery()`.
- `insertar` usa `RETURN_GENERATED_KEYS` para leer el `id_evento` que generó `AUTO_INCREMENT`.
- El `POINT` se escribe con `POINT(?, ?)` pasando **longitud y luego latitud** (`setDouble`); si `tieneUbicacion()` es falso, se usa `setNull(..., Types.DOUBLE)` (aquí entra mi método).
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

### Repaso espaciado sugerido (45 min cada vez)

| Fecha | Qué hacer |
|---|---|
| Viernes 27 nov | Leer secciones 2 y 3 en voz alta; recitar 8.1 a 8.3. |
| Sábado 28 nov | Sección 5: correr cada comando y predecir la salida antes de ejecutarlo; preguntas 1 a 8. |
| Domingo 29 nov | Preguntas 9 a 15 al azar (que alguien las lea); sección 6 sin mirar. |
| Lunes 30 nov (mañana) | Checklist de `guia_defensa.md` sección 0; recitar los tres ganchos y no aprender nada nuevo. |
