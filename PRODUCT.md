# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

Decidido en `documentacion/03-arquitectura.md`: frontend Angular, backend Java 21 + Spring Boot (Spring Web, Spring Data JPA), PostgreSQL con Flyway, Docker Compose para la base de datos en local. Interfaz con componentes propios y CSS propio, sin librería de componentes (`05-guia-interfaz.md` §51). `backend/` y `frontend/` tienen la base técnica (FASE 1), sin funcionalidades de negocio todavía.

## Users

- **MVP:** un único usuario (el dueño del proyecto) que gestiona su propio portafolio personal de inversiones en USD. Sin autenticación en la versión local.
- **Futuro:** se agregarán más usuarios. La arquitectura debe permitir incorporar autenticación y varios usuarios sin reconstruir el sistema. Quiénes serán esos usuarios no está definido todavía.

El trabajo del usuario: registrar lo que realmente pasó con su dinero (aportes, compras, ventas, referidos, dividendos, precios) y entender con claridad cómo va el portafolio, sin confundir el dinero nuevo que aportó con el rendimiento del mercado.

## Product Purpose

Aplicación web personal para registrar, consultar y analizar un portafolio de inversiones. Reemplaza y formaliza el Excel `referencia/registro_inversiones_v4_1.xlsx`, que es la referencia funcional y numérica del MVP.

Éxito: cargar los mismos datos del Excel y obtener resultados coherentes con él, con cálculos explicables, reproducibles y cubiertos por pruebas automatizadas (criterios completos en `documentacion/00-alcance-mvp.md` §10).

## Positioning

Separa con rigor lo que una hoja de cálculo o una app de intermediario suelen mezclar: dinero aportado desde el bolsillo, dinero neto recibido en el intermediario, costos de fondeo y conversión, patrimonio recibido por referidos (que no es aporte ni compra) y rendimiento real de los activos. Todo valor derivado se calcula a partir de las operaciones históricas y se puede rastrear hasta ellas.

## Operating Context

- Uso principal en computadora de escritorio: registrar operaciones y revisarlas con calma. Uso secundario y menos frecuente en el celular. La interfaz debe funcionar bien en ambos.
- El dinero se envía a un intermediario de inversión; la documentación menciona Hapi, aunque no habrá integración en el MVP. Los aportes tienen costos de fondeo que deben registrarse.
- Activos actuales: IVV, VXUS (ETF) y BTC (criptomoneda), con asignación objetivo 85% / 10% / 5%.
- Desarrollo guiado por especificaciones: ninguna funcionalidad importante se implementa sin una especificación aprobada en `specs/`.
- Idioma de la interfaz y la documentación: español. Fechas mostradas como DD/MM/AAAA (la API usa AAAA-MM-DD). Moneda principal de análisis: USD.

## Capabilities and Constraints

- **Incluido en el MVP:** activos, aportes, transacciones (COMPRA/VENTA), referidos, dividendos, precios históricos, asignación objetivo, posiciones calculadas, efectivo disponible calculado, resumen del portafolio, fotografías históricas de rendimiento, y tutoriales y ayuda contextual.
- **Valores calculados, nunca editables:** posiciones, cantidad actual, costo promedio, valor actual, ganancia o pérdida, rentabilidad, efectivo, valor total, resultado y rentabilidad frente al aporte, porcentaje actual y diferencia contra el objetivo. El backend es la fuente oficial de estos cálculos.
- **Historial inmutable:** un precio, una operación o una fotografía nuevos crean un registro nuevo; no se sobrescribe historia.
- **Ayuda:** disponible desde la interfaz sin abandonar el proceso actual y sin perder lo que se haya escrito en un formulario.
- **Fuera del alcance:** análisis fundamental, recomendaciones automáticas, operaciones reales, integración con intermediarios, cotizaciones automáticas, múltiples usuarios o portafolios en el MVP, app móvil nativa, IA para decisiones, importación de estados de cuenta, notificaciones y backtesting.
- **Por decidir:** método de costo para ventas con ganancia realizada (costo promedio o FIFO), que debe especificarse antes de implementar ventas.
- Terminología canónica: activo, aporte, transacción, referido, dividendo, precio, posición, efectivo disponible, resumen, fotografía, asignación objetivo.

## Brand Commitments

- Nombre: **Analizador de Inversiones Personales**.
- `documentacion/05-guia-interfaz.md` es una referencia visual importante y vinculante: con ella se creó el prototipo en Claude Design. Fija tema oscuro como tema oficial del MVP (sin tema claro), paleta y tipografía iniciales, principios visuales, patrones a evitar y el orden de prioridad de referencias: requisitos funcionales → especificaciones → guía de interfaz → prototipo aprobado → criterios de Impeccable.
- El prototipo aprobado será la referencia visual concreta para la implementación Angular. La versión v2 importada de Claude Design está en `referencia/prototipo-interfaz/Prototipo Interfaz v2.dc.html` (con su runtime `support.js`) y está aprobada (ver `05-guia-interfaz.md`, "Estado del prototipo visual").
- Impeccable complementa el diseño; nunca modifica reglas financieras, cálculos, datos, contratos de API ni el alcance.

## Evidence on Hand

- `referencia/registro_inversiones_v4_1.xlsx`: datos reales y fórmulas de referencia.
- `documentacion/00` a `07`: alcance, modelo funcional, base de datos, arquitectura, contratos de API, guía de interfaz, convenciones y hoja de ruta. `specs/` todavía no tiene especificaciones; la primera será `specs/001-activos.md`. `specs/README.md` está vacío.
- Caso de referido real: BTC 0.00001196 recibido a 83580.27 USD (valor recibido 1.00 USD).
- Ejemplo de aporte: 250.00 USD enviados, 243.33 USD recibidos, 6.67 USD de costo de fondeo.
- No hay testimonios, usuarios externos ni métricas de uso. No deben inventarse.

## Product Principles

1. **Distinguir aporte de rendimiento.** Toda vista de resultado separa dinero nuevo, costos, referidos y rendimiento del mercado.
2. **Calcular, no teclear.** Los valores derivados se obtienen de las operaciones históricas y se ven distintos de los datos editables.
3. **Cada número se puede explicar.** Un cálculo debe poder rastrearse hasta las operaciones que lo originan y coincidir con el Excel de referencia.
4. **La historia no se reescribe.** Los eventos nuevos se agregan; los pasados se conservan.
5. **Simplicidad antes que alcance.** No se construye nada antes de necesitarlo ni sin una especificación aprobada.

## Accessibility & Inclusion

Del §48 de la guía: contraste suficiente en tema oscuro, etiquetas visibles, navegación por teclado cuando sea posible, nunca comunicar solo con color (positivo y negativo llevan símbolo y signo), texto legible y áreas táctiles suficientes, sobre todo en el celular.
