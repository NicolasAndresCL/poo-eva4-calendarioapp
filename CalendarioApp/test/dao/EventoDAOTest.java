package dao;

import conexion.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import modelo.Evento;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Pruebas del CRUD (C1 a C5, V3) contra la tabla evento.
 * Usan codigos TEST-JU-* y los borran al terminar cada prueba.
 */
public class EventoDAOTest {

    private final EventoDAO dao = new EventoDAO();

    @After
    public void tearDown() throws SQLException {
        try (Connection con = Conexion.conectar();
                PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM evento WHERE codigo_evento LIKE ?")) {
            ps.setString(1, "TEST-JU-%");
            ps.executeUpdate();
        }
    }

    private Evento nuevo(String codigo) {
        Evento e = new Evento();
        e.setCodigoEvento(codigo);
        e.setTitulo("Prueba JUnit");
        e.setLugar("Sala 1");
        e.setLatitud(-33.4378);
        e.setLongitud(-70.6506);
        e.setDetalle("{\"notas\": \"creado por JUnit\"}");
        e.setCategoria("estudio");
        e.setFechaEvento(LocalDate.of(2026, 12, 10));
        e.setHoraInicio(LocalTime.of(10, 0));
        e.setHoraFin(LocalTime.of(11, 0));
        return e;
    }

    @Test
    public void testListarTraeLosRegistrosDelScript() throws Exception {
        ArrayList<Evento> lista = dao.listar();
        assertTrue(lista.size() >= 5);
    }

    @Test
    public void testInsertarYBuscar() throws Exception {
        int id = dao.insertar(nuevo("TEST-JU-1"));
        assertTrue(id > 0);
        Evento leido = dao.buscar("TEST-JU-1");
        assertNotNull(leido);
        assertEquals(id, leido.getIdEvento());
        assertEquals("Prueba JUnit", leido.getTitulo());
        assertEquals(LocalTime.of(11, 0), leido.getHoraFin());
    }

    @Test
    public void testPointSeGuardaYSeLee() throws Exception {
        dao.insertar(nuevo("TEST-JU-2"));
        Evento leido = dao.buscar("TEST-JU-2");
        assertEquals(-33.4378, leido.getLatitud(), 0.00001);
        assertEquals(-70.6506, leido.getLongitud(), 0.00001);
    }

    @Test
    public void testSinUbicacionNiHoraFinQuedanNull() throws Exception {
        Evento e = nuevo("TEST-JU-3");
        e.setLatitud(null);
        e.setLongitud(null);
        e.setHoraFin(null);
        dao.insertar(e);
        Evento leido = dao.buscar("TEST-JU-3");
        assertNull(leido.getLatitud());
        assertNull(leido.getLongitud());
        assertNull(leido.getHoraFin());
    }

    @Test
    public void testJsonSeGuardaYSeLee() throws Exception {
        dao.insertar(nuevo("TEST-JU-4"));
        assertTrue(dao.buscar("TEST-JU-4").getDetalle().contains("creado por JUnit"));
    }

    @Test
    public void testCodigoRepetidoNoSeInserta() throws Exception {
        dao.insertar(nuevo("TEST-JU-5"));
        try {
            dao.insertar(nuevo("TEST-JU-5"));
            fail("Debio lanzar SQLException por UNIQUE");
        } catch (SQLException e) {
            assertEquals("El registro ya existe.", Conexion.mensajeError(e));
        }
    }

    @Test(expected = SQLException.class)
    public void testJsonInvalidoLoRechazaLaBase() throws Exception {
        Evento e = nuevo("TEST-JU-6");
        e.setDetalle("esto no es json");
        dao.insertar(e);
    }

    @Test(expected = SQLException.class)
    public void testPrioridadFueraDeRangoLaRechazaLaBase() throws Exception {
        Evento e = nuevo("TEST-JU-7");
        e.setPrioridad(7);
        dao.insertar(e);
    }

    @Test
    public void testBuscarInexistenteDevuelveNull() throws Exception {
        assertNull(dao.buscar("NO-EXISTE"));
    }

    @Test
    public void testActualizar() throws Exception {
        int id = dao.insertar(nuevo("TEST-JU-8"));
        Evento e = dao.buscar("TEST-JU-8");
        e.setTitulo("Titulo cambiado");
        e.setPrioridad(3);
        assertTrue(dao.actualizar(e));
        Evento leido = dao.buscar("TEST-JU-8");
        assertEquals(id, leido.getIdEvento());
        assertEquals("Titulo cambiado", leido.getTitulo());
        assertEquals(3, leido.getPrioridad());
    }

    @Test
    public void testEliminar() throws Exception {
        int id = dao.insertar(nuevo("TEST-JU-9"));
        assertTrue(dao.eliminar(id));
        assertNull(dao.buscar("TEST-JU-9"));
        assertFalse(dao.eliminar(id));
    }
}
