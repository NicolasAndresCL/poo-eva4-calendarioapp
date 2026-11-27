# Defensa individual – Nicolás Cano

**Fundamentos de POO (IEI-085) · Evaluación 4 · CalendarioApp · Defensa: lunes 30 de noviembre de 2026**

Esta guía cubre **mi parte**: `EventoDAO` (C1 a C5), V3, POINT y JSON desde Java, `VentanaEventos` y las pruebas JUnit. Complementa a `docs/guia_defensa.md` (guía del equipo) sin contradecirla.

---

## 1. Mi parte en 30 segundos

«Yo hice la capa que habla con la tabla `evento`. `EventoDAO` tiene los cinco métodos del CRUD con `PreparedStatement` y `try-with-resources`: `insertar` (C1, devuelve el id generado), `listar` (C2), `buscar` por código (C3), `actualizar` (C4) y `eliminar` (C5). La ubicación se guarda en una columna `POINT` con `POINT(longitud, latitud)` y se lee con `ST_X` y `ST_Y`; el detalle es una columna `JSON` que viaja como texto y la base lo valida. El código de evento es `UNIQUE`: si se repite, MySQL da el error 1062 y `Conexion.mensajeError` lo traduce a "El registro ya existe." (V3). `VentanaEventos` es el formulario con la `JTable`: valida los datos antes de tocar la base y, después de cada operación, vuelve a cargar la tabla. Además escribí las 11 pruebas JUnit de `EventoDAOTest`.»

---

## 2. `EventoDAO.java` línea por línea

### 2.1 Cabecera

```java
package dao;
```
Paquete `dao`: capa de acceso a datos. La ventana (`vista`) no escribe SQL; le pide todo a esta clase.

```java
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
```
`Conexion` entrega la conexión (parte de Rodolfo). `Connection`, `PreparedStatement` y `ResultSet` son las tres piezas del flujo JDBC. `Date` y `Time` son los tipos SQL a los que se convierten `LocalDate`/`LocalTime`. `Types` da las constantes para `setNull`. `Evento` es el modelo (parte de Tais).

### 2.2 La constante `COLUMNAS`

```java
private static final String COLUMNAS = "id_evento, codigo_evento, titulo, lugar, "
        + "ST_Y(ubicacion) AS latitud, ST_X(ubicacion) AS longitud, detalle, categoria, "
        + "fecha_evento, hora_inicio, hora_fin, duracion_minutos, prioridad, activo";
```
Es la lista de columnas que usan `listar` y `buscar`, escrita una sola vez para que no se desordenen. `ubicacion` es un `POINT` binario: no se puede leer directo como número, así que se extrae con `ST_Y(ubicacion)` (latitud, eje Y) y `ST_X(ubicacion)` (longitud, eje X). `AS latitud` y `AS longitud` son los alias con que `leerFila` las busca por nombre. `detalle` (JSON) se trae como texto. No incluye `fecha_creacion` porque el modelo no la usa.

### 2.3 `insertar` (C1)

```java
public int insertar(Evento e) throws SQLException {
```
Recibe el evento armado y devuelve el `id_evento` generado. `throws SQLException`: el DAO no atrapa errores; los deja subir para que la ventana muestre el mensaje.

```java
String sql = "INSERT INTO evento (codigo_evento, titulo, lugar, ubicacion, detalle, "
        + "categoria, fecha_evento, hora_inicio, hora_fin, duracion_minutos, prioridad, activo) "
        + "VALUES (?, ?, ?, POINT(?, ?), ?, ?, ?, ?, ?, ?, ?, ?)";
```
12 columnas (no van `id_evento`, que es AUTO_INCREMENT, ni `fecha_creacion`, que tiene DEFAULT). Hay 13 `?` porque `POINT(?, ?)` consume dos: **primero la longitud (X), luego la latitud (Y)**. Los `?` son la "puerta cerrada": el valor viaja aparte de la sentencia, por eso no hay inyección SQL.

```java
try (Connection con = Conexion.conectar();
        PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
```
`try-with-resources`: los recursos declarados entre paréntesis se cierran solos al salir del bloque (en orden inverso), aunque haya excepción. `RETURN_GENERATED_KEYS` le pide a JDBC que conserve el id que MySQL genera con AUTO_INCREMENT.

```java
cargarParametros(ps, e);
```
Rellena los `?` 1 a 13 (ver 2.7).

```java
ps.executeUpdate();
```
Ejecuta el INSERT. `executeUpdate` es para sentencias que **escriben** (INSERT/UPDATE/DELETE) y devuelve las filas afectadas; aquí no se usa ese valor porque el éxito se confirma con el id (si falla, lanza `SQLException`).

```java
try (ResultSet claves = ps.getGeneratedKeys()) {
    if (claves.next()) {
        e.setIdEvento(claves.getInt(1));
    }
}
```
`getGeneratedKeys()` devuelve un `ResultSet` con las claves creadas; `next()` avanza a la primera (y única) fila y `getInt(1)` lee la columna 1 (el id). Se guarda en el objeto `e` para que quien llamó lo vea.

```java
return e.getIdEvento();
```
Devuelve el id nuevo (los tests comprueban `id > 0`).

