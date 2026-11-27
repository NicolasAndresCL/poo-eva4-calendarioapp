# -*- coding: utf-8 -*-
import glob, os
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE, MSO_CONNECTOR
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR

BASE = r"c:\dev\learning\SantoTomas\2doSemestre\POO\NetBeansProjects\Evaluacion4_Calendario"
OUT = os.path.join(BASE, "presentacion", "Presentacion_Evaluacion4_CalendarioApp.pptx")
os.makedirs(os.path.dirname(OUT), exist_ok=True)

BG = RGBColor(0xF7, 0xF8, 0xFA)
ACC = RGBColor(0x0F, 0x76, 0x6E)
ACC_L = RGBColor(0xD5, 0xEE, 0xEA)
TXT = RGBColor(0x1F, 0x29, 0x37)
MUT = RGBColor(0x6B, 0x72, 0x80)
WHITE = RGBColor(255, 255, 255)
CODE_BG = RGBColor(0x1F, 0x29, 0x37)
CODE_FG = RGBColor(0xE5, 0xE7, 0xEB)
FONT = "Calibri"

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)
BLANK = prs.slide_layouts[6]
state = {"n": 0}


def tb(sl, x, y, w, h, text, size=20, bold=False, color=TXT, align=PP_ALIGN.LEFT,
       anchor=MSO_ANCHOR.TOP, font=FONT):
    box = sl.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = box.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = anchor
    tf.margin_left = tf.margin_right = Inches(0.05)
    lines = text if isinstance(text, list) else [text]
    for i, ln in enumerate(lines):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align
        r = p.add_run()
        r.text = ln
        r.font.size = Pt(size)
        r.font.bold = bold
        r.font.color.rgb = color
        r.font.name = font
    return box


def bullets(sl, x, y, w, h, items, size=20, gap=8):
    box = sl.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = box.text_frame
    tf.word_wrap = True
    for i, it in enumerate(items):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.space_after = Pt(gap)
        r0 = p.add_run()
        r0.text = "▪  "
        r0.font.size = Pt(size)
        r0.font.color.rgb = ACC
        r0.font.name = FONT
        if isinstance(it, tuple):
            r1 = p.add_run()
            r1.text = it[0]
            r1.font.bold = True
            r1.font.size = Pt(size)
            r1.font.color.rgb = TXT
            r1.font.name = FONT
            it = it[1]
        r = p.add_run()
        r.text = it
        r.font.size = Pt(size)
        r.font.color.rgb = TXT
        r.font.name = FONT
    return box


def rect(sl, x, y, w, h, fill, line=None, shape=MSO_SHAPE.RECTANGLE):
    s = sl.shapes.add_shape(shape, Inches(x), Inches(y), Inches(w), Inches(h))
    s.fill.solid()
    s.fill.fore_color.rgb = fill
    if line is None:
        s.line.fill.background()
    else:
        s.line.color.rgb = line
        s.line.width = Pt(1.5)
    s.shadow.inherit = False
    return s


def box_text(s, text, size=16, color=WHITE, bold=True):
    tf = s.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = MSO_ANCHOR.MIDDLE
    tf.margin_left = tf.margin_right = Inches(0.08)
    lines = text if isinstance(text, list) else [text]
    for i, ln in enumerate(lines):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = PP_ALIGN.CENTER
        r = p.add_run()
        r.text = ln
        r.font.size = Pt(size)
        r.font.bold = bold if i == 0 else False
        r.font.color.rgb = color
        r.font.name = FONT


def code(sl, x, y, w, h, text, size=14):
    s = rect(sl, x, y, w, h, CODE_BG)
    tf = s.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = MSO_ANCHOR.MIDDLE
    tf.margin_left = tf.margin_right = Inches(0.2)
    for i, ln in enumerate(text.split("\n")):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = PP_ALIGN.LEFT
        r = p.add_run()
        r.text = ln
        r.font.size = Pt(size)
        r.font.name = "Consolas"
        r.font.color.rgb = CODE_FG
    return s


def arrow(sl, x1, y1, x2, y2):
    c = sl.shapes.add_connector(MSO_CONNECTOR.STRAIGHT, Inches(x1), Inches(y1), Inches(x2), Inches(y2))
    c.line.color.rgb = ACC
    c.line.width = Pt(3)
    ln = c.line._get_or_add_ln()
    from lxml import etree
    tail = etree.SubElement(ln, "{http://schemas.openxmlformats.org/drawingml/2006/main}tailEnd")
    tail.set("type", "triangle")
    return c


