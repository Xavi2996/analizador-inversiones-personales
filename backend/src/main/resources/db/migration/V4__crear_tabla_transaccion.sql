CREATE TABLE transaccion (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    activo_id           BIGINT         NOT NULL,
    tipo_transaccion    VARCHAR(20)    NOT NULL,
    fecha_transaccion   DATE           NOT NULL,
    cantidad            NUMERIC(28,12) NOT NULL,
    precio_unitario     NUMERIC(19,6)  NOT NULL,
    monto_bruto         NUMERIC(19,6)  NOT NULL,
    comision            NUMERIC(19,6)  NOT NULL DEFAULT 0,
    comentario          TEXT,
    fecha_creacion      TIMESTAMPTZ    NOT NULL,
    fecha_actualizacion TIMESTAMPTZ    NOT NULL,
    CONSTRAINT fk_transaccion_activo FOREIGN KEY (activo_id) REFERENCES activo (id),
    CONSTRAINT ck_transaccion_tipo CHECK (tipo_transaccion IN ('COMPRA', 'VENTA')),
    CONSTRAINT ck_transaccion_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_transaccion_precio CHECK (precio_unitario > 0),
    CONSTRAINT ck_transaccion_comision CHECK (comision >= 0),
    CONSTRAINT ck_transaccion_monto_bruto CHECK (monto_bruto = ROUND(cantidad * precio_unitario, 2))
);

CREATE INDEX ix_transaccion_activo_fecha ON transaccion (activo_id, fecha_transaccion);
CREATE INDEX ix_transaccion_fecha ON transaccion (fecha_transaccion);
