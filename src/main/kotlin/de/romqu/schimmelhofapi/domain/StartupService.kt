package de.romqu.schimmelhofapi.domain

import de.romqu.schimmelhofapi.data.week.WeekEntity
import de.romqu.schimmelhofapi.data.week.WeekRepository
import de.romqu.schimmelhofapi.data.week.WeekRepository.Companion.NUMBER_OF_WEEK_DAYS
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate


@Service
class StartupService(
    private val weekRepository: WeekRepository,
) {

    companion object {
        private const val DAY_MILLIS = 86400000L
        const val WEEK_MILLIS = DAY_MILLIS * 7
    }

    fun execute() {
        getDaysOfThreeWeeks()
            .saveAsWeek()
            .scheduleAddNewWeekAfterWeekEnded()
    }

    private fun getDaysOfThreeWeeks(): List<LocalDate> =
        datesOfWeekDays(LocalDate.now(), 3)

    private fun datesOfWeekDays(date: LocalDate, numberOfWeeks: Int): List<LocalDate> {

        val firstMonday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        val plusDays = (numberOfWeeks * DayOfWeek.values().size) - 1L

        return (0..plusDays).map { firstMonday.plusDays(it) }
    }

    private fun List<WeekEntity>.scheduleAddNewWeekAfterWeekEnded() {
        val nextMondayAtMidnight = LocalDateTime.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        val nextMondayAtMidnightDate = Date.from(nextMondayAtMidnight.atZone(ZoneId.systemDefault()).toInstant())

        Timer().scheduleAtFixedRate(time = nextMondayAtMidnightDate, period = WEEK_MILLIS) {
            val lastSunday = weekRepository.getAll().last().days.last()
            val nextMonday = lastSunday.plusDays(1)
            val weekDays = datesOfWeekDays(nextMonday, 1)

            weekRepository.save(weekDays.saveAsWeek())
            weekRepository.delete(weekRepository.getAll().first())
        }
    }


    fun List<LocalDate>.saveAsWeek(): List<WeekEntity> =
        chunked(NUMBER_OF_WEEK_DAYS) { days ->
            WeekEntity.of(days)
        }.run(weekRepository::save)
}