def slide(title, subtitle=None, notes="", quien=""):
    state["n"] += 1
    sl = prs.slides.add_slide(BLANK)
    bg = sl.background.fill
    bg.solid()
    bg.fore_color.rgb = BG
    rect(sl, 0, 0, 0.25, 7.5, ACC)
    tb(sl, 0.6, 0.35, 12.0, 0.9, title, size=36, bold=True, color=ACC)
    if subtitle:
        tb(sl, 0.6, 1.1, 12.0, 0.5, subtitle, size=18, color=MUT)
    tb(sl, 0.6, 7.0, 9, 0.35, "CalendarioApp · Evaluación 4 · Fundamentos de POO (IEI-085)", size=11, color=MUT)
    tb(sl, 12.0, 7.0, 1.0, 0.35, str(state["n"]), size=11, color=MUT, align=PP_ALIGN.RIGHT)
    if quien:
        t = rect(sl, 10.2, 0.45, 2.7, 0.4, ACC_L, shape=MSO_SHAPE.ROUNDED_RECTANGLE)
        box_text(t, "Presenta: " + quien, size=12, color=ACC)
    sl.notes_slide.notes_text_frame.text = ("PRESENTA: " + quien + "\n\n" + notes) if quien else notes
    return sl


# 1 PORTADA ---------------------------------------------------------------
sl = prs.slides.add_slide(BLANK)
state["n"] += 1
sl.background.fill.solid()
sl.background.fill.fore_color.rgb = ACC
tb(sl, 0.9, 1.3, 11.5, 1.2, "CalendarioApp", size=60, bold=True, color=WHITE)
tb(sl, 0.9, 2.5, 11.5, 0.9, "Agenda de eventos con acceso restringido · JDBC + MySQL", size=28, color=WHITE)
rect(sl, 0.95, 3.5, 2.5, 0.06, WHITE)
tb(sl, 0.9, 3.8, 11.5, 2.2, [
    "Evaluación 4 (20 %) · Fundamentos de Programación Orientada a Objetos (IEI-085)",
    "Universidad Santo Tomás · Docente: Rosa Rao",
    "Integrantes: Tais Montecinos · Rodolfo Prieto · Nicolás Cano",
    "Defensa: lunes 30 de noviembre de 2026"], size=22, color=WHITE)
sl.notes_slide.notes_text_frame.text = (
    "PRESENTA: Nicolás.\n\nSaludo y presentación del equipo (30 s). Decir el tema en una frase: "
    "'Una agenda de eventos donde solo entra un usuario registrado y activo, y todos los datos viven en MySQL, "
    "conectados con JDBC.' Anunciar el orden: Nicolás abre, Tais explica el modelo de datos y las pruebas, "
    "Rodolfo la conexión y el login, y Nicolás el CRUD; la demo la hacemos entre los tres.")

# 2 PROBLEMA --------------------------------------------------------------
sl = slide("El problema y el tema", "¿Qué resuelve CalendarioApp?", quien="Nicolás",
           notes="1 min. Problema: las agendas en papel o en memoria se pierden; aquí los datos persisten en MySQL. "
                 "Acceso restringido: solo usuarios de la tabla usuario con activo = TRUE (L1, V2). "
                 "Alcance de esta evaluación: solo base de datos y conexión (la interfaz y las pruebas de unidad "
                 "se califican en la Evaluación 5). Mencionar que el profesor pidió POINT y JSON.")
bullets(sl, 0.6, 1.9, 7.2, 4.8, [
    ("Problema: ", "los eventos de una agenda deben guardarse de forma persistente y solo debe verlos quien tiene acceso."),
    ("Solución: ", "login contra la tabla usuario y CRUD completo sobre la tabla evento."),
    ("Tecnología: ", "Java (NetBeans, Ant) + MySQL/MariaDB por JDBC."),
    ("Pedido del profesor: ", "una columna POINT (ubicación) y una JSON (detalle)."),
], size=22, gap=14)
for i, (t, d) in enumerate([("2 tablas", "usuario y evento"), ("1 conexión", "clase Conexion"), ("5 operaciones", "CRUD + login")]):
    s = rect(sl, 8.3, 1.9 + i * 1.6, 4.4, 1.35, WHITE, line=ACC, shape=MSO_SHAPE.ROUNDED_RECTANGLE)
    box_text(s, [t, d], size=26, color=ACC)

# 3 MODELO ----------------------------------------------------------------
sl = slide("Modelo de datos", "Dos tablas independientes (sin clave foránea) en la base calendario_app", quien="Tais",
           notes="1,5 min. Mostrar primero usuario (Tabla 1 de la pauta) y luego evento (Tabla 2). "
                 "Destacar: PK AUTO_INCREMENT, UNIQUE en nombre_usuario y codigo_evento (identificador de negocio), "
                 "DEFAULT (rol, categoria, duracion_minutos, prioridad, activo, fecha_creacion), CHECK (4 reglas), "
                 "POINT (ubicacion) y JSON (detalle). Evento tiene 14 columnas (la pauta exige al menos 8). "
                 "BOOLEAN en MySQL es TINYINT(1). La columna clave usa COLLATE utf8mb4_bin para distinguir mayúsculas.")


