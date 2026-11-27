# Plan de ensayo del martes — CalendarioApp

**Evaluación 4 (20 %) · Fundamentos de POO (IEI-085) · Universidad Santo Tomás · Profesor Pablo (pauta de la profesora Rosa Rao)**
Equipo: Tais Montecinos · Rodolfo Prieto · Nicolás Cano · **Ensayo: martes 1 de diciembre de 2026 · Defensa: viernes 4 de diciembre de 2026**
Conduce el ensayo: **Nicolás**. Se ensaya a: **Tais y Rodolfo** (Nicolás también responde cuando le toca).

> El profesor lo dijo claro: «un 7 si todo funciona y se defiende, un 1 si no saben defender». Ya intentó pillar a Nicolás con un SQL inválido (`JSON(255)`, `POINT(150)`) y, como nota que Nicolás guía al equipo, **preguntará sobre todo a Tais y Rodolfo, al azar**. Por eso este ensayo se arma para que **cada uno sepa explicar el proyecto completo**, no solo su parte.

**Meta:** 7 en el trabajo y 7 en la defensa para los tres, y que Tais y Rodolfo se eximan del examen final.

---

## 1. Objetivo y reglas del ensayo

**Objetivo:** que el viernes cualquiera de los tres pueda (a) demostrar una operación y verificarla en MySQL, (b) ubicar y explicar cualquier método o sentencia, (c) responder una pregunta técnica y aplicarla al proyecto, y (d) modificar una línea en vivo, **sin leer y sin ayuda**.

**Reglas (no se negocian):**

1. **Sin leer.** Ni guías, ni PDF, ni el código de otro compañero a la vista. Se puede mirar el código que se pide explicar *dentro de NetBeans*, como el viernes.
2. **Cronometrado.** Nicolás corre el reloj en el celular. La presentación no pasa de 10 minutos; cada respuesta, de 90 segundos.
3. **Nicolás hace de profesor.** Pregunta en tono neutro, sin ayudar con gestos ni completando frases, e interrumpe con un «¿por qué?» o «¿qué pasa si…?» al menos una vez por respuesta.
4. **Se responde en voz alta.** Aunque estén los tres en la misma pieza: pensar callado no cuenta. Quien está seguro, habla; quien duda, razona en voz alta (ver sección 7.4).
5. **Todo se verifica en MySQL.** Cada vez que la app cambia algo, se muestra el `SELECT` en phpMyAdmin o por consola. «Se ve en la tabla» no basta.
6. **Orden al azar.** Nicolás saca los números de tarjeta con dados o con una app de números aleatorios; **no se avisa quién va**. Regla extra: a Tais y Rodolfo les toca el **doble** de preguntas que a Nicolás.
7. **Se anota todo.** Como no hay un cuarto integrante, Nicolás anota en la rúbrica de la sección 6 (puntaje 0/2/4/6 por criterio) y la tarjeta que falló. Las tarjetas falladas se repiten al final del ensayo.
8. **Error = dato, no drama.** Si alguien se equivoca, se dice la respuesta correcta una vez, se anota y se sigue. Se corrige después, no en medio del flujo.

---

## 2. Checklist previo (cada uno con su notebook)

Cada integrante lo hace en **su** equipo, porque el viernes la demo puede salir de cualquiera. Orden y propósito de cada paso:

### 2.1 Levantar el entorno Docker

Desde PowerShell, en la **raíz del repo** (la carpeta donde está `docker-compose.yml`):

```powershell
docker compose up -d
```

- **Qué hace:** crea (o arranca) los tres contenedores del entorno: `calendario-netbeans`, `calendario-mysql` y `calendario-phpmyadmin`. `up` los levanta; `-d` (*detached*) los deja corriendo en segundo plano para que la terminal quede libre.
- **Salida esperada:** tres líneas `Container calendario-… Started` (o `Running` si ya estaban arriba). La primera vez baja imágenes y tarda unos minutos.
- **Si sale distinto:** si dice `Cannot connect to the Docker daemon`, falta abrir **Docker Desktop** y esperar a que el ícono quede en verde; luego repetir. Si dice `port is already allocated`, hay otro contenedor usando 3306, 3000 o 8081: bajarlo con `docker stop <nombre>` (ver nombres con `docker ps`) y repetir.

### 2.2 Comprobar que están sanos

```powershell
docker ps --format "table {{.Names}}\t{{.Status}}"
```

- **Qué hace:** lista los contenedores en marcha. `--format "table …"` deja solo nombre y estado en columnas, en vez de las ocho columnas por defecto.
- **Salida esperada:** `calendario-netbeans`, `calendario-mysql` y `calendario-phpmyadmin`, con `Up … (healthy)` en los que tienen *healthcheck*. Si `calendario-mysql` dice `(health: starting)`, esperar 30 segundos y repetir.
- **Si sale distinto:** si falta uno, `docker compose up -d` de nuevo. Si uno dice `Restarting` o `Exited`, ver por qué con `docker logs calendario-mysql --tail 30` y **no seguir con el ensayo** hasta resolverlo (pegar la salida en el grupo).

### 2.3 Abrir las tres pantallas

| Qué | Dirección | Qué debe verse |
|---|---|---|
| NetBeans (en el navegador) | http://localhost:3000 | El escritorio con NetBeans (se abre desde el menú de aplicaciones) |
| phpMyAdmin | http://localhost:8081 | Entra directo como `root`, sin pedir clave; en la izquierda, la base `calendario_app` |
| La app | desde NetBeans | `VentanaLogin` |

### 2.4 Abrir el proyecto y compilarlo

1. En NetBeans: *File → Open Project* → `CalendarioApp`. Debe aparecer sin íconos de error rojos y con el driver `mysql-connector-j-26.10.0.jar` en *Libraries*.
2. **F6** (*Run Project*): se abre `VentanaLogin`. Cerrar con *Salir*.
3. **Alt+F6** (*Test Project*): en la ventana *Test Results* deben aparecer **24 pruebas en verde** (`ConexionTest` 4, `UsuarioDAOTest` 6, `EventoDAOTest` 11, `EventoTest` 3).
4. Si F6 o Alt+F6 salen en rojo: lo más común es MySQL caído o base mal cargada. Resolver con 2.2 y 2.5 y repetir.

### 2.5 Recargar la base a su estado inicial

Desde PowerShell, **en la raíz del repo** (importa: la ruta `.\sql\…` es relativa):

```powershell
Get-Content -Encoding UTF8 .\sql\calendario_bd.sql | docker exec -i calendario-mysql mariadb -u root -t
```

- **Qué hace:** `Get-Content -Encoding UTF8` lee el script como UTF-8 (sin eso las tildes de «Reunión» salen rotas); `|` se lo pasa al comando siguiente; `docker exec -i calendario-mysql` ejecuta dentro del contenedor y `-i` deja que reciba el texto por la entrada estándar; `mariadb -u root` es el cliente SQL como `root`; `-t` imprime los resultados en tablas. El script borra y recrea las dos tablas y vuelve a insertar los 3 usuarios y los 5 eventos.
- **Salida esperada:** varias tablas ASCII: el `DESCRIBE` de `usuario` y de `evento`, los 3 usuarios, los 5 eventos y los dos `SELECT` de `ST_X`/`ST_Y` y `JSON_EXTRACT`, **sin** ninguna línea que empiece con `ERROR`.
- **Si sale distinto:** si aparece `ERROR 1049` o `Unknown database`, el contenedor aún no termina de arrancar: esperar y repetir. Si las tildes salen como `ReuniÃ³n`, faltó `-Encoding UTF8`. Si dice `no such container`, el nombre es otro: revisar con `docker ps`.

