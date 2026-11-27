# Explicación del script SQL: calendario_bd.sql

Archivo: `sql/calendario_bd.sql` · Motor probado: MariaDB 10.6 (compatible con MySQL 8.0.16 o superior) · Base: `calendario_app`.

**Equipo:** Tais Montecinos · Rodolfo Prieto · Nicolás Cano. **Para qué sirve este documento:** en la defensa oral piden "ubicar y explicar la sentencia SQL solicitada" (B4) y explicar el modelo de datos (B2). Cada bloque lleva el código de la pauta (S1 a S5) y el criterio de la rúbrica que cubre (A1 a A3).

## 0. Mapa rápido (30 segundos)

| Código pauta | Líneas | Qué es | Criterio rúbrica |
|---|---|---|---|
| S1 | 11 a 20 | `CREATE DATABASE IF NOT EXISTS` + `USE` (+ `DROP TABLE` de reinicio) | A3 |
| S2 | 22 a 30 | `CREATE TABLE usuario` | A1, A2, A3 |
| S3 | 32 a 67 | `CREATE TABLE evento` (14 columnas, 4 CHECK) | A1, A2, A3 |
| S4 | 69 a 73 | `INSERT` de 3 usuarios (ADMIN, OPERADOR, inactivo) | A3 |
| S5 | 75 a 100 | `INSERT` de 5 eventos (4 completos y 1 mínimo) | A3 |
| Verificación | 102 a 117 | `DESCRIBE` y `SELECT` (criterios de la Tabla 3) | A3 |
| Pruebas de restricciones | 119 a 136 | 4 `INSERT` comentados que deben FALLAR | A2 |

Las dos tablas son **independientes**: no hay clave foránea (así lo pide la pauta).

## 1. Cabecera (líneas 1 a 9)

Son comentarios. En SQL, `--` (con un espacio después) comenta hasta el final de la línea. No ejecutan nada. Dejan escrito el motor probado (MariaDB 10.6, contenedor `calendario-mysql`) y por qué importa la versión: los `CHECK` solo se **aplican** desde MySQL 8.0.16 (antes se aceptaban y se ignoraban).

## 2. S1: crear y usar la base de datos (líneas 11 a 16) · A3

```sql
CREATE DATABASE IF NOT EXISTS calendario_app
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_spanish_ci;

USE calendario_app;
```

- `CREATE DATABASE IF NOT EXISTS calendario_app`: crea la base `calendario_app`. `IF NOT EXISTS` hace que, si ya existe, **no dé error** (la pauta lo exige: "se ejecuta sin errores en una instalación limpia"). Ojo: si existe, tampoco la modifica.
- `CHARACTER SET utf8mb4`: juego de caracteres. `utf8mb4` guarda hasta 4 bytes por carácter, o sea **todo Unicode** (tildes, ñ, emojis). El `utf8` antiguo de MySQL solo llegaba a 3 bytes y no guardaba emojis. Sin esto, "Reunión", "Nicolás" o "exámenes" podrían quedar mal.
- `COLLATE utf8mb4_spanish_ci`: **collation** = reglas de comparación y orden. `spanish` = orden del español (la ñ va después de la n); `ci` = *case insensitive* (no distingue mayúsculas de minúsculas). Consecuencias reales que conviene saber decir:
  - `UNIQUE` en `codigo_evento` trata `EVT-0001` y `evt-0001` como **iguales** (daría error 1062).
  - `nombre_usuario` hereda esta collation `_ci`: `ADMIN` entra igual que `admin`. La **clave**, en cambio, tiene su propia collation `utf8mb4_bin` (ver S2), así que sí distingue mayúsculas.
- `USE calendario_app;`: selecciona la base para que las sentencias siguientes se apliquen en ella. En la app Java no se usa `USE`: la base va dentro de la URL JDBC (`.../calendario_app`).
- Las tablas heredan el charset y collation de la base si no se indican.

### Reinicio limpio (líneas 18 a 20)

```sql
DROP TABLE IF EXISTS evento;
DROP TABLE IF EXISTS usuario;
```

`DROP TABLE IF EXISTS` borra la tabla si existe y no falla si no existe. Sirve para **re-ejecutar el script desde cero** (las pruebas dejan datos y los `UNIQUE` harían fallar un segundo `INSERT`). Advertencia: **borra los datos**; no usar en una base con información real. El orden (primero `evento`, luego `usuario`) da lo mismo aquí porque no hay claves foráneas; si las hubiera, habría que borrar primero la tabla hija.

