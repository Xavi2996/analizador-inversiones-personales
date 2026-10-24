# Modelo Funcional

## 1. Objetivo

Este documento define los conceptos principales del MVP de portafolio y las reglas funcionales que deben cumplir.

La intención es describir el comportamiento del sistema sin depender todavía de:

- Java,
- Spring Boot,
- PostgreSQL,
- JPA,
- Angular,
- contratos REST.

El sistema debe representar correctamente el comportamiento validado en:

`referencia/registro_inversiones_v4_1.xlsx`

---

# 2. Activo

## 2.1 Definición

Un activo representa un instrumento financiero que puede formar parte del portafolio.

Ejemplos actuales:

- IVV
- VXUS
- BTC

---

## 2.2 Información principal

Un activo debe contener:

- símbolo,
- nombre,
- tipo de activo,
- moneda,
- porcentaje objetivo,

---

## 2.3 Tipos iniciales

Los tipos permitidos inicialmente serán:

- ACCION
- ETF
- CRIPTOMONEDA

---

## 2.4 Reglas

- El símbolo debe identificar de forma única al activo.
- Un activo puede existir aunque todavía no tenga transacciones.
- Un activo permanece registrado aunque actualmente no exista una posición abierta.
- La existencia de una posición depende de su cantidad actual calculada, no de un estado manual del activo.
- Si la cantidad actual de un activo es cero, no debe aparecer entre las posiciones actuales.
- El historial del activo debe conservarse aunque su cantidad actual sea cero.
- El tipo del activo no deberá ingresarse repetidamente en cada transacción.
- El porcentaje objetivo podrá modificarse sin alterar operaciones históricas.

---

# 3. Aporte

## 3.1 Definición

Un aporte representa dinero que sale del bolsillo del usuario y es enviado al intermediario de inversión.

Ejemplo:

El usuario envía:

250 USD

pero después de costos llegan:

243.33 USD

Entonces:

- monto aportado = 250.00
- monto neto recibido = 243.33
- costo de fondeo = 6.67

---

## 3.2 Información principal

Un aporte contiene:

- fecha,
- monto aportado,
- monto neto recibido,
- costo de fondeo,
- método de fondeo,
- comentario.

---

## 3.3 Reglas

- El monto aportado debe ser mayor que cero.
- El monto neto recibido no puede ser negativo.
- El costo de fondeo no puede ser negativo.
- El monto neto recibido no debe superar el monto aportado salvo que exista una razón explícita.
- El costo de fondeo puede calcularse como:

monto aportado - monto neto recibido

- Un aporte aumenta el total aportado desde bolsillo.
- Un aporte aumenta el dinero disponible en el intermediario únicamente por el monto neto recibido.

---

# 4. Transacción

## 4.1 Definición

Una transacción representa una operación de compra o venta de un activo.

Tipos:

- COMPRA
- VENTA

---

## 4.2 Información principal

Una transacción contiene:

- fecha,
- activo,
- tipo,
- cantidad,
- precio unitario,
- comisión,
- comentario.

---

## 4.3 Monto de operación

El monto bruto se calcula como:

cantidad × precio unitario

---

## 4.4 Compra

Una compra:

- aumenta la cantidad del activo,
- consume efectivo,
- puede generar comisión.

Conceptualmente:

efectivo utilizado =
monto bruto + comisión

---

## 4.5 Venta

Una venta:

- disminuye la cantidad del activo,
- genera efectivo,
- puede generar comisión.

Conceptualmente:

efectivo recibido =
monto bruto - comisión

---

## 4.6 Reglas

- La cantidad debe ser mayor que cero.
- El precio debe ser mayor que cero.
- La comisión no puede ser negativa.
- No se debe permitir una venta superior a la cantidad disponible.
- Las posiciones deben derivarse de las transacciones.
- Una transacción histórica no debe alterar automáticamente otras operaciones.

---

# 5. Referido

## 5.1 Definición

Un referido representa un activo recibido mediante un programa promocional o de recomendación.

No representa dinero aportado desde bolsillo.

Ejemplo actual:

- activo: BTC
- cantidad: 0.00001196
- precio al recibir: 83580.27 USD
- valor recibido: 1.00 USD

---

## 5.2 Información principal

Un referido contiene:

