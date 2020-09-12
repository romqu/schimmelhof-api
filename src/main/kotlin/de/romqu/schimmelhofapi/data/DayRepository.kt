package de.romqu.schimmelhofapi.data

import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class DayRepository {

    private val days = mutableListOf<LocalDate>()

    fun save(days: List<LocalDate>): List<LocalDate> {
        this.days.addAll(days)
        return days
    }

    fun getAll() = days.toList()
}