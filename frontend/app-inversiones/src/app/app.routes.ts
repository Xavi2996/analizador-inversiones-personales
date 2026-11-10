import { Routes } from '@angular/router';
import { ActivosPaginaComponent } from './activos/activos-pagina.component';
import { AportesPaginaComponent } from './aportes/aportes-pagina.component';
import { LayoutComponent } from './nucleo/layout/layout.component';

export const routes: Routes = [
  {
    path: '',
    component: LayoutComponent,
    children: [
      // Hasta que exista Resumen, la pantalla inicial es Activos.
      { path: '', pathMatch: 'full', redirectTo: 'activos' },
      { path: 'activos', component: ActivosPaginaComponent, title: 'Activos · Analizador de Inversiones' },
      { path: 'aportes', component: AportesPaginaComponent, title: 'Aportes · Analizador de Inversiones' },
    ],
  },
  { path: '**', redirectTo: '' },
];