### 2.4 `listar` (C2)

```java
String sql = "SELECT " + COLUMNAS + " FROM evento ORDER BY fecha_evento, hora_inicio";
```
Sin `?` porque no hay filtro. `ORDER BY fecha_evento, hora_inicio` da orden cronológico.

```java
ArrayList<Evento> lista = new ArrayList<>();
try (Connection con = Conexion.conectar();
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
```
Tres recursos en el mismo `try`. `executeQuery()` es para `SELECT` y devuelve un `ResultSet` (Query lee, Update escribe).

```java
while (rs.next()) {
    lista.add(leerFila(rs));
}
```
`rs.next()` pasa a la siguiente fila y devuelve `false` al terminar; cada fila se convierte en un `Evento` con `leerFila`.

```java
return lista;
```
Si la tabla está vacía devuelve una lista vacía, nunca `null`.

### 2.5 `buscar` (C3)

```java
String sql = "SELECT " + COLUMNAS + " FROM evento WHERE codigo_evento = ?";
```
Busca por el **identificador de negocio** (`codigo_evento`, UNIQUE), no por el id.

```java
ps.setString(1, codigoEvento);
```
Pone el texto en el primer `?`.

```java
try (ResultSet rs = ps.executeQuery()) {
    if (rs.next()) {
        return leerFila(rs);
    }
    return null;
}
```
`if` y no `while`: como el código es único, hay a lo más una fila. Si no existe devuelve `null`, y la ventana lo traduce a "No existe un evento con ese código."

### 2.6 `actualizar` (C4) y `eliminar` (C5)

```java
String sql = "UPDATE evento SET codigo_evento = ?, titulo = ?, lugar = ?, "
        + "ubicacion = POINT(?, ?), detalle = ?, categoria = ?, fecha_evento = ?, "
        + "hora_inicio = ?, hora_fin = ?, duracion_minutos = ?, prioridad = ?, activo = ? "
        + "WHERE id_evento = ?";
```
Los 13 primeros `?` están en el **mismo orden** que en el INSERT; el 14º es el `WHERE id_evento = ?`. Se filtra por la clave primaria para tocar exactamente una fila.

```java
cargarParametros(ps, e);
ps.setInt(14, e.getIdEvento());
return ps.executeUpdate() == 1;
```
Reutiliza los 13 parámetros, agrega el 14º y compara el retorno de `executeUpdate()` (filas afectadas) con 1: `true` si cambió justo una fila; `false` si el id no existía (0 filas).

```java
String sql = "DELETE FROM evento WHERE id_evento = ?";
...
ps.setInt(1, idEvento);
return ps.executeUpdate() == 1;
```
Misma lógica: borra por clave primaria y devuelve `true` si se borró una fila. Por eso `testEliminar` verifica que el segundo `eliminar(id)` devuelve `false`.

### 2.7 `cargarParametros` (compartido por INSERT y UPDATE)

```java
// Parametros 1 a 13, en el mismo orden para INSERT y UPDATE.
private void cargarParametros(PreparedStatement ps, Evento e) throws SQLException {
```
Método privado que existe para **no repetir** 13 líneas en dos métodos. Funciona porque INSERT y UPDATE se escribieron con los `?` en el mismo orden.

```java
ps.setString(1, e.getCodigoEvento());
ps.setString(2, e.getTitulo());
ps.setString(3, e.getLugar());
```
Índices 1, 2 y 3: código, título y lugar (JDBC cuenta desde 1, no desde 0). `setString` con `null` en `lugar` se guarda como NULL.

```java
if (e.tieneUbicacion()) {
    ps.setDouble(4, e.getLongitud());
    ps.setDouble(5, e.getLatitud());
} else {
    ps.setNull(4, Types.DOUBLE);
    ps.setNull(5, Types.DOUBLE);
}
```
Los índices 4 y 5 son los dos `?` de `POINT(?, ?)`: **4 = longitud, 5 = latitud**. `tieneUbicacion()` (del modelo) es `true` si ambas existen. Si no hay ubicación se manda `setNull` con `Types.DOUBLE`: `POINT(NULL, NULL)` deja NULL en la columna. No se puede usar `setDouble` con un `Double` nulo (reventaría con `NullPointerException` al desempaquetar); `setNull` exige indicar el tipo SQL para que el driver sepa qué NULL enviar.

```java
ps.setString(6, e.getDetalle());
ps.setString(7, e.getCategoria());
```
6 = detalle (el JSON viaja como texto; la base lo valida con `CHECK JSON_VALID`), 7 = categoría.

```java
ps.setDate(8, Date.valueOf(e.getFechaEvento()));
ps.setTime(9, Time.valueOf(e.getHoraInicio()));
```
8 y 9: el modelo usa `LocalDate`/`LocalTime`, JDBC pide `java.sql.Date`/`Time`; `valueOf` convierte uno en otro.

```java
if (e.getHoraFin() != null) {
    ps.setTime(10, Time.valueOf(e.getHoraFin()));
} else {
    ps.setNull(10, Types.TIME);
}
```
10 = hora de fin, opcional (la columna admite NULL); mismo patrón de `setNull`, ahora con `Types.TIME`.

