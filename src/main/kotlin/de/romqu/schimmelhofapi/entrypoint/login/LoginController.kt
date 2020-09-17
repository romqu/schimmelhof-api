package de.romqu.schimmelhofapi.entrypoint.login

import de.romqu.schimmelhofapi.domain.LoginService
import de.romqu.schimmelhofapi.entrypoint.RidingLessonDayDto
import de.romqu.schimmelhofapi.entrypoint.RidingLessonDto
import de.romqu.schimmelhofapi.entrypoint.WeekdayDto
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

@RestController
class LoginController(
    private val loginService: LoginService,
) {
    companion object {
        const val PATH_URL = "/api/v1/login"
        const val PROTOBUF_MEDIA_TYPE = "application/x-protobuf"
    }

    @PostMapping(PATH_URL, consumes = [PROTOBUF_MEDIA_TYPE])
    fun login(
        @RequestBody loginDtoIn: LoginDtoIn,
        httpServletResponse: HttpServletResponse,
    ): LoginDtoOut = loginService.execute(
        username = loginDtoIn.username,
        passwordPlain = loginDtoIn.passwordPlain
    ).doOn({ onSuccess(it, httpServletResponse) }, { onFailure(it, httpServletResponse) })

    fun onSuccess(response: LoginService.Response, httpServletResponse: HttpServletResponse): LoginDtoOut {
        httpServletResponse.status = HttpStatus.OK.value()
        val ridingLessonDayDtos = response.ridingLessonDays.map { ridingLessonDay ->

            val weekdayDto = WeekdayDto.valueOf(ridingLessonDay.weekday.name)

            RidingLessonDayDto.newBuilder().apply {
                weekday = weekdayDto

                val ridingLessons = ridingLessonDay.ridingLessons.map { ridingLesson ->
                    RidingLessonDto.newBuilder().apply {
                        weekday = weekdayDto
                        title = ridingLesson.title
                        time = ridingLesson.from.toString()
                        teacher = ridingLesson.teacher
                        place = ridingLesson.place
                        lessonCmd = ridingLesson.lessonCmd
                        lessonId = ridingLesson.lessonId
                        state = RidingLessonDto.RidingLessonState.valueOf(ridingLesson.state.name)
                        action = RidingLessonDto.RidingLessonAction.valueOf(ridingLesson.action.name)
                    }.build()
                }

                addAllRidingLessons(ridingLessons)
            }.build()

        }
        return LoginDtoOut.newBuilder().addAllRidingLessonDay(ridingLessonDayDtos).build()
    }

    fun onFailure(error: LoginService.Error, httpServletResponse: HttpServletResponse): LoginDtoOut {
        httpServletResponse.status = HttpStatus.BAD_REQUEST.value()
        return LoginDtoOut.newBuilder().setErrorMessage(error.toString()).build()

    }
}