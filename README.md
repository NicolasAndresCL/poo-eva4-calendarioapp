# CalendarioApp — agenda de eventos con login, CRUD en MySQL vía JDBC

[![CI](https://github.com/NicolasAndresCL/poo-eva4-calendarioapp/actions/workflows/ci.yml/badge.svg)](https://github.com/NicolasAndresCL/poo-eva4-calendarioapp/actions/workflows/ci.yml)
![Versión](https://img.shields.io/badge/versi%C3%B3n-1.1.0-blue)
![Java](https://img.shields.io/badge/java-21-ED8B00)
![JUnit](https://img.shields.io/badge/JUnit-4-25A162)
![MariaDB](https://img.shields.io/badge/mariadb-10.6-003545)

| | |
|---|---|
| **Versión** | 1.1.0 |
| **Fecha** | 27 de noviembre de 2026 |
| **Autores** | Tais Montecinos · Rodolfo Prieto · [Nicolás Andrés Cano Leal](#autor) |

**Evaluación 4 (20 %)** de Fundamentos de Programación Orientada a Objetos (IEI-085, Universidad Santo
Tomás), Unidad III: conexión a bases de datos con JDBC y MySQL. Defensa: **viernes 4 de diciembre de 2026** (ensayo del equipo: martes 1 de diciembre).

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
├── Dockerfile                      # imagen: escritorio Ubuntu en el navegador + NetBeans 31 + JDK 21
├── .dockerignore                   # el build solo recibe docker/ (lista blanca)
├── docker-compose.yml              # netbeans (localhost:3000) + MariaDB 10.6 + phpMyAdmin (localhost:8081)
├── docker/                         # menú de NetBeans, hook de arranque y config de phpMyAdmin
├── .github/
│   ├── workflows/ci.yml            # CI: pruebas contra MariaDB + build de la imagen de NetBeans
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
├── docs/                           # guía de principiantes (Git, GitHub, Docker), explicación línea a
│                                   # línea (SQL y Java), guía de defensa, defensa_<integrante>.pdf,
│                                   # plan de ensayo del martes y bitácora
├── presentacion/                   # PowerPoint de la defensa + gen_pptx.py que la genera
├── capturas/                       # pantallazos de la app (los toma el driver de interfaz)
└── generar_entrega.py              # arma entrega/CalendarioApp_Montecinos_Prieto_Cano.zip
```

**Flujo:** `vista → dao → Conexion.conectar() → PreparedStatement → ResultSet → modelo → vista`.
La vista nunca escribe SQL; los DAO nunca muestran ventanas.

## Levantar y probar

Si es tu primera vez con Git, GitHub o Docker, empieza por la guía paso a paso:
[docs/guia_principiantes.md](docs/guia_principiantes.md). Para colaborar (ramas, PR), ver
[CONTRIBUTING.md](CONTRIBUTING.md).

### Requisitos

- Windows 10/11 con **Git** y **Docker Desktop** (con WSL 2). Terminal: **PowerShell** (o Git Bash).
- 8 GB de RAM (ideal) y ~10 GB libres. No hace falta instalar NetBeans, JDK ni MySQL: vienen en el entorno Docker.

### Primera vez

```powershell
git clone https://github.com/NicolasAndresCL/poo-eva4-calendarioapp.git
```

- Descarga el repositorio en una carpeta `poo-eva4-calendarioapp`. **Salida esperada**: `done.` al final.

```powershell
cd poo-eva4-calendarioapp
```

- Entra en la carpeta; los comandos siguientes se corren desde la raíz del repo.

```powershell
docker compose up -d --build
```

- `up` crea y prende los tres servicios; `-d` (*detached*) los deja en segundo plano; `--build` construye
  antes la imagen del `Dockerfile` (escritorio Ubuntu + NetBeans 31 + JDK 21).
- **La primera vez descarga ~1,5 a 2 GB y tarda 10 a 20 minutos**; después arranca en segundos.
- **Salida esperada**: tres líneas `Started` (`calendario-netbeans`, `calendario-mysql`, `calendario-phpmyadmin`).
- **Si sale distinto**: `Cannot connect to the Docker daemon` = abre Docker Desktop; `port is already
  allocated` = otro programa (p. ej. XAMPP) usa el 3000, 3306 u 8081: apágalo y repite.

Luego, en el navegador:

1. Abre **http://localhost:3000** (si ves pantalla negra o «Waiting for stream», espera y recarga).
2. Abre NetBeans por **Applications → Development → Apache NetBeans** (el icono del escritorio puede no
   responder al doble clic: XFCE exige marcarlo «de confianza»).
3. **File → Open Project** → `/config/workspace/CalendarioApp` → **F6** para ejecutar.
4. **Alt+F6** corre las pruebas (24, 0 fallos). Si NetBeans pide instalar o activar JUnit, acepta y repite.

| Usuario | Clave | Resultado esperado |
|---|---|---|
| `admin` | `Admin2026` | Entra (ADMIN) |
| `operador1` | `Oper2026` | Entra (OPERADOR) |
| `inactivo1` | `Inac2026` | «Usuario inactivo.» (V2) |
| cualquiera | clave mala o vacía | «Usuario o contraseña incorrectos.» (V1) |

### Servicios

| Servicio | URL | Para qué |
|---|---|---|
| NetBeans (escritorio en el navegador) | http://localhost:3000 | Editar, ejecutar y probar el proyecto |
| phpMyAdmin | http://localhost:8081 | Mirar y consultar la base (entra directo) |
| MariaDB 10.6 | `localhost:3306` (solo dentro del entorno) | La usa el programa Java; `root` sin clave |

El repo se monta en `/config/workspace` dentro del contenedor: lo que editas en NetBeans queda en tu carpeta de
Windows. Los datos viven en los volúmenes `netbeans_config` y `mysql_data`.

### Día a día

```powershell
git pull
```

- Trae lo último que subió el equipo. **Salida esperada**: `Already up to date.` o la lista de archivos
  cambiados. Si dice que hay cambios locales que se sobrescribirían, no fuerces: `git stash` los aparta
  (`git stash pop` los devuelve) o pregunta al grupo.

```powershell
docker compose up -d
```

- Prende los servicios (sin `--build`: la imagen ya existe). Arranca en segundos y la base conserva sus datos.

### Apagar

```powershell
docker compose down
```

- Apaga y elimina los contenedores, pero **conserva los volúmenes** (base de datos y configuración de NetBeans).
- **Nunca** uses `docker compose down -v`: `-v` borra los volúmenes y perderías la base y la configuración.

### Recargar la base de datos

Para **reiniciar los datos de prueba** (antes de una demo), desde la raíz del repo, en PowerShell:

```powershell
Get-Content -Encoding UTF8 .\sql\calendario_bd.sql | docker exec -i calendario-mysql mariadb -u root -t
```

- `Get-Content -Encoding UTF8` lee el script respetando tildes; `|` se lo pasa al siguiente comando.
- `docker exec -i calendario-mysql` ejecuta un comando dentro del contenedor ya corriendo; `-i` deja abierta
  la entrada para recibir el script. `mariadb -u root` es el cliente SQL como `root`; `-t` muestra los
  `SELECT` como tablas.
- **Salida esperada**: la estructura de las tablas, los 3 usuarios, los 5 eventos, las coordenadas y las notas JSON.
- **Ojo**: el script hace `DROP TABLE` primero, así que **borra lo que hayas agregado**.

Para diagnosticar:

```powershell
docker ps
```

- Lista los contenedores corriendo; deben aparecer los tres `calendario-*` con `Up`.

```powershell
docker logs calendario-mysql --tail 30
```

- Muestra las últimas 30 líneas (`--tail 30`) del registro de la base; al final debe decir `ready for connections`.

### Alternativa: NetBeans instalado en Windows (con XAMPP)

Si ya tienes NetBeans 31 con JDK 21 en tu PC y prefieres no usar el escritorio Docker: apaga cualquier
servicio que use el 3306, inicia **MySQL** y **Apache** en XAMPP, abre http://localhost/phpmyadmin →
**Importar** y elige `sql/calendario_bd.sql`. Después, en NetBeans de Windows: **File → Open Project** →
carpeta `CalendarioApp` del repo clonado → **F6**. No mezcles ambos caminos a la vez: XAMPP y el entorno
Docker usan el mismo puerto 3306 (y 8081/3000 en el caso de Docker).

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

CalendarioApp v1.1.0 · 27 de noviembre de 2026
