package de.romqu.schimmelhofapi.data.week

import java.time.LocalDate

class WeekEntity private constructor(
    val days: Array<LocalDate>,
) {
    companion object {
        fun of(days: List<LocalDate>) = WeekEntity(days.take(WeekRepository.NUMBER_OF_WEEK_DAYS).toTypedArray())
    }
}