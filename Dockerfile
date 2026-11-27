# Escritorio Ubuntu en el navegador (KasmVNC, sobre linuxserver/webtop) con
# Apache NetBeans 31 y JDK 21 ya instalados: el mismo IDE del curso, sin
# instalar nada en Windows. NetBeans se abre desde el menú de aplicaciones
# (Applications > Development > Apache NetBeans); no arranca solo.
#
# Lo usa docker-compose.yml (servicio netbeans). Primera construcción:
# descarga ~1,5-2 GB y tarda 10-20 min; después queda en caché.

# ---------------------------------------------------------------- fetch
# Stage separado solo para descargar y descomprimir NetBeans: así la imagen
# final no arrastra el .zip de 500MB ni las herramientas de descarga.
FROM debian:bookworm-slim AS fetch
RUN apt-get update && apt-get install -y --no-install-recommends \
        curl ca-certificates unzip \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /opt
# archive.apache.org guarda TODAS las versiones para siempre; dlcdn.apache.org
# solo tiene la más reciente (desde que salió NetBeans 32, la 31 da 404 ahí).
# El .sha512 se verifica antes de descomprimir: si la descarga llega corrupta,
# el build se detiene en vez de dejar un IDE roto.
ARG NETBEANS_URL=https://archive.apache.org/dist/netbeans/netbeans/31/netbeans-31-bin.zip
RUN curl -fsSL -o netbeans.zip "$NETBEANS_URL" \
    && echo "$(curl -fsSL "$NETBEANS_URL.sha512" | awk '{print $1}')  netbeans.zip" | sha512sum -c - \
    && unzip -q netbeans.zip \
    && rm netbeans.zip

# ---------------------------------------------------------------- runtime
FROM lscr.io/linuxserver/webtop:ubuntu-xfce AS runtime

RUN apt-get update && apt-get install -y --no-install-recommends \
        openjdk-21-jdk curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=fetch /opt/netbeans /opt/netbeans
ENV NETBEANS_HOME=/opt/netbeans
ENV PATH="${NETBEANS_HOME}/bin:${PATH}"

# Entrada en el menú de aplicaciones del sistema (siempre visible y
# ejecutable, sin el problema de "trust" de los .desktop del escritorio).
COPY docker/netbeans.desktop /usr/share/applications/netbeans.desktop
RUN update-desktop-database /usr/share/applications 2>/dev/null || true

# Acceso directo en el escritorio (doble clic para abrir NetBeans). La sesión
# gráfica de esta imagen (webtop) corre como el usuario "abc" con HOME=/config
# (montado como volumen en runtime, no existe en tiempo de build), NO como
# root — por eso no basta con dejar el .desktop en /root/Desktop. Se usa el
# hook oficial /custom-cont-init.d/, que linuxserver ejecuta al arrancar el
# contenedor, cuando /config ya está montado.
COPY docker/init-netbeans-desktop.sh /custom-cont-init.d/10-netbeans-desktop-icon.sh
RUN chmod +x /custom-cont-init.d/10-netbeans-desktop-icon.sh

EXPOSE 3000 3001

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl -fsS http://localhost:3000 || exit 1