```java
ps.setInt(11, e.getDuracionMinutos());
ps.setInt(12, e.getPrioridad());
ps.setBoolean(13, e.isActivo());
```
11 = duración, 12 = prioridad, 13 = activo (BOOLEAN = TINYINT(1)).

### 2.8 `leerFila`

```java
Double latitud = rs.getDouble("latitud");
if (rs.wasNull()) {
    latitud = null;
}
```
`getDouble` devuelve el primitivo `double`: si la columna era NULL devuelve `0.0`, que se confundiría con una coordenada real (0° es válido). `wasNull()` pregunta si **el último valor leído** fue NULL; si sí, la variable `Double` (objeto, que admite null) se pone en `null`. Lo mismo se repite para `longitud`.

```java
Time fin = rs.getTime("hora_fin");
```
Se lee como objeto `Time`, que sí puede ser `null`, y se decide abajo.

```java
return new Evento(
        rs.getInt("id_evento"),
        rs.getString("codigo_evento"),
        ...
        rs.getDate("fecha_evento").toLocalDate(),
        rs.getTime("hora_inicio").toLocalTime(),
        fin == null ? null : fin.toLocalTime(),
        rs.getInt("duracion_minutos"),
        rs.getInt("prioridad"),
        rs.getBoolean("activo"));
```
Arma el `Evento` con el constructor completo. Se lee por **nombre de columna** (por eso los alias `latitud`/`longitud`). `toLocalDate()` y `toLocalTime()` hacen el camino inverso de `valueOf`. `hora_fin` puede ser nula, así que `fin == null ? null : fin.toLocalTime()` evita `NullPointerException`. `fecha_evento` y `hora_inicio` son `NOT NULL`, por eso no necesitan esa protección.

---

## 3. `VentanaEventos` línea por línea (métodos de eventos)

> `initComponents()` lo genera NetBeans desde el `.form` (Design); no se edita a mano. Solo se explica mi código.

### 3.1 Atributos y constructor

```java
private Usuario usuario;
private EventoDAO eventoDAO;
// Id del evento cargado en el formulario (0 = ninguno); lo usan Actualizar y Eliminar
private int idSeleccionado;
```
`usuario` es el que inició sesión (viene del login). `eventoDAO` es el único contacto con la base. `idSeleccionado` es el estado clave: `0` significa "no hay evento cargado"; mientras sea 0, Actualizar y Eliminar se niegan a actuar.

```java
public VentanaEventos(Usuario usuario) {
    initComponents();
    ...
    this.usuario = usuario;
    eventoDAO = new EventoDAO();
    idSeleccionado = 0;
    lblBienvenida.setText("Bienvenido/a " + usuario.getNombreCompleto() + " (" + usuario.getRol() + ")");
    chkActivo.setSelected(true);
    cboPrioridad.setSelectedIndex(1);
```
Primero se dibujan los componentes. Luego se guarda el usuario, se crea el DAO y se muestra el saludo. Valores por defecto: activo marcado y prioridad en el índice 1 (= "2", media), igual que los DEFAULT de la tabla.

```java
tblEventos.addMouseListener(new java.awt.event.MouseAdapter() {
    @Override
    public void mouseClicked(java.awt.event.MouseEvent e) {
        tblEventosClicked();
    }
});
```
`MouseAdapter` es una clase que ya implementa vacíos todos los métodos del listener; solo se sobrescribe `mouseClicked` (clase anónima). Al hacer clic en la tabla se llama a `tblEventosClicked()`.

```java
this.setLocationRelativeTo(null);
cargarTabla();
```
Centra la ventana y carga los eventos al abrir.

### 3.2 Mensajes

```java
private void mostrarError(String mensaje, javax.swing.JComponent campo) {
    campo.requestFocus();
    JOptionPane.showMessageDialog(this, mensaje, "Dato a corregir", JOptionPane.WARNING_MESSAGE);
    campo.requestFocus();
}
```
Muestra el aviso de validación y devuelve el cursor al campo con el problema. Pide el foco antes **y** después del diálogo: solo después, a veces el foco volvía al botón.

```java
private void mostrarErrorBD(SQLException e) {
    JOptionPane.showMessageDialog(this, Conexion.mensajeError(e), "Error de base de datos",
            JOptionPane.ERROR_MESSAGE);
}
```
Todo error de la base pasa por `Conexion.mensajeError(e)`, que traduce el código: 1062 → "El registro ya existe."; 4025/3819/3140 → "Los datos no cumplen las reglas de la tabla..."; conexión caída → "No fue posible conectar con la base de datos."

### 3.3 `cargarTabla` (C2 en pantalla)

```java
DefaultTableModel modelo = (DefaultTableModel) tblEventos.getModel();
modelo.setRowCount(0);
```
El `JTable` muestra lo que tenga su modelo (`DefaultTableModel`). `setRowCount(0)` lo vacía para no duplicar filas.

