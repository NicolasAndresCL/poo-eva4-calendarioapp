-- =====================================================================
-- Evaluación 4 (20 %) · POO IEI-085 · Unidad III: JDBC y MySQL
-- Base de datos: calendario_app
-- Tema: agenda de eventos con acceso restringido
-- Tablas: usuario (acceso) y evento (CRUD). Independientes, sin FK.
-- Motor probado: MariaDB 10.6 (contenedor poo-mysql). Compatible con
--                MySQL 8.0.16+ (desde ahí se aplican los CHECK).
-- Integrantes: Tais Montecinos · Rodolfo Prieto · Nicolás Cano
-- =====================================================================

-- S1) Crear la base (si ya existe, no falla) y usarla
CREATE DATABASE IF NOT EXISTS calendario_app
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_spanish_ci;

USE calendario_app;

-- Para re-ejecutar el script desde cero (instalación limpia)
DROP TABLE IF EXISTS evento;
DROP TABLE IF EXISTS usuario;

-- S2) Tabla usuario (Tabla 1 de la pauta)
CREATE TABLE IF NOT EXISTS usuario (
    id_usuario      INT AUTO_INCREMENT PRIMARY KEY,
    nombre_usuario  VARCHAR(30) NOT NULL UNIQUE,
    clave           VARCHAR(64) NOT NULL COLLATE utf8mb4_bin,  -- _bin: distingue mayúsculas
    nombre_completo VARCHAR(80) NOT NULL,
    rol             VARCHAR(20) NOT NULL DEFAULT 'OPERADOR',
    activo          BOOLEAN     NOT NULL DEFAULT TRUE
);

-- S3) Tabla del tema: evento (Tabla 2 de la pauta)
CREATE TABLE evento (
    -- Clave primaria: entero autoincremental
    id_evento        INT AUTO_INCREMENT PRIMARY KEY,

    -- Identificador de negocio: VARCHAR, obligatorio y único
    codigo_evento    VARCHAR(20)  NOT NULL UNIQUE,

    -- Datos del evento
    titulo           VARCHAR(100) NOT NULL,
    lugar            VARCHAR(150) NULL,              -- nombre del lugar (texto)
    ubicacion        POINT        NULL,              -- coordenadas: X = longitud, Y = latitud
    detalle          JSON         NULL,              -- datos libres en JSON: notas, participantes...
    categoria        VARCHAR(30)  NOT NULL DEFAULT 'personal',

    -- Fecha (DATE es el tipo exigido) y horas
    fecha_evento     DATE         NOT NULL,
    hora_inicio      TIME         NOT NULL,
    hora_fin         TIME         NULL,

    -- Numéricos (INT)
    duracion_minutos INT          NOT NULL DEFAULT 60,
    prioridad        INT          NOT NULL DEFAULT 2,   -- 1 = alta, 2 = media, 3 = baja

    -- Booleano (BOOLEAN = TINYINT(1): 1 verdadero, 0 falso)
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,

    -- Auditoría: se llena sola al insertar
    fecha_creacion   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Reglas de integridad (CHECK)
    CONSTRAINT chk_evento_duracion  CHECK (duracion_minutos > 0),
    CONSTRAINT chk_evento_prioridad CHECK (prioridad BETWEEN 1 AND 3),
    CONSTRAINT chk_evento_horas     CHECK (hora_fin IS NULL OR hora_fin > hora_inicio),
    CONSTRAINT chk_evento_detalle   CHECK (detalle IS NULL OR JSON_VALID(detalle))
) ENGINE = InnoDB;

-- S4) Tres usuarios de prueba: ADMIN, OPERADOR e inactivo (script base de la pauta)
INSERT INTO usuario (nombre_usuario, clave, nombre_completo, rol, activo) VALUES
('admin',     'Admin2026', 'Administrador',    'ADMIN',    TRUE),
('operador1', 'Oper2026',  'Operador Uno',     'OPERADOR', TRUE),
('inactivo1', 'Inac2026',  'Usuario Inactivo', 'OPERADOR', FALSE);