### 2.6 Verificar que la base quedó como se espera

```powershell
docker exec calendario-mysql mariadb -u root calendario_app -e "SELECT COUNT(*) AS eventos FROM evento; SELECT COUNT(*) AS usuarios FROM usuario;"
```

- **Qué hace:** ejecuta una consulta suelta (`-e`) sobre la base `calendario_app` sin abrir una sesión interactiva.
- **Salida esperada:** `eventos = 5` y `usuarios = 3`. Si es otro número, quedó basura de una prueba anterior: repetir 2.5.

### 2.7 Lista de verificación final (marcar en voz alta)

- [ ] Los tres contenedores arriba y `calendario-mysql` en `healthy`.
- [ ] phpMyAdmin abre y muestra `calendario_app` con 5 eventos y 3 usuarios.
- [ ] NetBeans con `CalendarioApp` abierto, sin errores rojos.
- [ ] F6 abre el login; Alt+F6 da 24 verdes.
- [ ] Cada uno tiene a mano, **en otra pestaña**, la consulta de verificación de cada operación (para pegarla, no para leerla: se ensaya escribirla de memoria).
- [ ] Celulares en silencio; uno con cronómetro.

---

## 3. Agenda por bloques (≈ 2 h 30 min)

| Hora | Min | Bloque | Quién conduce | Qué se hace |
|---|---|---|---|---|
| 0:00 | 10 | **0. Preparación** | Todos | Checklist de la sección 2. Sin esto no se empieza |
| 0:10 | 15 | **1. Presentación completa** | Nicolás cronometra | 16 diapositivas, **≤ 10 min**, más 5 min de comentarios |
| 0:25 | 20 | **2. Ronda B1: demostraciones** | Nicolás | Operación al azar + verificación en MySQL |
| 0:45 | 15 | **3. Ronda B2/B4: ubica y explica** | Nicolás | Nombra un método o sentencia; el otro lo encuentra y lo explica |
| 1:00 | 5 | Pausa | — | Agua. Cero pantallas de estudio |
| 1:05 | 15 | **4. Ronda B5: preguntas al azar** | Nicolás | Tarjetas del banco (sección 4) |
| 1:20 | 20 | **5. Modificar en vivo** | Nicolás | Un ejercicio de la sección 5 por persona |
| 1:40 | 12 | **6. Ronda de trampas** | Nicolás | SQL inválido, «¿qué hace esta línea?», «¿qué pasa si…?» |
| 1:52 | 25 | **7. Simulacro final completo** | Nicolás (profesor) | Como el viernes: presentación breve + preguntas de las 5 rúbricas, a cada uno |
| 2:17 | 13 | **8. Rúbrica y cierre** | Todos | Rúbrica de la sección 6, tarjetas falladas y plan de refuerzo |

### 3.1 Bloque 1 — Presentación cronometrada (≤ 10 min)

Orden de las 16 diapositivas según `presentacion/gen_pptx.py` (quién presenta cada una) y tiempo objetivo:

| # | Diapositiva | Presenta | Objetivo |
|---|---|---|---|
| 1 | Portada | Nicolás | 0:30 |
| 2 | El problema y el tema | Nicolás | 1:00 |
| 3 | Modelo de datos | **Tais** | 1:00 |
| 4 | Script SQL: S1 a S5 | **Tais** | 0:45 |
| 5 | Arquitectura en paquetes | **Tais** | 0:30 |
| 6 | Clase `Conexion` | **Rodolfo** | 0:45 |
| 7 | Flujo JDBC | **Rodolfo** | 0:45 |
| 8 | Login: L1, V1 y V2 | **Rodolfo** | 0:45 |
| 9 | CRUD sobre `evento`: C1 a C5 | Nicolás | 0:45 |
| 10 | Excepciones y recursos: V3, V4 | Nicolás | 0:30 |
| 11 | POINT y JSON en la práctica | Nicolás | 0:45 |
| 12 | Pruebas | **Tais** | 0:30 |
| 13 | Reparto del trabajo | **Tais** | 0:30 |
| 14 | Demostración (1/2): acceso | **Rodolfo** | 1:30 (en vivo) |
| 15 | Demostración (2/2): CRUD | Nicolás y Tais | 2:00 (en vivo) |
| 16 | Cierre | **Rodolfo** | 0:30 |

> **Ojo con dos diapositivas:** la 12 (**Pruebas**) la presenta Tais, pero las pruebas las escribió Nicolás, y la 15 mezcla a Nicolás y Tais en el CRUD. Si el profesor le pregunta a Tais por `EventoDAOTest`, **tiene que saberlo**. Se ensaya a propósito: en el ensayo, Tais explica la diapositiva 12 y Nicolás la corrige.

Reglas del bloque: nadie lee la diapositiva; se dice con palabras propias lo que la diapositiva muestra. Si se pasan de 10 minutos, se anota qué diapositiva se alargó y se acorta.

### 3.2 Bloque 2 — Ronda B1: demostraciones al azar (20 min)

Nicolás saca al azar **una operación y una persona**; la persona la ejecuta en la app, **dice en voz alta qué espera ver** y luego lo verifica en MySQL. Operaciones (códigos de la pauta, tabla de `docs/guia_defensa.md`):

| Dado | Operación | Verificación en MySQL |
|---|---|---|
| 1 | L1: login correcto con `admin` / `Admin2026` | `SELECT id_usuario, nombre_usuario, rol, activo FROM usuario;` |
| 2 | V1: campos vacíos y clave mala; además `ADMIN2026` (mayúsculas) | La misma consulta; explicar por qué no entra (`utf8mb4_bin`) |
| 3 | V2: `inactivo1` / `Inac2026` | `SELECT nombre_usuario, activo FROM usuario WHERE nombre_usuario = 'inactivo1';` |
| 4 | C1: insertar `EVT-0100` con latitud, longitud y JSON | `SELECT codigo_evento, ST_X(ubicacion), ST_Y(ubicacion), detalle FROM evento WHERE codigo_evento = 'EVT-0100';` |
| 5 | V3: guardar otro evento con el mismo código | `SELECT COUNT(*) FROM evento WHERE codigo_evento = 'EVT-0100';` debe seguir en 1 |
| 6 | C3 y C4: buscar y actualizar el título | `SELECT codigo_evento, titulo FROM evento WHERE codigo_evento = 'EVT-0100';` |
| 7 | C5: eliminar con confirmación | `SELECT COUNT(*) FROM evento WHERE codigo_evento = 'EVT-0100';` → 0 |
| 8 | V4: apagar MySQL y operar (ver 3.2.1) | `docker start calendario-mysql` y comprobar que la app sigue viva |

Puntúa Nicolás: ¿ejecutó sin ayuda?, ¿dijo qué esperaba ver?, ¿verificó en MySQL?, ¿explicó con el término técnico (`executeUpdate()`, `ResultSet`…)?

#### 3.2.1 Apagar y volver a levantar MySQL (V4)

Antes de este paso se **avisa en voz alta** («voy a apagar MySQL»), porque deja sin base a todo lo demás.

```powershell
docker stop calendario-mysql
```

- **Qué hace:** detiene el contenedor de la base. NetBeans y phpMyAdmin siguen corriendo, pero ya no tienen servidor al que conectarse.
- **Salida esperada:** imprime `calendario-mysql`. Después, en la app, *Ingresar* o *Guardar* debe mostrar «No fue posible conectar con la base de datos.» y **la ventana sigue abierta**.
- **Si sale distinto:** si la app se congela unos segundos antes del mensaje, es normal (el driver espera el *timeout*); si se cierra o aparece un error de Java en rojo, anotarlo: es lo que el profesor quiere ver bien manejado.

