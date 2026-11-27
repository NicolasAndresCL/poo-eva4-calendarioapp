/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package vista;

import conexion.Conexion;
import dao.EventoDAO;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import modelo.Evento;
import modelo.Usuario;

/**
 * Evaluacion IV - Calendario de eventos. CRUD de eventos (C1 a C5) sobre la tabla evento.
 *
 * @author Nicolas Cano Leal
 */
public class VentanaEventos extends javax.swing.JFrame {

    private Usuario usuario;
    private EventoDAO eventoDAO;
    // Id del evento cargado en el formulario (0 = ninguno); lo usan Actualizar y Eliminar
    private int idSeleccionado;

    public VentanaEventos(Usuario usuario) {
        initComponents();
        getContentPane().setBackground(new java.awt.Color(238, 242, 247));
        this.usuario = usuario;
        eventoDAO = new EventoDAO();
        idSeleccionado = 0;
        lblBienvenida.setText("Bienvenido/a " + usuario.getNombreCompleto() + " (" + usuario.getRol() + ")");
        chkActivo.setSelected(true);
        cboPrioridad.setSelectedIndex(1);
        tblEventos.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                tblEventosClicked();
            }
        });
        this.setLocationRelativeTo(null);
        cargarTabla();
    }

    private void mostrarError(String mensaje, javax.swing.JComponent campo) {
        campo.requestFocus();
        JOptionPane.showMessageDialog(this, mensaje, "Dato a corregir", JOptionPane.WARNING_MESSAGE);
        campo.requestFocus();
    }

    private void mostrarErrorBD(SQLException e) {
        JOptionPane.showMessageDialog(this, Conexion.mensajeError(e), "Error de base de datos",
                JOptionPane.ERROR_MESSAGE);
    }

    // C2: vuelve a leer todos los eventos y llena la tabla
    private void cargarTabla() {
        DefaultTableModel modelo = (DefaultTableModel) tblEventos.getModel();
        modelo.setRowCount(0);
        try {
            ArrayList<Evento> lista = eventoDAO.listar();
            for (int i = 0; i < lista.size(); i++) {
                Evento e = lista.get(i);
                modelo.addRow(new Object[]{e.getIdEvento(), e.getCodigoEvento(), e.getTitulo(),
                    e.getFechaEvento(), e.getHoraInicio(), e.getCategoria(), e.getPrioridad(),
                    e.isActivo() ? "Sí" : "No", e.getLugar()});
            }
        } catch (SQLException e) {
            mostrarErrorBD(e);
        }
    }

    private void limpiarFormulario() {
        idSeleccionado = 0;
        txtCodigo.setText("");
        txtTitulo.setText("");
        txtLugar.setText("");
        txtLatitud.setText("");
        txtLongitud.setText("");
        txtDetalle.setText("");
        cboCategoria.setSelectedIndex(0);
        txtFecha.setText("");
        txtHoraInicio.setText("");
        txtHoraFin.setText("");
        spnDuracion.setValue(60);
        cboPrioridad.setSelectedIndex(1);
        chkActivo.setSelected(true);
        tblEventos.clearSelection();
        txtCodigo.requestFocus();
    }

    private void cargarFormulario(Evento e) {
        idSeleccionado = e.getIdEvento();
        txtCodigo.setText(e.getCodigoEvento());
        txtTitulo.setText(e.getTitulo());
        txtLugar.setText(e.getLugar() == null ? "" : e.getLugar());
        txtLatitud.setText(e.getLatitud() == null ? "" : String.valueOf(e.getLatitud()));
        txtLongitud.setText(e.getLongitud() == null ? "" : String.valueOf(e.getLongitud()));
        txtDetalle.setText(e.getDetalle() == null ? "" : e.getDetalle());
        cboCategoria.setSelectedItem(e.getCategoria());
        txtFecha.setText(e.getFechaEvento().toString());
        txtHoraInicio.setText(e.getHoraInicio().toString());
        txtHoraFin.setText(e.getHoraFin() == null ? "" : e.getHoraFin().toString());
        spnDuracion.setValue(e.getDuracionMinutos());
        cboPrioridad.setSelectedItem(String.valueOf(e.getPrioridad()));
        chkActivo.setSelected(e.isActivo());
    }

    // Valida el formulario; si todo esta bien devuelve el evento armado, si no avisa y devuelve null
    private Evento leerFormulario() {
        String codigo = txtCodigo.getText().trim();
        if (codigo.isEmpty()) {
            mostrarError("Debe ingresar el código del evento.", txtCodigo);
            return null;
        }
        String titulo = txtTitulo.getText().trim();
        if (titulo.isEmpty()) {
            mostrarError("Debe ingresar el título del evento.", txtTitulo);
            return null;
        }

        LocalDate fecha;
        try {
            fecha = LocalDate.parse(txtFecha.getText().trim());
        } catch (DateTimeParseException ex) {
            mostrarError("La fecha debe tener el formato yyyy-MM-dd (ej: 2026-12-15).", txtFecha);
            return null;
        }
        LocalTime horaInicio;
        try {
            horaInicio = LocalTime.parse(txtHoraInicio.getText().trim());
        } catch (DateTimeParseException ex) {
            mostrarError("La hora de inicio debe tener el formato HH:mm (ej: 09:30).", txtHoraInicio);
            return null;
        }
        LocalTime horaFin = null;
        if (!txtHoraFin.getText().trim().isEmpty()) {
            try {
                horaFin = LocalTime.parse(txtHoraFin.getText().trim());
            } catch (DateTimeParseException ex) {
                mostrarError("La hora de fin debe tener el formato HH:mm (ej: 10:30) o quedar vacía.", txtHoraFin);
                return null;
            }
        }

        // Latitud y longitud: o vienen las dos o no viene ninguna
        Double latitud = null;
        Double longitud = null;
        String textoLat = txtLatitud.getText().trim();
        String textoLon = txtLongitud.getText().trim();
        if (!textoLat.isEmpty() || !textoLon.isEmpty()) {
            if (textoLat.isEmpty()) {
                mostrarError("Si ingresa la longitud, también debe ingresar la latitud.", txtLatitud);
                return null;
            }
            if (textoLon.isEmpty()) {
                mostrarError("Si ingresa la latitud, también debe ingresar la longitud.", txtLongitud);
                return null;
            }
            try {
                latitud = Double.valueOf(textoLat);
            } catch (NumberFormatException ex) {
                mostrarError("La latitud debe ser un número (ej: -33.4489).", txtLatitud);
                return null;
            }
            try {
                longitud = Double.valueOf(textoLon);
            } catch (NumberFormatException ex) {
                mostrarError("La longitud debe ser un número (ej: -70.6693).", txtLongitud);
                return null;
            }
        }

        Evento e = new Evento();
        e.setCodigoEvento(codigo);
        e.setTitulo(titulo);
        String lugar = txtLugar.getText().trim();
        e.setLugar(lugar.isEmpty() ? null : lugar);
        e.setLatitud(latitud);
        e.setLongitud(longitud);
        String detalle = txtDetalle.getText().trim();
        e.setDetalle(detalle.isEmpty() ? null : detalle);
        e.setCategoria((String) cboCategoria.getSelectedItem());
        e.setFechaEvento(fecha);
        e.setHoraInicio(horaInicio);
        e.setHoraFin(horaFin);
        e.setDuracionMinutos((Integer) spnDuracion.getValue());
        e.setPrioridad(Integer.parseInt((String) cboPrioridad.getSelectedItem()));
        e.setActivo(chkActivo.isSelected());
        return e;
    }

    // Clic en una fila: se busca el evento por su codigo y se carga en el formulario
    private void tblEventosClicked() {
        int fila = tblEventos.getSelectedRow();
        if (fila < 0) {
            return;
        }
        String codigo = String.valueOf(tblEventos.getValueAt(fila, 1));
        try {
            Evento e = eventoDAO.buscar(codigo);
            if (e != null) {
                cargarFormulario(e);
            }
        } catch (SQLException ex) {
            mostrarErrorBD(ex);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblEncabezado = new javax.swing.JLabel();
        lblBienvenida = new javax.swing.JLabel();
        pnlFormulario = new javax.swing.JPanel();
        lblCodigo = new javax.swing.JLabel();
        txtCodigo = new javax.swing.JTextField();
        lblTitulo = new javax.swing.JLabel();
        txtTitulo = new javax.swing.JTextField();
        lblLugar = new javax.swing.JLabel();
        txtLugar = new javax.swing.JTextField();
        lblCategoria = new javax.swing.JLabel();
        cboCategoria = new javax.swing.JComboBox<>();
        lblLatitud = new javax.swing.JLabel();
        txtLatitud = new javax.swing.JTextField();
        lblLongitud = new javax.swing.JLabel();
        txtLongitud = new javax.swing.JTextField();
        lblFecha = new javax.swing.JLabel();
        txtFecha = new javax.swing.JTextField();
        lblHoraInicio = new javax.swing.JLabel();
        txtHoraInicio = new javax.swing.JTextField();
        lblHoraFin = new javax.swing.JLabel();
        txtHoraFin = new javax.swing.JTextField();
        lblDuracion = new javax.swing.JLabel();
        spnDuracion = new javax.swing.JSpinner();
        lblPrioridad = new javax.swing.JLabel();
        cboPrioridad = new javax.swing.JComboBox<>();
        chkActivo = new javax.swing.JCheckBox();
        lblDetalle = new javax.swing.JLabel();
        scrDetalle = new javax.swing.JScrollPane();
        txtDetalle = new javax.swing.JTextArea();
        btnGuardar = new javax.swing.JButton();
        btnBuscar = new javax.swing.JButton();
        btnActualizar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        btnLimpiar = new javax.swing.JButton();
        btnCerrarSesion = new javax.swing.JButton();
        scrEventos = new javax.swing.JScrollPane();
        tblEventos = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Calendario de eventos");

        lblEncabezado.setBackground(new java.awt.Color(31, 78, 121));
        lblEncabezado.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblEncabezado.setForeground(new java.awt.Color(255, 255, 255));
        lblEncabezado.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblEncabezado.setText("Calendario de eventos");
        lblEncabezado.setOpaque(true);

        lblBienvenida.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblBienvenida.setForeground(new java.awt.Color(31, 78, 121));
        lblBienvenida.setText("Bienvenido/a");

        pnlFormulario.setBackground(new java.awt.Color(238, 242, 247));
        pnlFormulario.setBorder(javax.swing.BorderFactory.createTitledBorder("Datos del evento"));
        lblCodigo.setText("Código:");



        lblTitulo.setText("Título:");



        lblLugar.setText("Lugar:");



        lblCategoria.setText("Categoría:");

        cboCategoria.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "personal", "trabajo", "salud", "estudio" }));

        lblLatitud.setText("Latitud:");



        lblLongitud.setText("Longitud:");



        lblFecha.setText("Fecha:");



        lblHoraInicio.setText("Hora inicio:");



        lblHoraFin.setText("Hora fin:");



        lblDuracion.setText("Duración (min):");

        spnDuracion.setModel(new javax.swing.SpinnerNumberModel(60, 1, 1440, 15));

        lblPrioridad.setText("Prioridad:");

        cboPrioridad.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3" }));

        chkActivo.setText("Activo");
        chkActivo.setBackground(new java.awt.Color(238, 242, 247));

        lblDetalle.setText("Detalle (JSON):");

        txtDetalle.setColumns(20);
        txtDetalle.setRows(3);

        scrDetalle.setViewportView(txtDetalle);


        javax.swing.GroupLayout pnlFormularioLayout = new javax.swing.GroupLayout(pnlFormulario);
        pnlFormulario.setLayout(pnlFormularioLayout);
        pnlFormularioLayout.setHorizontalGroup(
            pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(pnlFormularioLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(pnlFormularioLayout.createSequentialGroup()
                            .addComponent(lblCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(lblTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(pnlFormularioLayout.createSequentialGroup()
                            .addComponent(lblLugar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtLugar, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(lblCategoria, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(cboCategoria, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(pnlFormularioLayout.createSequentialGroup()
                            .addComponent(lblLatitud, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtLatitud, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(lblLongitud, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtLongitud, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(pnlFormularioLayout.createSequentialGroup()
                            .addComponent(lblFecha, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtFecha, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(lblHoraInicio, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtHoraInicio, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(pnlFormularioLayout.createSequentialGroup()
                            .addComponent(lblHoraFin, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtHoraFin, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(lblDuracion, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(spnDuracion, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(pnlFormularioLayout.createSequentialGroup()
                            .addComponent(lblPrioridad, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(cboPrioridad, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(chkActivo, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(pnlFormularioLayout.createSequentialGroup()
                            .addComponent(lblDetalle, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(scrDetalle, javax.swing.GroupLayout.PREFERRED_SIZE, 470, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addContainerGap())
        );
        pnlFormularioLayout.setVerticalGroup(
            pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(pnlFormularioLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblLugar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtLugar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblCategoria, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(cboCategoria, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblLatitud, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtLatitud, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblLongitud, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtLongitud, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblFecha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtFecha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblHoraInicio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtHoraInicio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblHoraFin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtHoraFin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblDuracion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(spnDuracion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblPrioridad, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(cboPrioridad, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(chkActivo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(pnlFormularioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(lblDetalle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(scrDetalle, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        btnGuardar.setBackground(new java.awt.Color(52, 115, 170));
        btnGuardar.setForeground(new java.awt.Color(255, 255, 255));
        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(this::btnGuardarActionPerformed);

        btnBuscar.setBackground(new java.awt.Color(52, 115, 170));
        btnBuscar.setForeground(new java.awt.Color(255, 255, 255));
        btnBuscar.setText("Buscar");
        btnBuscar.addActionListener(this::btnBuscarActionPerformed);

        btnActualizar.setBackground(new java.awt.Color(52, 115, 170));
        btnActualizar.setForeground(new java.awt.Color(255, 255, 255));
        btnActualizar.setText("Actualizar");
        btnActualizar.addActionListener(this::btnActualizarActionPerformed);

        btnEliminar.setBackground(new java.awt.Color(52, 115, 170));
        btnEliminar.setForeground(new java.awt.Color(255, 255, 255));
        btnEliminar.setText("Eliminar");
        btnEliminar.addActionListener(this::btnEliminarActionPerformed);

        btnLimpiar.setBackground(new java.awt.Color(52, 115, 170));
        btnLimpiar.setForeground(new java.awt.Color(255, 255, 255));
        btnLimpiar.setText("Limpiar");
        btnLimpiar.addActionListener(this::btnLimpiarActionPerformed);

        btnCerrarSesion.setBackground(new java.awt.Color(52, 115, 170));
        btnCerrarSesion.setForeground(new java.awt.Color(255, 255, 255));
        btnCerrarSesion.setText("Cerrar sesión");
        btnCerrarSesion.addActionListener(this::btnCerrarSesionActionPerformed);

        tblEventos.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tblEventos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Id", "Código", "Título", "Fecha", "Inicio", "Categoría", "Prioridad", "Activo", "Lugar"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });

        scrEventos.setViewportView(tblEventos);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(lblEncabezado, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblBienvenida, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(pnlFormulario, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnActualizar, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnCerrarSesion, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(scrEventos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblEncabezado, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(lblBienvenida, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(pnlFormulario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnActualizar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnCerrarSesion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(scrEventos, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        Evento e = leerFormulario();
        if (e == null) {
            return;
        }
        try {
            eventoDAO.insertar(e);
            idSeleccionado = e.getIdEvento();
            cargarTabla();
            JOptionPane.showMessageDialog(this, "Evento " + e.getCodigoEvento() + " guardado.");
        } catch (SQLException ex) {
            mostrarErrorBD(ex);
        }
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnBuscarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarActionPerformed
        String codigo = txtCodigo.getText().trim();
        if (codigo.isEmpty()) {
            mostrarError("Ingrese el código del evento que desea buscar.", txtCodigo);
            return;
        }
        try {
            Evento e = eventoDAO.buscar(codigo);
            if (e == null) {
                JOptionPane.showMessageDialog(this, "No existe un evento con ese código.", "Buscar",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            cargarFormulario(e);
        } catch (SQLException ex) {
            mostrarErrorBD(ex);
        }
    }//GEN-LAST:event_btnBuscarActionPerformed

    private void btnActualizarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnActualizarActionPerformed
        if (idSeleccionado == 0) {
            JOptionPane.showMessageDialog(this, "Primero busque o seleccione un evento de la tabla.",
                    "Actualizar", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Evento e = leerFormulario();
        if (e == null) {
            return;
        }
        e.setIdEvento(idSeleccionado);
        try {
            if (eventoDAO.actualizar(e)) {
                cargarTabla();
                JOptionPane.showMessageDialog(this, "Evento " + e.getCodigoEvento() + " actualizado.");
            } else {
                JOptionPane.showMessageDialog(this, "No se actualizó ningún evento.", "Actualizar",
                        JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            mostrarErrorBD(ex);
        }
    }//GEN-LAST:event_btnActualizarActionPerformed

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        if (idSeleccionado == 0) {
            JOptionPane.showMessageDialog(this, "Primero busque o seleccione un evento de la tabla.",
                    "Eliminar", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de eliminar el evento " + txtCodigo.getText().trim() + "?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (eventoDAO.eliminar(idSeleccionado)) {
                limpiarFormulario();
                cargarTabla();
                JOptionPane.showMessageDialog(this, "Evento eliminado.");
            } else {
                JOptionPane.showMessageDialog(this, "No se eliminó ningún evento.", "Eliminar",
                        JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            mostrarErrorBD(ex);
        }
    }//GEN-LAST:event_btnEliminarActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        limpiarFormulario();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void btnCerrarSesionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCerrarSesionActionPerformed
        new VentanaLogin().setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnCerrarSesionActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnActualizar;
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnCerrarSesion;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JComboBox<String> cboCategoria;
    private javax.swing.JComboBox<String> cboPrioridad;
    private javax.swing.JCheckBox chkActivo;
    private javax.swing.JLabel lblBienvenida;
    private javax.swing.JLabel lblCategoria;
    private javax.swing.JLabel lblCodigo;
    private javax.swing.JLabel lblDetalle;
    private javax.swing.JLabel lblDuracion;
    private javax.swing.JLabel lblEncabezado;
    private javax.swing.JLabel lblFecha;
    private javax.swing.JLabel lblHoraFin;
    private javax.swing.JLabel lblHoraInicio;
    private javax.swing.JLabel lblLatitud;
    private javax.swing.JLabel lblLongitud;
    private javax.swing.JLabel lblLugar;
    private javax.swing.JLabel lblPrioridad;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JPanel pnlFormulario;
    private javax.swing.JScrollPane scrDetalle;
    private javax.swing.JScrollPane scrEventos;
    private javax.swing.JSpinner spnDuracion;
    private javax.swing.JTable tblEventos;
    private javax.swing.JTextField txtCodigo;
    private javax.swing.JTextArea txtDetalle;
    private javax.swing.JTextField txtFecha;
    private javax.swing.JTextField txtHoraFin;
    private javax.swing.JTextField txtHoraInicio;
    private javax.swing.JTextField txtLatitud;
    private javax.swing.JTextField txtLongitud;
    private javax.swing.JTextField txtLugar;
    private javax.swing.JTextField txtTitulo;
    // End of variables declaration//GEN-END:variables
}