- fecha,
- origen,
- activo,
- cantidad,
- precio al recibir,
- valor recibido,
- comentario.

---

## 5.3 Datos históricos

Los siguientes valores representan el momento en que el referido fue recibido:

- cantidad,
- precio al recibir,
- valor recibido.

Estos valores no deben cambiar posteriormente.

---

## 5.4 Valor actual

El valor actual del referido se calcula como:

cantidad × precio actual del activo

---

## 5.5 Variación desde la entrega

La variación se calcula como:

valor actual - valor recibido

---

## 5.6 Reglas

- La cantidad debe ser mayor que cero.
- El valor recibido debe ser mayor o igual a cero.
- El referido aumenta la cantidad actual del activo.
- El referido no aumenta el total aportado desde bolsillo.
- El referido no debe registrarse como compra.
- El referido no debe registrarse como dividendo.

---

# 6. Dividendo

## 6.1 Definición

Un dividendo representa dinero recibido por mantener un activo.

---

## 6.2 Información principal

Un dividendo contiene:

- fecha,
- activo,
- monto bruto,
- retención,
- otros cargos,
- monto neto,
- indicador de reinversión,
- comentario.

---

## 6.3 Cálculo

El monto neto se calcula como:

monto bruto

- retención
- otros cargos

---

## 6.4 Reglas

- Los montos no pueden ser negativos.
- El monto neto debe corresponder al cálculo establecido.
- Un dividendo recibido aumenta el efectivo disponible.
- Si el dividendo se reinvierte, debe existir una compra independiente.
- La reinversión no debe descontarse dos veces del efectivo.

---

# 7. Precio del activo

## 7.1 Definición

Representa la cotización de un activo en una fecha determinada.

---

## 7.2 Información principal

Contiene:

- activo,
- fecha,
- precio,
- fuente.

---

## 7.3 Reglas

- El precio debe ser mayor que cero.
- Se debe conservar el historial.
- Registrar un precio nuevo no debe eliminar el anterior.
- El precio actual será el registro más reciente disponible.
- Si no existe precio disponible, el sistema debe indicar que no puede valorar el activo correctamente.

---

# 8. Asignación objetivo

## 8.1 Definición

Representa el porcentaje deseado del portafolio para cada activo.

Ejemplo actual:

- IVV = 85%
- VXUS = 10%
- BTC = 5%

---

## 8.2 Reglas

- Cada porcentaje debe estar entre 0% y 100%.
- La suma de asignaciones activas debería ser 100%.
- Si no suma 100%, el sistema debe advertirlo.
- El sistema no realizará operaciones automáticamente para corregir diferencias.

---

# 9. Posición

## 9.1 Definición

Una posición representa el estado actual de un activo dentro del portafolio.

Es un concepto calculado.

No se registra manualmente.

---

## 9.2 Cantidad actual

Se calcula como:

cantidad comprada

- cantidad vendida

* cantidad recibida por referidos

---

## 9.3 Información calculada

Una posición debe mostrar:

- cantidad comprada,
- cantidad vendida,
- cantidad referida,
- cantidad actual,
- costo de compras,
- costo promedio,
- precio actual,
- valor actual,
- ganancia o pérdida no realizada,
- rentabilidad,
- porcentaje actual,
- porcentaje objetivo,
- diferencia contra objetivo.

---

## 9.4 Reglas

- La posición no es fuente de verdad.
- La fuente de verdad son las operaciones históricas.
- La posición no puede editarse manualmente.
- Si cambia una operación válida, la posición debe recalcularse.
- Si cambia el último precio, el valor actual debe recalcularse.

---

# 10. Costo promedio

## 10.1 Definición

Representa el costo promedio pagado por unidad de un activo.

Inicialmente se utilizará para las compras existentes.

---

## 10.2 Consideración sobre ventas

Cuando se incorporen ventas, el cálculo deberá utilizar un criterio definido y consistente.

Antes de implementar ventas con ganancia realizada se deberá especificar claramente el método utilizado.

Ejemplos posibles:

- costo promedio,
- FIFO.

El MVP no deberá asumir silenciosamente un método distinto al documentado.

---

# 11. Efectivo disponible

## 11.1 Definición

Representa el dinero disponible que todavía no se encuentra invertido.

Es un valor calculado.

---

## 11.2 Cálculo conceptual

El efectivo considera:

monto neto recibido por aportes

