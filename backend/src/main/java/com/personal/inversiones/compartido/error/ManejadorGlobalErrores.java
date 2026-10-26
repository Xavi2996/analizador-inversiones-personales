package com.personal.inversiones.compartido.error;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Traduce las excepciones a la estructura común de error (04-contratos-api §7).
 * Nunca expone stack traces, SQL ni nombres de clases.
 */
@RestControllerAdvice
public class ManejadorGlobalErrores {

	public static final String DATOS_INVALIDOS = "DATOS_INVALIDOS";
	public static final String SOLICITUD_INVALIDA = "SOLICITUD_INVALIDA";
	public static final String ERROR_INTERNO = "ERROR_INTERNO";

	private static final String MENSAJE_DATOS_INVALIDOS = "La solicitud contiene datos inválidos.";
	private static final String MENSAJE_VALOR_NO_VALIDO = "Tiene un valor no válido.";

	private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorRespuesta> manejarValidacion(MethodArgumentNotValidException ex,
			HttpServletRequest solicitud) {
		List<ErrorCampo> errores = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorCampo(error.getField(), error.getDefaultMessage()))
				.toList();
		return responder(HttpStatus.BAD_REQUEST, DATOS_INVALIDOS, MENSAJE_DATOS_INVALIDOS, solicitud, errores);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorRespuesta> manejarCuerpoIlegible(HttpMessageNotReadableException ex,
			HttpServletRequest solicitud) {
		List<ErrorCampo> errores = null;
		if (ex.getCause() instanceof MismatchedInputException causa) {
			String campo = nombreCampo(causa);
			if (!campo.isEmpty()) {
				errores = List.of(new ErrorCampo(campo, mensajeValorNoValido(causa)));
			}
		}
		return responder(HttpStatus.BAD_REQUEST, DATOS_INVALIDOS, MENSAJE_DATOS_INVALIDOS, solicitud, errores);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorRespuesta> manejarParametroInvalido(MethodArgumentTypeMismatchException ex,
			HttpServletRequest solicitud) {
		List<ErrorCampo> errores = List.of(new ErrorCampo(ex.getName(), MENSAJE_VALOR_NO_VALIDO));
		return responder(HttpStatus.BAD_REQUEST, DATOS_INVALIDOS, MENSAJE_DATOS_INVALIDOS, solicitud, errores);
	}

	@ExceptionHandler(RecursoNoEncontradoExcepcion.class)
	public ResponseEntity<ErrorRespuesta> manejarNoEncontrado(RecursoNoEncontradoExcepcion ex,
			HttpServletRequest solicitud) {
		return responder(HttpStatus.NOT_FOUND, ex.getCodigo(), ex.getMessage(), solicitud, null);
	}

	@ExceptionHandler(ConflictoExcepcion.class)
	public ResponseEntity<ErrorRespuesta> manejarConflicto(ConflictoExcepcion ex, HttpServletRequest solicitud) {
		List<ErrorCampo> errores = ex.getCampo() == null
				? null
				: List.of(new ErrorCampo(ex.getCampo(), ex.getMessage()));
		return responder(HttpStatus.CONFLICT, ex.getCodigo(), ex.getMessage(), solicitud, errores);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorRespuesta> manejarInesperado(Exception ex, HttpServletRequest solicitud) {
		// Errores propios de Spring MVC (ruta inexistente, método no permitido, etc.): se conserva su estado.
		if (ex instanceof ErrorResponse errorSpring && errorSpring.getStatusCode().is4xxClientError()) {
			HttpStatusCode estado = errorSpring.getStatusCode();
			String mensaje = estado.value() == HttpStatus.NOT_FOUND.value()
					? "El recurso solicitado no existe."
					: "La solicitud no es válida para este recurso.";
			return responder(estado, SOLICITUD_INVALIDA, mensaje, solicitud, null);
		}
		log.error("Error inesperado en {} {}", solicitud.getMethod(), solicitud.getRequestURI(), ex);
		return responder(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_INTERNO,
				"Ocurrió un error inesperado. Intenta nuevamente más tarde.", solicitud, null);
	}

	private ResponseEntity<ErrorRespuesta> responder(HttpStatusCode estado, String codigo, String mensaje,
			HttpServletRequest solicitud, List<ErrorCampo> errores) {
		ErrorRespuesta cuerpo = new ErrorRespuesta(OffsetDateTime.now(), estado.value(), codigo, mensaje,
				solicitud.getRequestURI(), errores);
		return ResponseEntity.status(estado).body(cuerpo);
	}

	private static String nombreCampo(JacksonException ex) {
		return ex.getPath().stream()
				.map(JacksonException.Reference::getPropertyName)
				.filter(nombre -> nombre != null)
				.collect(Collectors.joining("."));
	}

	private static String mensajeValorNoValido(MismatchedInputException ex) {
		if (ex instanceof InvalidFormatException && ex.getTargetType() != null && ex.getTargetType().isEnum()) {
			String permitidos = Arrays.stream(ex.getTargetType().getEnumConstants())
					.map(Object::toString)
					.collect(Collectors.joining(", "));
			return "Valor no permitido. Valores válidos: " + permitidos + ".";
		}
		return MENSAJE_VALOR_NO_VALIDO;
	}

}
