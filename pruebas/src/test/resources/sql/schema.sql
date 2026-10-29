-- =============================================================================
-- ESQUEMA RELACIONAL PARA FILA VIRTUAL DE TURNOS (PostgreSQL / Supabase)
-- =============================================================================

DROP TABLE IF EXISTS turnos CASCADE;

CREATE TABLE turnos (
    id SERIAL PRIMARY KEY,
    numero_turno INT NOT NULL,
    hora_solicitud TIME NOT NULL,
    hora_atencion TIME NULL,
    estado VARCHAR(20) NOT NULL,

    -- 1. Regla de unicidad: Ningún número de turno puede repetirse
    CONSTRAINT uq_numero_turno UNIQUE (numero_turno),

    -- 2. Dominio válido de estados del turno
    CONSTRAINT chk_estado_valido CHECK (estado IN ('atendido', 'pendiente', 'cancelado')),

    -- 3. Consistencia de estado: Si pasa a 'atendido', hora_atencion es obligatoria
    CONSTRAINT chk_atendido_requiere_hora CHECK (
        estado <> 'atendido' OR hora_atencion IS NOT NULL
    ),

    -- 4. Consistencia temporal: La hora de atención nunca puede ser anterior a la hora de solicitud
    CONSTRAINT chk_orden_temporal_atencion CHECK (
        hora_atencion IS NULL OR hora_atencion >= hora_solicitud
    )
);

-- Índice para optimizar la consulta FIFO de turnos pendientes por hora de solicitud
CREATE INDEX idx_turnos_pendientes_fifo
    ON turnos (hora_solicitud ASC)
    WHERE estado = 'pendiente';