def tabla(sl, x, y, w, filas, colw, size=12, rowh=0.3, hdr="Campo"):
    rows = len(filas) + 1
    gs = sl.shapes.add_table(rows, len(colw), Inches(x), Inches(y), Inches(w), Inches(rowh * rows))
    t = gs.table
    for j, cw in enumerate(colw):
        t.columns[j].width = Inches(cw)
    heads = ["Campo", "Tipo", "Restricciones"]
    for i in range(rows):
        t.rows[i].height = Inches(rowh)
        for j in range(len(colw)):
            c = t.cell(i, j)
            txt = heads[j] if i == 0 else filas[i - 1][j]
            c.text = txt
            c.margin_top = c.margin_bottom = Inches(0.02)
            p = c.text_frame.paragraphs[0]
            p.runs[0].font.size = Pt(size)
            p.runs[0].font.name = FONT
            if i == 0:
                c.fill.solid(); c.fill.fore_color.rgb = ACC
                p.runs[0].font.color.rgb = WHITE; p.runs[0].font.bold = True
            else:
                c.fill.solid(); c.fill.fore_color.rgb = WHITE if i % 2 else RGBColor(0xEE, 0xF2, 0xF1)
                p.runs[0].font.color.rgb = TXT
                key = any(k in filas[i - 1][2] + filas[i - 1][1] for k in ("PRIMARY", "POINT", "JSON", "UNIQUE"))
                if j == 0 and key:
                    p.runs[0].font.bold = True
    return t


tb(sl, 0.6, 1.7, 4.5, 0.4, "usuario (acceso)", size=20, bold=True, color=ACC)
tabla(sl, 0.6, 2.15, 4.6, [
    ("id_usuario", "INT", "PK, AUTO_INCREMENT"),
    ("nombre_usuario", "VARCHAR(30)", "NOT NULL, UNIQUE"),
    ("clave", "VARCHAR(64)", "NOT NULL, COLLATE utf8mb4_bin"),
    ("nombre_completo", "VARCHAR(80)", "NOT NULL"),
    ("rol", "VARCHAR(20)", "NOT NULL, DEFAULT 'OPERADOR'"),
    ("activo", "BOOLEAN", "NOT NULL, DEFAULT TRUE"),
], [1.45, 1.1, 2.05], size=11, rowh=0.42)
tb(sl, 5.5, 1.7, 7, 0.4, "evento (CRUD del tema)", size=20, bold=True, color=ACC)
tabla(sl, 5.5, 2.15, 7.3, [
    ("id_evento", "INT", "PRIMARY KEY, AUTO_INCREMENT"),
    ("codigo_evento", "VARCHAR(20)", "NOT NULL, UNIQUE"),
    ("titulo", "VARCHAR(100)", "NOT NULL"),
    ("lugar", "VARCHAR(150)", "NULL"),
    ("ubicacion", "POINT", "NULL (X = longitud, Y = latitud)"),
    ("detalle", "JSON", "NULL, CHECK JSON_VALID"),
    ("categoria", "VARCHAR(30)", "NOT NULL, DEFAULT 'personal'"),
    ("fecha_evento", "DATE", "NOT NULL"),
    ("hora_inicio / hora_fin", "TIME", "NOT NULL / NULL, CHECK fin > inicio"),
    ("duracion_minutos", "INT", "DEFAULT 60, CHECK > 0"),
    ("prioridad", "INT", "DEFAULT 2, CHECK BETWEEN 1 AND 3"),
    ("activo", "BOOLEAN", "NOT NULL, DEFAULT TRUE"),
    ("fecha_creacion", "TIMESTAMP", "DEFAULT CURRENT_TIMESTAMP"),
], [2.1, 1.5, 3.7], size=11, rowh=0.3)

# 4 SCRIPT ----------------------------------------------------------------
sl = slide("Script SQL: S1 a S5", "Archivo sql/calendario_bd.sql · se ejecuta sin errores en una instalación limpia", quien="Tais",
           notes="1 min. S1 crea la base con IF NOT EXISTS y utf8mb4. S2 usuario, S3 evento, S4 tres usuarios "
                 "(admin, operador1 e inactivo1 con activo = FALSE), S5 cinco eventos (más que el mínimo de 3). "
                 "El script incluye DROP TABLE al inicio para poder re-ejecutarlo. Tiene además pruebas de "
                 "restricciones comentadas: error 1062 (UNIQUE), 1364 (NOT NULL), 4025 (CHECK). "
                 "Probado en MariaDB 10.6; compatible con MySQL 8.0.16+ por los CHECK.")
filas = [("S1", "CREATE DATABASE IF NOT EXISTS + USE", "Base calendario_app creada"),
         ("S2", "CREATE TABLE usuario", "DESCRIBE coincide con la Tabla 1"),
         ("S3", "CREATE TABLE evento", "PK, UNIQUE, DEFAULT y 4 CHECK"),
         ("S4", "INSERT de 3 usuarios", "admin, operador1, inactivo1 (FALSE)"),
         ("S5", "INSERT de eventos", "5 filas (4 completas + 1 mínima)")]
