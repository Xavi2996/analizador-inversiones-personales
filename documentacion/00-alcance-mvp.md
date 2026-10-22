# Alcance del MVP

## 1. Nombre del proyecto

Analizador de Inversiones Personales

---

## 2. Objetivo

Construir una aplicación web personal para registrar, consultar y analizar un portafolio de inversiones.

La primera versión del sistema estará enfocada únicamente en la gestión del portafolio.

El comportamiento funcional deberá tomar como referencia el archivo:

`referencia/registro_inversiones_v4_1.xlsx`

Este archivo representa la referencia funcional y numérica del MVP.

---

## 3. Enfoque de trabajo

El proyecto seguirá un enfoque guiado por especificaciones.

El flujo será:

1. Definir el comportamiento.
2. Crear una especificación.
3. Revisarla.
4. Aprobarla.
5. Implementarla.
6. Probarla.
7. Comparar los resultados contra el Excel de referencia.
8. Realizar el cierre de la funcionalidad.

No se deberá implementar una funcionalidad importante sin una especificación aprobada previamente.

---

## 4. Usuario del MVP

La primera versión tendrá:

- un único usuario,
- un único portafolio,
- una única moneda principal de análisis: USD.

No existirá autenticación en la primera versión local.

La arquitectura deberá permitir incorporar autenticación posteriormente sin reconstruir completamente el sistema.

---

# 5. Funcionalidades incluidas

## 5.1 Activos

El sistema permitirá registrar los activos que pueden formar parte del portafolio.

Ejemplos:

- IVV
- VXUS
- BTC

Información mínima:

- símbolo,
- nombre,
- tipo de activo,
- moneda,
- porcentaje objetivo,
- estado.

Tipos iniciales:

- acción,
- ETF,
- criptomoneda.

---

## 5.2 Aportes

El sistema permitirá registrar dinero enviado al intermediario de inversión.

Información:

- fecha,
- monto aportado desde bolsillo,
- monto neto recibido,
- costo de fondeo,
- método de fondeo,
- comentario.

El sistema deberá diferenciar claramente:

- dinero aportado por el usuario,
- dinero recibido realmente en el intermediario,
- costos de fondeo o conversión.

---

## 5.3 Transacciones

El sistema permitirá registrar movimientos de compra y venta.

Tipos:

- COMPRA
- VENTA

Información:

- fecha,
- activo,
- tipo de transacción,
- cantidad,
- precio unitario,
- comisión,
- comentario.

El sistema deberá calcular automáticamente el monto bruto de la operación.

Las posiciones no se registrarán manualmente.

---

## 5.4 Referidos

El sistema permitirá registrar activos recibidos mediante programas de referidos.

Información:

- fecha,
- origen,
- activo,
- cantidad recibida,
- precio del activo al momento de recibirlo,
- valor recibido,
- comentario.

Ejemplo de referencia actual:

- activo: BTC
- cantidad: 0.00001196
- precio al recibir: 83580.27 USD
- valor recibido: 1.00 USD

La cantidad, el precio al recibir y el valor recibido representan datos históricos y no deben cambiar posteriormente.

El sistema podrá calcular el valor actual del referido utilizando el precio más reciente del activo.

Un referido:

- aumenta la cantidad del activo,
- no aumenta los aportes desde bolsillo,
- no se registra como compra,
- no se registra como dividendo.

---

## 5.5 Dividendos

El sistema permitirá registrar dividendos recibidos.

Información:

- fecha,
- activo,
- monto bruto,
- retención,
- otros cargos,
- monto neto,
- indicador de reinversión,
- comentario.

El monto neto podrá calcularse como:

monto bruto - retención - otros cargos

Si un dividendo es reinvertido, la compra deberá registrarse como una transacción independiente.

---

## 5.6 Precios

El sistema permitirá registrar precios históricos de los activos.

Información:

- activo,
- fecha,
- precio,
- fuente.

Los precios históricos no deberán sobrescribirse.

El precio actual de un activo será el precio más reciente disponible.

---

## 5.7 Asignación objetivo

El usuario podrá definir el porcentaje objetivo de cada activo.

Ejemplo:

- IVV: 85%
- VXUS: 10%
- BTC: 5%

El sistema deberá mostrar:

- porcentaje actual,
- porcentaje objetivo,
- diferencia.

El sistema no realizará compras ni ventas automáticamente para rebalancear el portafolio.

---

## 5.8 Posiciones

Las posiciones serán calculadas automáticamente.

No existirán como datos editables.

Para cada activo se deberá mostrar como mínimo:

- cantidad comprada,
- cantidad vendida,
- cantidad recibida por referidos,
- cantidad actual,
- costo de compras,
- costo promedio,
- precio actual,
- valor actual,
- ganancia o pérdida no realizada,
- rentabilidad,
- porcentaje actual del portafolio,
- porcentaje objetivo,
- diferencia contra objetivo.