```powershell
docker start calendario-mysql
```

- **Qué hace:** vuelve a arrancar el mismo contenedor, **conservando los datos** (a diferencia de `docker compose down -v`, que los borra).
- **Salida esperada:** imprime `calendario-mysql`. A los ~10 segundos el estado pasa a `healthy` (ver con `docker ps`).
- **Si sale distinto:** si la app sigue sin conectar tras 30 segundos, `docker logs calendario-mysql --tail 30`. Si la base apareció vacía, recargar con 2.5.

### 3.3 Bloque 3 — Ronda B2/B4: «ubica y explica» (15 min)

Nicolás **nombra** un método o una sentencia; la persona abre el archivo en NetBeans (sin buscar por menús: *Ctrl+Shift+O* o el navegador de proyecto), lo señala y lo explica con la **plantilla de 4 frases**:

> (1) Qué sentencia SQL ejecuta. (2) Qué `?` rellena y con qué `setXxx`. (3) Si usa `executeQuery()` o `executeUpdate()` y qué devuelve. (4) Cómo se cierran los recursos.

Lista de lo que Nicolás puede nombrar (cada uno se debe encontrar en **menos de 20 segundos**):

| Nombra… | Archivo |
|---|---|
| «La conexión» / `conectar()` | `conexion/Conexion.java` |
| `mensajeError()` (códigos 1062, 1045, 1049, estado `08`, 4025/3819/3140) | `conexion/Conexion.java` |
| `validar()` y `estaInactivo()` | `dao/UsuarioDAO.java` |
| `insertar()` | `dao/EventoDAO.java` |
| `listar()` / `buscar()` | `dao/EventoDAO.java` |
| `actualizar()` / `eliminar()` | `dao/EventoDAO.java` |
| `cargarParametros()` y `leerFila()` | `dao/EventoDAO.java` |
| La constante `COLUMNAS` (`ST_Y`/`ST_X`) | `dao/EventoDAO.java` |
| `leerFormulario()` (validaciones de formato) | `vista/VentanaEventos.java` |
| `btnIngresarActionPerformed` | `vista/VentanaLogin.java` |
| `CREATE TABLE evento` y sus 4 `CHECK` | `sql/calendario_bd.sql` |
| `tieneUbicacion()` | `modelo/Evento.java` |

Regla: **a Tais le toca ubicar código Java y a Rodolfo el SQL** al menos la mitad de las veces. El profesor no pregunta por «lo suyo».

### 3.4 Bloque 4 — Ronda B5: preguntas al azar (15 min)

Se usan las tarjetas del banco (sección 4). Nicolás lee la pregunta una sola vez. La persona responde en ≤ 90 segundos **y la aplica al proyecto** (nombrar archivo, método o columna). Respuesta sin aplicación al proyecto = 4 puntos como máximo.

### 3.5 Bloque 5 — Modificar en vivo (20 min)

Un ejercicio de la sección 5 por persona (Tais y Rodolfo hacen dos, Nicolás uno). Cronómetro: **5 minutos por ejercicio**. Se verifica con F6 y con el `SELECT`.

### 3.6 Bloque 6 — Ronda de trampas (12 min)

Tarjetas marcadas «trampa» del banco. Lo que se entrena aquí no es saber más, sino **no caer**: detectar que algo no tiene sentido, decirlo y razonar.

### 3.7 Bloque 7 — Simulacro final completo (25 min)

Como el viernes: Nicolás es el profesor y aplica **a cada uno** (Tais, Rodolfo, Nicolás; ~8 min por persona) una demostración (B1), un «ubica y explica» (B4), una pregunta (B5) y, a uno de los tres, una modificación en vivo. Sin interrupciones del equipo. Al final, cada uno se pone nota con la rúbrica de la sección 6.

### 3.8 Bloque 8 — Rúbrica y cierre (13 min)

Se completa la rúbrica, se ordenan las tarjetas falladas y se asigna el plan de refuerzo de la sección 6.2.

---

## 4. Banco de 40 tarjetas

**Cómo usarlas:** Nicolás lee solo la pregunta. «Esperada» es lo mínimo que debe decirse; «Alerta» es lo que delata que se memorizó sin entender (si aparece, repreguntar «¿y por qué?»). **Objetivo** = a quién se le asigna (Tais, Rodolfo o cualquiera). Las marcadas **(trampa)** buscan que la persona se detenga y detecte el problema.

### Bloque A — SQL y modelo (B2 y B5)

**T01 · Tais · B2** — Explique el modelo de datos en 60 segundos.
- *Esperada:* dos tablas independientes, sin clave foránea, en la base `calendario_app`: `usuario` (acceso) y `evento` (CRUD). `evento` tiene 14 columnas (la pauta pide 8), con PK `id_evento`, identificador de negocio `codigo_evento` (UNIQUE), `ubicacion POINT`, `detalle JSON`, DATE/TIME/INT/BOOLEAN y 4 `CHECK`.
- *Alerta:* «una tabla de eventos y una de usuarios» sin decir qué columnas ni por qué no hay FK.

**T02 · Tais · B5** — ¿Diferencia entre PRIMARY KEY y UNIQUE? Muéstrela en `evento`.
- *Esperada:* PK: una por tabla, nunca `NULL`, identifica la fila (`id_evento`). UNIQUE: puede haber varias, no repite el valor (`codigo_evento`; en `usuario`, `nombre_usuario`).
- *Alerta:* «son lo mismo» o no saber cuántas PK/UNIQUE tiene `evento` (1 y 1).

**T03 · cualquiera · B5** — ¿Por qué `id_evento` y además `codigo_evento`?
- *Esperada:* `id_evento` es clave técnica autogenerada (`AUTO_INCREMENT`); `codigo_evento` (`EVT-0001`) es el identificador de negocio que el usuario escribe y por el que se busca (C3). El UPDATE y el DELETE usan el id.
- *Alerta:* no saber por cuál campo busca `buscar()` (es `codigo_evento`).

**T04 · Tais · B2** — ¿Qué hace cada restricción: PK, UNIQUE, NOT NULL, DEFAULT, CHECK? Un ejemplo de cada una en `evento`.
- *Esperada:* `id_evento`, `codigo_evento`, `titulo`, `categoria DEFAULT 'personal'`, `prioridad BETWEEN 1 AND 3`. Qué error da cada una: 1062 duplicado, 1364 sin valor, 4025/3819 el `CHECK`.
- *Alerta:* confundir DEFAULT con NOT NULL.

**T05 · Tais · B5** — ¿Qué es un `CHECK` y cuáles tiene `evento`?
- *Esperada:* una regla que cada fila debe cumplir. Cuatro: `duracion_minutos > 0`, `prioridad BETWEEN 1 AND 3`, `hora_fin IS NULL OR hora_fin > hora_inicio`, `detalle IS NULL OR JSON_VALID(detalle)`. Se aplica desde MySQL 8.0.16 y MariaDB 10.2.
- *Alerta:* nombrar menos de 3, o no saber por qué `hora_fin IS NULL OR …` (porque `hora_fin` puede ser `NULL`).

**T06 · Rodolfo · B5** — ¿Por qué una columna `POINT` y en qué orden va?
- *Esperada:* guarda la ubicación como un solo dato geoespacial. Se escribe `POINT(longitud, latitud)`: X = longitud (horizontal), Y = latitud (vertical). Se lee con `ST_X` y `ST_Y`.
- *Alerta:* invertirlo («latitud, longitud»). En `POINT(-70.6506, -33.4378)` la latitud es la **segunda**.

