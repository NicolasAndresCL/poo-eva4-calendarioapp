package conexion;

import java.sql.SQLException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Pruebas de la clase Conexion. testProbarConexion necesita MySQL arriba.
 */
public class ConexionTest {

    @Test
    public void testProbarConexion() {
        assertTrue("MySQL debe estar arriba (poo-mysql)", Conexion.probarConexion());
    }

    @Test
    public void testMensajeConexionCaida() {
        SQLException e = new SQLException("Communications link failure", "08S01", 0);
        assertEquals("No fue posible conectar con la base de datos.", Conexion.mensajeError(e));
    }

    @Test
    public void testMensajeClaveDeBaseIncorrecta() {
        SQLException e = new SQLException("Access denied", "28000", 1045);
        assertEquals("No fue posible conectar con la base de datos.", Conexion.mensajeError(e));
    }

    @Test
    public void testMensajeRegistroDuplicado() {
        SQLException e = new SQLException("Duplicate entry", "23000", 1062);
        assertEquals("El registro ya existe.", Conexion.mensajeError(e));
    }
}