## 3. S2: tabla `usuario` (líneas 22 a 30) · A1, A2, A3

```sql
CREATE TABLE IF NOT EXISTS usuario (
    id_usuario      INT AUTO_INCREMENT PRIMARY KEY,
    nombre_usuario  VARCHAR(30) NOT NULL UNIQUE,
    clave           VARCHAR(64) NOT NULL COLLATE utf8mb4_bin,  -- _bin: distingue mayúsculas
    nombre_completo VARCHAR(80) NOT NULL,
    rol             VARCHAR(20) NOT NULL DEFAULT 'OPERADOR',
    activo          BOOLEAN     NOT NULL DEFAULT TRUE
);
```

Es el script base de la pauta (Tabla 1), con **un solo cambio**: `COLLATE utf8mb4_bin` en `clave`. Columna por columna:

| Columna | Tipo y restricciones | Por qué |
|---|---|---|
| `id_usuario` | `INT AUTO_INCREMENT PRIMARY KEY` | Identificador único. `AUTO_INCREMENT`: el motor asigna 1, 2, 3... al insertar. `PRIMARY KEY` = `NOT NULL` + `UNIQUE` + índice principal. |
| `nombre_usuario` | `VARCHAR(30) NOT NULL UNIQUE` | Nombre de ingreso. `UNIQUE` evita dos usuarios iguales. 30 caracteres alcanzan para un nombre de usuario. |
| `clave` | `VARCHAR(64) NOT NULL COLLATE utf8mb4_bin` | Contraseña. 64 es el largo de un hash SHA-256 en hexadecimal; aquí se guarda en texto plano (como el script base de la pauta), pero el largo deja espacio para migrar a hash. `COLLATE utf8mb4_bin` hace que se compare **byte a byte**: `Admin2026` y `admin2026` son claves distintas. |
| `nombre_completo` | `VARCHAR(80) NOT NULL` | Nombre y apellido para mostrar. |
| `rol` | `VARCHAR(20) NOT NULL DEFAULT 'OPERADOR'` | `ADMIN` u `OPERADOR`. Si el `INSERT` no lo indica, queda `OPERADOR` (el rol con menos privilegios: criterio de seguridad por defecto). |
| `activo` | `BOOLEAN NOT NULL DEFAULT TRUE` | Solo los activos pueden ingresar (V2). |

**`COLLATE utf8mb4_bin` vs `utf8mb4_spanish_ci` (pregunta probable).** La base usa `utf8mb4_spanish_ci` (orden del español, sin distinguir mayúsculas: sirve para títulos y códigos). Una contraseña, en cambio, **debe** distinguir mayúsculas; si no, `admin2026` entraría como `Admin2026`. Por eso a la columna `clave` se le pone, solo a ella, la collation **`utf8mb4_bin`** (`bin` = binaria: compara el valor exacto de cada carácter). Una collation de columna **pisa** la de la base. En `WHERE ... clave = ?` la comparación usa la collation de la columna, y la prueba `testClaveDistingueMayusculas` lo verifica. `nombre_usuario` sigue siendo `_ci` (`ADMIN` = `admin`).

Conceptos que preguntan:

- **`VARCHAR(n)` vs `CHAR(n)`**: `VARCHAR` es de largo variable, ocupa lo que se escribe (más 1 o 2 bytes de largo) y `n` es el máximo en **caracteres**. `CHAR(n)` rellena siempre hasta `n`. Para nombres y claves de largo variable corresponde `VARCHAR`.
- **`BOOLEAN` = `TINYINT(1)`**: MySQL y MariaDB no tienen un tipo booleano real; `BOOLEAN` es un sinónimo de `TINYINT(1)`. `TRUE` es 1 y `FALSE` es 0. Por eso `DESCRIBE` muestra `tinyint(1)`. En Java se lee con `getBoolean` (0 es false, distinto de 0 es true).
- **`NOT NULL`**: la columna no acepta valor ausente. **`NULL`** significa "sin dato", no es cero ni cadena vacía.
- **`DEFAULT`**: valor que se usa cuando el `INSERT` no menciona la columna.
- **`IF NOT EXISTS`** aquí es redundante porque el `DROP TABLE IF EXISTS` de antes ya la borró; se deja porque es el texto del script base.

