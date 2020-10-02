package de.romqu.schimmelhofapi.entrypoint.getridinglessondays

import de.romqu.schimmelhofapi.data.ridinglesson.RidingLessonDayEntity
import de.romqu.schimmelhofapi.domain.ridinglesson.GetRidingLessonDaysService
import de.romqu.schimmelhofapi.entrypoint.*
import de.romqu.schimmelhofapi.entrypoint.login.GetRidingLessonDaysOutDto
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.time.LocalTime
import java.util.*
import javax.servlet.http.HttpServletResponse

@RestController
class GetRidingLessonsDaysController(
    private val getRidingLessonDaysService: GetRidingLessonDaysService,
) {

    companion object {
        const val PATH_URL = "/api/v1/ridinglessonsdays"
        const val PROTOBUF_MEDIA_TYPE = "application/x-protobuf"
    }

    @PostMapping(PATH_URL, consumes = [PROTOBUF_MEDIA_TYPE])
    fun getRidingLessonDays(
        @RequestHeader("Authorization") tokenValue: String,
        httpServletResponse: HttpServletResponse,
    ): GetRidingLessonDaysOutDto = getRidingLessonDaysService.execute(UUID.fromString(tokenValue))
        .doOn({ onSuccess(it, httpServletResponse) }, { onFailure(it, httpServletResponse) })

    fun onSuccess(
        ridingLessonDays: List<RidingLessonDayEntity>,
        httpServletResponse: HttpServletResponse,
    ): GetRidingLessonDaysOutDto {
        httpServletResponse.status = HttpStatus.OK.value()
        return buildDto(ridingLessonDays)
    }

    private fun buildDto(ridingLessonDays: List<RidingLessonDayEntity>): GetRidingLessonDaysOutDto {
        val ridingLessonDayDtos = ridingLessonDays.map { ridingLessonDay ->

            val weekdayDto = WeekdayDto.valueOf(ridingLessonDay.weekday.name)
            val dateDto = LocalDateDto.newBuilder().apply {
                day = ridingLessonDay.date.dayOfMonth
                month = ridingLessonDay.date.monthValue
                year = ridingLessonDay.date.year
            }.build()


            RidingLessonDayDto.newBuilder().apply {

                weekday = weekdayDto
                date = dateDto

                val ridingLessons = ridingLessonDay.ridingLessons.map { ridingLesson ->

                    val fromDto = buildLocalTime(ridingLesson.from)

                    val toDto = buildLocalTime(ridingLesson.to)

                    RidingLessonDto.newBuilder().apply {
                        date = dateDto
                        weekday = weekdayDto
                        from = fromDto
                        to = toDto
                        title = ridingLesson.title
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
        return GetRidingLessonDaysOutDto.newBuilder()
            .addAllRidingLessonDayDtos(ridingLessonDayDtos).build()
    }

    private fun buildLocalTime(to: LocalTime): LocalTimeDto =
        LocalTimeDto.newBuilder().apply {
            hours = to.hour
            minutes = to.minute
        }.build()

    fun onFailure(
        error: GetRidingLessonDaysService.Error,
        httpServletResponse: HttpServletResponse,
    ): GetRidingLessonDaysOutDto {
        httpServletResponse.status = HttpStatus.BAD_REQUEST.value()
        return GetRidingLessonDaysOutDto.newBuilder().setErrorMessage(error.toString()).build()
    }
}