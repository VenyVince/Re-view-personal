package com.review.shop.config;

import com.review.shop.dto.ErrorResponseDTO;
import com.review.shop.exception.DatabaseException;
import com.review.shop.exception.FileProcessingException;
import com.review.shop.exception.ResourceNotFoundException;
import com.review.shop.exception.WrongRequestException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

// 프로젝트 전체 예외 처리 클래스
@RestControllerAdvice
@Slf4j
public class ExceptionHandlers {
 
    //exception 패키지 참조

    // 이외 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGlobalException(Exception e, HttpServletRequest r) {
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Other Exception!",
                e.getMessage(),
                r.getRequestURI(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponseDTO);
    }

    // 데이터 없음
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFount(ResourceNotFoundException e, HttpServletRequest r) {
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                e.getMessage(),
                r.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDTO);
    }

    // 잘못된 요청
    @ExceptionHandler(WrongRequestException.class)
    public ResponseEntity<ErrorResponseDTO> handleWrongRequest(WrongRequestException e, HttpServletRequest r) {
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Bad_Request",
                e.getMessage(),
                r.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDTO);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(
            MethodArgumentNotValidException e,
            HttpServletRequest r
    ) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                message,
                r.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDTO);
    }

    // DB 관련(Mapper.xml 오류 등)
    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<?> handleDatabase(DatabaseException e, HttpServletRequest r) {
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                e.getMessage(),
                r.getRequestURI(),
                LocalDateTime.now()
        );
        // 전체 스택을 콘솔에 출력
        log.error("Internal Server Error", e);

        // cause 까지 로깅
        if (e.getCause() != null) {
            log.error("Cause Exception : {}", e.getCause().getMessage(), e.getCause());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponseDTO);
    }

    // 로그인 실패
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleLoginException(BadCredentialsException e, HttpServletRequest r) {
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                e.getMessage(),
                r.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponseDTO);
    }

    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<ErrorResponseDTO> handleIOException(
            FileProcessingException exception,
            HttpServletRequest request
    ) {
        log.error("파일 처리 중 오류 발생", exception);

        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "File Processing Error",
                exception.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponseDTO);
    }
}