## 4. S3: tabla `evento` (líneas 32 a 67) · A1, A2, A3

Es la tabla del tema: una agenda de eventos. Tiene **14 columnas** (la pauta pide mínimo 8), todas en minúscula y sin espacios.

### 4.1 Identificadores

```sql
id_evento     INT AUTO_INCREMENT PRIMARY KEY,
codigo_evento VARCHAR(20)  NOT NULL UNIQUE,
```

- `id_evento`: **clave primaria técnica**, autoincremental. La usan `UPDATE` y `DELETE` (`WHERE id_evento = ?`).
- `codigo_evento`: **identificador de negocio** (el que el usuario reconoce, p. ej. `EVT-0001`). Pauta: `VARCHAR`, `NOT NULL` y `UNIQUE`. Se usa para buscar (C3) y su `UNIQUE` produce el error 1062 que se transforma en "El registro ya existe" (V3). `VARCHAR(20)` sobra para el formato `EVT-0001` (8 caracteres) y deja espacio para otros códigos.
- ¿Por qué dos identificadores? El `id` es interno y estable (nunca cambia, aunque cambie el código); el código es el que la persona escribe y busca.

### 4.2 Datos del evento

```sql
titulo     VARCHAR(100) NOT NULL,
lugar      VARCHAR(150) NULL,
ubicacion  POINT        NULL,
detalle    JSON         NULL,
categoria  VARCHAR(30)  NOT NULL DEFAULT 'personal',
```

- `titulo`: obligatorio. Es la columna que se omite en la prueba de `NOT NULL`.
- `lugar`: nombre del lugar en texto; `NULL` explícito = es opcional (puede haber eventos sin lugar). `NULL` es el comportamiento por defecto, escribirlo solo documenta la intención.
- `ubicacion POINT`: tipo **espacial** (geometría) que guarda un punto con dos coordenadas. **Convención clave de MySQL/MariaDB: X = longitud e Y = latitud**, y `POINT(x, y)` recibe primero la **longitud** y luego la **latitud**, o sea `POINT(longitud, latitud)`. Es lo contrario del orden en que la gente dice "latitud, longitud", y es el error típico. Ejemplo del script: Santiago centro `POINT(-70.6506, -33.4378)`: longitud -70.6506 (oeste) y latitud -33.4378 (sur).
- `detalle JSON`: guarda un documento JSON (notas, participantes, lista de cosas por llevar), es decir, datos con forma libre que no justifican una columna cada uno. En MySQL es un tipo nativo con validación y almacenamiento binario. En MariaDB `JSON` es un **alias de `LONGTEXT`**, y por eso el script agrega el `CHECK (JSON_VALID(...))` (sección 4.6): así la validación funciona igual en los dos motores.
- `categoria`: `VARCHAR(30)`, obligatorio y con `DEFAULT 'personal'` (cumple el requisito "al menos un DEFAULT").

### 4.3 Fecha y horas: DATE, TIME, TIMESTAMP

```sql
fecha_evento DATE NOT NULL,
hora_inicio  TIME NOT NULL,
hora_fin     TIME NULL,
```

| Tipo | Guarda | Formato | Uso aquí |
|---|---|---|---|
| `DATE` | Solo la fecha | `'2026-11-23'` (AAAA-MM-DD) | `fecha_evento`. La pauta **exige** un campo `DATE`. |
| `TIME` | Solo la hora (o una duración) | `'09:00:00'` (HH:MM:SS) | `hora_inicio`, `hora_fin`. |
| `TIMESTAMP` | Fecha y hora juntas (instante) | `'2026-11-23 09:00:00'` | `fecha_creacion` (auditoría). |

Separar `DATE` y `TIME` permite mostrar y ordenar por día y hora por separado (`ORDER BY fecha_evento, hora_inicio`) y mapea directo a `LocalDate` y `LocalTime` en Java. `hora_fin` es opcional (`NULL`): un cumpleaños de todo el día no tiene hora de término.

### 4.4 Numéricos

```sql
duracion_minutos INT NOT NULL DEFAULT 60,
prioridad        INT NOT NULL DEFAULT 2,   -- 1 = alta, 2 = media, 3 = baja
```

`INT` entero de 4 bytes. `duracion_minutos` por defecto 60 (una hora); `prioridad` por defecto 2 (media). La prioridad es un entero con significado fijo (1 alta, 2 media, 3 baja) y el `CHECK` de la sección 4.6 impide otro valor.

