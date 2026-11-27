import { CurrencyPipe, DOCUMENT } from '@angular/common';
import {
  afterNextRender,
  AfterViewInit,
  Component,
  computed,
  ElementRef,
  inject,
  Injector,
  OnDestroy,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { map } from 'rxjs';
import { Activo } from '../activos/activo.model';
import { ActivoService } from '../activos/activo.service';
import { leerErrorApi } from '../nucleo/api/error-api';
import { CrearReferido } from './referido.model';
import { ReferidoService } from './referido.service';

/** Mismos textos que el backend (spec 004 §3.2). */
const MENSAJES: Record<string, string> = {
  requerido: 'Es obligatorio.',
  numero: 'Debe ser un número.',
  mayorQueCero: 'Debe ser mayor que cero.',
  formatoCantidad: 'Admite como máximo 8 decimales y 16 dígitos enteros.',
  formatoPrecio: 'Admite como máximo 6 decimales y 13 dígitos enteros.',
  maxlengthOrigen: 'Admite como máximo 100 caracteres.',
  maxlengthComentario: 'Admite como máximo 500 caracteres.',
};

const FORMATO_CANTIDAD = /^\d{1,16}(\.\d{1,8})?$/;
const FORMATO_PRECIO = /^\d{1,13}(\.\d{1,6})?$/;

type EstadoActivos = 'cargando' | 'listo' | 'error';

/** Fecha de hoy del navegador en AAAA-MM-DD. El backend valida contra la fecha de negocio. */
function hoy(): string {
  const fecha = new Date();
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

function validarRequerido(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim() ? null : { requerido: true };
}

/** Valida formato y que el número sea mayor que cero. */
function validarPositivo(formato: RegExp, claveFormato: string) {
  return (control: AbstractControl<string>): ValidationErrors | null => {
    const valor = control.value.trim();
    if (!valor) {
      return { requerido: true };
    }
    if (!/^-?\d+(\.\d+)?$/.test(valor)) {
      return { numero: true };
    }
    if (Number(valor) <= 0) {
      return { mayorQueCero: true };
    }
    return formato.test(valor) ? null : { [claveFormato]: true };
  };
}

function longitudMaxima(maximo: number, clave: string) {
  return (control: AbstractControl<string>): ValidationErrors | null =>
    control.value.trim().length > maximo ? { [clave]: true } : null;
}

function textoOpcional(valor: string): string | null {
  const limpio = valor.trim();
  return limpio ? limpio : null;
}

/** Decimal válido como entero escalado (BigInt), o null. Evita errores de coma flotante en la vista previa. */
function escalado(valor: string, formato: RegExp, escala: number): bigint | null {
  const limpio = valor.trim();
  if (!formato.test(limpio)) {
    return null;
  }
  const [entero, decimales = ''] = limpio.split('.');
  return BigInt(entero + decimales.padEnd(escala, '0'));
}

/**
 * Vista previa del valor recibido: cantidad × precio al recibir redondeado a 2 decimales, mitad hacia arriba,
 * igual que el backend (01-modelo-funcional §5.3). Devuelve centavos. El valor oficial lo calcula el backend.
 */
function valorEnCentavos(cantidad: bigint, precio: bigint): bigint {
  // cantidad (escala 8) × precio (escala 6) = escala 14; a centavos (escala 2) se divide entre 10^12.
  const divisor = 10n ** 12n;
  return (cantidad * precio + divisor / 2n) / divisor;
}

/**
 * Panel lateral para registrar un referido. Solo datos históricos: el valor recibido es una vista previa y
 * el valor actual llega con Precios (guía §26).
 */
@Component({
  selector: 'app-referido-form',
  imports: [ReactiveFormsModule, CurrencyPipe],
  templateUrl: './referido-form.component.html',
  styleUrl: './referido-form.component.css',
})
export class ReferidoFormComponent implements AfterViewInit, OnDestroy {
  private readonly referidoService = inject(ReferidoService);
  private readonly activoService = inject(ActivoService);
  private readonly documento = inject(DOCUMENT);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly injector = inject(Injector);

  /** Se emite al registrar; la pantalla muestra el aviso y recarga el historial y el total. */
  readonly guardado = output<void>();
  readonly cerrado = output<void>();

  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  private readonly focoPrevio = this.documento.activeElement as HTMLElement | null;

  protected readonly activos = signal<Activo[]>([]);
  protected readonly estadoActivos = signal<EstadoActivos>('cargando');
  protected readonly guardando = signal(false);
  protected readonly enviado = signal(false);
  protected readonly errorGeneral = signal<string | null>(null);

  protected readonly formulario = this.fb.group({
    fechaReferido: [hoy(), validarRequerido],
    activoId: ['', validarRequerido],
    origen: ['', [validarRequerido, longitudMaxima(100, 'maxlengthOrigen')]],
    cantidad: ['', validarPositivo(FORMATO_CANTIDAD, 'formatoCantidad')],
    precioAlRecibir: ['', validarPositivo(FORMATO_PRECIO, 'formatoPrecio')],
    comentario: ['', longitudMaxima(500, 'maxlengthComentario')],
  });

  private readonly valores = toSignal(this.formulario.valueChanges.pipe(map(() => this.formulario.getRawValue())), {
    initialValue: this.formulario.getRawValue(),
  });

  /** Vista previa del valor recibido; null mientras cantidad o precio no sean válidos. */
  protected readonly previa = computed(() => {
    const valores = this.valores();
    const cantidad = escalado(valores.cantidad, FORMATO_CANTIDAD, 8);
    const precio = escalado(valores.precioAlRecibir, FORMATO_PRECIO, 6);
    if (cantidad === null || precio === null || cantidad === 0n || precio === 0n) {
      return null;
    }
    return {
      cantidad: valores.cantidad.trim(),
      precio: Number(valores.precioAlRecibir.trim()),
      valor: Number(valorEnCentavos(cantidad, precio)) / 100,
    };
  });

  constructor() {
    this.cargarActivos();
  }

  ngAfterViewInit(): void {
    const dialogo = this.dialogo().nativeElement;
    if (typeof dialogo.showModal === 'function') {
      dialogo.showModal();
    } else {
      dialogo.setAttribute('open', '');
    }
    dialogo.querySelector<HTMLElement>('#referido-fecha')?.focus();
  }

  ngOnDestroy(): void {
    this.dialogo().nativeElement.close?.();
    this.focoPrevio?.focus();
  }

  protected cargarActivos(): void {
    this.estadoActivos.set('cargando');
    this.activoService.listar().subscribe({
      next: (activos) => {
        this.activos.set(activos);
        this.estadoActivos.set('listo');
      },
      error: () => this.estadoActivos.set('error'),
    });
  }

  protected error(campo: keyof typeof this.formulario.controls): string | null {
    const control = this.formulario.controls[campo];
    if (control.valid || !(control.touched || this.enviado())) {
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

  protected alCancelar(evento: Event): void {
    evento.preventDefault();
    this.cerrar();
  }

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
    const referido: CrearReferido = {
      activoId: Number(valores.activoId),
      fechaReferido: valores.fechaReferido,
      origen: valores.origen.trim(),
      cantidad: Number(valores.cantidad.trim()),
      precioAlRecibir: Number(valores.precioAlRecibir.trim()),
      comentario: textoOpcional(valores.comentario),
    };

    this.guardando.set(true);
    this.referidoService.registrar(referido).subscribe({
      next: () => {
        this.guardando.set(false);
        this.guardado.emit();
      },
      error: (error) => this.mostrarErrorApi(error),
    });
  }

  /** Los errores de campo se muestran bajo el campo sin borrar lo ingresado. */
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
          : 'No fue posible registrar el referido. Inténtalo de nuevo en unos momentos.',
      );
    }
    this.guardando.set(false);
    this.enfocarPrimerError();
  }

  private enfocarPrimerError(): void {
    afterNextRender(
      () =>
        this.dialogo()
          .nativeElement.querySelector<HTMLElement>('[aria-invalid="true"], .error-general')
          ?.focus(),
      { injector: this.injector },
    );
  }
}