```java
ArrayList<Evento> lista = eventoDAO.listar();
for (int i = 0; i < lista.size(); i++) {
    Evento e = lista.get(i);
    modelo.addRow(new Object[]{e.getIdEvento(), e.getCodigoEvento(), e.getTitulo(),
        e.getFechaEvento(), e.getHoraInicio(), e.getCategoria(), e.getPrioridad(),
        e.isActivo() ? "Sí" : "No", e.getLugar()});
}
```
Pide la lista al DAO y agrega una fila por evento (un `Object[]` con las 9 columnas visibles; el booleano se muestra como Sí/No). El `catch (SQLException e)` llama a `mostrarErrorBD`. Se llama al abrir la ventana y después de cada guardar, actualizar y eliminar.

### 3.4 `limpiarFormulario` y `cargarFormulario`

`limpiarFormulario()` pone `idSeleccionado = 0`, vacía los campos, vuelve a los valores por defecto (duración 60, prioridad "2", activo marcado), ejecuta `tblEventos.clearSelection()` y deja el foco en el código. `cargarFormulario(Evento e)` hace lo inverso: copia el evento a los campos y fija `idSeleccionado = e.getIdEvento()`; los nulos se muestran como texto vacío (`e.getLugar() == null ? "" : e.getLugar()`), y `LocalDate`/`LocalTime` se muestran con `toString()` (formatos `yyyy-MM-dd` y `HH:mm`).

### 3.5 `leerFormulario` (todas las validaciones)

```java
String codigo = txtCodigo.getText().trim();
if (codigo.isEmpty()) {
    mostrarError("Debe ingresar el código del evento.", txtCodigo);
    return null;
}
```
`trim()` quita espacios. Si el código está vacío se avisa y se devuelve `null`: ese `null` es la señal de "hay un error, no sigas". Igual para el título ("Debe ingresar el título del evento.").

```java
LocalDate fecha;
try {
    fecha = LocalDate.parse(txtFecha.getText().trim());
} catch (DateTimeParseException ex) {
    mostrarError("La fecha debe tener el formato yyyy-MM-dd (ej: 2026-12-15).", txtFecha);
    return null;
}
```
`LocalDate.parse` espera ISO (`yyyy-MM-dd`) y lanza `DateTimeParseException` si no calza (incluye fechas imposibles como 2026-02-30). Mismo patrón con `LocalTime.parse` para la hora de inicio (`HH:mm`); la hora de fin solo se parsea si el campo no está vacío (`horaFin` parte en `null`).

```java
if (!textoLat.isEmpty() || !textoLon.isEmpty()) {
    if (textoLat.isEmpty()) { ... return null; }
    if (textoLon.isEmpty()) { ... return null; }
    latitud = Double.valueOf(textoLat);   // dentro de try/catch NumberFormatException
    longitud = Double.valueOf(textoLon);
}
```
Regla "o las dos o ninguna": si al menos una viene escrita, la otra es obligatoria. Después se convierten con `Double.valueOf` y un `NumberFormatException` da "La latitud debe ser un número (ej: -33.4489)." Si ambas están vacías, quedan `null` y el DAO guardará NULL.

```java
Evento e = new Evento();
e.setCodigoEvento(codigo);
...
e.setLugar(lugar.isEmpty() ? null : lugar);
e.setDetalle(detalle.isEmpty() ? null : detalle);
e.setCategoria((String) cboCategoria.getSelectedItem());
e.setDuracionMinutos((Integer) spnDuracion.getValue());
e.setPrioridad(Integer.parseInt((String) cboPrioridad.getSelectedItem()));
e.setActivo(chkActivo.isSelected());
return e;
```
Los textos opcionales vacíos (lugar, detalle) se convierten en `null` para que la base guarde NULL y no una cadena vacía. `getSelectedItem()` devuelve `Object`, por eso el *cast* a `String`; el spinner devuelve `Integer`; la prioridad del combo es texto "1"/"2"/"3" y se convierte con `Integer.parseInt` (no puede fallar: el combo solo ofrece esas tres opciones).

**Qué NO valida Java:** duración > 0, hora fin > inicio y JSON válido (la prioridad la limita el combo). Eso lo hacen los `CHECK` de la tabla (error 4025 en MariaDB / 3819 y 3140 en MySQL) y la ventana muestra el mensaje genérico de reglas.

### 3.6 `tblEventosClicked`

```java
int fila = tblEventos.getSelectedRow();
if (fila < 0) {
    return;
}
String codigo = String.valueOf(tblEventos.getValueAt(fila, 1));
Evento e = eventoDAO.buscar(codigo);
if (e != null) {
    cargarFormulario(e);
}
```
`getSelectedRow()` da -1 si no hay fila. La columna 1 de la tabla es el código; con él se llama a `buscar` (C3) y se carga el evento completo en el formulario, incluyendo `idSeleccionado`. Se busca en la base (no se arma desde la fila) porque la tabla no muestra latitud, longitud ni detalle.

### 3.7 `btnGuardarActionPerformed` (C1)

```java
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
```
Valida; si falla, termina. Si pasa, inserta, deja el id nuevo como seleccionado (así se puede actualizar enseguida), recarga la tabla y avisa. Si el código está repetido, `insertar` lanza `SQLException` (1062) y se muestra "El registro ya existe." (V3); la tabla no cambia.

### 3.8 `btnBuscarActionPerformed` (C3)

