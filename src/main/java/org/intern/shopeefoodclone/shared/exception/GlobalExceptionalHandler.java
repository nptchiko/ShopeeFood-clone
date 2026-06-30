package org.intern.shopeefoodclone.shared.exception;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.intern.shopeefoodclone.shared.api.ErrorApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Hidden
@Slf4j
@RestControllerAdvice
public class GlobalExceptionalHandler {

    @ExceptionHandler(AppException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorApiResponse handleAppException(AppException ex) {
        log.error("AppException occurred: {}", ex.getMessage(), ex);
        return ErrorApiResponse.builder()
                .message(ex.getErrorCode().getDetailedMessage())
                .status(ex.getErrorCode().getCode())
                .detailMessage(ex.getMessage())
                .build();
    }

}
