import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Prueba funcional de CalendarioApp (Evaluacion IV): maneja vista.VentanaLogin y
 * vista.VentanaEventos contra la base real calendario_app. Deja capturas en
 * /tmp/capturas-cal/ y borra al final todo evento TEST-GUI-%. No forma parte de la entrega.
 */
public class DriverCalendario {
    static String out = "/tmp/capturas-cal/";
    static int fallos = 0;
    static int total = 0;
    static Window ventana;

    static Object campo(Object o, String n) throws Exception {
        Field f = o.getClass().getDeclaredField(n);
        f.setAccessible(true);
        return f.get(o);
    }

    static void ui(Runnable r) throws Exception { SwingUtilities.invokeAndWait(r); }

    static void pausa(int ms) { try { Thread.sleep(ms); } catch (Exception e) {} }

    static JOptionPane buscar(Container c) {
        for (Component k : c.getComponents()) {
            if (k instanceof JOptionPane) return (JOptionPane) k;
            if (k instanceof Container) { JOptionPane p = buscar((Container) k); if (p != null) return p; }
        }
        return null;
    }

    static JOptionPane panelAbierto() {
        for (Window w : Window.getWindows()) {
            if (w.isShowing() && w instanceof JDialog) {
                JOptionPane p = buscar(((JDialog) w).getContentPane());
                if (p != null) return p;
            }
        }
        return null;
    }

    static void captura(String nombre) throws Exception {
        pausa(400);
        Rectangle r = ventana.getBounds();
        for (Window w : Window.getWindows()) {
            if (w.isShowing() && w instanceof JDialog) r = r.union(w.getBounds());
        }
        BufferedImage img = new Robot().createScreenCapture(r);
        ImageIO.write(img, "png", new File(out + nombre));
    }

    // Pulsa el boton; si aparece un JOptionPane lo captura (si se pide), lo responde y devuelve su mensaje
    static String pulsar(JButton b, String captura, int respuesta) throws Exception {
        SwingUtilities.invokeLater(b::doClick);
        for (int i = 0; i < 40; i++) {
            pausa(100);
            JOptionPane p = panelAbierto();
            if (p != null) {
                String m = String.valueOf(p.getMessage()).replace("\n", " ");
                if (captura != null) captura(captura);
                SwingUtilities.invokeAndWait(() -> p.setValue(respuesta));
                pausa(300);
                return m;
            }
        }
        return "(sin mensaje)";
    }

    static String pulsar(JButton b, String captura) throws Exception {
        return pulsar(b, captura, JOptionPane.OK_OPTION);
    }

    static void ok(String prueba, String obtenido, boolean bien) {
        total++;
        System.out.println((bien ? "  OK    " : "  FALLA ") + prueba + " -> " + obtenido);
        if (!bien) fallos++;
    }

    static JTextField txt(String n) throws Exception { return (JTextField) campo(ventana, n); }
    static JButton btn(String n) throws Exception { return (JButton) campo(ventana, n); }

    static void escribir(String n, String valor) throws Exception {
        JTextComponent_set(n, valor);
    }

    static void JTextComponent_set(String n, String valor) throws Exception {
        javax.swing.text.JTextComponent c = (javax.swing.text.JTextComponent) campo(ventana, n);
        ui(() -> c.setText(valor));
    }

    static String leer(String n) throws Exception {
        return ((javax.swing.text.JTextComponent) campo(ventana, n)).getText();
    }

    static Window ventanaMostrada(String clase) {
        for (int i = 0; i < 40; i++) {
            for (Window w : Window.getWindows()) {
                if (w.isShowing() && w.getClass().getName().equals(clase)) return w;
            }
            pausa(100);
        }
        return null;
    }

    static boolean enTabla(String codigo) throws Exception {
        JTable t = (JTable) campo(ventana, "tblEventos");
        for (int i = 0; i < t.getRowCount(); i++) {
            if (codigo.equals(String.valueOf(t.getValueAt(i, 1)))) return true;
        }
        return false;
    }

    static void login(String usuario, String clave) throws Exception {
        escribir("txtUsuario", usuario);
        JPasswordField p = (JPasswordField) campo(ventana, "txtClave");
        ui(() -> p.setText(clave));
    }

