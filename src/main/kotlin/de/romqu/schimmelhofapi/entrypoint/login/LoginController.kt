package de.romqu.schimmelhofapi.entrypoint.login

import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.domain.LoginService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

const val BEARER = "Bearer"

@RestController
class LoginController(
    private val loginService: LoginService,
) {
    companion object {
        const val PATH_URL = "/api/v1/users/login"
        const val PROTOBUF_MEDIA_TYPE = "application/x-protobuf"
    }

    @PostMapping(PATH_URL, consumes = [PROTOBUF_MEDIA_TYPE])
    fun login(
        @RequestBody loginDtoIn: LoginDtoIn,
        httpServletResponse: HttpServletResponse,
    ): LoginDtoOut {
        println("Login: $loginDtoIn")
        return loginService.execute(
            username = loginDtoIn.username,
            passwordPlain = loginDtoIn.passwordPlain
        ).doOn({ onSuccess(it, httpServletResponse) }, { onFailure(it, httpServletResponse) })
    }

    fun onSuccess(session: SessionEntity, httpServletResponse: HttpServletResponse): LoginDtoOut {
        httpServletResponse.status = HttpStatus.OK.value()
        httpServletResponse.addHeader("Authorization", "$BEARER ${session.uuid}")
        return LoginDtoOut.getDefaultInstance()
    }

    fun onFailure(error: LoginService.Error, httpServletResponse: HttpServletResponse): LoginDtoOut {
        println("Login Error: $error")
        httpServletResponse.status = HttpStatus.BAD_REQUEST.value()
        return LoginDtoOut.newBuilder().setErrorMessage(error.toString()).build()
    }
}