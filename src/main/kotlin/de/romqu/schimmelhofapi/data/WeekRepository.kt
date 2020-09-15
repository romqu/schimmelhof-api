package de.romqu.schimmelhofapi.data

import de.romqu.schimmelhofapi.data.WeekRepository.Companion.NUMBER_OF_WEEK_DAYS
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class WeekRepository {

    companion object {
        const val NUMBER_OF_WEEK_DAYS = 7
    }

    private val weeks = mutableListOf<Week>()

    fun save(weeks: List<Week>): List<Week> {
        this.weeks.addAll(weeks)
        return weeks
    }

    fun delete(week: Week) {
        weeks.remove(week)
    }

    fun getAll() = weeks.toList()
}

class Week private constructor(
    val days: Array<LocalDate>,
) {
    companion object {
        fun of(days: List<LocalDate>) = Week(days.take(NUMBER_OF_WEEK_DAYS).toTypedArray())
    }
}