for i, (c, a, r) in enumerate(filas):
    y = 1.9 + i * 0.95
    s = rect(sl, 0.6, y, 1.0, 0.75, ACC, shape=MSO_SHAPE.ROUNDED_RECTANGLE)
    box_text(s, c, size=24)
    tb(sl, 1.8, y - 0.05, 5.6, 0.45, a, size=19, bold=True)
    tb(sl, 1.8, y + 0.4, 5.6, 0.4, r, size=15, color=MUT)
code(sl, 7.6, 1.9, 5.2, 4.6,
     "CREATE DATABASE IF NOT EXISTS\n    calendario_app\n    CHARACTER SET utf8mb4;\nUSE calendario_app;\n\n"
     "CREATE TABLE evento (\n  id_evento INT AUTO_INCREMENT\n            PRIMARY KEY,\n"
     "  codigo_evento VARCHAR(20)\n            NOT NULL UNIQUE,\n  ubicacion POINT NULL,\n  detalle JSON NULL,\n  ...\n);", size=14)

# 5 ARQUITECTURA ----------------------------------------------------------
sl = slide("Arquitectura en paquetes", "Cada capa hace una sola cosa y depende solo de la de abajo", quien="Tais",
           notes="1 min. vista = ventanas (VentanaLogin, VentanaEventos con formulario y JTable): solo pide cosas y "
                 "muestra resultados, no escribe SQL. dao = todo el SQL con PreparedStatement (UsuarioDAO, EventoDAO). "
                 "modelo = Usuario y Evento, clases con atributos private, constructores, getters y setters. "
                 "conexion = Conexion, el único lugar con URL, usuario y clave. Ventaja: si cambia la clave de MySQL "
                 "se edita un solo archivo.")
caps = [("vista", "VentanaLogin · VentanaEventos", "ventanas Swing (JTable, formulario)"),
        ("dao", "UsuarioDAO · EventoDAO", "todo el SQL con PreparedStatement"),
        ("modelo", "Usuario · Evento", "una fila de cada tabla (private + getters/setters)"),
        ("conexion", "Conexion", "URL, usuario y clave en constantes")]
for i, (p, c, d) in enumerate(caps):
    y = 1.8 + i * 1.25
    s = rect(sl, 0.6 + i * 0.35, y, 3.0, 1.0, ACC if i != 2 else RGBColor(0x2F, 0x8F, 0x86), shape=MSO_SHAPE.ROUNDED_RECTANGLE)
    box_text(s, [p, c], size=20)
    tb(sl, 4.4 + i * 0.35, y + 0.2, 7.5 - i * 0.35, 0.6, d, size=18, color=TXT, anchor=MSO_ANCHOR.MIDDLE)
tb(sl, 0.6, 6.6, 12, 0.4, "Regla: la vista nunca habla con MySQL directamente; lo hace siempre a través de un DAO.", size=16, color=MUT)

# 6 CONEXION --------------------------------------------------------------
sl = slide("Clase Conexion", "conexion/Conexion.java · A4: parámetros centralizados y reutilizables", quien="Rodolfo",
           notes="1 min. Tres constantes private static final (URL, USUARIO, CLAVE): los datos viven solo aquí. "
                 "conectar() devuelve un Connection nuevo con DriverManager.getConnection y lanza SQLException: quien "
                 "lo pide lo cierra con try-with-resources. probarConexion() devuelve true/false usando isValid(2). "
                 "La URL lleva useSSL=false, allowPublicKeyRetrieval=true y serverTimezone=America/Santiago. "
                 "El driver es mysql-connector-j 26.10.0 en CalendarioApp/lib (agregado a Libraries del proyecto). "
                 "Además, mensajeError(SQLException) traduce códigos 1062, 1045, 1049 y 4025/3819/3140 a mensajes claros.")
code(sl, 0.6, 1.8, 7.6, 4.9,
     'private static final String URL =\n  "jdbc:mysql://localhost:3306/calendario_app"\n  + "?useSSL=false&allowPublicKeyRetrieval=true"\n  + "&serverTimezone=America/Santiago";\n'
     'private static final String USUARIO = "root";\nprivate static final String CLAVE = "";\n\n'
     'public static Connection conectar()\n        throws SQLException {\n  return DriverManager\n    .getConnection(URL, USUARIO, CLAVE);\n}', size=14)
bullets(sl, 8.5, 1.8, 4.5, 4.9, [
    ("conectar(): ", "abre una conexión nueva; quien la usa la cierra."),
    ("probarConexion(): ", "true si MySQL responde (isValid)."),
    ("mensajeError(): ", "traduce códigos SQL a mensajes claros."),
    ("Driver: ", "mysql-connector-j 26.10.0 en lib/."),
], size=18, gap=12)