Toma el código del campo; si está vacío avisa "Ingrese el código del evento que desea buscar.". Llama a `eventoDAO.buscar`: si devuelve `null` muestra "No existe un evento con ese código." (`INFORMATION_MESSAGE`); si no, `cargarFormulario(e)`.

### 3.9 `btnActualizarActionPerformed` (C4)

```java
if (idSeleccionado == 0) {
    JOptionPane.showMessageDialog(this, "Primero busque o seleccione un evento de la tabla.", ...);
    return;
}
Evento e = leerFormulario();
if (e == null) { return; }
e.setIdEvento(idSeleccionado);
if (eventoDAO.actualizar(e)) { cargarTabla(); ... "actualizado." } else { ... "No se actualizó ningún evento." }
```
Primero exige que haya un evento cargado (`idSeleccionado != 0`). `leerFormulario` arma el evento con los datos editados pero sin id; se le pone `idSeleccionado` porque el `WHERE id_evento = ?` lo necesita (así se puede cambiar incluso el código). El `boolean` devuelto por el DAO decide el mensaje.

### 3.10 `btnEliminarActionPerformed` (C5)

```java
int respuesta = JOptionPane.showConfirmDialog(this,
        "¿Está seguro de eliminar el evento " + txtCodigo.getText().trim() + "?",
        "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
if (respuesta != JOptionPane.YES_OPTION) {
    return;
}
```
Igual que Actualizar, exige `idSeleccionado != 0`. `showConfirmDialog` con `YES_NO_OPTION` devuelve un `int`; si no es `YES_OPTION` (No o cerrar la ventana) se cancela sin tocar la base. Si confirma: `eventoDAO.eliminar(idSeleccionado)`; si es `true`, `limpiarFormulario()`, `cargarTabla()` y "Evento eliminado."; si es `false`, "No se eliminó ningún evento.".

### 3.11 `btnLimpiarActionPerformed`

Solo llama a `limpiarFormulario()`: deja `idSeleccionado = 0` y el formulario listo para un evento nuevo.

---

## 4. Reglas de negocio de mi parte

| Regla | Dónde | Mensaje / error |
|---|---|---|
| Código obligatorio | Java (`leerFormulario`) y BD (`NOT NULL`) | "Debe ingresar el código del evento." |
| Título obligatorio | Java y BD (`NOT NULL`) | "Debe ingresar el título del evento." |
| Código único (V3) | BD (`UNIQUE`) → `Conexion.mensajeError` | Error 1062 → "El registro ya existe." |
| Formato de fecha | Java (`LocalDate.parse`) | "La fecha debe tener el formato yyyy-MM-dd (ej: 2026-12-15)." |
| Formato de hora inicio / fin | Java (`LocalTime.parse`) | "La hora de inicio debe tener el formato HH:mm..." / "La hora de fin debe tener el formato HH:mm... o quedar vacía." |
| Latitud y longitud: ambas o ninguna | Java | "Si ingresa la longitud, también debe ingresar la latitud." (y la inversa) |
| Latitud / longitud numéricas | Java (`Double.valueOf`) | "La latitud debe ser un número (ej: -33.4489)." |
| Prioridad 1–3 | Combo (solo ofrece 1, 2, 3) y BD (`chk_evento_prioridad`) | Error 4025/3819 → "Los datos no cumplen las reglas de la tabla (prioridad, horas, duracion o JSON)." |
| Duración > 0 | BD (`chk_evento_duracion`) | Mismo mensaje de reglas de la tabla |
| Hora fin > hora inicio | BD (`chk_evento_horas`) | Mismo mensaje de reglas de la tabla |
| JSON válido | BD (`chk_evento_detalle`, `JSON_VALID`) | Error 4025/3140 → mismo mensaje de reglas |
| Confirmación antes de eliminar | Java (`showConfirmDialog`) | "¿Está seguro de eliminar el evento X?" |
| Tabla refrescada tras cada operación | Java (`cargarTabla()` tras guardar, actualizar y eliminar) | Sin mensaje; se ve la fila nueva, cambiada o ausente |
| Actualizar/Eliminar solo con evento cargado | Java (`idSeleccionado == 0`) | "Primero busque o seleccione un evento de la tabla." |

Idea para explicarlo: **Java valida lo que se corrige en el campo (formatos, obligatorios); la base es la red de seguridad final (`UNIQUE` y `CHECK`)**.

---

## 5. Demostración C1 a C5 + V3 (con verificación en MySQL)

**Antes de la demo, recargar el script** para partir de un estado conocido (5 eventos, EVT-0001 a EVT-0005). Desde Git Bash, en la carpeta del proyecto:

En **PowerShell** el `<` no existe; ahí se usa (desde la carpeta `Evaluacion4_Calendario`):

```powershell
Get-Content -Encoding UTF8 .\sql\calendario_bd.sql | docker exec -i poo-mysql mariadb -u root -t
```

En **Git Bash** (probado: deja los 5 eventos con tildes correctas):

