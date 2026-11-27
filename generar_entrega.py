"""Arma la entrega de la Evaluacion 4.

Crea entrega/CalendarioApp_Montecinos_Prieto_Cano.zip con:
  - LEEME.txt               instrucciones (se edita en Evaluacion4_Calendario/LEEME.txt)
  - calendario_bd.sql       el script de sql/
  - CalendarioApp/          el proyecto NetBeans (src, test, lib, nbproject, build.xml)

Deja fuera lo que NetBeans regenera o es propio de cada equipo:
build/, dist/ y nbproject/private/.

Uso, desde cualquier carpeta:
    python NetBeansProjects/Evaluacion4_Calendario/generar_entrega.py
"""
import os
import sys
import zipfile

BASE = os.path.dirname(os.path.abspath(__file__))
PROYECTO = os.path.join(BASE, "CalendarioApp")
ZIP = os.path.join(BASE, "entrega", "CalendarioApp_Montecinos_Prieto_Cano.zip")
EXCLUIR = {"build", "dist", "nbproject/private"}

OBLIGATORIOS = [
    "build.xml", "manifest.mf", "lib/mysql-connector-j-26.10.0.jar",
    "nbproject/project.xml", "nbproject/project.properties", "nbproject/build-impl.xml",
    "src/conexion/Conexion.java", "src/modelo/Usuario.java", "src/modelo/Evento.java",
    "src/dao/UsuarioDAO.java", "src/dao/EventoDAO.java",
    "src/vista/VentanaLogin.java", "src/vista/VentanaLogin.form",
    "src/vista/VentanaEventos.java", "src/vista/VentanaEventos.form",
]


def excluido(relativo):
    partes = relativo.replace("\\", "/").split("/")
    return any("/".join(partes[:i]) in EXCLUIR for i in range(1, len(partes) + 1))


def main():
    faltan = [f for f in OBLIGATORIOS if not os.path.isfile(os.path.join(PROYECTO, f))]
    if faltan:
        sys.exit("Faltan archivos del proyecto: " + ", ".join(faltan))

    os.makedirs(os.path.dirname(ZIP), exist_ok=True)
    total = 0
    with zipfile.ZipFile(ZIP, "w", zipfile.ZIP_DEFLATED) as z:
        z.write(os.path.join(BASE, "LEEME.txt"), "LEEME.txt")
        z.write(os.path.join(BASE, "sql", "calendario_bd.sql"), "calendario_bd.sql")
        total += 2
        for carpeta, subcarpetas, nombres in os.walk(PROYECTO):
            rel_carpeta = os.path.relpath(carpeta, PROYECTO)
            subcarpetas[:] = [d for d in subcarpetas
                              if not excluido(os.path.normpath(os.path.join(rel_carpeta, d)))]
            for n in sorted(nombres):
                rel = os.path.normpath(os.path.join(rel_carpeta, n))
                z.write(os.path.join(PROYECTO, rel),
                        "CalendarioApp/" + rel.replace("\\", "/"))
                total += 1
    print(f"{ZIP}: {total} archivos")


if __name__ == "__main__":
    main()