# 7 FLUJO JDBC ------------------------------------------------------------
sl = slide("Flujo JDBC", "Tres objetos, siempre en este orden", quien="Rodolfo",
           notes="1 min. Connection = la línea abierta con MySQL (Conexion.conectar()). PreparedStatement = la sentencia "
                 "SQL precompilada con parámetros ?; se rellena con setString, setInt, etc. ResultSet = el cursor con las "
                 "filas que devuelve un SELECT; next() avanza y devuelve false cuando no hay más. "
                 "executeQuery() es para SELECT y devuelve ResultSet; executeUpdate() es para INSERT, UPDATE y DELETE "
                 "y devuelve el número de filas afectadas. Los tres se cierran solos con try-with-resources.")
xs = [0.6, 4.85, 9.1]
nom = [("1. Connection", "Conexion.conectar()"), ("2. PreparedStatement", "prepareStatement(sql) + setXxx(?)"),
       ("3. ResultSet", "executeQuery() → while (rs.next())")]
for x, (a, b) in zip(xs, nom):
    s = rect(sl, x, 2.2, 3.6, 1.5, ACC, shape=MSO_SHAPE.ROUNDED_RECTANGLE)
    box_text(s, [a, b], size=20)
arrow(sl, 4.25, 2.95, 4.8, 2.95)
arrow(sl, 8.5, 2.95, 9.05, 2.95)
s = rect(sl, 0.6, 4.2, 12.1, 0.9, WHITE, line=ACC, shape=MSO_SHAPE.ROUNDED_RECTANGLE)
box_text(s, "try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery())",
         size=14, color=TXT, bold=False)
bullets(sl, 0.6, 5.3, 12, 1.6, [
    ("executeQuery() ", "→ SELECT, devuelve un ResultSet."),
    ("executeUpdate() ", "→ INSERT / UPDATE / DELETE, devuelve las filas afectadas."),
    ("Los tres recursos ", "se cierran solos al salir del try."),
], size=18, gap=6)

# 8 LOGIN -----------------------------------------------------------------
sl = slide("Login: L1, V1 y V2", "UsuarioDAO.validar() · solo ingresa un usuario válido y activo", quien="Rodolfo",
           notes="1 min. L1: UsuarioDAO.validar(nombreUsuario, clave) usa SELECT ... WHERE nombre_usuario = ? AND clave = ? "
                 "AND activo = TRUE con executeQuery(); devuelve un Usuario o null. V1: si los campos están vacíos o "
                 "validar() devuelve null se muestra 'Usuario o contraseña incorrectos' y no se ingresa. "
                 "V2: UsuarioDAO.estaInactivo() cuenta si hay un usuario con esas credenciales y activo = FALSE; "
                 "entonces se muestra 'Usuario inactivo'. Credenciales de prueba: admin / Admin2026, "
                 "operador1 / Oper2026, inactivo1 / Inac2026. Los parámetros ? evitan la inyección SQL.")
code(sl, 0.6, 1.8, 7.8, 2.6,
     'SELECT id_usuario, nombre_usuario, clave,\n       nombre_completo, rol, activo\nFROM usuario\n'
     'WHERE nombre_usuario = ?\n  AND clave = ?\n  AND activo = TRUE', size=15)
rows = [("V1", "Campos vacíos o credenciales incorrectas", "«Usuario o contraseña incorrectos»"),
        ("V2", "Usuario con activo = FALSE", "«Usuario inactivo»")]
for i, (c, r, m) in enumerate(rows):
    y = 4.7 + i * 1.0
    s = rect(sl, 0.6, y, 0.9, 0.8, ACC, shape=MSO_SHAPE.ROUNDED_RECTANGLE)
    box_text(s, c, size=22)
    tb(sl, 1.7, y, 6.7, 0.8, [r, m], size=17, anchor=MSO_ANCHOR.MIDDLE)
bullets(sl, 8.8, 1.8, 4.2, 4.9, [
    ("Parametrizado: ", "los ? impiden la inyección SQL."),
    ("Devuelve ", "Usuario, o null si no es válido o está inactivo."),
    ("estaInactivo(): ", "separa el caso «inactivo» para dar su propio mensaje."),
    ("Prueba: ", "admin, operador1 e inactivo1."),
], size=18, gap=12)

# 9 CRUD ------------------------------------------------------------------
sl = slide("CRUD sobre evento: C1 a C5", "EventoDAO · todas las sentencias con PreparedStatement", quien="Nicolás",
           notes="1,5 min. C1 insertar: executeUpdate + RETURN_GENERATED_KEYS para leer el id AUTO_INCREMENT. "
                 "C2 listar: executeQuery y while (rs.next()) que arma un ArrayList<Evento> para la JTable. "
                 "C3 buscar por codigo_evento (identificador de negocio). C4 actualizar por id_evento y "
                 "executeUpdate() == 1. C5 eliminar por id_evento; la confirmación con JOptionPane la pide la ventana "
                 "antes de llamar a eliminar(). Insertar y actualizar comparten cargarParametros(): "
                 "los 13 parámetros van en el mismo orden.")