**T07 · Rodolfo · B5** — ¿Cómo se guarda y se lee la columna `JSON` desde Java?
- *Esperada:* se guarda con `ps.setString(6, e.getDetalle())` (texto) y se lee con `rs.getString("detalle")`. La base lo valida con `CHECK (JSON_VALID(detalle))`.
- *Alerta:* decir que Java tiene un tipo JSON o que se parsea con una librería: aquí es un `String`.

**T08 · Tais · B5** — ¿Qué es una *collation* y por qué `clave` usa `utf8mb4_bin`?
- *Esperada:* regla de comparación y orden del texto. Las `_ci` ignoran mayúsculas (la base usa `utf8mb4_spanish_ci`) y dejaban entrar `ADMIN2026` como `Admin2026`. `_bin` compara bytes exactos.
- *Alerta:* no saber que fue un bug real encontrado probando.

**T09 · cualquiera · B5** — ¿Qué es `BOOLEAN` en MySQL?
- *Esperada:* alias de `TINYINT(1)`: 1 verdadero, 0 falso. En Java se usa `getBoolean` / `setBoolean`.
- *Alerta:* decir que existe un tipo booleano real distinto.

**T10 · Tais · B5** — `DEFAULT` en acción: ¿qué pasa con `EVT-0005`, que se insertó con solo 4 campos?
- *Esperada:* toma `categoria = 'personal'`, `duracion_minutos = 60`, `prioridad = 2`, `activo = TRUE` y `fecha_creacion = CURRENT_TIMESTAMP`. Los campos sin `DEFAULT` y `NOT NULL` (código, título, fecha, hora de inicio) hay que darlos.
- *Alerta:* no distinguir qué columnas son obligatorias.

**T11 · Tais · B4** — Muestre en `sql/calendario_bd.sql` cómo se crea la base y por qué hay `DROP TABLE IF EXISTS`.
- *Esperada:* `CREATE DATABASE IF NOT EXISTS` con `utf8mb4`; los `DROP … IF EXISTS` permiten volver a correr el script desde cero sin error. Se borra primero `evento` y luego `usuario` (sin FK el orden no importa, pero es costumbre).
- *Alerta:* no saber que el script **borra los datos** al re-ejecutarse.

**T12 · cualquiera · B5 (trampa)** — «Escriba la columna `detalle JSON(255)` y la `ubicacion POINT(150)`.»
- *Esperada:* detenerse y decir que **no existe**: `JSON` y `POINT` son tipos sin largo (a diferencia de `VARCHAR(100)`); `POINT(150)` con un número sería la *función* `POINT(x, y)` que pide **dos** argumentos, no una definición de columna. Correcto: `detalle JSON NULL` y `ubicacion POINT NULL`.
- *Alerta:* «lo escribo como usted dice». Quien no cuestiona una sintaxis inválida pierde el criterio.

**T13 · Tais · B2 (trampa)** — «En MySQL, ¿`JSON` es un tipo nativo?»
- *Esperada:* en MySQL 8 sí (con validación y almacenamiento binario). En **MariaDB 10.6** es un alias de `LONGTEXT`, por eso el proyecto agrega `CHECK (JSON_VALID(detalle))` y funciona igual en ambos motores.
- *Alerta:* afirmar que es nativo en los dos sin matiz.

### Bloque B — JDBC y capas (B2 y B5)

**T14 · Rodolfo · B2** — Explique el flujo JDBC y el orden de los objetos.
- *Esperada:* `Connection` (línea con la base) → `PreparedStatement` (sentencia con `?`) → `ResultSet` (filas del `SELECT`). Se declaran en un `try-with-resources`.
- *Alerta:* mezclar el orden o decir que el `ResultSet` existe en un `INSERT` (no: `executeUpdate()` devuelve un `int`).

**T15 · Rodolfo · B5** — `executeQuery()` contra `executeUpdate()`.
- *Esperada:* `executeQuery()` → `SELECT` → `ResultSet` (L1, C2, C3). `executeUpdate()` → `INSERT`/`UPDATE`/`DELETE` → filas afectadas, un `int` (C1, C4, C5).
- *Alerta:* «da lo mismo» o no poder dar un ejemplo del proyecto de cada uno.

**T16 · Rodolfo · B5** — Explique la URL de conexión pieza por pieza.
- *Esperada:* `jdbc:mysql://localhost:3306/calendario_app?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Santiago`: protocolo, servidor, puerto 3306, base y parámetros (sin SSL en local, recuperar clave pública para autenticar y zona horaria).
- *Alerta:* no saber qué es `3306`, o que `localhost` funciona porque la base está en la red del contenedor de NetBeans.

**T17 · Rodolfo · B5** — ¿Para qué sirve la clase `Conexion` y por qué los datos son `static final`?
- *Esperada:* centraliza URL, usuario y clave (A4); los DAO solo llaman `Conexion.conectar()`. Si cambia la clave se edita un archivo. Son constantes porque no cambian en ejecución.
- *Alerta:* decir que cada DAO tiene su propia URL.

**T18 · Rodolfo · B5** — ¿Qué es el driver, dónde está y cómo lo encuentra Java?
- *Esperada:* implementación de JDBC para MySQL: `mysql-connector-j-26.10.0.jar` en `CalendarioApp/lib/`, agregado a *Libraries*. `DriverManager` lo encuentra solo por la URL `jdbc:mysql:`.
- *Alerta:* «lo instaló NetBeans» o no saber el nombre del jar.

**T19 · cualquiera · B5 (trampa)** — «¿Por qué no usan `Statement` y concatenan los datos con `+`? Es más corto.»
- *Esperada:* inyección SQL. Con `' OR '1'='1` en la clave, el texto se vuelve código y entra cualquiera. Con `?` el dato viaja aparte y siempre es un valor. Además la pauta exige `PreparedStatement` (A6 a A10).
- *Alerta:* «porque lo pide la pauta» sin explicar el riesgo.

**T20 · Rodolfo · B5** — ¿Qué es un `ResultSet` y para qué sirve `next()`?
- *Esperada:* cursor sobre las filas del resultado, que parte **antes** de la primera. `next()` avanza y devuelve `false` al terminar. `listar()` usa `while (rs.next())`; `buscar()` y `validar()` usan `if (rs.next())`.
- *Alerta:* intentar `rs.getString(...)` sin llamar antes a `next()`.

**T21 · cualquiera · B5 (trampa)** — «¿Qué pasa si borro el `try-with-resources` de `buscar()` y dejo solo `Connection con = Conexion.conectar();`?»
- *Esperada:* compila igual, pero si nadie llama a `close()` la conexión queda abierta (fuga): tras muchas llamadas, MySQL se queda sin conexiones. El `try (…)` cierra los recursos solos, aun con excepción, y reemplaza al `finally`. Se cierran al revés de como se abrieron.
- *Alerta:* «no pasa nada porque Java limpia solo»: eso confunde con la recolección de basura, que no cierra conexiones de red.

**T22 · Rodolfo · B5 (trampa)** — «¿Qué pasa si apago MySQL mientras la app está abierta?»
- *Esperada:* `getConnection` lanza `SQLException` con `SQLState` que empieza en `08`; `Conexion.mensajeError()` lo traduce a «No fue posible conectar con la base de datos.» y la ventana sigue viva (V4). Demostrable con `docker stop calendario-mysql`.
- *Alerta:* «se cierra la app» o no poder decir **dónde** está el `catch`.

**T23 · cualquiera · B5** — ¿Qué es una `SQLException` y qué datos trae?
- *Esperada:* la excepción de todo error JDBC: `getMessage()`, `getSQLState()` y `getErrorCode()`. Con el código identifican 1062 (duplicado), 1045/1049 (acceso o base inexistente) y los `CHECK`.
- *Alerta:* no saber que existe `getErrorCode()` (se usa en `mensajeError`).

