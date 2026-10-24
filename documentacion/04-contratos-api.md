# Contratos API

## 1. Objetivo

Este documento define los contratos iniciales de la API REST del MVP.

La API será consumida por Angular y expuesta por Spring Boot.

Todos los nombres funcionales estarán en español.

Prefijo general:

/api/v1

Ejemplo:

/api/v1/activos

---

# 2. Principios

Los contratos deberán ser:

- claros,
- predecibles,
- consistentes,
- fáciles de validar,
- independientes de las entidades JPA,
- compatibles con el enfoque guiado por especificaciones.

El frontend no deberá depender directamente de entidades de persistencia.

Se utilizarán DTO para solicitudes y respuestas.

---

# 3. Formato

La API utilizará:

Content-Type: application/json

Las respuestas serán JSON.

---

# 4. Fechas

Las fechas de negocio utilizarán:

AAAA-MM-DD

Ejemplo:

2026-10-22

---

# 5. Identificadores

Los identificadores serán numéricos.

Ejemplo:

{
"id": 1
}

---

# 6. Valores monetarios

Los valores monetarios se enviarán como números decimales.

Ejemplo:

{
"montoAportado": 250.00
}

En backend serán tratados con BigDecimal.

---

# 7. Respuesta estándar de error

Los errores deberán seguir una estructura común.

Ejemplo:

{
"fechaHora": "2026-10-22T15:00:00Z",
"estadoHttp": 400,
"codigo": "DATOS_INVALIDOS",
"mensaje": "La solicitud contiene datos inválidos.",
"ruta": "/api/v1/aportes",
"erroresCampos": [
{
"campo": "montoAportado",
"mensaje": "Debe ser mayor que cero."
}
]
}

Campos:

- fechaHora
- estadoHttp
- codigo
- mensaje
- ruta
- erroresCampos

erroresCampos será opcional.

---

# 8. Activos

## 8.1 Crear activo

POST /api/v1/activos

Solicitud:

{
"simbolo": "IVV",
"nombre": "iShares Core S&P 500 ETF",
"tipoActivo": "ETF",
"moneda": "USD"
}

Respuesta 201:

{
"id": 1,
"simbolo": "IVV",
"nombre": "iShares Core S&P 500 ETF",
"tipoActivo": "ETF",
"moneda": "USD",
}

---

## 8.2 Listar activos

GET /api/v1/activos

Respuesta 200:

[
{
"id": 1,
"simbolo": "IVV",
"nombre": "iShares Core S&P 500 ETF",
"tipoActivo": "ETF",
"moneda": "USD",
}
]

---

## 8.3 Obtener activo

GET /api/v1/activos/{id}

---

## 8.4 Actualizar activo

PUT /api/v1/activos/{id}

Solicitud:

{
"nombre": "iShares Core S&P 500 ETF",
"tipoActivo": "ETF",
"moneda": "USD"
}

---

# 9. Aportes

## 9.1 Registrar aporte

POST /api/v1/aportes

Solicitud:

{
"fechaAporte": "2026-10-22",
"montoAportado": 200.00,
"montoNetoBroker": 195.58,
"metodoFondeo": "BINANCE",
"comentario": "Aporte mensual"
}

El backend calculará:

costoFondeo =
montoAportado - montoNetoBroker

Respuesta 201:

{
"id": 1,
"fechaAporte": "2026-10-22",
"montoAportado": 200.00,
"montoNetoBroker": 195.58,
"costoFondeo": 4.42,
"metodoFondeo": "BINANCE",
"comentario": "Aporte mensual"
}

---

## 9.2 Listar aportes

GET /api/v1/aportes

Filtros futuros:

?desde=
?hasta=

---

## 9.3 Obtener aporte

GET /api/v1/aportes/{id}

---

# 10. Transacciones

## 10.1 Registrar transacción

POST /api/v1/transacciones

Solicitud de compra:

{
"activoId": 1,
"tipoTransaccion": "COMPRA",
"fechaTransaccion": "2026-10-22",
"cantidad": 0.12000000,
"precioUnitario": 770.50,
"comision": 0.15,
"comentario": "Compra IVV"
}

Respuesta 201:

{
"id": 1,
"activoId": 1,
"simbolo": "IVV",
"tipoTransaccion": "COMPRA",
"fechaTransaccion": "2026-10-22",
"cantidad": 0.12000000,
"precioUnitario": 770.50,
"montoBruto": 92.46,
"comision": 0.15,
"comentario": "Compra IVV"
}

---

## 10.2 Registrar venta

