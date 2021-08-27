package de.romqu.schimmelhofapi.entrypoint.booklesson

import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.domain.ridinglesson.CancelRidingLessonService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

@RestController
class CancelRidingLessonController(
    private val service: CancelRidingLessonService,
) {

    companion object {
        const val PATH_URL = "/api/v1/book/lesson/{id}"
        const val PROTOBUF_MEDIA_TYPE = "application/x-protobuf"
    }

    @DeleteMapping(PATH_URL, produces = [PROTOBUF_MEDIA_TYPE])
    fun bookRidingLesson(
        httpServletResponse: HttpServletResponse,
        @PathVariable id: String,
        session: SessionEntity,
    ) = service.execute(session, id)
        .doOn({ onSuccess(httpServletResponse) }, { onFailure(httpServletResponse) })

    fun onSuccess(
        httpServletResponse: HttpServletResponse,
    ) {
        httpServletResponse.status = HttpStatus.OK.value()
    }


    fun onFailure(
        httpServletResponse: HttpServletResponse,
    ) {
        httpServletResponse.status = HttpStatus.BAD_REQUEST.value()
    }
}