La cantidad actual se calculará conceptualmente como:

compras - ventas + referidos

---

## 5.9 Efectivo disponible

El sistema calculará automáticamente el efectivo disponible.

El cálculo deberá considerar como mínimo:

- dinero neto recibido,
- compras,
- ventas,
- comisiones,
- dividendos recibidos.

El efectivo no será ingresado manualmente como saldo.

Debe poder rastrearse hasta las operaciones que lo originaron.

---

## 5.10 Resumen del portafolio

El sistema deberá mostrar un resumen general.

Como mínimo:

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

El sistema deberá diferenciar correctamente:

- rendimiento de los activos,
- aportes nuevos,
- costos,
- patrimonio recibido mediante referidos.

---

## 5.11 Rendimiento histórico

El sistema permitirá conservar fotografías históricas del portafolio.

Cada fotografía deberá contener al menos:

- fecha,
- total aportado,
- valor de inversiones,
- valor de referidos,
- efectivo,
- valor total,
- costos acumulados,
- resultado frente al dinero aportado,
- rentabilidad.

Estas fotografías permitirán observar la evolución del portafolio sin confundir nuevos aportes con rendimiento de mercado.

---

## 5.12 Tutorial y ayuda

La aplicación deberá explicar cómo utilizar las principales funcionalidades.

Debe existir ayuda para:

- registrar un activo,
- registrar un aporte,
- registrar una compra,
- registrar una venta,
- registrar un referido,
- registrar un dividendo,
- actualizar precios,
- interpretar posiciones,
- interpretar el resumen,
- interpretar rendimiento,
- entender asignación objetivo.

La ayuda deberá estar disponible desde la interfaz sin obligar al usuario a abandonar el proceso actual.

---

# 6. Datos calculados

Los siguientes valores serán calculados por el sistema y no deberán editarse directamente:

- posiciones,
- cantidad actual,
- costo promedio,
- valor actual,
- ganancia o pérdida,
- rentabilidad,
- efectivo disponible,
- valor total del portafolio,
- resultado frente al dinero aportado,
- porcentaje actual,
- diferencia contra objetivo.

El backend será la fuente principal de verdad para estos cálculos.

---

# 7. Datos históricos

El sistema deberá conservar información histórica.

Ejemplos:

- una nueva cotización genera un nuevo registro de precio,
- una nueva operación genera una nueva transacción,
- una nueva fotografía genera un nuevo registro de rendimiento.

Los datos históricos no deberán sobrescribirse cuando representan momentos diferentes.

---

# 8. Referencia funcional

El archivo:

`referencia/registro_inversiones_v4_1.xlsx`

será utilizado para:

- entender el comportamiento esperado,
- validar fórmulas,
- comprobar resultados,
- construir casos de prueba,
- detectar diferencias entre Excel y software.

Cuando exista una diferencia entre el software y el Excel, deberá analizarse la regla antes de asumir cuál de los dos resultados es correcto.

El objetivo final es que el software represente correctamente las reglas financieras definidas y validadas durante el proyecto.

---

# 9. Fuera del alcance del MVP

No se implementará inicialmente:

- análisis fundamental de empresas,
- checklist financiero,
- recomendaciones automáticas,
- compra o venta real de activos,
- integración con intermediarios,
- integración con Hapi,
- actualización automática de cotizaciones,
- múltiples usuarios,
- múltiples portafolios,
- aplicación móvil,
- microservicios,
- inteligencia artificial para tomar decisiones,
- importación automática de estados de cuenta,
- notificaciones automáticas,
- backtesting.

Estas funcionalidades podrán evaluarse después de completar el MVP del portafolio.

---

# 10. Criterios de éxito

El MVP se considerará funcional cuando permita:

1. Registrar los activos actuales.
2. Registrar los aportes actuales.
3. Registrar las compras actuales.
4. Registrar el referido de BTC.
5. Registrar dividendos cuando existan.
6. Registrar precios históricos.
7. Calcular correctamente las cantidades actuales.
8. Calcular posiciones.
9. Calcular efectivo disponible.
10. Calcular el resumen del portafolio.
11. Registrar fotografías de rendimiento.
12. Consultar tutoriales de uso.
13. Cargar los mismos datos del Excel de referencia.
14. Obtener resultados coherentes con el Excel.
15. Tener pruebas automatizadas para los cálculos principales.

---

# 11. Principios del MVP

- La simplicidad tiene prioridad sobre la complejidad.
- Primero se especifica y luego se implementa.
- Los datos originales deben conservarse.
- Los valores derivados deben calcularse.
- Los cálculos deben poder explicarse.
- Los cálculos deben poder reproducirse.
- Las decisiones técnicas deben estar documentadas.
- El Excel de referencia debe utilizarse como apoyo de validación.
- No se implementarán funcionalidades futuras antes de necesitarlas.