    static void llenarEvento(String codigo, String titulo) throws Exception {
        escribir("txtCodigo", codigo);
        escribir("txtTitulo", titulo);
        escribir("txtLugar", "Sala de pruebas");
        escribir("txtLatitud", "-33.4489");
        escribir("txtLongitud", "-70.6693");
        escribir("txtDetalle", "{\"nota\": \"prueba gui\"}");
        escribir("txtFecha", "2026-12-20");
        escribir("txtHoraInicio", "10:30");
        escribir("txtHoraFin", "11:30");
    }

    static void limpiarBD() throws Exception {
        try (java.sql.Connection con = conexion.Conexion.conectar();
                java.sql.PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM evento WHERE codigo_evento LIKE 'TEST-GUI-%'")) {
            ps.executeUpdate();
        }
    }

    public static void main(String[] a) throws Exception {
        new File(out).mkdirs();
        dao.EventoDAO dao = new dao.EventoDAO();
        limpiarBD();
        try {
            SwingUtilities.invokeLater(() -> vista.VentanaLogin.main(new String[0]));
            ventana = ventanaMostrada("vista.VentanaLogin");
            ((JFrame) ventana).setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            captura("01-login.png");

            // V1: campos vacios
            String m = pulsar(btn("btnIngresar"), "02-v1-credenciales.png");
            ok("V1 campos vacios", m, m.equals("Usuario o contraseña incorrectos."));
            // V1: clave mala
            login("admin", "mala");
            m = pulsar(btn("btnIngresar"), null);
            ok("V1 clave incorrecta", m, m.equals("Usuario o contraseña incorrectos."));
            // V2: inactivo
            login("inactivo1", "Inac2026");
            m = pulsar(btn("btnIngresar"), "03-v2-inactivo.png");
            ok("V2 usuario inactivo", m, m.equals("Usuario inactivo."));

            // login correcto
            login("admin", "Admin2026");
            SwingUtilities.invokeLater(btn("btnIngresar")::doClick);
            Window login = ventana;
            ventana = ventanaMostrada("vista.VentanaEventos");
            ok("Login admin abre VentanaEventos", String.valueOf(ventana != null), ventana != null && !login.isShowing());
            ((JFrame) ventana).setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            pausa(500);
            String bienvenida = ((JLabel) campo(ventana, "lblBienvenida")).getText();
            ok("Etiqueta de bienvenida", bienvenida, bienvenida.contains("(") && bienvenida.contains("Bienvenido"));
            JTable tabla = (JTable) campo(ventana, "tblEventos");
            ok("C2 tabla con 5 o mas filas", tabla.getRowCount() + " filas", tabla.getRowCount() >= 5);
            captura("04-eventos-tabla.png");
            int filasAntes = tabla.getRowCount();

            // validaciones
            m = pulsar(btn("btnGuardar"), null);
            ok("Valida codigo obligatorio", m, m.contains("código"));
            escribir("txtCodigo", "TEST-GUI-0");
            escribir("txtTitulo", "x");
            escribir("txtFecha", "20-12-2026");
            m = pulsar(btn("btnGuardar"), null);
            ok("Valida formato de fecha", m, m.contains("yyyy-MM-dd"));
            escribir("txtFecha", "2026-12-20");
            escribir("txtHoraInicio", "10:30");
            escribir("txtLatitud", "-33.4");
            m = pulsar(btn("btnGuardar"), null);
            ok("Valida latitud sin longitud", m, m.contains("longitud"));
            m = pulsar(btn("btnActualizar"), null);
            ok("Actualizar sin evento seleccionado", m, m.contains("Primero busque"));
            pulsar(btn("btnLimpiar"), null);

            // C1 insertar
            llenarEvento("TEST-GUI-1", "Evento de prueba GUI");
            m = pulsar(btn("btnGuardar"), null);
            ok("C1 insertar", m, m.contains("guardado"));
            ok("C1 aparece en la tabla", tabla.getRowCount() + " filas", enTabla("TEST-GUI-1") && tabla.getRowCount() == filasAntes + 1);
            modelo.Evento e = dao.buscar("TEST-GUI-1");
            ok("C1 existe en la BD", e == null ? "null" : e.getTitulo(), e != null && e.getLatitud() != null);

            // V3: codigo repetido
            m = pulsar(btn("btnGuardar"), "05-v3-repetido.png");
            ok("V3 codigo repetido", m, m.equals("El registro ya existe."));
            ok("Ventana sigue abierta tras el error", String.valueOf(ventana.isShowing()), ventana.isShowing());

            // C3 buscar
            pulsar(btn("btnLimpiar"), null);
            escribir("txtCodigo", "TEST-GUI-1");
            pulsar(btn("btnBuscar"), null);
            pausa(300);
            ok("C3 buscar carga el formulario", leer("txtTitulo"), leer("txtTitulo").equals("Evento de prueba GUI")
                    && leer("txtLugar").equals("Sala de pruebas"));
            escribir("txtCodigo", "NO-EXISTE-99");
            m = pulsar(btn("btnBuscar"), null);
            ok("C3 buscar inexistente", m, m.equals("No existe un evento con ese código."));

            // clic en una fila de la tabla
            int fila = -1;
            for (int i = 0; i < tabla.getRowCount(); i++) {
                if ("TEST-GUI-1".equals(String.valueOf(tabla.getValueAt(i, 1)))) fila = i;
            }
            final int f = fila;
            pulsar(btn("btnLimpiar"), null);
            ui(() -> tabla.setRowSelectionInterval(f, f));
            Rectangle celda = tabla.getCellRect(f, 1, true);
            Point p = celda.getLocation();
            MouseEvent_click(tabla, celda.x + 5, celda.y + 5);
            pausa(500);
            ok("Clic en fila carga el formulario", leer("txtCodigo"), leer("txtCodigo").equals("TEST-GUI-1"));

            // C4 actualizar
            escribir("txtTitulo", "Titulo actualizado GUI");
            m = pulsar(btn("btnActualizar"), "06-eventos-actualizado.png");
            e = dao.buscar("TEST-GUI-1");
            ok("C4 actualizar", m + " / BD: " + (e == null ? "null" : e.getTitulo()),
                    m.contains("actualizado") && e != null && e.getTitulo().equals("Titulo actualizado GUI"));

            // C5 eliminar: primero que diga No, luego Si
            m = pulsar(btn("btnEliminar"), null, JOptionPane.NO_OPTION);
            ok("C5 confirmacion (No) no elimina", m, m.contains("¿Está seguro") && dao.buscar("TEST-GUI-1") != null);
            m = pulsar(btn("btnEliminar"), null, JOptionPane.YES_OPTION);
            ok("C5 confirmacion (Si) elimina", m, m.contains("¿Está seguro"));
            pausa(500);
            JOptionPane extra = panelAbierto();
            if (extra != null) {
                SwingUtilities.invokeAndWait(() -> extra.setValue(JOptionPane.OK_OPTION));
                pausa(300);
            }
            ok("C5 ya no esta en la BD ni en la tabla", String.valueOf(dao.buscar("TEST-GUI-1") == null),
                    dao.buscar("TEST-GUI-1") == null && !enTabla("TEST-GUI-1") && tabla.getRowCount() == filasAntes);

            // cerrar sesion vuelve al login
            SwingUtilities.invokeLater(btn("btnCerrarSesion")::doClick);
            Window w2 = ventanaMostrada("vista.VentanaLogin");
            ok("Cerrar sesion vuelve al login", String.valueOf(w2 != null), w2 != null && !ventana.isShowing());
        } finally {
            limpiarBD();
        }
        System.out.println(fallos == 0 ? "TODO OK " + (total - fallos) + "/" + total
                : fallos + " FALLO(S) de " + total);
        System.exit(fallos == 0 ? 0 : 1);
    }

    static void MouseEvent_click(JTable t, int x, int y) throws Exception {
        ui(() -> {
            long ahora = System.currentTimeMillis();
            t.dispatchEvent(new java.awt.event.MouseEvent(t, java.awt.event.MouseEvent.MOUSE_PRESSED, ahora, 0, x, y, 1, false));
            t.dispatchEvent(new java.awt.event.MouseEvent(t, java.awt.event.MouseEvent.MOUSE_RELEASED, ahora, 0, x, y, 1, false));
            t.dispatchEvent(new java.awt.event.MouseEvent(t, java.awt.event.MouseEvent.MOUSE_CLICKED, ahora, 0, x, y, 1, false));
        });
    }
}