crud = [("C1", "Insertar", "INSERT INTO evento (...) VALUES (?, ?, ?, POINT(?, ?), ...)", "executeUpdate()"),
        ("C2", "Listar", "SELECT ... FROM evento ORDER BY fecha_evento, hora_inicio", "executeQuery()"),
        ("C3", "Buscar", "SELECT ... FROM evento WHERE codigo_evento = ?", "executeQuery()"),
        ("C4", "Actualizar", "UPDATE evento SET ... WHERE id_evento = ?", "executeUpdate()"),
        ("C5", "Eliminar", "DELETE FROM evento WHERE id_evento = ?", "executeUpdate()")]
for i, (c, n, q, m) in enumerate(crud):
    y = 1.85 + i * 0.98
    s = rect(sl, 0.6, y, 0.9, 0.8, ACC, shape=MSO_SHAPE.ROUNDED_RECTANGLE)
    box_text(s, c, size=22)
    tb(sl, 1.65, y - 0.02, 2.1, 0.82, n, size=22, bold=True, anchor=MSO_ANCHOR.MIDDLE)
    code(sl, 3.8, y, 6.7, 0.8, q, size=13)
    tb(sl, 10.7, y, 2.4, 0.8, m, size=17, bold=True, color=ACC, anchor=MSO_ANCHOR.MIDDLE)

# 10 EXCEPCIONES ----------------------------------------------------------
sl = slide("Excepciones y recursos: V3, V4", "A5: SQLException con mensajes claros y recursos cerrados", quien="Nicolás",
           notes="1 min. V3: codigo_evento tiene UNIQUE; al insertar uno repetido MySQL lanza SQLException con "
                 "getErrorCode() == 1062; Conexion.mensajeError() lo traduce a 'El registro ya existe.' y no se inserta. "
                 "V4: si MySQL está detenido o la clave está mala, getConnection lanza SQLException con SQLState que "
                 "empieza en '08' (o error 1045/1049); mensaje 'No fue posible conectar con la base de datos.' mostrado "
                 "con JOptionPane, sin cerrar la aplicación. try-with-resources cierra Connection, PreparedStatement y "
                 "ResultSet aunque haya excepción, sin escribir finally.")
code(sl, 0.6, 1.8, 6.3, 2.8,
     'try {\n    dao.insertar(evento);\n} catch (SQLException ex) {\n    mostrar(Conexion.mensajeError(ex));\n}', size=17)
bullets(sl, 0.6, 4.9, 6.3, 1.6, [
    ("try-with-resources: ", "cierra los recursos solo, incluso con error."),
], size=18)
rows = [("V3", "Código repetido (UNIQUE) → error 1062", "«El registro ya existe.»  No inserta."),
        ("V4", "MySQL detenido o datos erróneos → SQLState 08xx", "«No fue posible conectar con la base de datos.»  La app sigue abierta."),
        ("CHECK", "Prioridad, horas o JSON inválidos → 4025 / 3819 / 3140", "«Los datos no cumplen las reglas de la tabla.»")]
for i, (c, r, m) in enumerate(rows):
    y = 1.8 + i * 1.65
    s = rect(sl, 7.3, y, 1.1, 1.4, ACC, shape=MSO_SHAPE.ROUNDED_RECTANGLE)
    box_text(s, c, size=20)
    tb(sl, 8.55, y, 4.5, 1.4, [r, m], size=17, anchor=MSO_ANCHOR.MIDDLE)

# 11 POINT y JSON ---------------------------------------------------------
sl = slide("POINT y JSON en la práctica", "Las dos columnas especiales que pidió el profesor", quien="Nicolás",
           notes="1,5 min. POINT: se escribe con POINT(?, ?) y los parámetros van (longitud, latitud): X = longitud, "
                 "Y = latitud. Se lee con ST_X(ubicacion) y ST_Y(ubicacion), porque el POINT binario no se puede leer "
                 "directamente en Java. Si el evento no tiene ubicación se usa setNull(..., Types.DOUBLE). "
                 "JSON: se guarda como texto (setString) y el CHECK con JSON_VALID(detalle) rechaza texto inválido "
                 "(error 4025 en MariaDB, 3140 en MySQL). Se lee con rs.getString('detalle'); en SQL se puede sacar "
                 "un campo con JSON_EXTRACT(detalle, '$.notas'). Por qué: POINT guarda un par de coordenadas como un "
                 "solo dato geoespacial; JSON permite datos libres (notas, participantes) sin crear más tablas. "
                 "En MariaDB JSON es un alias de LONGTEXT; en MySQL es un tipo nativo.")
tb(sl, 0.6, 1.7, 6, 0.5, "POINT · ubicación", size=24, bold=True, color=ACC)
code(sl, 0.6, 2.3, 6.0, 2.3,
     '-- escribir (X = longitud, Y = latitud)\nPOINT(?, ?)\n\n-- leer\nST_X(ubicacion) AS longitud,\nST_Y(ubicacion) AS latitud', size=15)