```bash
docker exec -i poo-mysql mariadb -uroot < sql/calendario_bd.sql
```
`docker exec -i` ejecuta un comando dentro del contenedor `poo-mysql` y `-i` deja pasar por la entrada estándar el archivo que se redirige con `<`. `mariadb -uroot` es el cliente (usuario `root`, sin clave). El script borra y recrea la base `calendario_app`. No debe imprimir nada; si da error de ruta, revisar que se esté en la carpeta que contiene `sql/`.

Dejar el login listo (`admin` / `Admin2026`) para llegar a `VentanaEventos`. Decir siempre en voz alta qué se espera ver antes de correr el comando.

### 5.1 C1: Insertar

**En la app:** código `EVT-0100`, título `Demo defensa`, lugar `Sala 5`, latitud `-33.4489`, longitud `-70.6693`, detalle `{"notas": "demo"}`, fecha `2026-12-15`, hora inicio `10:00`, hora fin `11:00` → **Guardar**. Sale "Evento EVT-0100 guardado." y la fila aparece en la tabla.

**Verificar en MySQL:**
```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "SELECT id_evento, codigo_evento, titulo, ST_X(ubicacion) AS longitud, ST_Y(ubicacion) AS latitud, JSON_EXTRACT(detalle, '$.notas') AS notas FROM evento WHERE codigo_evento = 'EVT-0100';"
```
Pide al cliente ejecutar un `SELECT` (`-e`) sobre la base `calendario_app`. `ST_X`/`ST_Y` sacan longitud y latitud del `POINT`; `JSON_EXTRACT(detalle, '$.notas')` saca el campo `notas` del JSON.
**Salida esperada:** una fila con `id_evento` 6, `EVT-0100`, `Demo defensa`, longitud `-70.6693`, latitud `-33.4489`, notas `"demo"` (con comillas). Si no aparece ninguna fila, la app no insertó: revisar el mensaje de error.

### 5.2 C2: Listar

**En la app:** la tabla ya lista todos los eventos ordenados por fecha y hora (se recarga al abrir y tras cada operación).

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "SELECT id_evento, codigo_evento, titulo, fecha_evento, hora_inicio FROM evento ORDER BY fecha_evento, hora_inicio;"
```
Es la misma consulta que hace `listar()`. **Esperado:** 6 filas (EVT-0001 a EVT-0005 más EVT-0100), en el mismo orden que la `JTable`.

### 5.3 C3: Buscar

**En la app:** escribir `EVT-0002` en Código → **Buscar** (o clic en la fila). El formulario se llena con "Control médico". Para el caso negativo, buscar `NO-EXISTE` → "No existe un evento con ese código."

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "SELECT id_evento, codigo_evento, titulo, lugar, categoria FROM evento WHERE codigo_evento = 'EVT-0002';"
```
Equivale al `WHERE codigo_evento = ?` de `buscar`. **Esperado:** una fila con título "Control médico", lugar "CESFAM Centro", categoría `salud`. (Se evita `SELECT *` porque `ubicacion` sale como binario ilegible: por eso la app usa `ST_X`/`ST_Y`.)

### 5.4 C4: Actualizar

**En la app:** buscar `EVT-0100`, cambiar el título a `Demo editada` y la prioridad a `3` → **Actualizar**. Sale "Evento EVT-0100 actualizado." y la fila cambia.

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "SELECT id_evento, codigo_evento, titulo, prioridad FROM evento WHERE codigo_evento = 'EVT-0100';"
```
**Esperado:** `Demo editada` y prioridad `3`, con el **mismo** `id_evento` (6): se modificó, no se creó otra fila.

### 5.5 C5: Eliminar

**En la app:** con `EVT-0100` cargado → **Eliminar** → responder **Sí** en "¿Está seguro de eliminar el evento EVT-0100?". Sale "Evento eliminado." y la fila desaparece. (Mostrar también que **No** cancela.)

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "SELECT COUNT(*) AS cuantos FROM evento WHERE codigo_evento = 'EVT-0100';"
```
**Esperado:** `cuantos = 0`. Si da 1, se respondió No o el borrado falló.

### 5.6 V3: Código repetido

**En la app:** código `EVT-0001` (ya existe), título `Repetido`, fecha `2026-12-20`, hora `09:00` → **Guardar**. Aparece el diálogo "El registro ya existe." y no se agrega fila.

```bash
docker exec poo-mysql mariadb -uroot calendario_app -e "SELECT COUNT(*) AS cuantos FROM evento WHERE codigo_evento = 'EVT-0001';"
```
**Esperado:** `cuantos = 1`: la restricción `UNIQUE` impidió el duplicado (error 1062).

---

## 6. Pruebas JUnit (`EventoDAOTest`, 11 pruebas)

Todas usan códigos `TEST-JU-*` y corren contra la base real.

