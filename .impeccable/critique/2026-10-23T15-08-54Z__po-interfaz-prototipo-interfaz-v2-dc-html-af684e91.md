---
target: Prototipo Interfaz v2.dc.html (10 pantallas)
total_score: 25
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 3
target_identity: "file:C:\\Users\\xavie\\OneDrive\\Documents\\proyectos personales\\analizador-inversiones-personales\\referencia\\prototipo-interfaz\\Prototipo Interfaz v2.dc.html"
target_fingerprint: "sha256:96e3345fa2931bc2b78a95c94bbaa0387c8fa975b892f726d596126a208406a2"
target_path: "C:\\Users\\xavie\\OneDrive\\Documents\\proyectos personales\\analizador-inversiones-personales\\referencia\\prototipo-interfaz\\Prototipo Interfaz v2.dc.html"
timestamp: 2026-10-23T15-08-54Z
slug: po-interfaz-prototipo-interfaz-v2-dc-html-af684e91
closed: true
---
Method: dual-agent (A: design review · B: detector + browser)

## Design Health Score: 25/40 (Aceptable)
| # | Heurística | Nota | Problema |
|---|---|---|---|
| 1 | Estado | 3 | Sin aviso de precio desactualizado |
| 2 | Mundo real | 3 | pp / Desvío / fotografía sin explicar |
| 3 | Control | 2 | Escape no cierra panel; no hay ruta de corrección |
| 4 | Consistencia | 2 | ±2 pp vs 0.5 pp; Limpiar/Cancelar; ~22 tamaños de fuente; cifra héroe 84/44/40/34 |
| 5 | Prevención | 2 | Fecha texto libre; venta sin validar contra disponible |
| 6 | Reconocimiento | 3 | Fórmulas visibles |
| 7 | Flexibilidad | 2 | Precios uno a uno; sin orden/filtros |
| 8 | Minimalismo | 3 | Cifras repetidas 3-4 veces |
| 9 | Errores | 2 | Un solo error diseñado |
| 10 | Ayuda | 3 | Un solo ? contextual |

## Especificidad
Contenido muy específico (cascada aporte→valor, gráfico escalonado, Al recibir vs Hoy, bloque Σ). Esqueleto repetido tabla+formulario 440px en 5 pantallas; franja lateral 3px por ticker en 6 tablas; rellenos con degradado.
Detector: 3 avisos (overused-font Geist Mono ~falso positivo; 2 cramped-padding sin selector). Overlay: limpio en 5 pantallas. Contraste ≥4.5 en todo.

## Problemas prioritarios
- [P1] Tablas recortadas a 1440 por formulario fijo de 440px (Transacciones oculta Monto; Referidos oculta grupo Hoy·calculado; Dividendos; Posiciones trunca desvío). → layout
- [P1] Overflow horizontal móvil en Activos (664px) y Precios (719px) por span sr-only absoluto sin ancestro posicionado. → adapt
- [P1] Venta y fotografía crean registros inmutables sin revisión ni validación. → harden
- [P2] Incoherencias de sistema: umbral, nombres de botón, signo retenciones, eje $, escala tipográfica. → typeset/polish
- [P2] Móvil y accesibilidad: etiquetas SVG ~6-8px, Posiciones solo 2 columnas, táctiles 20-40px, sin Escape/focus trap, ayuda tapa input. → adapt/audit

## Personas
Alex: precios en 3 envíos, scroll horizontal en Posiciones, toast sin enlace. Sam: sin Escape/foco atrapado, ? 20px, tabs ARIA incompletas, asignación solo por color, sin skip link. Celular: overflow, gráficos ilegibles, sin valor en cabecera.

## Menores
Superficies 1.03-1.08:1; hover de input invertido; dato de referido distinto al real; «Compra pendiente» no está en el modelo; rojo polisémico; errores SVG de runtime; fechas fijas.

## Preguntas
Corrección de registros inmutables; qué cifra es la protagonista; formulario siempre abierto; dónde poner ayuda contextual.