tb(sl, 6.9, 1.7, 6, 0.5, "JSON · detalle", size=24, bold=True, color=ACC)
code(sl, 6.9, 2.3, 6.0, 2.3,
     '-- se guarda como texto válido\n{"notas": "Chequeo anual",\n "llevar": ["carnet"]}\n\n-- validación en la tabla\nCHECK (detalle IS NULL\n   OR JSON_VALID(detalle))', size=15)
bullets(sl, 0.6, 4.9, 6.0, 2.0, [
    "Sin ubicación: setNull(..., Types.DOUBLE).",
    "Orden clave: POINT(longitud, latitud).",
], size=17, gap=6)
bullets(sl, 6.9, 4.9, 6.0, 2.0, [
    "JSON inválido: el CHECK lanza el error 4025 / 3140.",
    "Leer un campo: JSON_EXTRACT(detalle, '$.notas').",
], size=17, gap=6)

# 12 PRUEBAS --------------------------------------------------------------
sl = slide("Pruebas", "24 pruebas JUnit 4 en verde + verificación en MySQL", quien="Tais",
           notes="1 min. Hay 24 pruebas JUnit 4 en CalendarioApp/test/, todas en verde con Alt+F6 (ant test): "
                 "ConexionTest 4, UsuarioDAOTest 6, EventoDAOTest 11 y EventoTest 3. Autor de las pruebas: Nicolás. "
                 "Se usa JUnit 4 y no 5 porque en un proyecto Ant de NetBeans 'ant test' no ejecuta JUnit 5. "
                 "Además hay un driver de interfaz (herramientas/DriverCalendario.java) con 23 de 23 comprobaciones. "
                 "Cada operación se verifica con un SELECT en phpMyAdmin (http://localhost:8081) o con "
                 "docker exec poo-mysql mariadb -uroot calendario_app -e '...'. Las pruebas de unidad se califican "
                 "en la Evaluación 5; aquí son respaldo.")
for i, (n, t) in enumerate([("4", "ConexionTest"), ("6", "UsuarioDAOTest"), ("11", "EventoDAOTest"), ("3", "EventoTest")]):
    x = 0.6 + i * 3.1
    s_ = rect(sl, x, 1.9, 2.9, 1.8, ACC, shape=MSO_SHAPE.ROUNDED_RECTANGLE)
    box_text(s_, [n, t], size=20)
    sl.shapes[-1].text_frame.paragraphs[0].runs[0].font.size = Pt(40)
bullets(sl, 0.6, 4.1, 12.2, 2.8, [
    ("24 pruebas JUnit 4 ", "en CalendarioApp/test/, todas en verde con Alt+F6 (ant test). Autor: Nicolás."),
    ("Driver de interfaz: ", "23 de 23 comprobaciones sobre las ventanas."),
    ("En MySQL: ", "cada operación se confirma con un SELECT (phpMyAdmin o mariadb)."),
    ("JUnit 4 y no 5: ", "en Ant, «ant test» no ejecuta JUnit 5."),
], size=19, gap=8)

# 13 REPARTO --------------------------------------------------------------
sl = slide("Reparto del trabajo", "Registrado en la bitácora del equipo (criterio B3)", quien="Tais",
           notes="1 min. Cada integrante explica el código que desarrolló. Propuesta de reparto (el equipo debe ajustarla "
                 "a lo que realmente hizo cada uno): Tais, script SQL y modelo de datos (S1 a S5, clases modelo); "
                 "Rodolfo, clase Conexion, UsuarioDAO, login y validaciones V1, V2 y V4; Nicolás, EventoDAO "
                 "(C1 a C5), V3, POINT/JSON y pruebas JUnit; los tres, ventanas e integración. "
                 "Recordar: la defensa es individual y cualquiera puede ser preguntado por cualquier parte.")
reparto = [("Tais", "Script SQL y modelo de datos", "S1–S5 · clases Usuario y Evento"),
           ("Rodolfo", "Conexión, login y validaciones", "Conexion · UsuarioDAO · V1, V2, V4"),
           ("Nicolás", "CRUD, POINT/JSON y pruebas", "EventoDAO C1–C5 · V3 · JUnit")]
for i, (n, a, b) in enumerate(reparto):
    x = 0.6 + i * 4.15
    s = rect(sl, x, 1.9, 3.9, 3.3, WHITE, line=ACC, shape=MSO_SHAPE.ROUNDED_RECTANGLE)
    tb(sl, x + 0.2, 2.1, 3.5, 0.6, n, size=28, bold=True, color=ACC)
    tb(sl, x + 0.2, 2.9, 3.5, 1.0, a, size=20, bold=True)
    tb(sl, x + 0.2, 4.0, 3.5, 1.0, b, size=16, color=MUT)