**T24 · cualquiera · B2** — ¿Qué es un DAO y por qué existe?
- *Esperada:* *Data Access Object*: la clase que concentra el SQL de una tabla. La ventana no sabe SQL y solo llama `eventoDAO.insertar(e)`. Separa responsabilidades y deja cambiar la base sin tocar la interfaz.
- *Alerta:* «una clase que guarda datos» (eso es el modelo).

**T25 · Tais · B2** — Nombre las cuatro capas y qué hace cada una.
- *Esperada:* `vista` (ventanas, sin SQL) → `dao` (todo el SQL) → `modelo` (`Usuario`, `Evento`: una fila de cada tabla) y `conexion` (la única con URL, usuario y clave).
- *Alerta:* poner SQL en la vista o creer que `modelo` accede a la base.

**T26 · cualquiera · B5** — ¿Qué hace `RETURN_GENERATED_KEYS` en `insertar()`?
- *Esperada:* pide al driver devolver los valores autogenerados; después `getGeneratedKeys()` entrega el `id_evento` que creó `AUTO_INCREMENT` y se guarda en el objeto con `setIdEvento`.
- *Alerta:* creer que sirve para «generar» la clave desde Java.

**T27 · Rodolfo · B5** — ¿Qué hace `probarConexion()` y qué devuelve?
- *Esperada:* abre una conexión en un `try-with-resources` y devuelve `con.isValid(2)` (2 segundos de espera); `false` si hay `SQLException`. Los tests de `ConexionTest` la usan.
- *Alerta:* decir que lanza la excepción (la captura).

### Bloque C — Login y validaciones (B1, B4, B5)

**T28 · Rodolfo · B4** — Explique `UsuarioDAO.validar()` línea por línea.
- *Esperada:* `SELECT … WHERE nombre_usuario = ? AND clave = ? AND activo = TRUE`; dos `setString`; `executeQuery()`; si `rs.next()` arma y devuelve un `Usuario`, si no, `null`; recursos en `try-with-resources`.
- *Alerta:* no saber por qué devuelve `null` para un usuario inactivo (el filtro `activo = TRUE` lo excluye).

**T29 · Rodolfo · B4** — ¿Cómo distingue la app «clave mala» de «usuario inactivo»?
- *Esperada:* `validar()` devuelve `null` en ambos; después `estaInactivo()` cuenta (`SELECT COUNT(*) … AND activo = FALSE`) si las credenciales **sí** coinciden con un inactivo. Si da mayor que 0: «Usuario inactivo.»; si no: «Usuario o contraseña incorrectos.».
- *Alerta:* decir que `validar()` ya sabe el motivo.

**T30 · Rodolfo · B5 (trampa)** — «Un usuario escribe `admin' --` en el usuario y nada en la clave. ¿Entra?»
- *Esperada:* no. Primero, el campo vacío se rechaza antes de consultar (V1). Aunque pusiera clave, `?` trata todo el texto como valor: busca un usuario llamado literalmente `admin' --`, que no existe.
- *Alerta:* «sí, el `--` comenta el resto»: eso es verdad solo con concatenación.

**T31 · cualquiera · B5** — ¿Por qué la clave está en texto plano? ¿Qué harían en un sistema real?
- *Esperada:* la pauta entrega el script de `usuario` con claves como `'Admin2026'`. En real se guardaría un *hash* con sal (bcrypt, Argon2) y se compararía el hash. Reconocerlo suma.
- *Alerta:* defenderlo como buena práctica.

### Bloque D — CRUD, POINT y JSON (B1, B4, B5)

**T32 · Rodolfo · B4** — Explique `EventoDAO.insertar()`.
- *Esperada:* `INSERT` de 12 columnas con `POINT(?, ?)` en `ubicacion`; `prepareStatement(sql, RETURN_GENERATED_KEYS)`; `cargarParametros` rellena 13 `?`; `executeUpdate()`; recupera el id con `getGeneratedKeys()`.
- *Alerta:* contar mal los `?` (son 13 porque `POINT(?, ?)` consume dos).

**T33 · Rodolfo · B4** — ¿Por qué `actualizar()` hace `ps.setInt(14, e.getIdEvento())`?
- *Esperada:* `cargarParametros` llena los parámetros 1 a 13 (iguales a los del INSERT); el `?` 14 es el del `WHERE id_evento = ?`. Devuelve `true` si `executeUpdate() == 1` (cambió exactamente una fila).
- *Alerta:* «el 14 es un error» o no saber que el UPDATE usa el `id` y no el código.

**T34 · Tais · B4** — Explique `cargarParametros()`: ¿qué pasa si el evento no tiene ubicación?
- *Esperada:* si `tieneUbicacion()` es falso, `ps.setNull(4, Types.DOUBLE)` y `ps.setNull(5, Types.DOUBLE)`: `POINT(NULL, NULL)` da `NULL` en la columna. Mismo patrón con `hora_fin` (`Types.TIME`).
- *Alerta:* creer que se guarda `0,0` (que sería un punto real en el golfo de Guinea).

**T35 · Tais · B4** — ¿Para qué sirve `rs.wasNull()` en `leerFila()`?
- *Esperada:* `getDouble` devuelve `0.0` cuando el valor es `NULL`; `wasNull()` dice si la **última lectura** fue `NULL`, para distinguir un 0 real de «sin dato» y dejar `latitud = null`.
- *Alerta:* llamarlo antes de leer la columna (hay que llamarlo **después**).

**T36 · Tais · B4** — ¿Qué hace la constante `COLUMNAS` y por qué usa `ST_Y(ubicacion) AS latitud`?
- *Esperada:* es el texto de las columnas del `SELECT` de `listar()` y `buscar()`. Un `POINT` no se lee directo: se extrae `ST_Y` (latitud) y `ST_X` (longitud), con alias para leerlos por nombre.
- *Alerta:* creer que `rs.getString("ubicacion")` devuelve «-70.65, -33.43».

**T37 · cualquiera · B5 (trampa)** — «Dos usuarios guardan `EVT-0100` a la vez. ¿Qué pasa?»
- *Esperada:* el `UNIQUE` de `codigo_evento` hace que el segundo `INSERT` falle con error 1062; `mensajeError` responde «El registro ya existe.» y no se inserta nada (V3). La base es la que garantiza la unicidad, no el código Java.
- *Alerta:* «lo valido antes con un `SELECT`»: tiene una carrera; el `UNIQUE` es la defensa real.

**T38 · cualquiera · B4** — ¿Cómo se confirma la eliminación y dónde está esa confirmación?
- *Esperada:* en `btnEliminarActionPerformed` se muestra `JOptionPane.showConfirmDialog` con Sí/No **antes** de llamar `eventoDAO.eliminar(idSeleccionado)`; si responde distinto de `YES_OPTION`, retorna sin borrar. Es la capa de vista, no el DAO.
- *Alerta:* ubicar la confirmación dentro del DAO.

**T39 · Tais · B5** — ¿Cómo comprueban que todo funciona? Hable de las pruebas.
- *Esperada:* 24 pruebas JUnit 4 en `CalendarioApp/test/` (`ConexionTest` 4, `UsuarioDAOTest` 6, `EventoDAOTest` 11, `EventoTest` 3), todas verdes con Alt+F6 (`ant test`). JUnit 4 y no 5 porque `ant test` en NetBeans no ejecuta JUnit 5. Las de DAO usan códigos `TEST-JU-*` y limpian lo que crean.
- *Alerta:* no saber que las pruebas de DAO necesitan MySQL arriba, o decir «las hizo Nicolás» y no poder explicar una.

