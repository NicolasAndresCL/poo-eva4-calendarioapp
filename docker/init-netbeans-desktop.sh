#!/bin/bash
# Hook de linuxserver: corre en /custom-cont-init.d/ al arrancar el
# contenedor, cuando /config (volumen) ya está montado.
set -e

mkdir -p /config/Desktop

cat > /config/Desktop/netbeans.desktop <<'EOF'
[Desktop Entry]
Type=Application
Name=Apache NetBeans
Exec=/opt/netbeans/bin/netbeans --jdkhome /usr/lib/jvm/java-21-openjdk-amd64
Icon=/opt/netbeans/nb/netbeans.png
Terminal=false
EOF

chmod +x /config/Desktop/netbeans.desktop
chown abc:abc /config/Desktop/netbeans.desktop

# XFCE/Thunar no ejecuta un .desktop del escritorio al doble clic hasta que
# se marca como "de confianza" (si no, el doble clic no hace nada). gio lo
# marca sin que el usuario tenga que hacerlo manualmente la primera vez.
gio set /config/Desktop/netbeans.desktop metadata::trusted true 2>/dev/null || true

# Si en algún momento se entró al contenedor como root (p.ej. `docker exec`
# sin --user) y se corrió NetBeans a mano, tanto el userdir como la caché
# OSGi/netigso quedan con dueño root y el usuario real de la sesión (abc) no
# puede escribir ahí (falla con "Permission denied" al iniciar el framework
# OSGi, aunque el userdir sí sea escribible).
for dir in /config/.netbeans /config/.cache/netbeans; do
    if [ -d "$dir" ]; then
        chown -R abc:abc "$dir"
    fi
done
