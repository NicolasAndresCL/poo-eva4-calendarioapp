# Cómo colaborar

Guía para que Tais, Rodolfo y Nicolás trabajen en el mismo repositorio sin pisarse. Cada comando va en su
propio bloque, con la terminal donde se corre y una explicación. Si algo sale distinto de lo esperado,
**detente** y pega la salida completa en el grupo antes de seguir.

## 1. Aceptar la invitación de colaborador

Nicolás te invita desde GitHub (*Settings → Collaborators*). Te llega un correo y una notificación.
Abre el enlace y pulsa **Accept invitation**. Sin esto no podrás hacer `push` al repositorio.

## 2. Instalar lo necesario

- **Git** (https://git-scm.com) y, opcional, **GitHub CLI** (`gh`).
- **Docker Desktop** (o **XAMPP** si prefieres no usar Docker).
- **NetBeans 31** con JDK 21.

## 3. Clonar el repositorio

PowerShell o Git Bash, en la carpeta donde guardas tus proyectos:

```bash
git clone https://github.com/NicolasAndresCL/poo-eva4-calendarioapp.git
```

- `git clone <url>` descarga el repositorio completo con su historial y crea la carpeta `poo-eva4-calendarioapp`.
- **Salida esperada**: `Cloning into 'poo-eva4-calendarioapp'...` y al final `done.`
- **Si sale distinto**: `Repository not found` o `403` significa que aún no aceptaste la invitación (paso 1)
  o no iniciaste sesión en GitHub desde Git.

```bash
cd poo-eva4-calendarioapp
```

- `cd` entra en la carpeta recién creada. Todos los comandos siguientes se corren desde aquí.
- **Salida esperada**: ninguna.

## 4. Levantar la base de datos

### Opción A: Docker (recomendada: trae NetBeans, la base y phpMyAdmin)

Si nunca usaste Docker, sigue primero [docs/guia_principiantes.md](docs/guia_principiantes.md)
(instalación de Docker Desktop y Git, paso a paso).

PowerShell o Git Bash, en la raíz del repo. **La primera vez** (construye la imagen de NetBeans):

```bash
docker compose up -d --build
```

- `docker compose up` crea y arranca los tres servicios de `docker-compose.yml`: NetBeans 31 en el
  navegador (http://localhost:3000), MariaDB 10.6 y phpMyAdmin (http://localhost:8081).
- `--build` construye la imagen de NetBeans: la primera vez descarga ~1,5–2 GB y tarda 10–20 minutos.
  Las siguientes veces se omite y arranca en segundos (`docker compose up -d`).
- `-d` (*detached*) los deja corriendo en segundo plano y te devuelve la terminal.
- La primera vez la base carga sola `sql/calendario_bd.sql`; las siguientes reutiliza los datos guardados.
- **Salida esperada**: líneas `Container calendario-... Started` (o `Running`).
- **Si sale distinto**: `Cannot connect to the Docker daemon` significa que Docker Desktop no está abierto.
  Si dice `port is already allocated` (3000, 3306 u 8081), otro programa (XAMPP, otro MySQL) usa ese
  puerto: ciérralo y repite.

```bash
docker compose ps
```

- Lista los servicios del proyecto y su estado.
- **Salida esperada**: la base con estado `running` (o `healthy`) y phpMyAdmin con `running`.
- **Si sale distinto**: mira los registros con `docker compose logs mysql` y pégalos en el grupo.

Para revisar los datos, abre http://localhost:8081 (phpMyAdmin) y busca la base `calendario_app`.

### Opción B: XAMPP

1. Abre el panel de XAMPP e inicia **MySQL** (y **Apache** para phpMyAdmin).
2. Abre http://localhost/phpmyadmin → pestaña **Importar**.
3. Elige el archivo `sql/calendario_bd.sql` del repo y pulsa **Importar**.

- **Salida esperada**: mensaje verde de importación exitosa y la base `calendario_app` en el panel izquierdo.
- El script hace `DROP TABLE` primero: **borra** lo que hubiera en esas tablas y deja los datos de prueba.

## 5. Abrir el proyecto en NetBeans

1. **Con Docker**: abre http://localhost:3000, menú **Applications → Development → Apache NetBeans**
   (el icono del escritorio puede no responder al doble clic). Luego **File → Open Project** →
   `/config/workspace/CalendarioApp`. **Con NetBeans instalado en Windows (XAMPP)**: File → Open Project
   y elige la carpeta `CalendarioApp` dentro del repo.
2. **F6** (Run Project): abre `VentanaLogin`. Entra con `admin` / `Admin2026`.
3. **Alt+F6** (Test Project): corre las pruebas JUnit 4.

- **Salida esperada**: 24 pruebas, 0 fallos. Las de `dao` necesitan la base arriba.
- **Si sale distinto**: `No fue posible conectar con la base de datos` significa que la base no está
  levantada (paso 4). Si un test falla, copia el mensaje del panel *Test Results*.

## 6. Flujo de trabajo (GitHub Flow)

Nunca se trabaja directamente en `main`. `main` solo cambia por **Pull Request con el CI en verde**.

Terminal en la raíz del repo:

```bash
git switch main
```

- Cambia a la rama `main`. **Salida esperada**: `Switched to branch 'main'` o `Already on 'main'`.

```bash
git pull --ff-only
```

- Trae lo último del remoto. `--ff-only` (*fast-forward only*) solo avanza si no hay divergencia; si la hay,
  falla en vez de crear un *merge* sorpresa.
- **Salida esperada**: `Already up to date.` o la lista de archivos actualizados.
- **Si sale distinto**: `Not possible to fast-forward` significa que tienes commits locales en `main`.
  No sigas: avisa en el grupo.

```bash
git switch -c feat/descripcion-corta
```

- `switch` cambia de rama y `-c` (*create*) la crea. El prefijo indica el tipo (`feat/`, `fix/`, `docs/`,
  `test/`, `chore/`); la descripción va en minúsculas con guiones, por ejemplo `fix/validar-latitud`.
- **Salida esperada**: `Switched to a new branch 'feat/descripcion-corta'`.

Trabaja y prueba (F6 y Alt+F6). Luego revisa qué cambió:

```bash
git status
```

- Muestra archivos modificados y sin seguimiento. **Revisa que no aparezcan** `build/`, `dist/` ni
  `nbproject/private/` (ver sección 7).

```bash
git add CalendarioApp/src/dao/EventoDAO.java
```

- `git add <archivo>` prepara ese archivo para el commit. Agrega los archivos de a uno (o por carpeta), no
  `git add .`, para no colar residuos. Repite el comando por cada archivo.
- **Salida esperada**: ninguna.

```bash
git commit -m "feat: validar latitud entre -90 y 90 al guardar evento"
```

- `commit` guarda lo preparado; `-m` es el mensaje. Usamos **Conventional Commits en español**:
  `tipo: descripción` con tipos `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `ci`.
- **Salida esperada**: `[feat/descripcion-corta abc1234] feat: ...` y el conteo de archivos.

```bash
git push -u origin feat/descripcion-corta
```

- `push` sube la rama a GitHub; `origin` es el remoto; `-u` (*set upstream*) enlaza la rama local con la
  remota, así después basta `git push`.
- **Salida esperada**: `new branch ... -> feat/descripcion-corta` y un enlace para abrir el PR.
- **Si sale distinto**: `Permission denied`/`403` indica invitación sin aceptar; `rejected` (non-fast-forward)
  indica que la rama remota tiene cambios que no tienes: avisa antes de forzar nada.

Abre el Pull Request, con la CLI:

```bash
gh pr create --title "feat: validar latitud al guardar evento" --body "Qué cambia, por qué y cómo se probó."
```

- `gh pr create` abre el PR contra `main`; `--title` usa el mismo formato Conventional Commit y `--body` es
  la descripción. También puedes abrirlo desde la web con el enlace que imprime `git push`; allí se carga
  la plantilla `.github/pull_request_template.md`, con su checklist.
- **Salida esperada**: la URL del PR.

```bash
gh pr checks --watch
```

- Muestra los chequeos del CI y espera a que terminen. **Salida esperada**: el job `ci` en `pass`.
- **Si sale distinto**: si `ci` queda en `fail`, abre el enlace, lee el paso que falló, corrígelo, haz un
  commit nuevo y `git push`: el PR se actualiza solo.

Con el CI en verde y la revisión de un compañero, se hace **Squash and merge** (botón en la web, o
`gh pr merge --squash --delete-branch`). *Squash* junta todos los commits de la rama en uno solo en `main`
con el título del PR.

Después, limpia:

```bash
git switch main
```

```bash
git pull --ff-only
```

```bash
git branch -d feat/descripcion-corta
```

- `-d` borra la rama local ya fusionada; si Git dice `not fully merged`, no uses `-D`: revisa que el PR
  realmente se fusionó.

## 7. Qué no se commitea

| Carpeta | Por qué |
|---|---|
| `CalendarioApp/build/` | La regenera Ant al compilar |
| `CalendarioApp/dist/` | El `.jar` generado |
| `CalendarioApp/nbproject/private/` | Configuración personal de cada NetBeans (rutas de tu PC) |

Están en `.gitignore`. Tampoco se suben claves reales ni capturas con datos personales.

## 8. Reglas del código

- Toda sentencia SQL usa **`PreparedStatement`**: nunca concatenar texto del usuario en el SQL.
- La vista no escribe SQL; los DAO no muestran ventanas.
- Si editas una ventana en Design, no regeneres con `herramientas/ventanas_calendario.py` (pisa tus cambios).
- Cambió algo visible: actualiza `README.md` y `CHANGELOG.md` (sección `[Sin publicar]`).