### 4.5 Booleano y auditoría

```sql
activo         BOOLEAN   NOT NULL DEFAULT TRUE,
fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
```

- `activo BOOLEAN`: `TINYINT(1)`, 1 = verdadero, 0 = falso (requisito: "al menos un BOOLEAN"). Permite "desactivar" un evento sin borrarlo.
- `fecha_creacion TIMESTAMP ... DEFAULT CURRENT_TIMESTAMP`: se llena **sola** con la fecha y hora del servidor al insertar. Por eso el `INSERT` de la aplicación (Java) no la menciona. Es una columna de **auditoría**: dice cuándo se creó el registro.

### 4.6 Restricciones CHECK (líneas 62 a 66)

```sql
CONSTRAINT chk_evento_duracion  CHECK (duracion_minutos > 0),
CONSTRAINT chk_evento_prioridad CHECK (prioridad BETWEEN 1 AND 3),
CONSTRAINT chk_evento_horas     CHECK (hora_fin IS NULL OR hora_fin > hora_inicio),
CONSTRAINT chk_evento_detalle   CHECK (detalle IS NULL OR JSON_VALID(detalle))
```

`CONSTRAINT nombre CHECK (condición)` define una regla que **cada fila debe cumplir**; si el `INSERT` o `UPDATE` la rompe, el motor rechaza la sentencia con error. Darles nombre (`chk_evento_...`) hace que el mensaje de error diga qué regla falló.

| Nombre | Regla | Detalle |
|---|---|---|
| `chk_evento_duracion` | `duracion_minutos > 0` | No hay duraciones cero ni negativas. |
| `chk_evento_prioridad` | `prioridad BETWEEN 1 AND 3` | `BETWEEN` es inclusivo: 1, 2 o 3. |
| `chk_evento_horas` | `hora_fin IS NULL OR hora_fin > hora_inicio` | Si hay hora de fin, debe ser posterior al inicio. El `IS NULL` deja explícito que `hora_fin` puede faltar (además, en un `CHECK` una comparación con `NULL` da "desconocido" y la fila se **acepta**). |
| `chk_evento_detalle` | `detalle IS NULL OR JSON_VALID(detalle)` | `JSON_VALID` devuelve 1 si el texto es JSON bien formado. |

### 4.7 Motor de almacenamiento

```sql
) ENGINE = InnoDB;
```

`InnoDB` es el motor con **transacciones, claves foráneas e integridad**; es el predeterminado actual. Se declara explícito para que no dependa de la configuración del servidor.

### 4.8 Resumen de restricciones de `evento` (criterio A2)

| Restricción | Dónde |
|---|---|
| `PRIMARY KEY` + `AUTO_INCREMENT` | `id_evento` |
| `UNIQUE` | `codigo_evento` |
| `NOT NULL` | `codigo_evento`, `titulo`, `categoria`, `fecha_evento`, `hora_inicio`, `duracion_minutos`, `prioridad`, `activo`, `fecha_creacion` |
| `DEFAULT` | `categoria` ('personal'), `duracion_minutos` (60), `prioridad` (2), `activo` (TRUE), `fecha_creacion` (CURRENT_TIMESTAMP) |
| `CHECK` | duración, prioridad, horas, JSON |

## 5. S4: usuarios de prueba (líneas 69 a 73) · A3

```sql
INSERT INTO usuario (nombre_usuario, clave, nombre_completo, rol, activo) VALUES
('admin',     'Admin2026', 'Administrador',    'ADMIN',    TRUE),
('operador1', 'Oper2026',  'Operador Uno',     'OPERADOR', TRUE),
('inactivo1', 'Inac2026',  'Usuario Inactivo', 'OPERADOR', FALSE);
```

`INSERT INTO tabla (columnas) VALUES (fila), (fila), ...` inserta **varias filas en una sola sentencia**. Se listan las columnas (no `id_usuario`, que lo pone `AUTO_INCREMENT`). La tercera fila tiene `activo = FALSE`: sirve para probar V2 (usuario inactivo). Los tres cubren el requisito "ADMIN, OPERADOR e inactivo".

## 6. S5: eventos de prueba (líneas 75 a 100) · A3

