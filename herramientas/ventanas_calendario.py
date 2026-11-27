"""Ventanas de CalendarioApp: vista.VentanaLogin y vista.VentanaEventos.

Ejecutarlo regenera los .java y .form de src/vista. Si las ventanas ya se editaron desde
Design en NetBeans, NO volver a correrlo (pisaria esos cambios).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genform import C, P, R, S, FILL, generate

VISTA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "CalendarioApp", "src", "vista")

AZUL = (31, 78, 121)
AZUL_CLARO = (52, 115, 170)
FONDO = (238, 242, 247)
BLANCO = (255, 255, 255)

LBL = "javax.swing.JLabel"
TXT = "javax.swing.JTextField"
BTN = "javax.swing.JButton"


def etiqueta(name, texto, w=90):
    return C(LBL, name, ("text", texto), w=w)


def campo(name, w=170):
    return C(TXT, name, w=w)


def boton(name, texto, w):
    return C(BTN, name, ("background", AZUL_CLARO), ("foreground", BLANCO), ("text", texto), w=w,
             ev=name + "ActionPerformed")


def leer(nombre):
    """Los trozos de Java viven en herramientas/calendario/ para no mezclar dos lenguajes."""
    ruta = os.path.join(os.path.dirname(os.path.abspath(__file__)), "calendario", nombre)
    with open(ruta, encoding="utf-8") as f:
        return f.read()


login = dict(type="frame", title="Calendario de eventos - Acceso", head=leer("login_head.txt"),
             body1=leer("login_body1.txt"), body2=leer("login_body2.txt"), rows=[
    R(C(LBL, "lblTitulo", ("background", AZUL), ("font", ("Segoe UI", 1, 18)), ("foreground", BLANCO),
        ("horizontalAlignment", 0), ("text", "Calendario de eventos"), ("opaque", True), w=FILL, h=40)),
    R(etiqueta("lblUsuario", "Usuario:"), campo("txtUsuario", 190), gap="unrelated"),
    R(etiqueta("lblClave", "Contraseña:"), C("javax.swing.JPasswordField", "txtClave", w=190)),
    R(boton("btnIngresar", "Ingresar", 110), boton("btnSalir", "Salir", 110), gap="unrelated"),
])

pnl_form = P("pnlFormulario", [
    R(etiqueta("lblCodigo", "Código:"), campo("txtCodigo"), etiqueta("lblTitulo", "Título:"), campo("txtTitulo")),
    R(etiqueta("lblLugar", "Lugar:"), campo("txtLugar"), etiqueta("lblCategoria", "Categoría:"),
      C("javax.swing.JComboBox", "cboCategoria", ("combo", ["personal", "trabajo", "salud", "estudio"]),
        w=170, tp="String")),
    R(etiqueta("lblLatitud", "Latitud:"), campo("txtLatitud"), etiqueta("lblLongitud", "Longitud:"),
      campo("txtLongitud")),
    R(etiqueta("lblFecha", "Fecha:"), campo("txtFecha"), etiqueta("lblHoraInicio", "Hora inicio:"),
      campo("txtHoraInicio")),
    R(etiqueta("lblHoraFin", "Hora fin:"), campo("txtHoraFin"), etiqueta("lblDuracion", "Duración (min):"),
      C("javax.swing.JSpinner", "spnDuracion", ("spinner", (60, 1, 1440, 15)), w=170)),
    R(etiqueta("lblPrioridad", "Prioridad:"),
      C("javax.swing.JComboBox", "cboPrioridad", ("combo", ["1", "2", "3"]), w=170, tp="String"),
      C("javax.swing.JCheckBox", "chkActivo", ("text", "Activo"), ("background", FONDO), w=170)),
    R(etiqueta("lblDetalle", "Detalle (JSON):"),
      S("scrDetalle", C("javax.swing.JTextArea", "txtDetalle", ("columns", 20), ("rows", 3)), w=470, h=60),
      align="leading"),
], ("background", FONDO), ("border", "Datos del evento"), w=FILL)

eventos = dict(type="frame", title="Calendario de eventos", head=leer("eventos_head.txt"),
               body1=leer("eventos_body1.txt"), body2=leer("eventos_body2.txt"), rows=[
    R(C(LBL, "lblEncabezado", ("background", AZUL), ("font", ("Segoe UI", 1, 18)), ("foreground", BLANCO),
        ("horizontalAlignment", 0), ("text", "Calendario de eventos"), ("opaque", True), w=FILL, h=40)),
    R(C(LBL, "lblBienvenida", ("font", ("Segoe UI", 1, 12)), ("foreground", AZUL), ("text", "Bienvenido/a"),
        w=FILL)),
    R(pnl_form),
    R(boton("btnGuardar", "Guardar", 100), boton("btnBuscar", "Buscar", 100),
      boton("btnActualizar", "Actualizar", 100), boton("btnEliminar", "Eliminar", 100),
      boton("btnLimpiar", "Limpiar", 100), boton("btnCerrarSesion", "Cerrar sesión", 120), gap="unrelated"),
    R(S("scrEventos", C("javax.swing.JTable", "tblEventos", ("selectionMode", 0),
        ("table", ["Id", "Código", "Título", "Fecha", "Inicio", "Categoría", "Prioridad", "Activo", "Lugar"])),
        w=FILL, h=190)),
])

if __name__ == "__main__":
    os.makedirs(VISTA, exist_ok=True)
    generate(login, os.path.join(VISTA, "VentanaLogin.java"), os.path.join(VISTA, "VentanaLogin.form"))
    generate(eventos, os.path.join(VISTA, "VentanaEventos.java"), os.path.join(VISTA, "VentanaEventos.form"))
    print("ok VentanaLogin, VentanaEventos")
