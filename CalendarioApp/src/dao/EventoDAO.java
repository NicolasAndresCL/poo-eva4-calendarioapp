package dao;

import conexion.Conexion;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.util.ArrayList;
import modelo.Evento;

/**
 * CRUD de la tabla evento (C1 a C5). Todas las sentencias usan PreparedStatement.
 * La columna POINT se escribe con POINT(longitud, latitud) y se lee con ST_X / ST_Y.
 */
public class EventoDAO {

    private static final String COLUMNAS = "id_evento, codigo_evento, titulo, lugar, "
            + "ST_Y(ubicacion) AS latitud, ST_X(ubicacion) AS longitud, detalle, categoria, "
            + "fecha_evento, hora_inicio, hora_fin, duracion_minutos, prioridad, activo";

    /**
     * C1: guarda un evento nuevo. Devuelve el id generado por AUTO_INCREMENT.
     */
    public int insertar(Evento e) throws SQLException {
        String sql = "INSERT INTO evento (codigo_evento, titulo, lugar, ubicacion, detalle, "
                + "categoria, fecha_evento, hora_inicio, hora_fin, duracion_minutos, prioridad, activo) "
                + "VALUES (?, ?, ?, POINT(?, ?), ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = Conexion.conectar();
                PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            cargarParametros(ps, e);
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    e.setIdEvento(claves.getInt(1));
                }
            }
            return e.getIdEvento();
        }
    }

    /**
     * C2: todos los eventos, en orden cronologico.
     */
    public ArrayList<Evento> listar() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM evento ORDER BY fecha_evento, hora_inicio";
        ArrayList<Evento> lista = new ArrayList<>();
        try (Connection con = Conexion.conectar();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(leerFila(rs));
            }
        }
        return lista;
    }

    /**
     * C3: busca por el identificador de negocio. Devuelve null si no existe.
     */
    public Evento buscar(String codigoEvento) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM evento WHERE codigo_evento = ?";
        try (Connection con = Conexion.conectar();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigoEvento);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return leerFila(rs);
                }
                return null;
            }
        }
    }

    /**
     * C4: modifica el evento con ese id. Devuelve true si cambio una fila.
     */
    public boolean actualizar(Evento e) throws SQLException {
        String sql = "UPDATE evento SET codigo_evento = ?, titulo = ?, lugar = ?, "
                + "ubicacion = POINT(?, ?), detalle = ?, categoria = ?, fecha_evento = ?, "
                + "hora_inicio = ?, hora_fin = ?, duracion_minutos = ?, prioridad = ?, activo = ? "
                + "WHERE id_evento = ?";
        try (Connection con = Conexion.conectar();
                PreparedStatement ps = con.prepareStatement(sql)) {
            cargarParametros(ps, e);
            ps.setInt(14, e.getIdEvento());
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * C5: elimina el evento con ese id. Devuelve true si borro una fila.
     */
    public boolean eliminar(int idEvento) throws SQLException {
        String sql = "DELETE FROM evento WHERE id_evento = ?";
        try (Connection con = Conexion.conectar();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idEvento);
            return ps.executeUpdate() == 1;
        }
    }

    // Parametros 1 a 13, en el mismo orden para INSERT y UPDATE.
    private void cargarParametros(PreparedStatement ps, Evento e) throws SQLException {
        ps.setString(1, e.getCodigoEvento());
        ps.setString(2, e.getTitulo());
        ps.setString(3, e.getLugar());
        if (e.tieneUbicacion()) {
            ps.setDouble(4, e.getLongitud());
            ps.setDouble(5, e.getLatitud());
        } else {
            ps.setNull(4, Types.DOUBLE);
            ps.setNull(5, Types.DOUBLE);
        }
        ps.setString(6, e.getDetalle());
        ps.setString(7, e.getCategoria());
        ps.setDate(8, Date.valueOf(e.getFechaEvento()));
        ps.setTime(9, Time.valueOf(e.getHoraInicio()));
        if (e.getHoraFin() != null) {
            ps.setTime(10, Time.valueOf(e.getHoraFin()));
        } else {
            ps.setNull(10, Types.TIME);
        }
        ps.setInt(11, e.getDuracionMinutos());
        ps.setInt(12, e.getPrioridad());
        ps.setBoolean(13, e.isActivo());
    }

    private Evento leerFila(ResultSet rs) throws SQLException {
        Double latitud = rs.getDouble("latitud");
        if (rs.wasNull()) {
            latitud = null;
        }
        Double longitud = rs.getDouble("longitud");
        if (rs.wasNull()) {
            longitud = null;
        }
        Time fin = rs.getTime("hora_fin");
        return new Evento(
                rs.getInt("id_evento"),
                rs.getString("codigo_evento"),
                rs.getString("titulo"),
                rs.getString("lugar"),
                latitud,
                longitud,
                rs.getString("detalle"),
                rs.getString("categoria"),
                rs.getDate("fecha_evento").toLocalDate(),
                rs.getTime("hora_inicio").toLocalTime(),
                fin == null ? null : fin.toLocalTime(),
                rs.getInt("duracion_minutos"),
                rs.getInt("prioridad"),
                rs.getBoolean("activo"));
    }
}
