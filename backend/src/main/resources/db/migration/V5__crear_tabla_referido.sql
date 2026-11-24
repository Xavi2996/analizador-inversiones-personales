CREATE TABLE referido (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    activo_id           BIGINT         NOT NULL,
    fecha_referido      DATE           NOT NULL,
    origen              VARCHAR(100)   NOT NULL,
    cantidad            NUMERIC(28,12) NOT NULL,
    precio_al_recibir   NUMERIC(19,6)  NOT NULL,
    valor_recibido      NUMERIC(19,6)  NOT NULL,
    comentario          TEXT,
    fecha_creacion      TIMESTAMPTZ    NOT NULL,
    fecha_actualizacion TIMESTAMPTZ    NOT NULL,
    CONSTRAINT fk_referido_activo FOREIGN KEY (activo_id) REFERENCES activo (id),
    CONSTRAINT ck_referido_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_referido_precio CHECK (precio_al_recibir > 0),
    CONSTRAINT ck_referido_valor_recibido CHECK (valor_recibido = ROUND(cantidad * precio_al_recibir, 2))
);

CREATE INDEX ix_referido_activo_fecha ON referido (activo_id, fecha_referido);
