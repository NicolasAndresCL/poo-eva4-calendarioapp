# Guía de inicio: Git, GitHub, Docker y NetBeans

**Para:** Tais Montecinos y Rodolfo Prieto
**Tiempo:** unos 12 minutos de lectura (más la espera de la primera construcción, 10 a 20 minutos)
**Proyecto:** CalendarioApp, Evaluación 4 de POO

---

## Antes de empezar

Git, GitHub y Docker se ven recién en 4.º semestre. Se los mostramos ahora porque así el proyecto
funciona **igual en los tres computadores**: nadie instala NetBeans, Java ni MySQL a mano, y nadie dice
«en mi PC no corre». No tienen que dominarlo: basta con seguir esta guía, un comando a la vez.

Cada comando indica **en qué terminal** se escribe, **qué hace**, **qué significa cada pieza**, **qué
verán** y **qué hacer si sale distinto**. Si algo no calza, no improvisen: copien el mensaje de error y
pregunten en el grupo.

**Requisitos del equipo:** Windows 10 u 11, 8 GB de RAM (ideal) y unos 10 GB libres en disco.

---

## 1. Conceptos en 1 minuto cada uno

### Git y GitHub

- **Git**: programa que guarda el **historial de versiones** de una carpeta. Es como «guardar partida»
  en un videojuego: puedes volver a cualquier punto anterior y ver quién cambió qué.
- **GitHub**: página web donde vive una copia del proyecto en la **nube**. Git es el programa; GitHub es
  el lugar donde se comparte.
- **Repositorio (repo)**: la carpeta del proyecto **más** todo su historial. Este se llama
  `poo-eva4-calendarioapp`.
- **Commit**: una «partida guardada» con un mensaje que dice qué cambió. Ejemplo: `fix: validar latitud`.
- **Rama (branch)**: una copia paralela donde pruebas cambios sin tocar lo bueno. La rama principal se
  llama `main` y **nadie escribe directo en ella**.
- **Clonar (`clone`)**: bajar el repo completo de GitHub a tu computador, una sola vez.
- **`pull`**: traer lo último que otros subieron. **`push`**: subir tus commits a GitHub.
- **Pull Request (PR)**: una solicitud en GitHub que dice «mi rama tiene cambios, revísenlos y
  súmenlos a `main`». Ahí corre el CI (las pruebas automáticas) y alguien lo revisa antes de aceptarlo.

### Docker

- **Docker**: programa que corre aplicaciones dentro de «cajas» que ya traen todo instalado.
- **Imagen**: la **receta** (NetBeans 31 + JDK 21 + un escritorio Linux). Se construye una vez.
- **Contenedor**: el **plato** hecho con esa receta: la caja corriendo. Se puede apagar y volver a prender.
- **Volumen**: un «disco» que **sobrevive** aunque borres el contenedor. Aquí guarda la base de datos y la
  configuración de NetBeans.
- **Docker Compose**: levanta **varias cajas juntas** con un solo comando, leyendo `docker-compose.yml`.
  Aquí son tres: NetBeans, la base de datos (MariaDB) y phpMyAdmin.
- **Puerto**: la «puerta de entrada» de cada servicio. `localhost:3000` significa «este computador,
  puerta 3000». Dos programas no pueden usar la misma puerta a la vez.

### Cómo encaja todo

```
Tu Windows
 ├─ carpeta del repo  ←──────────────┐ (es la misma carpeta, no una copia)
 └─ Docker Desktop                   │
     ├─ calendario-netbeans   escritorio con NetBeans  →  http://localhost:3000
     ├─ calendario-mysql      base de datos            →  localhost:3306 (solo para el programa)
     └─ calendario-phpmyadmin administrador de la base →  http://localhost:8081
```

Editas el código dentro de NetBeans (que está en el navegador) y los cambios quedan en tu carpeta de
Windows. Por eso Git, que corre en Windows, los ve.

---

## 2. Instalación en Windows 10/11

Se hace **una sola vez**. Total: 3 cosas.

### 2.1 Git for Windows

1. Entra a https://git-scm.com/download/win y descarga el instalador.
2. Ábrelo y presiona **Next** en todo: las opciones por defecto sirven. Al final se instala también
   **Git Bash**, una terminal.

Para verificar, abre **PowerShell** (menú Inicio, escribe «PowerShell») y escribe:

```powershell
git --version
```

- **Qué hace:** pregunta a Git qué versión tiene. `--version` es una bandera que significa «solo dime tu
  versión».
