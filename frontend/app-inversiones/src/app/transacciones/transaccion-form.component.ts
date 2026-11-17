import { CurrencyPipe, DecimalPipe, DOCUMENT } from '@angular/common';
import {
  afterNextRender,
  AfterViewInit,
  Component,
  computed,
  ElementRef,
  inject,
  Injector,
  input,
  OnDestroy,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { catchError, distinctUntilChanged, map, merge, Observable, of, startWith, Subject, switchMap } from 'rxjs';
import { Activo } from '../activos/activo.model';
import { leerErrorApi } from '../nucleo/api/error-api';
import { CrearTransaccion, TipoTransaccion } from './transaccion.model';
import { TransaccionService } from './transaccion.service';

/** Mismos textos que el backend (spec 003 §3.2). */
const MENSAJES: Record<string, string> = {
  requerido: 'Es obligatorio.',
  numero: 'Debe ser un número.',
  mayorQueCero: 'Debe ser mayor que cero.',
  negativo: 'No puede ser negativo.',
  formatoCantidad: 'Admite como máximo 8 decimales y 16 dígitos enteros.',
  formatoPrecio: 'Admite como máximo 6 decimales y 13 dígitos enteros.',
  formatoComision: 'Admite como máximo 2 decimales y 13 dígitos enteros.',
  maxlengthComentario: 'Admite como máximo 500 caracteres.',
};

const FORMATO_CANTIDAD = /^\d{1,16}(\.\d{1,8})?$/;
const FORMATO_PRECIO = /^\d{1,13}(\.\d{1,6})?$/;
const FORMATO_COMISION = /^\d{1,13}(\.\d{1,2})?$/;

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

/** Valida formato y signo. Las reglas de venta (disponible, comisión ≤ monto) las valida el backend. */
function validarNumero(formato: RegExp, claveFormato: string, permiteCero: boolean) {
  return (control: AbstractControl<string>): ValidationErrors | null => {
    const valor = control.value.trim();
    if (!valor) {
      return { requerido: true };
    }
    if (!/^-?\d+(\.\d+)?$/.test(valor)) {
      return { numero: true };
    }
    const numero = Number(valor);
    if (numero < 0) {
      return permiteCero ? { negativo: true } : { mayorQueCero: true };
    }
    if (numero === 0 && !permiteCero) {
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
 * Vista previa del monto: cantidad × precio redondeado a 2 decimales, mitad hacia arriba, igual que el backend
 * (01-modelo-funcional §4.3). Devuelve centavos. Solo es ayuda visual: el monto oficial lo calcula el backend.
 */
function montoEnCentavos(cantidad: bigint, precio: bigint): bigint {
  // cantidad (escala 8) × precio (escala 6) = escala 14; a centavos (escala 2) se divide entre 10^12.
  const divisor = 10n ** 12n;
  return (cantidad * precio + divisor / 2n) / divisor;
}

type EstadoDisponible =
  | { estado: 'cargando' }
  | { estado: 'listo'; cantidad: number }
  | { estado: 'error' };

/**
 * Panel lateral para registrar una compra o una venta. El monto y el efectivo son una vista previa;
 * la cantidad disponible para vender la calcula el backend para la fecha elegida (spec 003 §3.3).
 */
@Component({
  selector: 'app-transaccion-form',
  imports: [ReactiveFormsModule, CurrencyPipe, DecimalPipe],
  templateUrl: './transaccion-form.component.html',
  styleUrl: './transaccion-form.component.css',
})
export class TransaccionFormComponent implements AfterViewInit, OnDestroy {
  private readonly transaccionService = inject(TransaccionService);
  private readonly documento = inject(DOCUMENT);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly injector = inject(Injector);

  /** Activos del selector; la pantalla ya los consultó para el filtro. */
  readonly activos = input.required<Activo[]>();

  /** Se emite con el tipo registrado; la pantalla muestra el aviso y recarga historial y totales. */
  readonly guardado = output<TipoTransaccion>();
  readonly cerrado = output<void>();

  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  private readonly focoPrevio = this.documento.activeElement as HTMLElement | null;

  protected readonly guardando = signal(false);
  protected readonly enviado = signal(false);
  protected readonly errorGeneral = signal<string | null>(null);

  protected readonly formulario = this.fb.group({
    tipoTransaccion: this.fb.control<TipoTransaccion>('COMPRA'),
    activoId: ['', validarRequerido],
    fechaTransaccion: [hoy(), validarRequerido],
    cantidad: ['', validarNumero(FORMATO_CANTIDAD, 'formatoCantidad', false)],
    precioUnitario: ['', validarNumero(FORMATO_PRECIO, 'formatoPrecio', false)],
    comision: ['', validarNumero(FORMATO_COMISION, 'formatoComision', true)],
    comentario: ['', longitudMaxima(500, 'maxlengthComentario')],
  });

  private readonly valores = toSignal(this.formulario.valueChanges.pipe(map(() => this.formulario.getRawValue())), {
    initialValue: this.formulario.getRawValue(),
  });

  protected readonly esVenta = computed(() => this.valores().tipoTransaccion === 'VENTA');

  protected readonly simboloSeleccionado = computed(
    () => this.activos().find((activo) => String(activo.id) === this.valores().activoId)?.simbolo ?? '',
  );

  /** Vista previa de monto y efectivo; null mientras cantidad o precio no sean válidos. */
  protected readonly previa = computed(() => {
    const valores = this.valores();
    const cantidad = escalado(valores.cantidad, FORMATO_CANTIDAD, 8);
    const precio = escalado(valores.precioUnitario, FORMATO_PRECIO, 6);
    if (cantidad === null || precio === null || cantidad === 0n || precio === 0n) {
      return null;
    }
    const monto = montoEnCentavos(cantidad, precio);
    const comision = escalado(valores.comision, FORMATO_COMISION, 2);
    const efectivo =
      comision === null ? null : valores.tipoTransaccion === 'COMPRA' ? monto + comision : monto - comision;
    return {
      cantidad: valores.cantidad.trim(),
      precio: Number(valores.precioUnitario.trim()),
      monto: Number(monto) / 100,
      efectivo: efectivo === null ? null : Number(efectivo) / 100,
    };
  });

  private readonly recargarDisponible = new Subject<void>();

  /**
   * Cantidad disponible para vender en la fecha elegida, consultada al backend al elegir Venta y cada vez que
   * cambian el activo o la fecha. null cuando no aplica (compra, o falta el activo o la fecha).
   */
  protected readonly disponible = toSignal(this.consultarDisponible(), { initialValue: null });

  protected readonly cantidadDisponible = computed(() => {
    const disponible = this.disponible();
    return disponible?.estado === 'listo' ? disponible.cantidad : null;
  });

  ngAfterViewInit(): void {
    const dialogo = this.dialogo().nativeElement;
    if (typeof dialogo.showModal === 'function') {
      dialogo.showModal();
    } else {
      dialogo.setAttribute('open', '');
    }
    dialogo.querySelector<HTMLElement>('[role="radio"][aria-checked="true"]')?.focus();
  }

  ngOnDestroy(): void {
    this.dialogo().nativeElement.close?.();
    this.focoPrevio?.focus();
  }

  protected seleccionarTipo(tipo: TipoTransaccion): void {
    this.formulario.controls.tipoTransaccion.setValue(tipo);
  }

  /** Flechas dentro del grupo Compra / Venta (patrón ARIA radiogroup). */
  protected moverTipo(evento: KeyboardEvent): void {
    if (!['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(evento.key)) {
      return;
    }
    evento.preventDefault();
    const tipo: TipoTransaccion = this.esVenta() ? 'COMPRA' : 'VENTA';
    this.seleccionarTipo(tipo);
    const grupo = (evento.currentTarget as HTMLElement).closest('[role="radiogroup"]');
    grupo?.querySelectorAll<HTMLElement>('[role="radio"]')[tipo === 'COMPRA' ? 0 : 1]?.focus();
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
    const transaccion: CrearTransaccion = {
      activoId: Number(valores.activoId),
      tipoTransaccion: valores.tipoTransaccion,
      fechaTransaccion: valores.fechaTransaccion,
      cantidad: Number(valores.cantidad.trim()),
      precioUnitario: Number(valores.precioUnitario.trim()),
      comision: Number(valores.comision.trim()),
      comentario: textoOpcional(valores.comentario),
    };

    this.guardando.set(true);
    this.transaccionService.registrar(transaccion).subscribe({
      next: () => {
        this.guardando.set(false);
        this.guardado.emit(transaccion.tipoTransaccion);
      },
      error: (error) => this.mostrarErrorApi(error),
    });
  }

  private consultarDisponible(): Observable<EstadoDisponible | null> {
    const clave = () => {
      const { tipoTransaccion, activoId, fechaTransaccion } = this.formulario.getRawValue();
      return tipoTransaccion === 'VENTA' && activoId && fechaTransaccion ? `${activoId}|${fechaTransaccion}` : '';
    };
    const cambios = this.formulario.valueChanges.pipe(startWith(null), map(clave), distinctUntilChanged());
    const recargas = this.recargarDisponible.pipe(map(clave));
    return merge(cambios, recargas).pipe(
      switchMap((valor): Observable<EstadoDisponible | null> => {
        if (!valor) {
          return of(null);
        }
        const [activoId, fecha] = valor.split('|');
        return this.transaccionService.disponible(Number(activoId), fecha).pipe(
          map((respuesta): EstadoDisponible => ({ estado: 'listo', cantidad: respuesta.cantidadDisponible })),
          startWith<EstadoDisponible>({ estado: 'cargando' }),
          catchError(() => of<EstadoDisponible>({ estado: 'error' })),
        );
      }),
      takeUntilDestroyed(),
    );
  }

  /** Los errores de campo (incluido el 409 de cantidad) se muestran bajo el campo sin borrar lo ingresado. */
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
          : 'No fue posible registrar la transacción. Inténtalo de nuevo en unos momentos.',
      );
    }
    if (errorApi?.codigo === 'CANTIDAD_INSUFICIENTE') {
      // Otra venta pudo cambiar el disponible: se vuelve a consultar.
      this.recargarDisponible.next();
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