```sql
INSERT INTO evento
    (codigo_evento, titulo, lugar, ubicacion, detalle, categoria,
     fecha_evento, hora_inicio, hora_fin, duracion_minutos, prioridad)
VALUES
    ('EVT-0001', 'Reunión de equipo', 'Sala 3, Santo Tomás Santiago',
     POINT(-70.6506, -33.4378),
     '{"notas": "Revisión semanal de avances", "participantes": ["Tais", "Rodolfo", "Nicolás"]}',
     'trabajo', '2026-11-23', '09:00:00', '10:00:00', 60, 1),
    ...
```

- La lista de columnas **no incluye** `activo`, `fecha_creacion` ni `id_evento`: toman su `DEFAULT` o el autoincremento.
- `POINT(-70.6506, -33.4378)`: construye la coordenada, **longitud primero** y luego latitud. Es una función, no un texto.
- El JSON va **como texto entre comillas simples**; sus claves y textos van con comillas dobles. Si se mezclan al revés, `JSON_VALID` falla.
- Los valores de `DATE` y `TIME` van como texto en formato fijo: `'2026-11-23'` y `'09:00:00'`.
- Filas: **EVT-0001** reunión (con POINT y JSON); **EVT-0002** control médico; **EVT-0003** cumpleaños con `ubicacion = NULL`, `hora_fin = NULL` y `duracion_minutos = 1440` (24 horas × 60), para probar los nulos; **EVT-0004** la defensa del 30-11 (08:30 a 10:00, 90 minutos). Todas usan prioridad 1 o 2, dentro del `CHECK`.

### Inserción mínima (líneas 98 a 100)

```sql
INSERT INTO evento (codigo_evento, titulo, fecha_evento, hora_inicio)
VALUES ('EVT-0005', 'Pagar cuentas', '2026-12-05', '20:00:00');
```

Solo los campos `NOT NULL` **sin** `DEFAULT`. El resto toma su valor por defecto: `categoria = 'personal'`, `duracion_minutos = 60`, `prioridad = 2`, `activo = 1`, `fecha_creacion = ahora`, y `lugar`, `ubicacion`, `detalle`, `hora_fin` quedan `NULL`. Demuestra que los `DEFAULT` funcionan. Con las 4 filas anteriores suman **5 eventos** (la pauta pide 3 o más; `SELECT *` debe devolver 5 o más).

## 7. Consultas de verificación (líneas 102 a 117) · A3

```sql
DESCRIBE usuario;
DESCRIBE evento;
```

`DESCRIBE` muestra la estructura: nombre, tipo, si acepta `NULL`, clave (`PRI`, `UNI`), valor por defecto y extras (`auto_increment`). Es el criterio de verificación de S2 y S3: debe coincidir con la Tabla 1 y la Tabla 2.

```sql
SELECT id_usuario, nombre_usuario, rol, activo FROM usuario;
```

Debe mostrar los 3 usuarios (criterio de S4). No incluye `clave` a propósito (no mostrar contraseñas).

```sql
SELECT id_evento, codigo_evento, titulo, fecha_evento, hora_inicio, prioridad, activo
FROM evento ORDER BY fecha_evento, hora_inicio;
```

Devuelve los 5 eventos en orden cronológico: `ORDER BY fecha_evento, hora_inicio` ordena primero por fecha y, a igualdad de fecha, por hora. Es la misma ordenación que usa `EventoDAO.listar()`.

```sql
SELECT codigo_evento, ST_X(ubicacion) AS longitud, ST_Y(ubicacion) AS latitud
FROM evento WHERE ubicacion IS NOT NULL;
```

- `ST_X(punto)` devuelve la coordenada X (**longitud**); `ST_Y(punto)` devuelve la Y (**latitud**). El prefijo `ST_` viene del estándar SQL/MM de datos espaciales.
- Un `POINT` se guarda en formato binario interno; no se puede leer directamente en texto, por eso se extrae con `ST_X` y `ST_Y` (la app Java hace lo mismo en `EventoDAO`).
- `AS longitud` crea un **alias** de columna (el nombre con el que se lee en `ResultSet`).
- `WHERE ubicacion IS NOT NULL`: se excluye el evento sin ubicación. Para comparar con `NULL` se usa `IS NULL` / `IS NOT NULL`, **nunca** `= NULL` (eso devuelve desconocido y no encuentra nada). Resultado: 3 filas (EVT-0001, EVT-0002 y EVT-0004).