| # | Test | Qué prueba |
|---|---|---|
| 1 | `testListarTraeLosRegistrosDelScript` | C2: `listar()` trae al menos 5 filas (las del script). |
| 2 | `testInsertarYBuscar` | C1 + C3: el id generado es > 0 y `buscar` devuelve el mismo id, título y hora fin. |
| 3 | `testPointSeGuardaYSeLee` | POINT: latitud -33.4378 y longitud -70.6506 vuelven iguales (tolerancia 0.00001) y no invertidas. |
| 4 | `testSinUbicacionNiHoraFinQuedanNull` | `setNull` al guardar y `wasNull` al leer: latitud, longitud y hora fin quedan `null`. |
| 5 | `testJsonSeGuardaYSeLee` | El JSON se guarda y el texto leído contiene "creado por JUnit". |
| 6 | `testCodigoRepetidoNoSeInserta` | V3: el 2º insert lanza `SQLException` y `Conexion.mensajeError` da "El registro ya existe." |
| 7 | `testJsonInvalidoLoRechazaLaBase` | Un detalle "esto no es json" lanza `SQLException` (`CHECK JSON_VALID`). |
| 8 | `testPrioridadFueraDeRangoLaRechazaLaBase` | Prioridad 7 lanza `SQLException` (`CHECK` 1–3). |
| 9 | `testBuscarInexistenteDevuelveNull` | C3: `buscar("NO-EXISTE")` devuelve `null`. |
| 10 | `testActualizar` | C4: `actualizar` devuelve `true`, mantiene el id y cambia título y prioridad. |
| 11 | `testEliminar` | C5: `eliminar` devuelve `true`, `buscar` da `null` y un 2º `eliminar` devuelve `false`. |

**¿Por qué `@After` borra `TEST-JU-*`?** `@After` corre después de **cada** prueba (incluso si falla). Ejecuta `DELETE FROM evento WHERE codigo_evento LIKE 'TEST-JU-%'`, así cada test parte limpio, no quedan datos basura que contaminen la demo y no hay choque de `UNIQUE` al repetir la suite. El `LIKE` con `%` solo borra lo de las pruebas, nunca los EVT-xxxx del script.

**Cómo correr:** en NetBeans, **Alt+F6** (Test Project) corre todas las pruebas del proyecto. Resultado esperado: **24 pruebas en total** (las 11 de `EventoDAOTest` más las del resto del equipo), todas en verde. Requiere MySQL arriba y la base cargada.

---

## 7. Lo mínimo de las partes de Tais y Rodolfo

**Tais (script SQL y modelo)**
- `sql/calendario_bd.sql` crea la base `calendario_app` con las tablas `usuario` y `evento`; recargarlo deja todo limpio.
- Tabla `evento`: PK `id_evento` AUTO_INCREMENT, `codigo_evento` UNIQUE NOT NULL, `ubicacion` POINT (X = longitud, Y = latitud), `detalle` JSON, DATE y TIME, BOOLEAN `activo` y cuatro `CHECK` (duración > 0, prioridad 1–3, hora fin > inicio, JSON válido).
- Datos de prueba: 3 usuarios (`admin`, `operador1`, `inactivo1`) y 5 eventos; EVT-0003 sin ubicación ni hora fin, EVT-0005 solo con los campos mínimos (el resto por DEFAULT).
- El paquete `modelo` tiene `Evento` (atributos privados, getters/setters, `tieneUbicacion()`) y `Usuario`; latitud, longitud y hora fin son `Double`/`LocalTime` para poder ser `null`.

**Rodolfo (Conexion, UsuarioDAO, login)**
- `Conexion.conectar()` abre la conexión JDBC (driver `mysql-connector-j-26.10.0.jar`, base `calendario_app`, `root` sin clave).
- `Conexion.mensajeError(SQLException)` traduce códigos: 08xx/1045/1049 → no conecta; 1062 → "El registro ya existe."; 4025/3819/3140 → reglas de la tabla; otro → "Error de base de datos: ...".
- `UsuarioDAO.validar()` hace `SELECT ... WHERE nombre_usuario = ? AND clave = ? AND activo = TRUE`; devuelve `Usuario` o `null`; `estaInactivo()` distingue al usuario inactivo.
- `VentanaLogin` valida campos vacíos (V1) y muestra "Usuario inactivo." (V2); si entra, abre `VentanaEventos` con el `Usuario`. La clave usa `COLLATE utf8mb4_bin` (distingue mayúsculas).

---

## 8. 15 preguntas probables con respuesta

