import { DOCUMENT } from '@angular/common';
import {
  afterNextRender,
  AfterViewInit,
  Component,
  ElementRef,
  inject,
  Injector,
  input,
  OnDestroy,
  OnInit,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { leerErrorApi } from '../nucleo/api/error-api';
import { Activo, GuardarActivo, Moneda, TipoActivo } from './activo.model';
import { ActivoService } from './activo.service';
import { AsignacionService } from './asignacion.service';

/** Panel que abre la pantalla Activos. */
export type PanelActivo = { modo: 'crear' } | { modo: 'editar'; activo: Activo };

export interface ResultadoGuardado {
  tipo: 'exito' | 'advertencia';
  mensaje: string;
}

const TIPOS: { valor: TipoActivo; etiqueta: string }[] = [
  { valor: 'ACCION', etiqueta: 'Acción' },
  { valor: 'ETF', etiqueta: 'ETF' },
  { valor: 'CRIPTOMONEDA', etiqueta: 'Criptomoneda' },
];

const MONEDAS: Moneda[] = ['USD'];

const MENSAJES: Record<string, string> = {
  requerido: 'Es obligatorio.',
  formatoSimbolo: 'Solo se permiten letras, números, punto y guion (máximo 20).',
  maxlength: 'Admite como máximo 150 caracteres.',
  numero: 'Debe ser un número.',
  rango: 'Debe estar entre 0 y 100.',
  decimales: 'Admite como máximo 4 decimales.',
  quitarObjetivo: 'No se puede dejar vacío. Para excluir el activo del objetivo, indica 0.',
};

/** Igual que el backend: sin espacios en los extremos y en mayúsculas. */
function normalizarSimbolo(valor: string): string {
  return valor.trim().toUpperCase();
}

function validarSimbolo(control: AbstractControl<string>): ValidationErrors | null {
  const simbolo = normalizarSimbolo(control.value);
  if (!simbolo) {
    return { requerido: true };
  }
  return /^[A-Z0-9.-]{1,20}$/.test(simbolo) ? null : { formatoSimbolo: true };
}

function validarRequerido(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim() ? null : { requerido: true };
}

function validarPorcentaje(control: AbstractControl<string>): ValidationErrors | null {
  const valor = control.value.trim();
  if (!valor) {
    return null;
  }
  if (!/^-?\d+(\.\d+)?$/.test(valor)) {
    return { numero: true };
  }
  const numero = Number(valor);
  if (numero < 0 || numero > 100) {
    return { rango: true };
  }
  const decimales = valor.split('.')[1] ?? '';
  return decimales.length > 4 ? { decimales: true } : null;
}

/**
 * Panel lateral para registrar o editar un activo y su porcentaje objetivo.
 * Guarda con dos llamadas: primero el activo y, si hay porcentaje, después la asignación (spec 001 §6).
 */
@Component({
  selector: 'app-activo-form',
  imports: [ReactiveFormsModule],
  templateUrl: './activo-form.component.html',
  styleUrl: './activo-form.component.css',
})
export class ActivoFormComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly activoService = inject(ActivoService);
  private readonly asignacionService = inject(AsignacionService);
  private readonly documento = inject(DOCUMENT);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly injector = inject(Injector);

  readonly panel = input.required<PanelActivo>();
  /** Porcentaje objetivo registrado del activo en edición; null si no tiene. */
  readonly porcentajeActual = input<number | null>(null);

  readonly guardado = output<ResultadoGuardado>();
  readonly cerrado = output<void>();

  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  /** Elemento que abrió el panel; recupera el foco al cerrarlo. */
  private readonly focoPrevio = this.documento.activeElement as HTMLElement | null;

  protected readonly tipos = TIPOS;
  protected readonly monedas = MONEDAS;
  protected readonly guardando = signal(false);
  protected readonly enviado = signal(false);
  protected readonly errorGeneral = signal<string | null>(null);

  protected readonly formulario = this.fb.group({
    simbolo: ['', validarSimbolo],
    nombre: ['', [validarRequerido, Validators.maxLength(150)]],
    tipoActivo: this.fb.control<TipoActivo | ''>('', (c) => (c.value ? null : { requerido: true })),
    moneda: this.fb.control<Moneda>('USD', Validators.required),
    porcentajeObjetivo: ['', validarPorcentaje],
  });

  protected get editando(): Activo | null {
    const panel = this.panel();
    return panel.modo === 'editar' ? panel.activo : null;
  }

  ngOnInit(): void {
    const activo = this.editando;
    if (!activo) {
      return;
    }
    const porcentaje = this.porcentajeActual();
    this.formulario.setValue({
      simbolo: activo.simbolo,
      nombre: activo.nombre,
      tipoActivo: activo.tipoActivo,
      moneda: activo.moneda,
      porcentajeObjetivo: porcentaje === null ? '' : String(porcentaje),
    });
    if (!activo.simboloEditable) {
      this.formulario.controls.simbolo.disable();
    }
    // No existe eliminación de asignaciones: si ya tiene objetivo, vaciar el campo no lo quitaría.
    if (porcentaje !== null) {
      this.formulario.controls.porcentajeObjetivo.addValidators((c) =>
        c.value.trim() ? null : { quitarObjetivo: true },
      );
    }
  }

  ngAfterViewInit(): void {
    const dialogo = this.dialogo().nativeElement;
    if (typeof dialogo.showModal === 'function') {
      dialogo.showModal();
    } else {
      dialogo.setAttribute('open', '');
    }
    dialogo.querySelector<HTMLInputElement>('input:not([disabled])')?.focus();
  }

  ngOnDestroy(): void {
    this.dialogo().nativeElement.close?.();
    this.focoPrevio?.focus();
  }

  protected error(campo: keyof typeof this.formulario.controls): string | null {
    const control = this.formulario.controls[campo];
    if (control.disabled || control.valid || !(control.touched || this.enviado())) {
      return null;
    }
    const errores = control.errors ?? {};
    if (typeof errores['api'] === 'string') {
      return errores['api'];
    }
    const clave = Object.keys(errores).find((k) => MENSAJES[k]);
    return clave ? MENSAJES[clave] : MENSAJES['requerido'];
  }

  protected cerrar(): void {
    if (!this.guardando()) {
      this.cerrado.emit();
    }
  }

  /** Escape: lo gestiona el componente para no cerrar mientras se guarda. */
  protected alCancelar(evento: Event): void {
    evento.preventDefault();
    this.cerrar();
  }

  /** Clic en el fondo oscuro: el destino es el propio <dialog>, no su contenido. */
  protected alHacerClic(evento: MouseEvent): void {
    if (evento.target === this.dialogo().nativeElement) {
      this.cerrar();
    }
  }

  protected guardar(): void {
    this.enviado.set(true);
    this.errorGeneral.set(null);
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      this.enfocarPrimerError();
      return;
    }

    const valores = this.formulario.getRawValue();
    const datos: GuardarActivo = {
      simbolo: normalizarSimbolo(valores.simbolo),
      nombre: valores.nombre.trim(),
      tipoActivo: valores.tipoActivo as TipoActivo,
      moneda: valores.moneda,
    };
    const activo = this.editando;
    const solicitud = activo
      ? this.activoService.actualizar(activo.id, datos)
      : this.activoService.crear(datos);

    this.guardando.set(true);
    solicitud.subscribe({
      next: (guardado) => this.guardarObjetivo(guardado, valores.porcentajeObjetivo.trim()),
      error: (error) => this.mostrarErrorApi(error),
    });
  }

  private guardarObjetivo(activo: Activo, porcentaje: string): void {
    const mensaje = this.editando ? 'Cambios guardados correctamente.' : 'Activo registrado correctamente.';
    if (!porcentaje) {
      this.guardado.emit({ tipo: 'exito', mensaje });
      return;
    }
    this.asignacionService.guardar(activo.id, Number(porcentaje)).subscribe({
      next: () => this.guardado.emit({ tipo: 'exito', mensaje }),
      // Sin rollback: el activo ya quedó guardado y se informa que el objetivo no (spec 001 §6).
      error: () =>
        this.guardado.emit({
          tipo: 'advertencia',
          mensaje: `El activo ${activo.simbolo} se guardó, pero no fue posible guardar su porcentaje objetivo. Edítalo para intentarlo de nuevo.`,
        }),
    });
  }

  private mostrarErrorApi(error: unknown): void {
    const errorApi = leerErrorApi(error);
    let asignados = 0;
    for (const { campo, mensaje } of errorApi?.erroresCampos ?? []) {
      const control = this.formulario.get(campo);
      if (control) {
        control.setErrors({ api: mensaje });
        control.markAsTouched();
        asignados++;
      }
    }
    if (asignados === 0) {
      this.errorGeneral.set(
        errorApi && errorApi.estadoHttp < 500
          ? errorApi.mensaje
          : 'No fue posible guardar el activo. Inténtalo de nuevo en unos momentos.',
      );
    }
    this.guardando.set(false);
    this.enfocarPrimerError();
  }

  private enfocarPrimerError(): void {
    // Después del siguiente renderizado, para que el campo ya tenga aria-invalid.
    afterNextRender(
      () =>
        this.dialogo()
          .nativeElement.querySelector<HTMLElement>(
            'input[aria-invalid="true"], select[aria-invalid="true"], fieldset[aria-invalid="true"] input, .error-general',
          )
          ?.focus(),
      { injector: this.injector },
    );
  }
}
