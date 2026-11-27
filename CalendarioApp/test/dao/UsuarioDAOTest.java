package dao;

import modelo.Usuario;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Pruebas del login (L1, V1, V2) contra la tabla usuario del script.
 */
public class UsuarioDAOTest {

    private final UsuarioDAO dao = new UsuarioDAO();

    @Test
    public void testValidarAdminActivo() throws Exception {
        Usuario u = dao.validar("admin", "Admin2026");
        assertNotNull(u);
        assertEquals("ADMIN", u.getRol());
        assertTrue(u.isActivo());
    }

    @Test
    public void testValidarClaveIncorrectaDevuelveNull() throws Exception {
        assertNull(dao.validar("admin", "otra"));
        assertFalse(dao.estaInactivo("admin", "otra"));
    }

    @Test
    public void testClaveDistingueMayusculas() throws Exception {
        assertNull(dao.validar("admin", "ADMIN2026"));
        assertNull(dao.validar("admin", "admin2026"));
    }

    @Test
    public void testValidarCamposVaciosDevuelveNull() throws Exception {
        assertNull(dao.validar("", ""));
    }

    @Test
    public void testUsuarioInactivoNoIngresa() throws Exception {
        assertNull(dao.validar("inactivo1", "Inac2026"));
        assertTrue(dao.estaInactivo("inactivo1", "Inac2026"));
    }

    @Test
    public void testInyeccionSqlNoEntra() throws Exception {
        assertNull(dao.validar("admin' -- ", "cualquiera"));
        assertNull(dao.validar("admin", "' OR '1'='1"));
    }
}
