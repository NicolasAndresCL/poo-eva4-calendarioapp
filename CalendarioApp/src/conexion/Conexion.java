package conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centraliza la conexion a MySQL: los datos de acceso viven solo aqui.
 */
public class Conexion {

    private static final String URL = "jdbc:mysql://localhost:3306/calendario_app"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Santiago";
    private static final String USUARIO = "root";
    private static final String CLAVE = "";

    /**
     * Abre una conexion nueva. Quien la pide la cierra (try-with-resources).
     */
    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }

    /**
     * Devuelve true si MySQL responde con los datos configurados.
     */
    public static boolean probarConexion() {
        try (Connection con = conectar()) {
            return con.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Traduce una SQLException a un mensaje claro para el usuario (V3 y V4).
     */
    public static String mensajeError(SQLException e) {
        String estado = e.getSQLState();
        int codigo = e.getErrorCode();
        if ((estado != null && estado.startsWith("08")) || codigo == 1045 || codigo == 1049) {
            return "No fue posible conectar con la base de datos.";
        }
        if (codigo == 1062) {
            return "El registro ya existe.";
        }
        if (codigo == 4025 || codigo == 3819 || codigo == 3140) {
            return "Los datos no cumplen las reglas de la tabla (prioridad, horas, duracion o JSON).";
        }
        return "Error de base de datos: " + e.getMessage();
    }
}
