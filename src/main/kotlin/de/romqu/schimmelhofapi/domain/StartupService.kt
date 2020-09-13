package de.romqu.schimmelhofapi.domain

import de.romqu.schimmelhofapi.data.DayRepository
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
    private val dayRepository: DayRepository,
) {

    companion object {
        private const val DAY_MILLIS = 86400000L
        const val WEEK_MILLIS = DAY_MILLIS * 7
    }

    fun execute() {
        getDaysOfThreeWeeks()
            .saveDays()
            .scheduleAddNewWeekAfterWeekEnded()
    }

    private fun getDaysOfThreeWeeks(): List<LocalDate> =
        datesOfWeekDays(LocalDate.now(), 3)

    private fun datesOfWeekDays(date: LocalDate, numberOfWeeks: Int): List<LocalDate> {

        val firstMonday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        val plusDays = (numberOfWeeks * DayOfWeek.values().size) - 1L

        return (0..plusDays).map { firstMonday.plusDays(it) }
    }

    fun List<LocalDate>.saveDays() = dayRepository.save(this)

    private fun List<LocalDate>.scheduleAddNewWeekAfterWeekEnded() {
        val nextMondayAtMidnight = LocalDateTime.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        val nextMondayAtMidnightDate = Date.from(nextMondayAtMidnight.atZone(ZoneId.systemDefault()).toInstant())

        Timer().scheduleAtFixedRate(time = nextMondayAtMidnightDate, period = WEEK_MILLIS) {
            val lastSunday = dayRepository.getAll().lastOrNull()!!
            val monday = lastSunday.plusDays(1)
            val weekDays = datesOfWeekDays(monday, 1)

            dayRepository.save(weekDays)
            dayRepository.delete(0..6)
        }
    }
}