**T40 · cualquiera · B5 (trampa)** — Se señala **una línea al azar** del código («¿qué hace esta línea?»). Ejemplos que Nicolás puede señalar:
- `ps.setDouble(4, e.getLongitud());` → pone la **longitud** en el 4.º `?` (la X del `POINT`); la latitud va en el 5.º.
- `return ps.executeUpdate() == 1;` → ejecuta el UPDATE/DELETE y da `true` solo si afectó exactamente una fila.
- `rs.getDate("fecha_evento").toLocalDate()` → convierte el `java.sql.Date` de la base a `LocalDate`.
- `Time.valueOf(e.getHoraInicio())` → convierte `LocalTime` a `java.sql.Time` para poder enviarla.
- `txtClave.getPassword()` → un `JPasswordField` devuelve `char[]`, no `String`; por eso se hace `new String(...)`.
- *Esperada:* decir **qué hace y por qué está ahí**, y a qué criterio de la pauta sirve.
- *Alerta:* describir la sintaxis («llama al método set Double») sin decir qué columna o qué efecto tiene.

---

## 5. Ejercicios de modificación en vivo (6)

Cada ejercicio tiene **5 minutos**. Para todos: guardar (Ctrl+S), compilar (F6), probar y **verificar en MySQL**. Los nombres son los reales del código.

> Tras cada ejercicio, **recargar la base** con 2.5 (y deshacer el cambio en el código: Ctrl+Z o `git checkout -- <archivo>` si el repo está limpio) para que el siguiente parta limpio.

### M1 · Filtro por categoría (objetivo: **Rodolfo**)

**Pedido:** «Agregue un método que liste solo los eventos de una categoría.»

En `dao/EventoDAO.java`, debajo de `listar()`:

```java
public ArrayList<Evento> listarPorCategoria(String categoria) throws SQLException {
    String sql = "SELECT " + COLUMNAS + " FROM evento WHERE categoria = ? "
            + "ORDER BY fecha_evento, hora_inicio";
    ArrayList<Evento> lista = new ArrayList<>();
    try (Connection con = Conexion.conectar();
            PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setString(1, categoria);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(leerFila(rs));
            }
        }
    }
    return lista;
}
```

- **Verificación:** `SELECT codigo_evento, categoria FROM evento WHERE categoria = 'trabajo';` → solo `EVT-0001`. Las categorías posibles son `personal`, `trabajo`, `salud` y `estudio` (las del `cboCategoria`).
- **Qué debe decir:** «`executeQuery()` porque es un `SELECT`; el `?` va con `setString(1, …)`; el `ResultSet` está **dentro** del `try` de la sentencia porque necesita el parámetro ya puesto antes de ejecutar».
- **Error típico:** concatenar `"… WHERE categoria = '" + categoria + "'"` (inyección). Si lo escribe así, la trampa se activa: preguntar «¿y esto qué riesgo tiene?».

### M2 · Cambiar un mensaje de validación (objetivo: **Tais**)

**Pedido:** «Cuando falte el título, el mensaje debe decir ‹El evento necesita un título para guardarse.›»

En `vista/VentanaEventos.java`, dentro de `leerFormulario()`, cambiar la línea:

```java
mostrarError("Debe ingresar el título del evento.", txtTitulo);
```

por

```java
mostrarError("El evento necesita un título para guardarse.", txtTitulo);
```

- **Verificación:** dejar el título vacío y pulsar *Guardar*: sale el mensaje nuevo, el foco vuelve a `txtTitulo` y **no se inserta nada** (`SELECT COUNT(*) FROM evento;` sigue igual).
- **Qué debe decir:** «la validación está en la vista, antes de llamar al DAO; devuelve `null` y `btnGuardarActionPerformed` hace `return`; por eso no llega a MySQL».
- **Variante para Rodolfo:** el mensaje del login «Usuario o contraseña incorrectos.» aparece **dos veces** en `btnIngresarActionPerformed` (campos vacíos y credenciales malas). Debe cambiar **las dos** y explicar por qué hay dos (V1 en dos caminos distintos).

### M3 · Columna nueva de punta a punta (objetivo: **Tais** con apoyo de Rodolfo)

**Pedido:** «Agregue el campo `organizador` (texto de hasta 50) a los eventos.» Se hace **en este orden** y se dice el porqué de cada paso:

1. **SQL.** En phpMyAdmin (pestaña SQL) o por consola:

   ```sql
   ALTER TABLE evento ADD COLUMN organizador VARCHAR(50) NULL;
   ```

   Verificar con `DESCRIBE evento;` (aparece al final). Luego agregarla también a `sql/calendario_bd.sql` en el `CREATE TABLE`, porque si no, al recargar el script **desaparece**.

2. **Modelo** (`modelo/Evento.java`): atributo `private String organizador;` con `getOrganizador()` y `setOrganizador(String)`. **No** se agrega al constructor de 14 parámetros: lo usan `leerFila` y las pruebas, y cambiarlo rompería los 24 tests.

3. **DAO** (`dao/EventoDAO.java`), cuatro retoques:
   - `COLUMNAS`: agregar `, organizador` al final.
   - `insertar()`: agregar `, organizador` a la lista de columnas y un `?` más al final de `VALUES`.
   - `actualizar()`: agregar `, organizador = ?` justo antes del `WHERE`; el `id` pasa de `setInt(14, …)` a `setInt(15, …)`.
   - `cargarParametros()`: al final, `ps.setString(14, e.getOrganizador());`.
   - `leerFila()`: guardar el objeto en una variable, `ev.setOrganizador(rs.getString("organizador"));` y devolverlo.

4. **Vista:** un `JTextField txtOrganizador` agregado desde *Design*, leído en `leerFormulario()` y mostrado en `cargarFormulario()`. Si el tiempo no alcanza, se declara «hasta el DAO» y se verifica con SQL: `UPDATE evento SET organizador = 'Tais' WHERE codigo_evento = 'EVT-0001';` y luego `buscar("EVT-0001")` desde un `main` o prueba.

- **Verificación:** guardar un evento con organizador y `SELECT codigo_evento, organizador FROM evento WHERE codigo_evento = 'EVT-0100';`.
- **Qué debe decir:** el orden **base → modelo → DAO → vista** y que el número de `?` y el índice de `setXxx` deben calzar (si no, `Parameter index out of range`).

### M4 · SELECT con `JSON_EXTRACT` (objetivo: **Rodolfo**)

**Pedido:** «Muéstreme los eventos cuya nota contenga la palabra ‹semanal›.»

```sql
SELECT codigo_evento, titulo,
       JSON_UNQUOTE(JSON_EXTRACT(detalle, '$.notas')) AS notas
FROM evento
WHERE JSON_UNQUOTE(JSON_EXTRACT(detalle, '$.notas')) LIKE '%semanal%';
```

- **Salida esperada:** una fila: `EVT-0001`, «Reunión de equipo», «Revisión semanal de avances».
- **Variante (participante en el arreglo):** `WHERE JSON_CONTAINS(JSON_EXTRACT(detalle, '$.participantes'), '"Tais"');` → `EVT-0001`. La comilla doble va **dentro** de la comilla simple porque el candidato debe ser JSON válido.
- **Si lo piden en Java**, método en `EventoDAO` con la misma estructura de M1, `LIKE ?` y `ps.setString(1, "%" + texto + "%");` (los `%` se agregan al **valor**, no a la sentencia).
- **Qué debe decir:** `$` es la raíz del documento, `$.notas` el campo; `JSON_EXTRACT` devuelve JSON (con comillas) y `JSON_UNQUOTE` las quita; en MariaDB no existe el atajo `->` de MySQL.