- **Salida esperada:** algo como `git version 2.47.0.windows.1`.
- **Si sale distinto:** `git no se reconoce como un comando` significa que PowerShell se abrió antes de
  instalar. Ciérralo, ábrelo de nuevo y repite. Si sigue igual, reinstala Git.

### 2.2 Docker Desktop (con WSL 2)

Docker en Windows usa **WSL 2** (un Linux liviano integrado en Windows). Primero se instala eso.

Abre **PowerShell como administrador** (clic derecho sobre PowerShell → *Ejecutar como administrador*):

```powershell
wsl --install
```

- **Qué hace:** instala WSL 2 y una distribución Ubuntu. Necesita permisos de administrador por eso.
- **Salida esperada:** barras de progreso y, al final, un mensaje que pide **reiniciar**. Reinicia el PC.
- **Si sale distinto:** si dice que WSL ya está instalado, no pasa nada: sigue al paso siguiente. Si dice
  que falta la virtualización, hay que activarla en la BIOS (busca «habilitar virtualización» + la marca
  de tu PC) y pedir ayuda en el grupo.

Luego:

1. Entra a https://www.docker.com/products/docker-desktop/ y descarga **Docker Desktop for Windows**.
2. Instálalo con las opciones por defecto (deja marcado «Use WSL 2»). Ábrelo y acepta los términos.
3. Espera a que abajo a la izquierda el icono de la ballena diga **Engine running** (en verde).

Verifica en PowerShell (esta vez normal, no administrador):

```powershell
docker --version
```

- **Qué hace:** muestra la versión de Docker instalada.
- **Salida esperada:** `Docker version 27.x.x, build ...` (el número exacto puede variar).
- **Si sale distinto:** si no se reconoce, cierra y abre PowerShell. Si sigue, reinicia el PC.

```powershell
docker run hello-world
```

- **Qué hace:** descarga una imagen diminuta de prueba y crea un contenedor que imprime un saludo.
  `run` = crear y ejecutar un contenedor; `hello-world` = nombre de la imagen.
- **Salida esperada:** `Hello from Docker! This message shows that your installation appears to be
  working correctly.`
- **Si sale distinto:** `error during connect` o `cannot connect to the Docker daemon` significa que
  Docker Desktop no está abierto o no terminó de iniciar. Ábrelo, espera el «Engine running» y repite.

### 2.3 Cuenta de GitHub y la invitación

1. Crea tu cuenta gratis en https://github.com/signup (si ya tienes, úsala).
2. Dile a Nicolás tu **nombre de usuario** de GitHub para que te agregue como colaborador.
3. Te llegará una invitación por **correo**, o la ves en https://github.com/notifications. Ábrela y
   presiona **Accept invitation**.

El repo es **público**: pueden descargarlo (clonarlo) aunque aún no hayan aceptado. La invitación solo es
necesaria para **subir** cambios.

---

## 3. Primera vez: dejar todo funcionando

Todos los comandos de esta sección son para **PowerShell** (también valen en Git Bash). Antes de
empezar, asegúrate de que **Docker Desktop esté abierto** y diga «Engine running».

### 3.1 Descargar el proyecto

Primero elige dónde quieres la carpeta. Por ejemplo, entra a tu carpeta de usuario:

```powershell
cd $HOME
```

- **Qué hace:** `cd` (*change directory*) cambia de carpeta. `$HOME` es una variable de PowerShell que
  vale tu carpeta de usuario (`C:\Users\TuNombre`).
- **Salida esperada:** ninguna; el prompt ahora muestra esa carpeta.

```powershell
git clone https://github.com/NicolasAndresCL/poo-eva4-calendarioapp.git
```

- **Qué hace:** `clone` baja el repo completo (código e historial) desde GitHub a una carpeta nueva.
  Lo que va después es la dirección del repo.
- **Salida esperada:** líneas `Cloning into 'poo-eva4-calendarioapp'...` y al final `done.`
- **Si sale distinto:** `fatal: unable to access` es un problema de internet o de la dirección
  (cópiala de nuevo, sin espacios). `already exists` significa que ya clonaste antes: salta al paso
  siguiente.

```powershell
cd poo-eva4-calendarioapp
```

- **Qué hace:** entra a la carpeta del repo. **Todos los comandos siguientes se corren desde aquí.**
- **Salida esperada:** el prompt cambia a `...\poo-eva4-calendarioapp>`.

### 3.2 Construir y levantar el entorno

```powershell
docker compose up -d --build
```