s = rect(sl, 0.6, 5.5, 12.1, 0.9, ACC_L, shape=MSO_SHAPE.ROUNDED_RECTANGLE)
box_text(s, "Los tres: ventanas (VentanaLogin, VentanaEventos) e integración", size=20, color=ACC)

# 14 DEMO ----------------------------------------------------------------
from PIL import Image


def foto(sl, nombre, x, y, maxw, maxh, leyenda):
    p = os.path.join(BASE, "capturas", nombre)
    w, h = Image.open(p).size
    k = min(maxw / w, maxh / h)
    pw, ph = w * k, h * k
    rect(sl, x - 0.04 + (maxw - pw) / 2, y - 0.04, pw + 0.08, ph + 0.08, MUT)
    sl.shapes.add_picture(p, Inches(x + (maxw - pw) / 2), Inches(y), Inches(pw), Inches(ph))
    tb(sl, x, y + ph + 0.1, maxw, 0.7, leyenda, size=15, bold=True, color=TXT, align=PP_ALIGN.CENTER)


sl = slide("Demostración (1/2): acceso", "Login contra la tabla usuario · L1, V1 y V2", quien="Rodolfo",
           notes="Demo en vivo (1,5 min). Rodolfo: 1) abrir VentanaLogin; 2) campos vacíos o clave mala: V1; "
                 "3) inactivo1 / Inac2026: V2; 4) admin / Admin2026 entra (L1). Mostrar en phpMyAdmin "
                 "(http://localhost:8081) SELECT nombre_usuario, activo FROM usuario. La clave distingue mayúsculas "
                 "(columna clave con COLLATE utf8mb4_bin): ADMIN2026 no entra. Antes: docker compose up -d.")
foto(sl, "01-login.png", 0.6, 2.0, 3.9, 2.4, "Ventana de acceso")
foto(sl, "02-v1-credenciales.png", 4.7, 2.0, 3.9, 2.4, "V1: «Usuario o contraseña incorrectos.»")
foto(sl, "03-v2-inactivo.png", 8.8, 2.0, 3.9, 2.4, "V2: «Usuario inactivo.»")
bullets(sl, 0.6, 5.3, 12, 1.6, [
    "Verificación: SELECT nombre_usuario, activo FROM usuario;",
    "La clave distingue mayúsculas: ADMIN2026 no entra (collation utf8mb4_bin).",
], size=18, gap=6)

sl = slide("Demostración (2/2): CRUD", "Cada operación se confirma con un SELECT en MySQL", quien="Nicolás y Tais",
           notes="Demo en vivo (2 min). Nicolás: insertar un evento (C1) y se ve en la JTable; intentar el mismo código: "
                 "V3 'El registro ya existe.'; buscar (C3), actualizar (C4), eliminar con confirmación (C5). Tais: tras "
                 "cada paso, el SELECT en phpMyAdmin o con docker exec poo-mysql mariadb -uroot calendario_app -e "
                 "'SELECT ...'. Opcional: docker stop poo-mysql para mostrar V4 (la app no se cierra) y luego "
                 "docker start poo-mysql.")
foto(sl, "04-eventos-tabla.png", 0.6, 1.75, 3.9, 3.8, "Eventos cargados desde MySQL (C2)")
foto(sl, "05-v3-repetido.png", 4.7, 1.75, 3.9, 3.8, "V3: código repetido, no inserta")
foto(sl, "06-eventos-actualizado.png", 8.8, 1.75, 3.9, 3.8, "Actualizado (C4), reflejado en MySQL")
tb(sl, 0.6, 6.45, 12, 0.5, "Después de cada paso: SELECT ... FROM evento WHERE codigo_evento = '...';", size=16, color=MUT)

# 15 CIERRE ---------------------------------------------------------------
sl = slide("Cierre", "Lo que logramos y lo que aprendimos", quien="Rodolfo",
           notes="30 s. Resumen: dos tablas con restricciones, script S1 a S5, Conexion centralizada, login L1 con V1/V2, "
                 "CRUD C1 a C5 con PreparedStatement, manejo de excepciones V3/V4 y las columnas POINT y JSON. "
                 "Aprendizaje: separar en capas, parametrizar siempre y cerrar recursos con try-with-resources. "
                 "Agradecer y abrir a preguntas.")
bullets(sl, 0.6, 1.9, 7.4, 4.8, [
    "Modelo relacional con PK, UNIQUE, DEFAULT, CHECK, POINT y JSON.",
    "Conexión JDBC centralizada en una sola clase.",
    "Login y CRUD que persisten de verdad en MySQL.",
    "Sentencias parametrizadas: sin inyección SQL.",
    "Errores controlados: la aplicación no se cae.",
], size=22, gap=14)
s = rect(sl, 8.6, 2.2, 4.2, 3.0, ACC, shape=MSO_SHAPE.ROUNDED_RECTANGLE)
box_text(s, ["¡Gracias!", "¿Preguntas?"], size=36)

prs.save(OUT)
print("OK", state["n"], OUT)
