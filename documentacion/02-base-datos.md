# Modelo de Base de Datos

## 1. Objetivo

Este documento define la estructura inicial de la base de datos del MVP de portafolio.

La base de datos será PostgreSQL.

Las tablas utilizarán nombres en español para facilitar el aprendizaje y mantener consistencia con la documentación.

La creación y evolución del esquema se administrará mediante Flyway.

---

# 2. Principios generales

La base de datos deberá:

- conservar información histórica,
- evitar datos duplicados innecesariamente,
- permitir trazabilidad,
- soportar cálculos reproducibles,
- proteger reglas básicas mediante restricciones,
- evolucionar mediante migraciones versionadas.

---

# 3. Convenciones de nombres

Las tablas utilizarán:

snake_case

Ejemplos:

activo

precio_activo

asignacion_objetivo

fotografia_portafolio

Las columnas también utilizarán snake_case.

Ejemplos:

fecha_aporte

monto_neto_broker

precio_unitario

---

# 4. Claves primarias

Las tablas utilizarán una clave primaria numérica:

id

Tipo sugerido:

BIGINT

La generación concreta del identificador se definirá en la migración SQL.

---

# 5. Fechas

## 5.1 Fechas de negocio

Se utilizará:

DATE

para:

- fecha_aporte,
- fecha_transaccion,
- fecha_referido,
- fecha_dividendo,
- fecha_precio,
- fecha_fotografia.

---

## 5.2 Fechas técnicas

Las tablas principales podrán incluir:

- fecha_creacion,
- fecha_actualizacion.

Tipo sugerido:

TIMESTAMP WITH TIME ZONE

Estas fechas representan eventos técnicos del sistema.

---

# 6. Valores monetarios

Los valores monetarios no utilizarán:

FLOAT

ni:

DOUBLE PRECISION

Se utilizará:

NUMERIC

Sugerencia inicial:

NUMERIC(19,6)

para:

- montos,
- precios,
- comisiones,
- costos.

---

# 7. Cantidades de activos

Las cantidades de activos pueden tener muchos decimales.

Ejemplo:

BTC = 0.00001196

Por ello se utilizará una precisión mayor.

Sugerencia inicial:

NUMERIC(28,12)

---

# 8. Estados

Cuando una tabla realmente necesite representar un estado funcional,
se utilizará una columna `estado` con valores explícitos en lugar de
booleanos ambiguos.

La existencia de esta convención no implica que todas las tablas deban
tener estado.

La tabla `activo` no utiliza estado en el MVP.

# 9. Tabla activo

Representa un activo financiero.

Nombre:

activo

## Campos

- id BIGINT GENERATED ALWAYS AS IDENTITY
- simbolo VARCHAR(20) NOT NULL
- nombre VARCHAR(150) NOT NULL
- tipo_activo VARCHAR(30) NOT NULL
- moneda VARCHAR(10) NOT NULL
- fecha_creacion TIMESTAMPTZ NOT NULL
- fecha_actualizacion TIMESTAMPTZ NOT NULL

## Restricciones

- PRIMARY KEY(id)
- uk_activo_simbolo: UNIQUE(simbolo)
- ck_activo_tipo: tipo_activo IN ('ACCION', 'ETF', 'CRIPTOMONEDA')
- ck_activo_moneda: moneda IN ('USD')

El símbolo se almacena normalizado (sin espacios en los extremos y en mayúsculas).

Agregar una moneda requiere una nueva migración que modifique `ck_activo_moneda`.

## Valores iniciales de tipo_activo

- ACCION
- ETF
- CRIPTOMONEDA

## Ejemplos

IVV

VXUS

BTC

# 10. Tabla aporte

Representa dinero enviado al intermediario de inversión.

Nombre:

aporte

## Campos

- id BIGINT
- fecha_aporte DATE NOT NULL
- monto_aportado NUMERIC(19,6) NOT NULL
- monto_neto_broker NUMERIC(19,6) NOT NULL
- costo_fondeo NUMERIC(19,6) NOT NULL
- metodo_fondeo VARCHAR(50)
- comentario TEXT
- fecha_creacion TIMESTAMPTZ NOT NULL
- fecha_actualizacion TIMESTAMPTZ NOT NULL

## Reglas

- monto_aportado > 0
- monto_neto_broker >= 0
- costo_fondeo >= 0

Cálculo esperado:

costo_fondeo =
monto_aportado - monto_neto_broker

---

# 11. Tabla transaccion

Representa una compra o venta.

Nombre:

transaccion

## Campos

- id BIGINT
- activo_id BIGINT NOT NULL
- tipo_transaccion VARCHAR(20) NOT NULL
- fecha_transaccion DATE NOT NULL
- cantidad NUMERIC(28,12) NOT NULL
- precio_unitario NUMERIC(19,6) NOT NULL
- comision NUMERIC(19,6) NOT NULL DEFAULT 0
- comentario TEXT
- fecha_creacion TIMESTAMPTZ NOT NULL
- fecha_actualizacion TIMESTAMPTZ NOT NULL

## Relación

activo_id
→ activo.id

## Valores de tipo_transaccion

- COMPRA
- VENTA

## Reglas

- cantidad > 0
- precio_unitario > 0
- comision >= 0

## Índices sugeridos

- activo_id
- fecha_transaccion
- activo_id + fecha_transaccion

---

# 12. Tabla referido

Representa un activo recibido mediante un programa de referidos.

Nombre:

referido

## Campos