- **Qué hace:** lee `docker-compose.yml` y levanta los tres servicios.
  - `docker compose`: la herramienta que maneja varias cajas juntas.
  - `up`: crea (si hace falta) y prende los contenedores.
  - `-d` (*detached*): los deja corriendo **en segundo plano**; sin él, la terminal quedaría ocupada
    mostrando registros y al cerrarla se apagaría todo.
  - `--build`: construye primero la imagen según el `Dockerfile`. Solo es necesario la **primera vez**
    (o cuando alguien cambie el `Dockerfile`).
- **Cuánto tarda:** la primera vez descarga unos **1,5 a 2 GB** (escritorio Linux, NetBeans 31, JDK 21)
  y demora **10 a 20 minutos** según tu internet. Es normal que parezca detenido: déjalo trabajar. Las
  siguientes veces arranca en segundos.
- **Salida esperada:** al final, tres líneas parecidas a `Container calendario-netbeans  Started`,
  `calendario-mysql  Started` y `calendario-phpmyadmin  Started`.
- **Si sale distinto:**
  - `Cannot connect to the Docker daemon` o `error during connect`: Docker Desktop no está abierto.
  - `port is already allocated`: otro programa usa el 3000, 3306 u 8081 (ver sección 6).
  - Se corta a mitad de la descarga: vuelve a correr el mismo comando; retoma lo ya descargado.

```powershell
docker ps
```

- **Qué hace:** lista los contenedores que están corriendo. `ps` viene de *process status*.
- **Salida esperada:** tres filas: `calendario-netbeans`, `calendario-mysql` y `calendario-phpmyadmin`. En
  la columna STATUS debe decir `Up ...`. NetBeans puede mostrar `(health: starting)` durante un minuto y
  luego `(healthy)`.
- **Si sale distinto:** si falta alguno, mira sus registros con
  `docker logs calendario-mysql --tail 30` (ver sección 6).

### 3.3 Abrir el escritorio con NetBeans

1. Abre el navegador (Chrome o Edge) y entra a **http://localhost:3000**.
2. Verás un escritorio Linux dentro de la pestaña. Si sale pantalla negra o «Waiting for stream», espera
   30 segundos y recarga con F5.
3. Abre NetBeans por el **menú de aplicaciones** (arriba a la izquierda): **Applications → Development →
   Apache NetBeans**. *No uses el icono del escritorio*: Linux pide marcarlo «de confianza» y, si no se
   hace, el doble clic no hace nada (no es un error suyo).
4. NetBeans tarda 30 a 60 segundos en abrir la primera vez. Si pregunta por actualizaciones o
   estadísticas, puedes cerrar el aviso.

### 3.4 Abrir el proyecto y ejecutarlo

1. En NetBeans: **File → Open Project**.
2. Escribe en la ruta `/config/workspace/CalendarioApp` y presiona **Open Project**. Esa es la carpeta
   `CalendarioApp` del repo, vista desde dentro de la caja.
3. Presiona **F6** (*Run Project*). Aparece la ventana de login de la aplicación.
4. Entra con usuario `admin` y clave `Admin2026`.

Para correr las pruebas: **Alt+F6** (*Test Project*). **Salida esperada:** 24 pruebas, 0 fallos. Si
NetBeans pide **instalar o activar JUnit** la primera vez, acepta (incluye la licencia) y vuelve a pulsar
Alt+F6.

Con eso ya tienen el programa corriendo. También pueden mirar la base en http://localhost:8081
(phpMyAdmin entra directo, sin clave).

---

## 4. Día a día

### 4.1 Al comenzar

```powershell
cd $HOME\poo-eva4-calendarioapp
```

- **Qué hace:** vuelve a la carpeta del repo (ajusta la ruta si la clonaste en otro lado).

```powershell
git pull
```

- **Qué hace:** trae desde GitHub lo último que subió Nicolás u otra persona y lo une con tu carpeta.
  Hazlo **siempre antes de empezar a trabajar**, para no partir con una versión vieja.
- **Salida esperada:** `Already up to date.` (no había nada nuevo) o una lista de archivos que cambiaron.
- **Si sale distinto:**
  - `Your local changes to the following files would be overwritten by merge`: tienes cambios sin guardar
    en un archivo que también cambió en GitHub. **No fuerces nada.** Pregunta en el grupo. Si quieres
    apartar tus cambios un momento, corre `git stash` (los guarda en un cajón, deja la carpeta limpia),
    luego `git pull`, y después `git stash pop` (los saca del cajón). Si al sacarlos aparece un
    `CONFLICT`, detente y pide ayuda.
  - `fatal: not a git repository`: no estás dentro de la carpeta del repo; revisa el `cd`.