menos:

- compras,
- comisiones de compra,

más:

- ventas netas,
- dividendos netos,

menos cualquier otra salida de efectivo válida.

---

## 11.3 Reglas

- El efectivo no se registra manualmente.
- Debe poder explicarse a partir de movimientos históricos.
- Un referido en especie no aumenta el efectivo.
- Un dividendo reinvertido se refleja mediante su dividendo y una compra independiente.

---

# 12. Resumen del portafolio

## 12.1 Definición

Representa el estado general actual del portafolio.

Es un concepto calculado.

---

## 12.2 Información principal

Debe mostrar al menos:

- total aportado desde bolsillo,
- monto neto recibido,
- costos de fondeo,
- monto utilizado en compras,
- comisiones,
- valor actual de inversiones,
- valor actual de referidos,
- efectivo disponible,
- valor total del portafolio,
- resultado frente al dinero aportado,
- rentabilidad frente al dinero aportado.

---

## 12.3 Valor total

Conceptualmente:

valor de posiciones

- efectivo disponible

El valor de las posiciones incluye los activos provenientes de compras y referidos.

---

## 12.4 Resultado frente al dinero aportado

Conceptualmente:

valor total del portafolio

- total aportado desde bolsillo

---

## 12.5 Rentabilidad frente al dinero aportado

Conceptualmente:

resultado frente al dinero aportado
/
total aportado desde bolsillo

---

## 12.6 Reglas

- No debe editarse manualmente.
- Debe recalcularse usando las operaciones originales.
- Debe diferenciar claramente aportes, costos y rendimiento.

---

# 13. Fotografía del portafolio

## 13.1 Definición

Representa el estado del portafolio en una fecha determinada.

Su objetivo es conservar el histórico de rendimiento.

---

## 13.2 Información

Debe contener:

- fecha,
- total aportado,
- valor de inversiones,
- valor de referidos,
- efectivo,
- valor total,
- costos acumulados,
- resultado frente al dinero aportado,
- rentabilidad.

---

## 13.3 Reglas

- Una fotografía histórica no debe cambiar por precios futuros.
- Los datos deben representar el estado calculado al momento de crearla.
- Permitirá construir gráficos de evolución.
- Permitirá diferenciar crecimiento por nuevos aportes de crecimiento por rendimiento.

---

# 14. Tutorial

## 14.1 Definición

Representa contenido de ayuda para utilizar correctamente el sistema.

---

## 14.2 Temas iniciales

- activos,
- aportes,
- compras,
- ventas,
- referidos,
- dividendos,
- precios,
- posiciones,
- resumen,
- asignación objetivo,
- rendimiento histórico.

---

## 14.3 Reglas

- La ayuda debe estar disponible desde la interfaz.
- Consultar ayuda no debe provocar pérdida de información ingresada en un formulario.
- Los tutoriales deben utilizar lenguaje sencillo.
- Cuando sea útil, deberán incluir ejemplos.

---

# 15. Fuente de verdad

## 15.1 Datos ingresados

Son fuente de verdad:

- activos,
- aportes,
- transacciones,
- referidos,
- dividendos,
- precios,
- asignaciones objetivo.

---

## 15.2 Datos derivados

No son fuente de verdad:

- posiciones,
- efectivo disponible,
- costo promedio,
- valor actual,
- porcentaje actual,
- ganancia o pérdida,
- resumen del portafolio.

Estos valores deben calcularse.

---

# 16. Historial

El sistema debe priorizar la conservación histórica.

Ejemplos:

Nuevo precio:

→ nuevo registro.

Nueva compra:

→ nueva transacción.

Nueva venta:

→ nueva transacción.

Nuevo snapshot:

→ nueva fotografía.

No se debe sobrescribir información histórica cuando represente un evento distinto.

---

# 17. Reglas generales

1. Los cálculos deben poder explicarse.
2. Los cálculos deben ser reproducibles.
3. Los datos calculados no se editan manualmente.
4. Las operaciones históricas son la base de los cálculos.
5. Los referidos no son aportes.
6. Los dividendos reinvertidos generan una compra independiente.
7. Los precios conservan historial.
8. Las fotografías conservan el estado histórico.
9. El backend será la fuente oficial para cálculos financieros.
10. Las reglas se validarán utilizando el Excel de referencia.