### M5 · SELECT con `ST_X` / `ST_Y` (objetivo: **Tais**)

**Pedido:** «Muéstreme los eventos que están más al sur que la latitud -33,45.»

```sql
SELECT codigo_evento, titulo, ST_X(ubicacion) AS longitud, ST_Y(ubicacion) AS latitud
FROM evento
WHERE ST_Y(ubicacion) < -33.45;
```

- **Salida esperada:** solo `EVT-0002` (latitud `-33.4569`). `EVT-0003` y `EVT-0005` no aparecen porque su `ubicacion` es `NULL` y la comparación con `NULL` nunca es verdadera.
- **Variante de longitud:** `WHERE ST_X(ubicacion) < -70.649` → `EVT-0001` y `EVT-0004` (longitud `-70.6506`).
- **Qué debe decir:** «más al sur» = latitud **más negativa**, o sea, **menor**; `ST_Y` es la latitud; `NULL` queda fuera sin necesidad de `IS NOT NULL`.
- **Error típico:** usar `ST_X` para la latitud, o invertir `<` por `>` sin razonar el signo.

### M6 · Cambiar una regla del login (objetivo: **Rodolfo**)

**Pedido:** «Que solo los usuarios con rol ADMIN puedan entrar.»

En `dao/UsuarioDAO.java`, `validar()`, agregar al final de la consulta:

```java
+ "FROM usuario WHERE nombre_usuario = ? AND clave = ? AND activo = TRUE AND rol = 'ADMIN'";
```

- **Verificación:** `admin` / `Admin2026` entra; `operador1` / `Oper2026` recibe «Usuario o contraseña incorrectos.». Comprobar los roles con `SELECT nombre_usuario, rol FROM usuario;`.
- **Qué debe decir:** un valor fijo (`'ADMIN'`) puede ir dentro de la sentencia, porque **no viene del usuario**; lo que nunca se concatena son los datos que escribe el usuario. Si pidieran «el rol por parámetro», sería un tercer `?` con `setString(3, …)`.
- **Consecuencia a notar en voz alta:** el mensaje de `operador1` es engañoso («contraseña incorrecta» cuando en realidad le falta el rol). Con tiempo, se mejora con otro método como `estaInactivo()`.

---

## 6. Rúbrica de autoevaluación y plan de refuerzo

### 6.1 Rúbrica del ensayo (criterios B1 a B5 de la pauta, 0/2/4/6)

Cada integrante se pone nota **y** la pone Nicolás. Si no coinciden, manda la más baja hasta el siguiente ensayo.

| Criterio | 0 | 2 | 4 | 6 | Qué debe mejorar si saca menos de 6 |
|---|---|---|---|---|---|
| **B1. Demostración** | No logra ejecutar la operación | Ejecuta con ayuda o sin verificar en MySQL | Ejecuta y verifica, pero sin explicar qué espera ver ni cómo funciona | Ejecuta **sin apoyo**, dice qué espera, verifica en MySQL y explica con términos técnicos | Repetir la demo completa dos veces seguidas, con el `SELECT` escrito de memoria. Practicar el plan B (3.2.1 y sección 7) |
| **B2. Dominio del contenido** | No explica el modelo ni JDBC | Nombra tablas o clases pero sin relacionarlas | Explica modelo **o** flujo JDBC, con imprecisiones | Explica modelo, flujo `Connection → PreparedStatement → ResultSet` y capas con lenguaje técnico y ejemplos del proyecto | Releer `explicacion_sql.pdf` y la sección 2 de `guia_defensa.pdf`; explicar en voz alta en 60 segundos |
| **B3. Aportes** | No sabe qué hizo | Describe sus aportes sin detalle | Explica su código con dudas | Explica con seguridad **su** código y sabe qué hizo cada uno de los otros dos | Releer su `defensa_<nombre>.pdf` y la bitácora; completar fechas reales en `docs/bitacora.md` |
| **B4. Explicación de código** | No ubica el método ni la sentencia | Lo ubica pero no puede explicarlo | Lo ubica y explica la sentencia, olvida parámetros o cierre de recursos | Ubica en < 20 segundos y explica con las **4 frases** (SQL, `?`, `execute…`, recursos) | Ensayar la tabla de la sección 3 de `guia_defensa.pdf`; practicar con `explicacion_java.pdf` |
| **B5. Pregunta técnica al azar** | No responde | Responde de forma incorrecta o incompleta | Responde bien pero sin aplicarla al proyecto | Responde correctamente **y** la aplica al proyecto, y ante una trampa la detecta | Repasar las tarjetas falladas; usar el gancho mnemotécnico correspondiente (sección 8) |

**Hoja de puntaje del ensayo** (copiar y completar; máximo 30 por persona):

| Integrante | B1 | B2 | B3 | B4 | B5 | Total | Tarjetas falladas |
|---|---|---|---|---|---|---|---|
| Tais | | | | | | | |
| Rodolfo | | | | | | | |
| Nicolás | | | | | | | |

**Meta del martes:** 24/30 o más para cada uno. **Meta del jueves 3:** 30/30 en el simulacro.

### 6.2 Plan de refuerzo: miércoles 2 a viernes 4 de diciembre

El ensayo es el **martes 1 de diciembre**; el miércoles y el jueves se cierran los huecos que dejó y el viernes por la mañana solo se activa lo ya sabido. Se usan los PDF de `docs/`. Regla: llegar al viernes con las tarjetas falladas en cero.

| Día | Foco | Qué hacer | PDF de `docs/` |
|---|---|---|---|
| **Miércoles 2 dic** (60 min) | Corregir lo fallado y entender línea por línea | Repetir **solo las tarjetas falladas** y los ejercicios M que no salieron. Cada uno lee **su** `defensa_<nombre>.pdf` y luego el de los otros dos, y explica un método ajeno a otro compañero sin mirar | `explicacion_java.pdf`, `explicacion_sql.pdf`, los tres `defensa_*.pdf` |
| **Jueves 3 dic** (60 min) | Simulacro final y plan B | Simulacro de 25 minutos como el viernes, con otro orden y otras preguntas; si alguien saca menos de 24/30, una vuelta extra con sus puntos débiles. Demo completa cronometrada, **dos veces**; practicar V4 (apagar y levantar MySQL) y recargar la base. Autoevaluaciones de la sección 8. Completar la bitácora con fechas reales | `guia_defensa.pdf` (secciones 0, 1, 4 y 5), `bitacora.pdf` |
| **Viernes 4 dic, mañana** (20 min) | Repaso ligero | Checklist de la sección 7.2, los 5 ganchos en 1 minuto y una pasada por la tabla de la sección 3 de `guia_defensa.pdf`. **No aprender nada nuevo** | Ninguno nuevo |

---

## 7. Día de la defensa (viernes 4 de diciembre de 2026)

### 7.1 Qué llevar

- **Notebook cargado y cargador**, y que **cada uno** tenga el proyecto abierto y funcionando en el suyo.
- **Respaldo:** `CalendarioApp_Montecinos_Prieto_Cano.zip` y la presentación (`Presentacion_Evaluacion4_CalendarioApp.pptx`) en un pendrive y en la nube.
- Los **PDF** de `docs/` a mano para consultarlos **antes** de entrar; durante la defensa no se leen.
- Conexión a internet no es necesaria (todo corre local), pero llevar el celular como respaldo.
- Un **cable HDMI o adaptador** por si el proyector pide otro.

### 7.2 Antes de entrar (15 minutos antes)

