package modelo;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Pruebas del modelo Evento (no necesitan base de datos).
 */
public class EventoTest {

    @Test
    public void testConstructorVacioUsaLosDefaultDeLaTabla() {
        Evento e = new Evento();
        assertEquals("personal", e.getCategoria());
        assertEquals(60, e.getDuracionMinutos());
        assertEquals(2, e.getPrioridad());
        assertTrue(e.isActivo());
    }

    @Test
    public void testTieneUbicacionSoloConLatitudYLongitud() {
        Evento e = new Evento();
        assertFalse(e.tieneUbicacion());
        e.setLatitud(-33.4378);
        assertFalse(e.tieneUbicacion());
        e.setLongitud(-70.6506);
        assertTrue(e.tieneUbicacion());
    }

    @Test
    public void testConstructorCompletoYGetters() {
        Evento e = new Evento(7, "EVT-0007", "Estudiar JDBC", "Biblioteca",
                -33.4, -70.6, "{\"notas\": \"capitulo 3\"}", "estudio",
                LocalDate.of(2026, 11, 29), LocalTime.of(15, 0), LocalTime.of(17, 0),
                120, 1, true);
        assertEquals(7, e.getIdEvento());
        assertEquals("EVT-0007", e.getCodigoEvento());
        assertEquals(LocalDate.of(2026, 11, 29), e.getFechaEvento());
        assertEquals(LocalTime.of(17, 0), e.getHoraFin());
        assertEquals(120, e.getDuracionMinutos());
    }
}