```powershell
docker compose up -d
```

- **Qué hace:** prende los tres servicios. Sin `--build` porque la imagen ya existe: arranca en segundos
  y la base conserva sus datos.
- **Salida esperada:** tres líneas `Started` (o `Running` si ya estaban prendidos).

Luego entra a http://localhost:3000, abre NetBeans (menú Applications) y trabaja. Si NetBeans recuerda
el proyecto, lo verás ya abierto.

### 4.2 Al terminar

```powershell
docker compose down
```

- **Qué hace:** apaga y elimina los **contenedores** (las cajas). Los **volúmenes** (la base de datos y
  la configuración de NetBeans) **se conservan**, así que mañana todo sigue donde lo dejaste.
- **Salida esperada:** `Container ... Removed` por cada servicio.
- **Importante:** **nunca** escribas `docker compose down -v`. La bandera `-v` borra también los
  volúmenes: perderían la base de datos y la configuración de NetBeans.

Apagar al terminar libera RAM: si lo dejas corriendo, el PC se pondrá lento.

---

## 5. Si quieren aportar cambios (resumen)

Nunca se escribe directo en `main`: se trabaja en una **rama**, se sube y se abre un **Pull Request**. El
detalle completo, con cada comando explicado, está en [CONTRIBUTING.md](../CONTRIBUTING.md). El resumen:

1. **Rama nueva** desde un `main` actualizado: `git switch main`, `git pull --ff-only` y
   `git switch -c fix/descripcion-corta` (el prefijo es el tipo: `feat/`, `fix/`, `docs/`, `test/`).
2. **Trabajar y probar** en NetBeans (F6 y Alt+F6).
3. **Guardar en un commit:** `git status` para ver qué cambió, `git add <archivo>` y
   `git commit -m "fix: descripción corta"`.
4. **Subir la rama:** `git push -u origin <nombre-de-la-rama>`.
5. **Abrir el Pull Request** desde GitHub (aparece un botón verde **Compare & pull request**), esperar
   que el CI quede en verde y que alguien lo revise.

---

## 6. Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| Docker Desktop no arranca, o dice «WSL 2 installation is incomplete» | WSL 2 no está instalado o falta reiniciar | PowerShell **como administrador**: `wsl --install`, reiniciar el PC, abrir Docker Desktop de nuevo. Si pide virtualización, activarla en la BIOS |
| `port is already allocated` o `address already in use` | Otro programa usa el puerto 3000, 3306 u 8081 (típico: XAMPP o un MySQL instalado) | Apaga ese programa (en XAMPP, **Stop** en MySQL y Apache; o detén el servicio MySQL de Windows), luego `docker compose up -d` |
| `localhost:3000` muestra pantalla negra o «Waiting for stream» | El escritorio aún está iniciando | Espera 30 a 60 segundos y recarga con F5. Si sigue, `docker ps` y verifica que `calendario-netbeans` esté `Up` |
| `localhost:3000` no carga nada | Los contenedores están apagados | `docker compose up -d` y revisa con `docker ps` |
| NetBeans no abre al hacer doble clic en el icono del escritorio | Linux exige marcar el icono como «de confianza» | Abre NetBeans por **Applications → Development → Apache NetBeans** |
| Alt+F6 no corre o NetBeans pide instalar JUnit | Primera vez: falta activar el módulo de pruebas | Acepta la instalación (incluye la licencia), espera y vuelve a pulsar Alt+F6 |
| «No fue posible conectar con la base de datos» al ejecutar | El contenedor de la base está apagado o cayó | `docker ps`. Si falta `calendario-mysql`, corre `docker start calendario-mysql`; si no basta, mira `docker logs calendario-mysql --tail 30` |
| La base quedó con datos raros o sin los datos de prueba | Alguien borró o editó eventos y usuarios | Recarga el script SQL (ver más abajo). **Borra lo que hayas agregado** |
| `git pull` dice que hay cambios locales o un `CONFLICT` | Tocaste un archivo que también cambió en GitHub | No fuerces. `git status` para ver cuáles, y pregunta en el grupo. Para apartar tus cambios un rato: `git stash`, `git pull`, `git stash pop` |
| El PC se pone lento | Los contenedores siguen corriendo en segundo plano | `docker compose down` al terminar de trabajar; cierra también Docker Desktop si no lo usas |

Los dos comandos de diagnóstico que más se usan:

```powershell
docker ps
```

- **Qué hace:** lista los contenedores corriendo. Si `calendario-mysql` no aparece, está apagado.
- **Salida esperada:** una fila por servicio, con STATUS `Up ...`.

