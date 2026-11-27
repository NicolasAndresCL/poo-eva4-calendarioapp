package modelo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Una fila de la tabla evento.
 * La columna POINT se maneja como latitud y longitud; la columna JSON como texto.
 */
public class Evento {

    private int idEvento;
    private String codigoEvento;
    private String titulo;
    private String lugar;
    private Double latitud;
    private Double longitud;
    private String detalle;
    private String categoria;
    private LocalDate fechaEvento;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private int duracionMinutos;
    private int prioridad;
    private boolean activo;

    public Evento() {
        categoria = "personal";
        duracionMinutos = 60;
        prioridad = 2;
        activo = true;
    }

    public Evento(int idEvento, String codigoEvento, String titulo, String lugar,
            Double latitud, Double longitud, String detalle, String categoria,
            LocalDate fechaEvento, LocalTime horaInicio, LocalTime horaFin,
            int duracionMinutos, int prioridad, boolean activo) {
        this.idEvento = idEvento;
        this.codigoEvento = codigoEvento;
        this.titulo = titulo;
        this.lugar = lugar;
        this.latitud = latitud;
        this.longitud = longitud;
        this.detalle = detalle;
        this.categoria = categoria;
        this.fechaEvento = fechaEvento;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.duracionMinutos = duracionMinutos;
        this.prioridad = prioridad;
        this.activo = activo;
    }

    /**
     * True si el evento tiene coordenadas completas para la columna POINT.
     */
    public boolean tieneUbicacion() {
        return latitud != null && longitud != null;
    }

    public int getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(int idEvento) {
        this.idEvento = idEvento;
    }

    public String getCodigoEvento() {
        return codigoEvento;
    }

    public void setCodigoEvento(String codigoEvento) {
        this.codigoEvento = codigoEvento;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getLugar() {
        return lugar;
    }

    public void setLugar(String lugar) {
        this.lugar = lugar;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public LocalDate getFechaEvento() {
        return fechaEvento;
    }

    public void setFechaEvento(LocalDate fechaEvento) {
        this.fechaEvento = fechaEvento;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(int duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(int prioridad) {
        this.prioridad = prioridad;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
