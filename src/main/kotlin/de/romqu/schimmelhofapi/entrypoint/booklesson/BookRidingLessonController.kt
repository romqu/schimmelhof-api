package de.romqu.schimmelhofapi.entrypoint.booklesson

import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.domain.ridinglesson.BookRidingLessonService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

@RestController
class BookRidingLessonController(
    private val service: BookRidingLessonService,
) {

    companion object {
        const val PATH_URL = "/api/v1/book/lesson/{id}"
        const val PROTOBUF_MEDIA_TYPE = "application/x-protobuf"
    }

    @PostMapping(PATH_URL, produces = [PROTOBUF_MEDIA_TYPE])
    fun bookRidingLesson(
        httpServletResponse: HttpServletResponse,
        @PathVariable id: String,
        session: SessionEntity,
    ): BookRidingLessonOutDto = service.execute(session, id)
        .doOn({ onSuccess(httpServletResponse, it) }, { onFailure(httpServletResponse) })

    fun onSuccess(
        httpServletResponse: HttpServletResponse,
        ridingLessonId: String,
    ): BookRidingLessonOutDto {
        httpServletResponse.status = HttpStatus.OK.value()
        return BookRidingLessonOutDto.newBuilder().setRidingLessonId(ridingLessonId).build()
    }


    fun onFailure(
        httpServletResponse: HttpServletResponse,
    ): BookRidingLessonOutDto {
        httpServletResponse.status = HttpStatus.BAD_REQUEST.value()
        return BookRidingLessonOutDto.newBuilder().setErrorMessage("").build()
    }
}