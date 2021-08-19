package de.romqu.schimmelhofapi.entrypoint.shared

import de.romqu.schimmelhofapi.entrypoint.AuthErrorDto
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus


@ControllerAdvice
@ResponseBody
class InvalidSessionExceptionHandler {

    object InvalidSessionException : Exception()

    @ExceptionHandler(InvalidSessionException::class)
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    fun invalidSessionException(
        ex: InvalidSessionException,
    ): AuthErrorDto = AuthErrorDto
        .newBuilder()
        .setErrorMessage("UNAUTHORIZED")
        .build()
}