Se utilizará el mismo endpoint:

POST /api/v1/transacciones

Solicitud:

{
"activoId": 1,
"tipoTransaccion": "VENTA",
"fechaTransaccion": "2027-01-15",
"cantidad": 0.05000000,
"precioUnitario": 830.00,
"comision": 0.15
}

Antes de implementar ventas deberá existir una especificación aprobada sobre:

- costo de venta,
- ganancia realizada,
- método de costo.

---

## 10.3 Listar transacciones

GET /api/v1/transacciones

Filtros previstos:

?activoId=
?tipo=
?desde=
?hasta=

---

## 10.4 Obtener transacción

GET /api/v1/transacciones/{id}

---

# 11. Referidos

## 11.1 Registrar referido

POST /api/v1/referidos

Solicitud:

{
"activoId": 3,
"fechaReferido": "2026-10-13",
"origen": "HAPI",
"cantidad": 0.00001196,
"precioAlRecibir": 83580.27,
"valorRecibido": 1.00,
"comentario": "Premio por referido"
}

Respuesta 201:

{
"id": 1,
"activoId": 3,
"simbolo": "BTC",
"fechaReferido": "2026-10-13",
"origen": "HAPI",
"cantidad": 0.00001196,
"precioAlRecibir": 83580.27,
"valorRecibido": 1.00,
"comentario": "Premio por referido"
}

---

## 11.2 Listar referidos

GET /api/v1/referidos

---

## 11.3 Obtener referido

GET /api/v1/referidos/{id}

---

# 12. Dividendos

## 12.1 Registrar dividendo

POST /api/v1/dividendos

Solicitud:

{
"activoId": 1,
"fechaDividendo": "2026-12-20",
"montoBruto": 10.00,
"retencion": 3.00,
"otrosCargos": 0.00,
"reinvertido": false,
"comentario": "Dividendo trimestral"
}

El backend calculará:

montoNeto =
montoBruto

- retencion
- otrosCargos

Respuesta 201:

{
"id": 1,
"activoId": 1,
"simbolo": "IVV",
"fechaDividendo": "2026-12-20",
"montoBruto": 10.00,
"retencion": 3.00,
"otrosCargos": 0.00,
"montoNeto": 7.00,
"reinvertido": false
}

---

## 12.2 Listar dividendos

GET /api/v1/dividendos

---

# 13. Precios

## 13.1 Registrar precio

POST /api/v1/precios

Solicitud:

{
"activoId": 1,
"fechaPrecio": "2026-10-22",
"precio": 771.09,
"fuente": "HAPI"
}

Respuesta 201:

{
"id": 1,
"activoId": 1,
"simbolo": "IVV",
"fechaPrecio": "2026-10-22",
"precio": 771.09,
"fuente": "HAPI"
}

---

## 13.2 Listar precios de un activo

GET /api/v1/activos/{activoId}/precios

---

## 13.3 Obtener último precio

GET /api/v1/activos/{activoId}/precios/ultimo

Respuesta:

{
"activoId": 1,
"simbolo": "IVV",
"fechaPrecio": "2026-10-22",
"precio": 771.09
}

---

# 14. Asignación objetivo

## 14.1 Crear o actualizar asignación

PUT /api/v1/asignaciones/{activoId}

Solicitud:

{
"porcentajeObjetivo": 85.00
}

Respuesta:

{
"activoId": 1,
"simbolo": "IVV",
"porcentajeObjetivo": 85.00,
}

---

## 14.2 Listar asignaciones

GET /api/v1/asignaciones

---

# 15. Posiciones

## 15.1 Obtener posiciones

GET /api/v1/portafolio/posiciones

Respuesta conceptual:

[
{
"activoId": 1,
"simbolo": "IVV",
"cantidadComprada": 0.39755000,
"cantidadVendida": 0,
"cantidadReferida": 0,
"cantidadActual": 0.39755000,
"costoCompras": 308.07,
"costoPromedio": 774.90,
"precioActual": 771.09,
"valorActual": 306.54,
"gananciaPerdidaNoRealizada": -1.53,
"rentabilidadPorcentaje": -0.50,
"porcentajeActual": 84.60,
"porcentajeObjetivo": 85.00,
"diferenciaObjetivo": -0.40
}
]

Estos datos son calculados.

No existirán:

POST /api/v1/portafolio/posiciones

PUT /api/v1/portafolio/posiciones

---

# 16. Resumen

## 16.1 Obtener resumen

GET /api/v1/portafolio/resumen

Respuesta conceptual:

{
"totalAportadoBolsillo": 575.00,
"totalNetoBroker": 559.13,
"costosFondeo": 15.87,
"totalCompras": 503.07,
"comisiones": 1.02,
"efectivoDisponible": 55.04,
"valorActualPosiciones": 502.77,
"valorActualReferidos": 0.98,
"valorTotalPortafolio": 557.81,
"resultadoVsBolsillo": -17.19,
"rentabilidadVsBolsillo": -2.99
}

Los valores son ejemplos basados en el Excel de referencia.

Los resultados reales dependerán de los datos y precios almacenados.

---

# 17. Fotografías del portafolio

## 17.1 Crear fotografía

POST /api/v1/portafolio/fotografias

No requerirá body en la primera versión.

El backend calculará el estado actual.

Respuesta 201:

{
"id": 1,
"fechaFotografia": "2026-10-22",
"totalAportado": 575.00,
"valorInversiones": 501.79,
"valorReferidos": 0.98,
"efectivo": 55.04,
"valorTotal": 557.81,
"costosAcumulados": 16.89,
"resultadoVsBolsillo": -17.19,
"rentabilidadVsBolsillo": -2.99
}

---

## 17.2 Listar fotografías

GET /api/v1/portafolio/fotografias

---

# 18. Tutoriales

La estrategia definitiva de tutoriales se definirá en la especificación correspondiente.

Inicialmente podrá existir un endpoint futuro como:

GET /api/v1/ayuda/{codigo}

Pero no se considera obligatorio definirlo todavía.

No se implementarán endpoints sin una necesidad aprobada.

---

# 19. Códigos HTTP

Se utilizarán principalmente:

200 OK

Consulta o actualización correcta.

201 Created

Recurso creado correctamente.

204 No Content

Operación correcta sin contenido de respuesta.

400 Bad Request

Solicitud inválida.

404 Not Found

Recurso inexistente.

409 Conflict

Conflicto con reglas o estado actual.

500 Internal Server Error

Error inesperado.

---

# 20. Códigos funcionales de error

La API deberá utilizar códigos internos estables.

Ejemplos:

ACTIVO_NO_ENCONTRADO

SIMBOLO_DUPLICADO

APORTE_INVALIDO

TRANSACCION_INVALIDA

CANTIDAD_INSUFICIENTE

REFERIDO_INVALIDO

DIVIDENDO_INVALIDO

PRECIO_INVALIDO

PRECIO_DUPLICADO

ASIGNACION_INVALIDA

FOTOGRAFIA_YA_EXISTE

Estos códigos podrán ampliarse conforme se aprueben especificaciones.

---

# 21. Validaciones

El frontend podrá validar para mejorar la experiencia.

Sin embargo, el backend deberá volver a validar las reglas importantes.

Ejemplo:

cantidad > 0

deberá validarse aunque Angular ya haya bloqueado valores negativos.

---

# 22. Datos calculados

Los siguientes datos deberán ser calculados principalmente por backend:

- costo de fondeo,
- monto bruto,
- monto neto del dividendo,
- cantidad actual,
- costo promedio,
- efectivo,
- valor actual,
- rentabilidad,
- resumen,
- pesos del portafolio.

El frontend podrá formatearlos y representarlos, pero no será la fuente oficial de esos resultados.

---

# 23. Versionado

El prefijo inicial será:

/api/v1

Si en el futuro existe un cambio incompatible de contratos podrá considerarse:

/api/v2

No se creará una nueva versión por cada cambio pequeño.

---

# 24. DTO

No se expondrán directamente entidades JPA.

Se utilizarán objetos específicos para:

- solicitudes,
- respuestas.

Ejemplo conceptual:

CrearAporteSolicitud

AporteRespuesta

Esto permite mantener separados:

- almacenamiento,
- negocio,
- contrato HTTP.

---

# 25. Documentación de contratos

Cuando una especificación aprobada requiera modificar un contrato público, también deberá actualizarse este documento.

No se deberán realizar cambios silenciosos de API.

---

# 26. Principios de la API

1. Los endpoints estarán en español.
2. Los campos JSON estarán en español.
3. Se utilizarán DTO.
4. Las entidades JPA no se expondrán directamente.
5. Los cálculos oficiales estarán en backend.
6. Los errores tendrán una estructura consistente.
7. Los códigos de error serán estables.
8. Los contratos deberán corresponder con especificaciones aprobadas.
9. No se crearán endpoints que no tengan una necesidad clara.
10. Las modificaciones relevantes deberán documentarse.
