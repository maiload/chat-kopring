package org.example.chatkopring.common.exception

import org.apache.tomcat.util.http.fileupload.impl.InvalidContentTypeException
import org.example.chatkopring.common.dto.BaseResponse
import org.example.chatkopring.common.status.ResultCode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@ControllerAdvice
class CustomExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    protected fun methodArgumentNotValidException(ex: MethodArgumentNotValidException): ResponseEntity<BaseResponse<Map<String, String>>> {
        val errors = mutableMapOf<String, String>()
        var errorMessage = ""
        ex.bindingResult.allErrors.forEach { error ->
            val fieldName = (error as FieldError).field
            errorMessage = error.defaultMessage ?: "Not Exception Message"
            errors[fieldName] = errorMessage
        }

        return ResponseEntity(BaseResponse(ResultCode.ERROR.name, errors, errorMessage), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(InvalidInputException::class)
    protected fun invalidInputException(ex: InvalidInputException): ResponseEntity<BaseResponse<Map<String, String>>> {
        val errors = mapOf(ex.fieldName to ex.message)
        return ResponseEntity(BaseResponse(ResultCode.ERROR.name, errors, ex.message), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(BadCredentialsException::class)
    protected fun badCredentialsException(ex: BadCredentialsException): ResponseEntity<BaseResponse<Map<String, String>>> {
        val message = "아이디 혹은 비밀번호를 다시 확인하세요."
        val errors = mapOf("로그인 실패" to message)
        return ResponseEntity(BaseResponse(ResultCode.ERROR.name, errors, ex.message ?: message), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(UnAuthorizationException::class)
    protected fun unAuthorizationException(ex: UnAuthorizationException): ResponseEntity<BaseResponse<Map<String, String>>> {
        val errors = mapOf(ex.loginId to ex.message)
        return ResponseEntity(BaseResponse(ResultCode.ERROR.name, errors, ex.message), HttpStatus.BAD_REQUEST)
    }
    
    @ExceptionHandler(IllegalArgumentException::class)
    protected fun illegalArgumentException(ex: IllegalArgumentException): ResponseEntity<BaseResponse<Map<String, String>>> {
        return ResponseEntity(BaseResponse(ResultCode.ERROR.name, null, ex.message ?: "미처리 에러"), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(InvalidContentTypeException::class)
    protected fun invalidContentTypeException(ex: InvalidContentTypeException): ResponseEntity<BaseResponse<Map<String, String>>> {
        return ResponseEntity(BaseResponse(ResultCode.ERROR.name, null, ex.message ?: "Content-Type 에러"), HttpStatus.BAD_REQUEST)
    }

//    @ExceptionHandler(Exception::class)
//    protected fun defaultException(ex: Exception): ResponseEntity<BaseResponse<Map<String, String>>> {
//        val message = ex.message ?: "Not Exception Message"
//        val errors = mapOf("미처리 에러" to message)
//        return ResponseEntity(BaseResponse(ResultCode.ERROR.name, errors, message), HttpStatus.BAD_REQUEST)
//    }
}