```sql
SELECT codigo_evento, JSON_UNQUOTE(JSON_EXTRACT(detalle, '$.notas')) AS notas
FROM evento WHERE detalle IS NOT NULL;
```

- `JSON_EXTRACT(detalle, '$.notas')` extrae del documento el valor de la clave `notas`. En la ruta (`path`), `$` es la raíz del documento y `.notas` la clave.
- Devuelve el valor **con comillas** (como JSON: `"Revisión semanal de avances"`). `JSON_UNQUOTE(...)` quita las comillas y deja el texto plano. Por eso se anidan: `JSON_UNQUOTE(JSON_EXTRACT(...))`.
- Una clave que no existe devuelve `NULL` (el evento EVT-0005 tiene `detalle` en `NULL`, por eso se filtra).

## 8. Pruebas de restricciones (líneas 119 a 136) · A2

Están **comentadas** (`--`). Se descomenta **una a la vez**; **todas deben fallar**, y que fallen demuestra que las restricciones funcionan.

| Prueba | Sentencia (resumen) | Error esperado | Por qué |
|---|---|---|---|
| `UNIQUE` | `INSERT` con `codigo_evento = 'EVT-0001'` otra vez | **1062** `Duplicate entry 'EVT-0001' for key 'codigo_evento'` | Ya existe ese valor en una columna `UNIQUE`. Es el que la app traduce a "El registro ya existe" (V3). |
| `NOT NULL` | `INSERT` sin `titulo` | **1364** `Field 'titulo' doesn't have a default value` | `titulo` es obligatorio y no tiene `DEFAULT`. Ocurre en modo estricto (el predeterminado). |
| `CHECK` prioridad | `prioridad = 7` | MariaDB **4025** `CONSTRAINT chk_evento_prioridad failed` / MySQL **3819** | 7 está fuera de `BETWEEN 1 AND 3`. |
| `CHECK` JSON | `detalle = 'esto no es json'` | MariaDB **4025** / MySQL **3140** `Invalid JSON text` | `JSON_VALID` devuelve 0. |

Detalle: el mismo tipo de falla tiene **código distinto según el motor** (4025 en MariaDB; 3819 y 3140 en MySQL). Por eso `Conexion.mensajeError` revisa los tres códigos. En cambio el 1364 **no** está en `mensajeError` (cae en el mensaje genérico "Error de base de datos: ...") porque la app siempre envía el título validado.

## 9. Preguntas de defensa que salen de este script

1. **¿Por qué `VARCHAR(20)` para `codigo_evento`?** Es texto de largo variable (el código tiene letras, guion y números); 20 caracteres de máximo bastan para `EVT-0001`. Si fuera solo numérico se usaría `INT`.
2. **¿Por qué `DATE` y no `VARCHAR` para la fecha?** Porque valida que sea una fecha real, permite ordenar y comparar por fecha y se mapea a `LocalDate`.
3. **¿Qué es `BOOLEAN` en MySQL?** Sinónimo de `TINYINT(1)`: 1 verdadero, 0 falso.
4. **¿Orden de `POINT`?** `POINT(longitud, latitud)`; `ST_X` = longitud, `ST_Y` = latitud.
5. **¿`UNIQUE` vs `PRIMARY KEY`?** Ambos impiden repetidos; la clave primaria además es `NOT NULL`, solo hay una por tabla y es el identificador técnico de la fila. Puede haber varios `UNIQUE`.
6. **¿Qué hace `AUTO_INCREMENT`?** El motor asigna el siguiente entero libre; por eso el `INSERT` no menciona `id_evento` y Java lo recupera con `RETURN_GENERATED_KEYS`.
7. **¿Por qué `DROP TABLE IF EXISTS`?** Para poder re-ejecutar el script desde cero sin errores de "tabla ya existe" ni de datos duplicados.
8. **¿Por qué el `CHECK` del JSON si ya existe el tipo `JSON`?** En MariaDB `JSON` es un alias de `LONGTEXT` y no valida; el `CHECK` con `JSON_VALID` lo garantiza en ambos motores.
9. **¿Qué pasa si `hora_fin` es `NULL`?** El `CHECK` lo permite (`hora_fin IS NULL OR ...`).
10. **¿Por qué el script entrega 5 eventos si piden 3?** La pauta pide "al menos 3"; 5 deja margen y prueba además los valores `NULL` y los `DEFAULT`.
