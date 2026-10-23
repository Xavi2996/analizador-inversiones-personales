---
target: Prototipo Interfaz v2.dc.html (evaluación final 10 pantallas)
total_score: 27
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 5
target_identity: "file:C:\\Users\\xavie\\OneDrive\\Documents\\proyectos personales\\analizador-inversiones-personales\\referencia\\prototipo-interfaz\\Prototipo Interfaz v2.dc.html"
target_fingerprint: "sha256:fbadffcf62127fd7e89186d508d93fc7a606856a10507e2af238141efae5e7e1"
target_path: "C:\\Users\\xavie\\OneDrive\\Documents\\proyectos personales\\analizador-inversiones-personales\\referencia\\prototipo-interfaz\\Prototipo Interfaz v2.dc.html"
timestamp: 2026-10-23T18-05-16Z
slug: po-interfaz-prototipo-interfaz-v2-dc-html-af684e91
---
Method: dual-agent (A: revisión de diseño · B: detector + navegador)

## Design Health Score: 27/40 (Aceptable, límite con Bueno). Anterior: 25/40
| # | Heurística | Nota | Problema |
|---|---|---|---|
| 1 | Estado | 3 | Sin aviso de precio desactualizado ni estados carga/vacío |
| 2 | Mundo real | 3 | pp solo explicado en Ayuda |
| 3 | Control | 3 | Escape/trap/retorno de foco OK; sin corrección de inmutables |
| 4 | Consistencia | 3 | Escala tipográfica 10 niveles; quedan Limpiar/Cancelar, signo de costos, ±2 vs 0.5 pp, eje sin $ |
| 5 | Prevención | 2 | Venta 999 vs 0.56 disponible registra con ✓; Σ estático |
| 6 | Reconocimiento | 3 | Σ visibles, Disponible para vender |
| 7 | Flexibilidad | 2 | Pestañas/select de filtro no filtran; precios uno a uno |
| 8 | Minimalismo | 3 | Superficies 1.03–1.09:1 |
| 9 | Errores | 2 | Un solo error diseñado |
| 10 | Ayuda | 3 | Popover Monto neto tapa el input |

## Especificidad
Alta: cascada aporte→valor, Al recibir vs Hoy, saltos de aporte, Σ Calculado, desglose Comprada/Vendida/Referidos. Genérica solo la cáscara.
Detector CLI: 6 (overused-font Geist Mono FP; 4 cramped-padding probable FP; 1 low-contrast hover #93c5fd/#2563eb 2.9:1 real). Overlay: 2 stragglers. Sin overflow en 40 combinaciones; contraste de texto OK en 40.

## Problemas prioritarios
- [P1] Venta sin validación ni recálculo de Σ; Disponible fijo en IVV → harden tras spec de ventas
- [P1] Crear fotografía sin confirmación ni vista previa → harden tras spec
- [P1] Posiciones a 1024 oculta Diferencia y Desvío (706/940) → adapt
- [P1] Popover de ayuda Monto neto tapa el input → clarify/layout
- [P1] Pestañas role=tab de Transacciones/Activos no funcionan → implementación
- [P2] Signo/color de costos y retenciones incoherente; rojo polisémico
- [P2] Umbral desvío ±2 pp vs 0.5 pp
- [P2] Bordes de control <3:1 (Crear fotografía, select, Menú móvil 1.38–1.71)
- [P2] Rendimiento SVG 9.5px a 1024; tabla fotografías móvil oculta Resultado
- [P2] Sin estados carga/vacío/red/precio desactualizado
- [P2] Toast tapa CTA del drawer y persiste entre pantallas
- [P2] Compra pendiente no está en el modelo; fechas fijas 22 vs 23/10; ejes sin unidades

## Conservado a propósito
Formulario lateral fijo; franjas por activo; Hanken + Geist Mono; héroe 84px solo en Resumen; cabeceras Posiciones en 2 líneas; eyebrows y glifos; solo tema oscuro; venta/fotografía/historial pendientes de spec; filtros no funcionales; restos del audit (tab order Compra/Venta, ámbar visual Resumen, buscador sin borde, título de pestaña, 1M/3M en dl, errores de consola del runtime).

## Madurez
Lista para cimientos Angular (tokens, shell, navegación, responsive, a11y de capas, pantallas de consulta). Antes de flujos de escritura: specs de ventas (costo promedio/FIFO), fotografía, umbrales §31, convención de signo de costos, Compra pendiente, catálogo de estados, librería de componentes.
Decisión del usuario: specs primero para venta/fotografía; congelar el prototipo como referencia visual.
