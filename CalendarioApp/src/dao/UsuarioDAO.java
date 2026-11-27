package dao;

import conexion.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import modelo.Usuario;

/**
 * Autenticacion contra la tabla usuario (L1, V1 y V2).
 */
public class UsuarioDAO {

    /**
     * Devuelve el usuario si las credenciales son correctas y esta activo; si no, null.
     */
    public Usuario validar(String nombreUsuario, String clave) throws SQLException {
        String sql = "SELECT id_usuario, nombre_usuario, clave, nombre_completo, rol, activo "
                + "FROM usuario WHERE nombre_usuario = ? AND clave = ? AND activo = TRUE";
        try (Connection con = Conexion.conectar();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            ps.setString(2, clave);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Usuario(
                            rs.getInt("id_usuario"),
                            rs.getString("nombre_usuario"),
                            rs.getString("clave"),
                            rs.getString("nombre_completo"),
                            rs.getString("rol"),
                            rs.getBoolean("activo"));
                }
                return null;
            }
        }
    }

    /**
     * True si las credenciales son correctas pero el usuario esta inactivo (V2).
     */
    public boolean estaInactivo(String nombreUsuario, String clave) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuario "
                + "WHERE nombre_usuario = ? AND clave = ? AND activo = FALSE";
        try (Connection con = Conexion.conectar();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            ps.setString(2, clave);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }
}
