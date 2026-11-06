package com.bga.facturacion_services.facturacion.infrastructure.rest;

import com.bga.facturacion_services.facturacion.application.EmitirFactura.FacturaNoEncontrada;
import com.bga.facturacion_services.facturacion.infrastructure.persistence.JpaFacturaStore.Conflicto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalida(IllegalArgumentException ex) { return problema(400, ex.getMessage()); }
    @ExceptionHandler(FacturaNoEncontrada.class)
    ProblemDetail inexistente(FacturaNoEncontrada ex) { return problema(404, ex.getMessage()); }
    @ExceptionHandler(Conflicto.class)
    ProblemDetail conflicto(Conflicto ex) { return problema(409, ex.getMessage()); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail duplicada(DataIntegrityViolationException ex) {
        return problema(409, "Solicitud concurrente; reintente con la misma Idempotency-Key");
    }
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var detalle = problema(400, "Datos de factura invalidos");
        detalle.setProperty("errores", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).toList());
        return ResponseEntity.badRequest().body(detalle);
    }
    private ProblemDetail problema(int status, String detalle) {
        return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detalle);
    }
}
