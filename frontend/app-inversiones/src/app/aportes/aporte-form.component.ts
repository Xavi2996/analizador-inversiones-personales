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
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { leerErrorApi } from '../nucleo/api/error-api';
import { CrearAporte } from './aporte.model';
import { AporteService } from './aporte.service';

/** Mismos textos que el backend (spec 002 §3.4). */
const MENSAJES: Record<string, string> = {
  requerido: 'Es obligatorio.',
  numero: 'Debe ser un número.',
  mayorQueCero: 'Debe ser mayor que cero.',
  negativo: 'No puede ser negativo.',
  formatoMonto: 'Admite como máximo 2 decimales y 13 dígitos enteros.',
  maxlengthMetodo: 'Admite como máximo 50 caracteres.',
  maxlengthComentario: 'Admite como máximo 500 caracteres.',
};

const FORMATO_MONTO = /^\d{1,13}(\.\d{1,2})?$/;

/** Fecha de hoy del navegador en AAAA-MM-DD. El backend valida contra la fecha de negocio (spec 002 §6). */
function hoy(): string {
  const fecha = new Date();
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

function validarRequerido(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim() ? null : { requerido: true };
}

/** Valida formato y signo. "Neto ≤ aportado" lo valida el backend y su mensaje se muestra bajo el campo. */
function validarMonto(permiteCero: boolean) {
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
    return FORMATO_MONTO.test(valor) ? null : { formatoMonto: true };
  };
}

function longitudMaxima(maximo: number, clave: string) {
  return (control: AbstractControl<string>): ValidationErrors | null =>
    control.value.trim().length > maximo ? { [clave]: true } : null;
}

/** Texto opcional: sin espacios en los extremos; vacío se envía como null (igual que el backend). */
function textoOpcional(valor: string): string | null {
  const limpio = valor.trim();
  return limpio ? limpio : null;
}

/** Monto válido en centavos enteros, o null. Evita errores de coma flotante en la vista previa. */
function centavos(valor: string): number | null {
  const limpio = valor.trim();
  return FORMATO_MONTO.test(limpio) ? Math.round(Number(limpio) * 100) : null;
}

/**
 * Panel lateral para registrar un aporte. El costo de fondeo no se ingresa: la vista previa solo informa
 * y el valor oficial es el que calcula el backend.
 */
@Component({
  selector: 'app-aporte-form',
  imports: [ReactiveFormsModule, CurrencyPipe],
  templateUrl: './aporte-form.component.html',
  styleUrl: './aporte-form.component.css',
})
export class AporteFormComponent implements AfterViewInit, OnDestroy {
  private readonly aporteService = inject(AporteService);
  private readonly documento = inject(DOCUMENT);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly injector = inject(Injector);

  /** Se emite después de registrar el aporte; la pantalla muestra el aviso y recarga historial y totales. */
  readonly guardado = output<void>();
  readonly cerrado = output<void>();

  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  /** Elemento que abrió el panel; recupera el foco al cerrarlo. */
  private readonly focoPrevio = this.documento.activeElement as HTMLElement | null;

  protected readonly guardando = signal(false);
  protected readonly enviado = signal(false);
  protected readonly errorGeneral = signal<string | null>(null);

  protected readonly formulario = this.fb.group({
    fechaAporte: [hoy(), validarRequerido],
    montoAportado: ['', validarMonto(false)],
    montoNetoBroker: ['', validarMonto(true)],
    metodoFondeo: ['', longitudMaxima(50, 'maxlengthMetodo')],
    comentario: ['', longitudMaxima(500, 'maxlengthComentario')],
  });

  private readonly valores = toSignal(this.formulario.valueChanges, {
    initialValue: this.formulario.getRawValue(),
  });

  /** Vista previa de aportado − neto; null mientras algún monto no sea válido o el neto supere el aportado. */
  protected readonly costoPrevio = computed(() => {
    const aportado = centavos(this.valores().montoAportado ?? '');
    const neto = centavos(this.valores().montoNetoBroker ?? '');
    if (aportado === null || neto === null || neto > aportado) {
      return null;
    }
    return { aportado: aportado / 100, neto: neto / 100, costo: (aportado - neto) / 100 };
  });

  ngAfterViewInit(): void {
    const dialogo = this.dialogo().nativeElement;
    if (typeof dialogo.showModal === 'function') {
      dialogo.showModal();
    } else {
      dialogo.setAttribute('open', '');
    }
    dialogo.querySelector<HTMLInputElement>('input')?.focus();
  }

  ngOnDestroy(): void {
    this.dialogo().nativeElement.close?.();
    this.focoPrevio?.focus();
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
    const aporte: CrearAporte = {
      fechaAporte: valores.fechaAporte,
      montoAportado: Number(valores.montoAportado.trim()),
      montoNetoBroker: Number(valores.montoNetoBroker.trim()),
      metodoFondeo: textoOpcional(valores.metodoFondeo),
      comentario: textoOpcional(valores.comentario),
    };

    this.guardando.set(true);
    this.aporteService.registrar(aporte).subscribe({
      next: () => {
        this.guardando.set(false);
        this.limpiar();
        this.guardado.emit();
      },
      error: (error) => this.mostrarErrorApi(error),
    });
  }

  private limpiar(): void {
    this.formulario.reset({ fechaAporte: hoy() });
    this.enviado.set(false);
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
          : 'No fue posible registrar el aporte. Inténtalo de nuevo en unos momentos.',
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
          .nativeElement.querySelector<HTMLElement>('[aria-invalid="true"], .error-general')
          ?.focus(),
      { injector: this.injector },
    );
  }
}