```powershell
docker logs calendario-mysql --tail 30
```

- **Qué hace:** muestra el registro (lo que el programa «va diciendo») del contenedor `calendario-mysql`.
  `--tail 30` limita la salida a las **últimas 30 líneas**; sin él saldría todo el historial.
- **Salida esperada:** al final, una línea como `ready for connections`. Eso indica que la base está lista.
- **Si sale distinto:** si ves `ERROR`, copia esas líneas y pásalas al grupo.

Y el que sirve cuando la base quedó con datos raros. Se corre desde la raíz del repo, en **PowerShell**:

```powershell
Get-Content -Encoding UTF8 .\sql\calendario_bd.sql | docker exec -i calendario-mysql mariadb -u root -t
```

- **Qué hace:** toma el script SQL y se lo entrega al cliente de la base **dentro** del contenedor, que lo
  ejecuta y deja los datos de prueba originales. Pieza por pieza:
  - `Get-Content`: lee el archivo.
  - `-Encoding UTF8`: lo lee respetando las tildes y la ñ (sin esto saldrían rotas).
  - `|` (tubería): lo que sale del comando de la izquierda entra como entrada al de la derecha.
  - `docker exec`: ejecuta un comando dentro de un contenedor que ya corre.
  - `-i` (*interactive*): deja abierta la entrada para recibir el texto de la tubería; sin él, el script no llega.
  - `calendario-mysql`: el contenedor donde se ejecuta.
  - `mariadb -u root`: el cliente SQL, entrando como usuario `root` (sin clave en este entorno).
  - `-t`: muestra los resultados de los `SELECT` como tablas legibles.
- **Salida esperada:** tablas con la estructura, los 3 usuarios, los 5 eventos, las coordenadas y las notas JSON.
- **Si sale distinto:** `No such container` significa que la base está apagada (`docker compose up -d`).
  Si las tildes se ven mal, estás en Git Bash o en una consola con otra codificación: usa **PowerShell**.
- **Ojo:** el script comienza con `DROP TABLE`: **borra lo que hayas agregado** y deja los datos de prueba.

---

## 7. Glosario

| Término | Significado corto |
|---|---|
| **Git** | Programa que guarda el historial de versiones de una carpeta |
| **GitHub** | Sitio web donde se aloja y se comparte el repositorio |
| **Repositorio** | Carpeta del proyecto con todo su historial |
| **Clonar** | Bajar el repositorio completo a tu PC (una vez) |
| **Commit** | Versión guardada, con un mensaje que explica el cambio |
| **Rama (branch)** | Línea paralela de trabajo; `main` es la oficial |
| **Pull** | Traer lo último que hay en GitHub |
| **Push** | Subir tus commits a GitHub |
| **Pull Request (PR)** | Solicitud para sumar tu rama a `main`, con revisión y pruebas automáticas |
| **CI** | Pruebas que GitHub corre solo en cada PR; si fallan, no se acepta |
| **Docker** | Programa que corre aplicaciones en cajas aisladas con todo instalado |
| **Imagen** | La receta de la caja; se construye una vez |
| **Contenedor** | La caja corriendo, hecha a partir de una imagen |
| **Volumen** | Disco que sobrevive aunque se borre el contenedor |
| **Docker Compose** | Levanta varias cajas juntas con un solo comando |
| **Puerto** | Puerta de entrada de un servicio, como `localhost:3000` |
| **WSL 2** | Linux liviano dentro de Windows; Docker Desktop lo necesita |
| **MariaDB** | La base de datos (compatible con MySQL) que usa el programa |
| **phpMyAdmin** | Página web para mirar y administrar la base de datos |

---

## Resumen de una página

| Cuándo | Comando | Terminal |
|---|---|---|
| Primera vez | `git clone ...` → `cd poo-eva4-calendarioapp` → `docker compose up -d --build` | PowerShell |
| Cada día, al empezar | `git pull` → `docker compose up -d` → http://localhost:3000 | PowerShell |
| Al terminar | `docker compose down` (**nunca** con `-v`) | PowerShell |
| Algo falla | `docker ps` y `docker logs calendario-mysql --tail 30` | PowerShell |

| Servicio | Dirección | Para qué |
|---|---|---|
| NetBeans | http://localhost:3000 | Escritorio con el IDE en el navegador |
| phpMyAdmin | http://localhost:8081 | Mirar la base de datos |
| Base de datos | `localhost:3306` | Solo la usa el programa Java |

Login de la aplicación: `admin` / `Admin2026`.
