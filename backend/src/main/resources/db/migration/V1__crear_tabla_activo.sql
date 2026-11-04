CREATE TABLE activo (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    simbolo             VARCHAR(20)  NOT NULL,
    nombre              VARCHAR(150) NOT NULL,
    tipo_activo         VARCHAR(30)  NOT NULL,
    moneda              VARCHAR(10)  NOT NULL,
    fecha_creacion      TIMESTAMPTZ  NOT NULL,
    fecha_actualizacion TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_activo_simbolo UNIQUE (simbolo),
    CONSTRAINT ck_activo_tipo CHECK (tipo_activo IN ('ACCION', 'ETF', 'CRIPTOMONEDA')),
    CONSTRAINT ck_activo_moneda CHECK (moneda IN ('USD'))
);
