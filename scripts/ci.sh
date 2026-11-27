#!/usr/bin/env bash
# Lo mismo que corre el job "ci" de GitHub Actions (.github/workflows/ci.yml).
# Requisitos: MySQL/MariaDB escuchando en 127.0.0.1:3306 con root sin clave,
# JDK 21, Apache Ant con ant-optional (tarea <junit>), curl y un cliente mysql.
#
# Uso, desde la raíz del repo:   bash scripts/ci.sh
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
LIBS="$RAIZ/.ci-libs"
JUNIT="$LIBS/junit-4.13.2.jar"
HAMCREST="$LIBS/hamcrest-core-1.3.jar"

echo "==> 1/3 Cargar sql/calendario_bd.sql"
CLIENTE="$(command -v mysql || command -v mariadb)"
# El cliente de MariaDB 11+ exige TLS por defecto y MariaDB 10.6 no lo trae
# habilitado: con ese cliente se apaga (--skip-ssl). El de MySQL no lo necesita.
TLS=""
"$CLIENTE" --version | grep -qi mariadb && TLS="--skip-ssl"
"$CLIENTE" $TLS -h 127.0.0.1 -P 3306 -u root --default-character-set=utf8mb4 < "$RAIZ/sql/calendario_bd.sql" > /dev/null
"$CLIENTE" $TLS -h 127.0.0.1 -u root -N -e \
  "SELECT CONCAT('usuarios=', (SELECT COUNT(*) FROM calendario_app.usuario), ' eventos=', (SELECT COUNT(*) FROM calendario_app.evento))"

echo "==> 2/3 Descargar JUnit 4 (en NetBeans vienen con el IDE)"
mkdir -p "$LIBS"
[ -f "$JUNIT" ] || curl -fsSL -o "$JUNIT" https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar
[ -f "$HAMCREST" ] || curl -fsSL -o "$HAMCREST" https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar

echo "==> 3/3 Compilar, empaquetar y probar con Ant (igual que F11 + Alt+F6)"
cd "$RAIZ/CalendarioApp"
ant -noinput \
  -Dlibs.junit_4.classpath="$JUNIT" \
  -Dlibs.hamcrest.classpath="$HAMCREST" \
  clean jar test
