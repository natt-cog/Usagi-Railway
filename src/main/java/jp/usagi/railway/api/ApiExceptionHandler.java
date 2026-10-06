package jp.usagi.railway.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jp.usagi.railway.api.dto.ApiError;
import jp.usagi.railway.service.BusinessRuleException;
import jp.usagi.railway.service.NotFoundException;
import jp.usagi.railway.service.UrmsException;

@RestControllerAdvice(basePackages = "jp.usagi.railway.api")
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(UrmsException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> businessRule(UrmsException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiError(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        ApiError error = new ApiError("UR-0001", "入力内容に誤りがあります");
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            error.getDetails().add(fe.getField() + ": " + fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e) {
        log.error("予期しないエラー", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("UR-9999", "システムエラーが発生しました"));
    }
}
