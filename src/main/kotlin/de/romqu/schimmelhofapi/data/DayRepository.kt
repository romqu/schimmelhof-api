package de.romqu.schimmelhofapi.data

import org.springframework.stereotype.Repository
import java.time.DayOfWeek
import java.time.LocalDate

@Repository
class DayRepository {

    private val days = mutableListOf<LocalDate>()

    fun save(days: List<LocalDate>): List<LocalDate> {
        this.days.addAll(days)
        return days
    }

    fun delete(range: IntRange) {

        days.removeAll(days.subList(range.first, range.last))
    }

    fun getAll() = days.toList()

    fun getFirst(dayOfWeek: DayOfWeek): LocalDate =
        days.first { it.dayOfWeek == dayOfWeek }
}