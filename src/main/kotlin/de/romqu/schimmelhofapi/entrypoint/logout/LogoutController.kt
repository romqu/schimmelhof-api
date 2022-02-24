package de.romqu.schimmelhofapi.entrypoint.login

import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.domain.LogoutService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse


@RestController
class LogoutController(
    private val service: LogoutService,
) {
    companion object {
        const val PATH_URL = "/api/v1/sessions/"
        const val PROTOBUF_MEDIA_TYPE = "application/x-protobuf"
    }

    @DeleteMapping(PATH_URL)
    fun logout(
        session: SessionEntity,
        httpServletResponse: HttpServletResponse,
    ): Unit = service.execute(session)
        .doOn({ onSuccess(it, httpServletResponse) }, { onFailure(it, httpServletResponse) })

    fun onSuccess(unit: Unit, httpServletResponse: HttpServletResponse) {
        httpServletResponse.status = HttpStatus.OK.value()
    }

    fun onFailure(error: LogoutService.Error, httpServletResponse: HttpServletResponse) {
        httpServletResponse.status = HttpStatus.BAD_REQUEST.value()
    }
}