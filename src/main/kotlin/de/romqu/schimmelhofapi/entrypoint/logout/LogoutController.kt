package de.romqu.schimmelhofapi.entrypoint.login

import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.domain.LoginService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse


@RestController
class LogoutController(
    private val loginService: LoginService,
) {
    companion object {
        const val PATH_URL = "/api/v1/sessions/"
        const val PROTOBUF_MEDIA_TYPE = "application/x-protobuf"
    }

    @DeleteMapping(PATH_URL, consumes = [PROTOBUF_MEDIA_TYPE])
    fun login(
        @RequestBody loginDtoIn: LoginDtoIn,
        httpServletResponse: HttpServletResponse,
    ): LoginDtoOut = loginService.execute(
        username = loginDtoIn.username,
        passwordPlain = loginDtoIn.passwordPlain
    ).doOn({ onSuccess(it, httpServletResponse) }, { onFailure(it, httpServletResponse) })

    fun onSuccess(session: SessionEntity, httpServletResponse: HttpServletResponse): LoginDtoOut {
        httpServletResponse.status = HttpStatus.OK.value()
        httpServletResponse.addHeader("Authorization", "$BEARER ${session.uuid}")
        return LoginDtoOut.getDefaultInstance()
    }

    fun onFailure(error: LoginService.Error, httpServletResponse: HttpServletResponse): LoginDtoOut {
        httpServletResponse.status = HttpStatus.BAD_REQUEST.value()
        return LoginDtoOut.newBuilder().setErrorMessage(error.toString()).build()
    }
}