-- S5) Registros de prueba del tema (5 filas)
--     POINT(longitud, latitud) arma la coordenada; el JSON va como texto válido.
INSERT INTO evento
    (codigo_evento, titulo, lugar, ubicacion, detalle, categoria,
     fecha_evento, hora_inicio, hora_fin, duracion_minutos, prioridad)
VALUES
    ('EVT-0001', 'Reunión de equipo', 'Sala 3, Santo Tomás Santiago',
     POINT(-70.6506, -33.4378),
     '{"notas": "Revisión semanal de avances", "participantes": ["Tais", "Rodolfo", "Nicolás"]}',
     'trabajo', '2026-11-23', '09:00:00', '10:00:00', 60, 1),
    ('EVT-0002', 'Control médico', 'CESFAM Centro',
     POINT(-70.6483, -33.4569),
     '{"notas": "Chequeo anual", "llevar": ["carnet", "exámenes"]}',
     'salud', '2026-11-25', '11:30:00', '12:00:00', 30, 1),
    ('EVT-0003', 'Cumpleaños de mamá', 'Casa',
     NULL,
     '{"notas": "Comprar torta"}',
     'personal', '2026-11-28', '00:00:00', NULL, 1440, 2),
    ('EVT-0004', 'Defensa Evaluación 4', 'Sala 101, Santo Tomás',
     POINT(-70.6506, -33.4378),
     '{"notas": "Demostración y defensa oral", "ramo": "POO IEI-085"}',
     'estudio', '2026-11-30', '08:30:00', '10:00:00', 90, 1);

-- Inserción mínima: solo los obligatorios sin DEFAULT; el resto toma su valor por defecto
INSERT INTO evento (codigo_evento, titulo, fecha_evento, hora_inicio)
VALUES ('EVT-0005', 'Pagar cuentas', '2026-12-05', '20:00:00');

-- =====================================================================
-- Verificación (criterios de la Tabla 3)
-- =====================================================================
DESCRIBE usuario;
DESCRIBE evento;
SELECT id_usuario, nombre_usuario, rol, activo FROM usuario;
SELECT id_evento, codigo_evento, titulo, fecha_evento, hora_inicio, prioridad, activo
FROM evento ORDER BY fecha_evento, hora_inicio;

-- POINT: leer la coordenada (ST_X = longitud, ST_Y = latitud)
SELECT codigo_evento, ST_X(ubicacion) AS longitud, ST_Y(ubicacion) AS latitud
FROM evento WHERE ubicacion IS NOT NULL;

-- JSON: sacar un campo del documento
SELECT codigo_evento, JSON_UNQUOTE(JSON_EXTRACT(detalle, '$.notas')) AS notas
FROM evento WHERE detalle IS NOT NULL;

-- =====================================================================
-- Pruebas de restricciones (descomentar una a la vez: TODAS deben FALLAR)
-- =====================================================================
-- UNIQUE: código repetido      -> Error 1062 Duplicate entry 'EVT-0001'
-- INSERT INTO evento (codigo_evento, titulo, fecha_evento, hora_inicio)
-- VALUES ('EVT-0001', 'Duplicado', '2026-12-10', '10:00:00');

-- NOT NULL: falta el título     -> Error 1364 Field 'titulo' doesn't have a default value
-- INSERT INTO evento (codigo_evento, fecha_evento, hora_inicio)
-- VALUES ('EVT-0099', '2026-12-10', '10:00:00');

-- CHECK: prioridad fuera de rango -> MariaDB 4025 / MySQL 3819
-- INSERT INTO evento (codigo_evento, titulo, fecha_evento, hora_inicio, prioridad)
-- VALUES ('EVT-0098', 'Mala prioridad', '2026-12-10', '10:00:00', 7);

-- CHECK: JSON inválido          -> MariaDB 4025 / MySQL 3140
-- INSERT INTO evento (codigo_evento, titulo, fecha_evento, hora_inicio, detalle)
-- VALUES ('EVT-0097', 'Mal JSON', '2026-12-10', '10:00:00', 'esto no es json');