1. `docker compose up -d` y `docker ps` (sección 2.1 y 2.2): tres contenedores `healthy`.
2. Recargar la base (2.5) y verificar `5` eventos y `3` usuarios (2.6).
3. Abrir NetBeans con el proyecto, F6 una vez para comprobar que abre el login, y dejarlo cerrado en *Login*.
4. Abrir phpMyAdmin con la consulta de verificación lista (sin ejecutarla).

### 7.3 Orden de la demo

Mismo orden de `docs/guia_defensa.md`, sección 1, y de las diapositivas 14 y 15:

1. **L1:** login correcto con `admin`.
2. **V1** y **V2:** vacío o clave mala, `ADMIN2026` y `inactivo1`.
3. **C1:** insertar un evento (con POINT y JSON) y verificarlo con `ST_X`/`ST_Y`.
4. **V3:** repetir el código.
5. **C3** y **C4:** buscar y actualizar.
6. **C5:** eliminar con confirmación.
7. **V4:** apagar MySQL (avisando) y volver a levantarlo.

Regla de oro: **operación, «espero ver…», `SELECT`**. En ese orden, siempre.

### 7.4 Si algo falla en vivo

| Qué pasa | Qué hacer (en voz alta, con calma) |
|---|---|
| **MySQL caído** («No fue posible conectar con la base de datos.» sin haberlo apagado) | Decir «MySQL no está arriba; lo levanto». En PowerShell: `docker start calendario-mysql` y esperar ~10 segundos hasta que `docker ps` diga `healthy`. Es justo el escenario V4: bien manejado, suma |
| **NetBeans lento o congelado** | No cerrar nada ni repetir clics. Esperar 20 segundos. Si sigue, pasar a **plan B**: mostrar el efecto con el `SELECT` en phpMyAdmin y explicar el método del DAO (B4) |
| **Base con datos raros** (más filas, códigos repetidos, un `EVT-0100` olvidado) | Recargar el script con el comando de la sección 2.5 (2 minutos) y explicar «recargo la base a su estado inicial para partir limpio» |
| **Error de compilación al abrir** | Mirar el primer error del panel *Output* y decirlo en voz alta; casi siempre es el *driver* no agregado a *Libraries* o MySQL caído |
| **Tests en rojo** | Antes de la defensa: MySQL arriba y base recargada (los tests de DAO la necesitan). Si aún así, ejecutar la clase fallida sola (clic derecho → *Test File*) |

**Cómo pedir repetir una pregunta** (es legítimo y se ve como seguridad, no como debilidad):

- «¿Me puede repetir la pregunta, por favor?»
- «Para estar seguro de que respondo lo que usted pregunta: ¿se refiere a… (decirla con sus palabras)?»

**Cómo razonar en voz alta cuando no sabe algo** (mejor que el silencio, y mejor que inventar):

1. **Reconocer el punto de partida:** «Esa parte no la tengo memorizada, pero lo razono.»
2. **Nombrar lo que sí se sabe:** «Sé que es una sentencia `SELECT`, así que usaría `executeQuery()` y me devuelve un `ResultSet`…»
3. **Apoyarse en el proyecto:** «En `EventoDAO.buscar()` se hace algo parecido: …»
4. **Cerrar con una hipótesis explícita:** «Entonces diría que… Si me equivoco, ¿me corrige?»
5. **Si el profesor da una pista, usarla**, no defenderse.

**Tres cosas que nunca se hacen:** inventar una respuesta con seguridad, culpar a otro compañero, o decir «eso lo hizo X». El viernes los tres responden por **todo** el proyecto.

---

## 8. Ganchos mnemotécnicos (5)

Cada gancho lleva al **término técnico exacto**. Primero se dice el gancho; después se responde la pregunta **sin mirar**. Son los cinco que más se olvidan o se confunden.

### 8.1 POINT: «**X** cruza de lado a lado, **Y** sube al cielo» (orden y lectura)

Imagen absurda: un mapa-mundi gigante sobre el piso de la sala; **X** cruza **de lado a lado** como una cinta y se llama **LON**gitud; **Y** **sube al cielo** como un cohete y se llama **LAT**itud. Orden alfabético inverso al que uno diría: **«X antes que Y, LON antes que LAT»**. Entonces `POINT(lon, lat)`, `ST_X` = lon, `ST_Y` = lat.

- *Autoevaluación:* En `POINT(-70.6506, -33.4378)`, ¿cuál es la latitud y cuál función SQL la lee? ¿Y qué `setDouble` pone la longitud en `cargarParametros`? (R: -33.4378, `ST_Y`; `ps.setDouble(4, e.getLongitud())`.)

### 8.2 Flujo JDBC: «**C**ada **P**izza **R**ica»

**C**onnection → **P**reparedStatement → **R**esultSet, en el orden en que se abren (línea, sentencia, resultados) y **al revés** al cerrarse (el último abierto se cierra primero).

- *Autoevaluación:* ¿Cuál de los tres no existe en un `INSERT` y qué devuelve en su lugar `executeUpdate()`? ¿Quién se cierra primero en `buscar()`? (R: el `ResultSet`; un `int` con las filas afectadas; el `ResultSet`.)

### 8.3 `execute`: «**Q**uiero ver — **U**sa cambios»

`executeQuery()` → **Q**uiero **ver** filas (`SELECT`, devuelve `ResultSet`). `executeUpdate()` → **U**sa **c**ambios (`INSERT`, `UPDATE`, `DELETE`, devuelve `int`).

- *Autoevaluación:* ¿Qué método usan `listar()` y `eliminar()`? ¿Cómo sabe `actualizar()` que cambió una sola fila? (R: `executeQuery()` y `executeUpdate()`; `executeUpdate() == 1`.)

### 8.4 Validaciones: «**V**acío, **I**nactivo, **R**epetido, **R**oto» (V1 a V4)

V1 **V**acío o incorrecto (login) · V2 **I**nactivo (`activo = FALSE`) · V3 **R**epetido (`UNIQUE`, error **1062**) · V4 **R**oto (conexión caída, `SQLState` que empieza en **08**).

- *Autoevaluación:* ¿Qué código de error dispara V3 y qué mensaje ve el usuario? ¿Se cierra la app en V4? (R: 1062, «El registro ya existe.»; no, `mensajeError` lo traduce y la ventana sigue.)

### 8.5 Restricciones: «**P**edro **U**sa **N**ombres **D**istintos y **C**orrectos» (P-U-N-D-C)

**P**K · **U**NIQUE · **N**OT NULL · **D**EFAULT · **C**HECK. Y un contraste para los dos que se confunden: PK = **U**na y **nunca nula**; UNIQUE = **v**arias y **no repetidas**.

- *Autoevaluación:* Nombre una columna de `evento` por cada letra y el error que da cada una al romperla. (R: `id_evento`; `codigo_evento` → 1062; `titulo` → 1364; `categoria` toma `'personal'`; `prioridad` → 4025 en MariaDB o 3819 en MySQL.)

### 8.6 Calendario de repaso de los ganchos

| Cuándo | Qué |
|---|---|
| Al terminar el ensayo | Decir los 5 ganchos de memoria, sin mirar |
| Día siguiente | Autoevaluación de cada gancho, en voz alta |
| Miércoles 2 dic | Autoevaluación al azar con tarjetas |
| Jueves 3 dic | Los 5 ganchos en 1 minuto, antes de dormir |
| Viernes 4 dic, camino a la defensa | Una pasada rápida, **sin aprender nada nuevo** |

---

*Documento de trabajo del equipo para el ensayo previo a la defensa del viernes 4 de diciembre de 2026. Complementa `docs/guia_defensa.md` y los `docs/defensa_<integrante>.md`; no los reemplaza.*
