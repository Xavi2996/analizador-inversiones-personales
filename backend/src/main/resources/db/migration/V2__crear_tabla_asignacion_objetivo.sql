CREATE TABLE asignacion_objetivo (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    activo_id           BIGINT       NOT NULL REFERENCES activo (id),
    porcentaje_objetivo NUMERIC(8,4) NOT NULL,
    fecha_creacion      TIMESTAMPTZ  NOT NULL,
    fecha_actualizacion TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_asignacion_activo UNIQUE (activo_id),
    CONSTRAINT ck_asignacion_rango CHECK (porcentaje_objetivo >= 0 AND porcentaje_objetivo <= 100)
);