1. **¿Por qué `PreparedStatement` y no `Statement`?** Porque los valores van por `?` separados de la sentencia: evita la inyección SQL, el motor reutiliza el plan y el driver convierte los tipos. En `EventoDAO` todas las sentencias lo usan.
2. **¿Qué diferencia hay entre `executeQuery` y `executeUpdate`?** `executeQuery` ejecuta `SELECT` y devuelve un `ResultSet`; `executeUpdate` ejecuta INSERT/UPDATE/DELETE y devuelve las filas afectadas (`int`).
3. **¿Para qué sirve `== 1` en `actualizar` y `eliminar`?** `executeUpdate` devuelve cuántas filas cambió. Como se filtra por la PK, lo esperado es 1; 0 significa que el id no existía, y el método devuelve `false` para que la ventana avise.
4. **¿Cómo obtienes el id de un INSERT?** Se prepara con `RETURN_GENERATED_KEYS`; tras `executeUpdate` se llama `getGeneratedKeys()`, y `next()` + `getInt(1)` dan el valor AUTO_INCREMENT.
5. **¿Por qué `POINT(?, ?)` lleva la longitud primero?** En el tipo espacial X es la longitud e Y la latitud, y `POINT(x, y)` toma X primero. Por eso en `cargarParametros` el índice 4 es la longitud y el 5 la latitud.
6. **¿Cómo lees el POINT en Java?** No se lee directo (es binario): el SELECT usa `ST_X(ubicacion) AS longitud` y `ST_Y(ubicacion) AS latitud`, y `leerFila` los toma con `getDouble("latitud")` y `getDouble("longitud")`.
7. **¿Qué hace `wasNull()` y por qué lo usas?** `getDouble` devuelve `0.0` si la columna es NULL, que se confundiría con una coordenada real. `wasNull()` indica si el último valor leído era NULL para dejar el `Double` en `null`.
8. **¿Para qué `setNull(4, Types.DOUBLE)`?** Para mandar NULL a un `?` cuando el evento no tiene ubicación. JDBC exige el tipo SQL, y `setDouble` no sirve con un `Double` nulo (daría `NullPointerException`).
9. **¿Por qué `cargarParametros` sirve para INSERT y UPDATE?** Porque ambas sentencias tienen los 13 `?` en el mismo orden; el UPDATE solo agrega el 14º (`id_evento`) después. Evita duplicar código.
10. **¿Qué es `try-with-resources` y por qué lo usas?** Es el `try (...)` que cierra automáticamente `Connection`, `PreparedStatement` y `ResultSet`, incluso si hay excepción. Evita fugas de conexiones sin escribir `finally`.
11. **¿Cómo detectas un código repetido?** El `UNIQUE` de `codigo_evento` hace que MySQL lance el error 1062; el DAO deja subir la `SQLException` y `Conexion.mensajeError` la convierte en "El registro ya existe." No hay SELECT previo: la base es quien garantiza la unicidad (un SELECT previo tendría condición de carrera).
12. **¿Dónde se valida que el JSON sea válido?** En la base, con `CHECK (detalle IS NULL OR JSON_VALID(detalle))`. Java manda el texto tal cual; si es inválido falla el INSERT y se muestra el mensaje de reglas de la tabla. `testJsonInvalidoLoRechazaLaBase` lo prueba.
13. **¿Qué hace `idSeleccionado` y por qué es necesario?** Guarda el id del evento cargado en el formulario (0 = ninguno). Actualizar y Eliminar lo necesitan para su `WHERE id_evento = ?` y no actúan si vale 0. Además permite editar el código sin perder la fila.
14. **¿Cómo se refresca la tabla y por qué `setRowCount(0)`?** `cargarTabla()` obtiene el `DefaultTableModel`, lo vacía con `setRowCount(0)` y agrega una fila por cada evento de `listar()`. Sin vaciarlo, las filas se acumularían duplicadas.
15. **¿Por qué la prueba de Actualizar hace `buscar` antes de modificar?** Para traer un `Evento` real con su `idEvento`, que el `WHERE` necesita; luego cambia título y prioridad, llama `actualizar` y vuelve a buscar para comprobar que el cambio quedó en la base y que el id no cambió.

---

## 9. Ganchos mnemotécnicos

### 9.1 Orden de POINT: «X cruza, Y sube»
**X** cruza de lado a lado = **LON**gitud; **Y** sube al cielo = **LAT**itud. Se escribe `POINT(lon, lat)` y se lee `ST_X` = lon, `ST_Y` = lat. Regla corta: *«X antes que Y, lon antes que lat»*.
**Autoevaluación:** ¿Qué función da la latitud de `ubicacion` y en qué índice del `PreparedStatement` se escribe? *(R: `ST_Y(ubicacion)`; se escribe en el parámetro 5, porque el 4 es la longitud.)*

### 9.2 Los 5 pasos del DAO: «**C**on **P**ocas **P**apas **E**ncendí **L**a parrilla»
**C**onectar (`Conexion.conectar()`), **P**reparar (`prepareStatement`), **P**arámetros (`setXxx` desde el 1), **E**jecutar (`executeQuery` si lee / `executeUpdate` si escribe), **L**eer (`rs.next()` + `getXxx`). Imagen absurda: una parrilla enchufada a la red que prepara papas numeradas, las ejecuta en el fuego y las lee en voz alta.
**Autoevaluación:** ¿Qué paso cambia entre `listar` y `eliminar`, y qué devuelve cada uno? *(R: Ejecutar: `listar` usa `executeQuery` y devuelve un `ResultSet`; `eliminar` usa `executeUpdate` y devuelve las filas afectadas, que se compara con 1.)*

### 9.3 Quién valida qué: «**J**ava **F**ormatea, **B**ase **R**egla»
Java revisa **F**ormatos y obligatorios (fecha, hora, números, vacío); la **B**ase hace cumplir las **R**eglas (UNIQUE y CHECK de prioridad, duración, horas y JSON). Imagen: un portero (Java) que revisa cómo vas vestido y un juez (la base) que revisa la ley.
**Autoevaluación:** Si ingreso hora fin menor que hora inicio, ¿quién lo rechaza y con qué mensaje? *(R: la base, por `chk_evento_horas`; el error 4025/3819 se muestra como "Los datos no cumplen las reglas de la tabla (prioridad, horas, duracion o JSON).")*