- id BIGINT
- activo_id BIGINT NOT NULL
- fecha_referido DATE NOT NULL
- origen VARCHAR(100)
- cantidad NUMERIC(28,12) NOT NULL
- precio_al_recibir NUMERIC(19,6) NOT NULL
- valor_recibido NUMERIC(19,6) NOT NULL
- comentario TEXT
- fecha_creacion TIMESTAMPTZ NOT NULL
- fecha_actualizacion TIMESTAMPTZ NOT NULL

## Relación

activo_id
→ activo.id

## Reglas

- cantidad > 0
- precio_al_recibir > 0
- valor_recibido >= 0

Los datos:

- cantidad,
- precio_al_recibir,
- valor_recibido

representan información histórica y no deben recalcularse posteriormente.

---

# 13. Tabla dividendo

Representa un dividendo recibido.

Nombre:

dividendo

## Campos

- id BIGINT
- activo_id BIGINT NOT NULL
- fecha_dividendo DATE NOT NULL
- monto_bruto NUMERIC(19,6) NOT NULL
- retencion NUMERIC(19,6) NOT NULL DEFAULT 0
- otros_cargos NUMERIC(19,6) NOT NULL DEFAULT 0
- monto_neto NUMERIC(19,6) NOT NULL
- reinvertido BOOLEAN NOT NULL DEFAULT FALSE
- comentario TEXT
- fecha_creacion TIMESTAMPTZ NOT NULL
- fecha_actualizacion TIMESTAMPTZ NOT NULL

## Relación

activo_id
→ activo.id

## Reglas

- monto_bruto >= 0
- retencion >= 0
- otros_cargos >= 0
- monto_neto >= 0

Cálculo esperado:

monto_neto =
monto_bruto

- retencion
- otros_cargos

---

# 14. Tabla precio_activo

Representa el precio histórico de un activo.

Nombre:

precio_activo

## Campos

- id BIGINT
- activo_id BIGINT NOT NULL
- fecha_precio DATE NOT NULL
- precio NUMERIC(19,6) NOT NULL
- fuente VARCHAR(100)
- comentario TEXT
- fecha_creacion TIMESTAMPTZ NOT NULL

## Relación

activo_id
→ activo.id

## Reglas

- precio > 0

## Restricción

UNIQUE(activo_id, fecha_precio)

Esto impide registrar dos precios para el mismo activo en la misma fecha.

---

# 15. Tabla asignacion_objetivo

Representa el porcentaje deseado para un activo.

Nombre:

asignacion_objetivo

## Campos

- id BIGINT GENERATED ALWAYS AS IDENTITY
- activo_id BIGINT NOT NULL
- porcentaje_objetivo NUMERIC(8,4) NOT NULL
- fecha_creacion TIMESTAMPTZ NOT NULL
- fecha_actualizacion TIMESTAMPTZ NOT NULL

## Relación

activo_id
→ activo.id

## Restricciones

- uk_asignacion_activo: UNIQUE(activo_id)
- ck_asignacion_rango: porcentaje_objetivo >= 0 AND porcentaje_objetivo <= 100

## Reglas

- La aplicación deberá advertir cuando la suma de las asignaciones no sea 100%.
- Para excluir un activo del objetivo se asigna 0%.
- Un activo sin fila en esta tabla no tiene objetivo; para la suma cuenta como 0%.
- Crear un activo no crea su asignación.

---

# 16. Tabla fotografia_portafolio

Representa una fotografía histórica del portafolio.

Nombre:

fotografia_portafolio

## Campos

- id BIGINT
- fecha_fotografia DATE NOT NULL
- total_aportado NUMERIC(19,6) NOT NULL
- valor_inversiones NUMERIC(19,6) NOT NULL
- valor_referidos NUMERIC(19,6) NOT NULL
- efectivo NUMERIC(19,6) NOT NULL
- valor_total NUMERIC(19,6) NOT NULL
- costos_acumulados NUMERIC(19,6) NOT NULL
- resultado_vs_bolsillo NUMERIC(19,6) NOT NULL
- rentabilidad_vs_bolsillo NUMERIC(12,6) NOT NULL
- fecha_creacion TIMESTAMPTZ NOT NULL

## Restricción inicial

UNIQUE(fecha_fotografia)

En el MVP se permitirá una fotografía por día.

---

# 17. Posición

No existirá una tabla llamada:

posicion

La posición será un resultado calculado a partir de:

- transaccion,
- referido,
- precio_activo,
- asignacion_objetivo.

Esto evita inconsistencias entre movimientos históricos y cantidades actuales.

---

# 18. Resumen del portafolio

No existirá una tabla llamada:

resumen_portafolio

El resumen se calculará utilizando:

- aporte,
- transaccion,
- referido,
- dividendo,
- precio_activo,
- asignacion_objetivo.

---

# 19. Tutoriales

Los tutoriales podrán incorporarse posteriormente.

En la primera implementación se podrá mantener el contenido de ayuda en archivos de configuración o en el frontend.

Si posteriormente se necesita administrar tutoriales desde la base de datos, se añadirá mediante una nueva migración.

No se creará una tabla de tutoriales anticipadamente.

---

# 20. Relaciones principales

Conceptualmente:

activo
│
├── transaccion
├── referido
├── dividendo
├── precio_activo
└── asignacion_objetivo

aporte

fotografia_portafolio

---

# 21. Diagrama simplificado

ACTIVO
|
|---- TRANSACCION
|
|---- REFERIDO
|
|---- DIVIDENDO
|
|---- PRECIO_ACTIVO
|
└---- ASIGNACION_OBJETIVO

APORTE

FOTOGRAFIA_PORTAFOLIO
