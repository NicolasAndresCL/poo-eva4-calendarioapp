# CalendarioApp — agenda de eventos con login, CRUD en MySQL vía JDBC

[![CI](https://github.com/NicolasAndresCL/poo-eva4-calendarioapp/actions/workflows/ci.yml/badge.svg)](https://github.com/NicolasAndresCL/poo-eva4-calendarioapp/actions/workflows/ci.yml)
![Versión](https://img.shields.io/badge/versi%C3%B3n-1.0.0-blue)
![Java](https://img.shields.io/badge/java-21-ED8B00)
![JUnit](https://img.shields.io/badge/JUnit-4-25A162)
![MariaDB](https://img.shields.io/badge/mariadb-10.6-003545)

| | |
|---|---|
| **Versión** | 1.0.0 |
| **Fecha** | 27 de noviembre de 2026 |
| **Autores** | Tais Montecinos · Rodolfo Prieto · [Nicolás Andrés Cano Leal](#autor) |

**Evaluación 4 (20 %)** de Fundamentos de Programación Orientada a Objetos (IEI-085, Universidad Santo
Tomás), Unidad III: conexión a bases de datos con JDBC y MySQL. Defensa: **lunes 30 de noviembre de 2026**.

Aplicación de escritorio Swing donde solo usuarios autorizados administran una **agenda de eventos**.
Dos tablas independientes: `usuario` (login) y `evento` (CRUD), con una columna **`POINT`**
(ubicación, longitud/latitud) y una columna **`JSON`** (detalle libre), como pidió el profesor.

- **IDE**: NetBeans 31, proyecto *Java with Ant*, JDK 21.
- **Driver**: `mysql-connector-j 26.10.0` (en `CalendarioApp/lib/`, referencia relativa: funciona en cualquier PC).
- **Base**: MariaDB 10.6 (contenedor del `docker-compose.yml` del repo, o XAMPP); compatible con MySQL 8.0.16+.

## Arquitectura

```
poo-eva4-calendarioapp/
├── README.md                       # este archivo
├── CHANGELOG.md                    # historial de versiones (Keep a Changelog)
├── CONTRIBUTING.md                 # guía paso a paso para colaborar
├── .gitignore                      # excluye build/, dist/, nbproject/private/
├── docker-compose.yml              # MariaDB 10.6 (localhost:3306) + phpMyAdmin (localhost:8081)
├── .github/
│   ├── workflows/ci.yml            # CI: compila con Ant y corre las 24 pruebas contra MariaDB
│   ├── workflows/release.yml       # crea la GitHub Release al subir un tag vX.Y.Z
│   ├── dependabot.yml              # actualiza las acciones de GitHub cada semana
│   ├── pull_request_template.md    # plantilla de PR
│   └── ISSUE_TEMPLATE/             # plantillas bug.md y tarea.md
├── herramientas/                   # generador de ventanas y driver de prueba de la interfaz
├── sql/
│   └── calendario_bd.sql           # S1-S5: BD, tablas usuario y evento, datos de prueba y verificación
├── CalendarioApp/                  # proyecto NetBeans (File → Open Project)
│   ├── lib/mysql-connector-j-26.10.0.jar
│   ├── src/
│   │   ├── conexion/Conexion.java  # URL/usuario/clave en constantes; conectar(), probarConexion(), mensajeError()
│   │   ├── modelo/Usuario.java     # una fila de usuario
│   │   ├── modelo/Evento.java      # una fila de evento (POINT → latitud/longitud, JSON → texto)
│   │   ├── dao/UsuarioDAO.java     # L1 validar(), V2 estaInactivo()
│   │   ├── dao/EventoDAO.java      # C1 insertar, C2 listar, C3 buscar, C4 actualizar, C5 eliminar
│   │   └── vista/                  # VentanaLogin (main) y VentanaEventos (formulario + JTable), con .form
│   └── test/                       # JUnit 4: 24 pruebas (modelo, conexion, dao)
├── docs/                           # explicación línea a línea (SQL y Java), guía de defensa, bitácora
│                                   # y defensa_<integrante>.pdf: la parte de cada uno, línea por línea
├── presentacion/                   # PowerPoint de la defensa + gen_pptx.py que la genera
├── capturas/                       # pantallazos de la app (los toma el driver de interfaz)
└── generar_entrega.py              # arma entrega/CalendarioApp_Montecinos_Prieto_Cano.zip
```

**Flujo:** `vista → dao → Conexion.conectar() → PreparedStatement → ResultSet → modelo → vista`.
La vista nunca escribe SQL; los DAO nunca muestran ventanas.

## Levantar y probar

Requisitos: Git, NetBeans 31 con JDK 21 y Docker Desktop (o XAMPP). Terminal: **PowerShell** o Git Bash.
Para colaborar (ramas, PR), ver [CONTRIBUTING.md](CONTRIBUTING.md).

### 1. Clonar el repositorio

```powershell
git clone https://github.com/NicolasAndresCL/poo-eva4-calendarioapp.git
```

- Descarga el repositorio en una carpeta `poo-eva4-calendarioapp`. **Salida esperada**: `done.` al final.

```powershell
cd poo-eva4-calendarioapp
```

- Entra en la carpeta; los comandos siguientes se corren desde la raíz del repo.

### 2. Levantar la base de datos

```powershell
docker compose up -d
```

- Crea y arranca MariaDB 10.6 (`localhost:3306`, usuario `root` sin clave) y phpMyAdmin
  (http://localhost:8081). `-d` los deja en segundo plano.
- La **primera vez** carga sola `sql/calendario_bd.sql`; si el volumen ya existe, conserva lo que hubiera.
- **Salida esperada**: `Container ... Started`. Si dice `Cannot connect to the Docker daemon`, abre Docker
  Desktop; si dice `port is already allocated`, otro programa (p. ej. XAMPP) usa el 3306 o el 8081.

**Alternativa con XAMPP**: inicia MySQL y Apache, abre http://localhost/phpmyadmin → **Importar** y elige
`sql/calendario_bd.sql`.

Para **reiniciar los datos de prueba** (antes de una demo) sin tocar el volumen:

```powershell
Get-Content -Encoding UTF8 .\sql\calendario_bd.sql | docker compose exec -T mysql mariadb -u root -t
```

- `Get-Content -Encoding UTF8` lee el script respetando tildes; `|` se lo pasa al siguiente comando.
- `docker compose exec -T mysql mariadb -u root` corre el cliente SQL dentro del servicio `mysql`
  (nombre del servicio en `docker-compose.yml`; confírmalo con `docker compose ps`). `-T` desactiva la
  terminal interactiva, necesario para recibir el script por la tubería. `-t` muestra los `SELECT` como tablas.
- **Salida esperada**: el `DESCRIBE` de las dos tablas, los 3 usuarios, los 5 eventos, las coordenadas y
  las notas JSON.
- **Ojo**: el script hace `DROP TABLE` primero, así que **borra lo que hayas agregado** y deja los datos
  de prueba.

### 3. Abrir y ejecutar en NetBeans

**File → Open Project** → carpeta `CalendarioApp` dentro del repo clonado → **F6**.

| Usuario | Clave | Resultado esperado |
|---|---|---|
| `admin` | `Admin2026` | Entra (ADMIN) |
| `operador1` | `Oper2026` | Entra (OPERADOR) |
| `inactivo1` | `Inac2026` | «Usuario inactivo.» (V2) |
| cualquiera | clave mala o vacía | «Usuario o contraseña incorrectos.» (V1) |

### 4. Correr las pruebas

En NetBeans: **Alt+F6** (Test Project). **Salida esperada**: 24 pruebas, 0 fallos. Las pruebas de `dao`
necesitan la base arriba; crean eventos `TEST-JU-*` y los borran al terminar.

La interfaz se prueba aparte con `herramientas/DriverCalendario.java` (23 comprobaciones: V1, V2, login,
C1–C5, V3 y validaciones del formulario). Las ventanas las genera `herramientas/ventanas_calendario.py`;
ambos están en la carpeta `herramientas/` de este repo. **Regenerar las ventanas pisa lo que se edite en Design.**

## Cobertura de la pauta

| Código | Dónde |
|---|---|
| S1–S5 | `sql/calendario_bd.sql` |
| A4 / Tabla 4 | `conexion/Conexion.java` (constantes, `conectar()`, `probarConexion()`) |
| L1, V1, V2 | `dao/UsuarioDAO.java` + `vista/VentanaLogin.java` |
| C1–C5 | `dao/EventoDAO.java` + `vista/VentanaEventos.java` |
| V3 | UNIQUE de `codigo_evento` → error 1062 → «El registro ya existe.» |
| V4 | `Conexion.mensajeError()`: SQLState `08…`, 1045, 1049 → «No fue posible conectar con la base de datos.» |
| A5 | `try-with-resources` en todos los DAO; ninguna excepción cierra la app |

## Decisiones de diseño

- **`POINT` se escribe con `POINT(?, ?)` y se lee con `ST_X`/`ST_Y`.** Así viaja como dos `double`
  parametrizados (sin concatenar texto WKT) y el modelo Java queda con `latitud`/`longitud` simples.
  Convención: X = longitud, Y = latitud.
- **`JSON` como `String` en Java y validado por la base.** El `CHECK (JSON_VALID(detalle))` hace que MySQL
  rechace un JSON mal escrito; la app muestra el error sin caerse.
- **Una conexión por operación, cerrada con `try-with-resources`.** Simple, sin conexiones colgando; para
  una app de escritorio con un usuario a la vez es suficiente.
- **`mensajeError()` centraliza la traducción de `SQLException`.** Una sola regla para V3 y V4 en toda la app.
- **La clave distingue mayúsculas (`COLLATE utf8mb4_bin`).** La base usa `utf8mb4_spanish_ci`, que compara
  sin distinguir mayúsculas: `ADMIN2026` entraba como `Admin2026`. La collation binaria solo en `clave` lo
  corrige sin tocar el resto (lo cubre `testClaveDistingueMayusculas`).
- **JUnit 4 y no 5**: en un proyecto Ant de NetBeans, JUnit 5 compila pero no se ejecuta (`No tests executed.`).

## Integración continua

El workflow `.github/workflows/ci.yml` corre en cada push y Pull Request. Tiene un job, `ci`, con estos pasos:

1. **Levanta un servicio `mariadb:10.6`** junto al job y espera a que responda.
2. **Descarga el código** del repo (`actions/checkout`).
3. **Instala JDK 21 y Ant**, las herramientas con que NetBeans construye el proyecto.
4. **Carga `sql/calendario_bd.sql`** en la base del servicio, igual que en un equipo local.
5. **Compila `CalendarioApp` con Ant**: si algo no compila, el CI falla aquí.
6. **Corre las 24 pruebas JUnit 4** contra esa base (las de `dao` necesitan conexión real).

Los pasos 4 a 6 son `scripts/ci.sh`: el mismo script corre en local (Git Bash/WSL, con la base de
`docker compose` arriba, JDK 21 y Ant) con `bash scripts/ci.sh`, para ver el resultado antes de pushear.

`main` solo cambia por Pull Request con este job en verde. Al subir un tag `vX.Y.Z`, `release.yml` publica
la GitHub Release con las notas de la sección correspondiente de `CHANGELOG.md`.

## Equipo

| Integrante | GitHub |
|---|---|
| Tais Montecinos | colaboradora del repositorio |
| Rodolfo Prieto | colaborador del repositorio |
| Nicolás Andrés Cano Leal | [@NicolasAndresCL](https://github.com/NicolasAndresCL) (dueño) |

Para sumarte al trabajo (clonar, ramas, Pull Requests y CI), sigue [CONTRIBUTING.md](CONTRIBUTING.md).

## Autor

**Nicolás Andrés Cano Leal**\
Backend Engineer → SRE | Python · Django · Kubernetes · Terraform · CI/CD

- LinkedIn: [linkedin.com/in/nicolasandrescl](https://www.linkedin.com/in/nicolasandrescl/)
- Portafolio: [nicolasandrescl.github.io](https://nicolasandrescl.github.io/)
- Correo: [nicolas.cano.leal@gmail.com](mailto:nicolas.cano.leal@gmail.com)

CalendarioApp v1.0.0 · 27 de noviembre de 2026
