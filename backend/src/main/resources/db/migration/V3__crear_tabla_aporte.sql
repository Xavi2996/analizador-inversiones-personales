CREATE TABLE aporte (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fecha_aporte        DATE          NOT NULL,
    monto_aportado      NUMERIC(19,6) NOT NULL,
    monto_neto_broker   NUMERIC(19,6) NOT NULL,
    costo_fondeo        NUMERIC(19,6) NOT NULL,
    metodo_fondeo       VARCHAR(50),
    comentario          TEXT,
    fecha_creacion      TIMESTAMPTZ   NOT NULL,
    fecha_actualizacion TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_aporte_monto_aportado CHECK (monto_aportado > 0),
    CONSTRAINT ck_aporte_monto_neto CHECK (monto_neto_broker >= 0 AND monto_neto_broker <= monto_aportado),
    CONSTRAINT ck_aporte_costo_fondeo CHECK (costo_fondeo = monto_aportado - monto_neto_broker)
);
