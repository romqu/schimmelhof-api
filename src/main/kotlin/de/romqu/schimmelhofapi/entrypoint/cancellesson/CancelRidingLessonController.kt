package de.romqu.schimmelhofapi.entrypoint.cancellesson

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
    fun cancelRidingLesson(
        httpServletResponse: HttpServletResponse,
        @PathVariable id: String,
        session: SessionEntity,
    ): CancelRidingLessonOutDto = service.execute(session, id)
        .doOn({ onSuccess(httpServletResponse, it) }, { onFailure(httpServletResponse) })

    fun onSuccess(
        httpServletResponse: HttpServletResponse,
        ridingLessonId: String,
    ): CancelRidingLessonOutDto {
        httpServletResponse.status = HttpStatus.OK.value()
        return CancelRidingLessonOutDto.newBuilder().setRidingLessonId(ridingLessonId).build()
    }


    fun onFailure(
        httpServletResponse: HttpServletResponse,
    ): CancelRidingLessonOutDto {
        httpServletResponse.status = HttpStatus.BAD_REQUEST.value()
        return CancelRidingLessonOutDto.newBuilder().setErrorMessage("").build()
    }
}