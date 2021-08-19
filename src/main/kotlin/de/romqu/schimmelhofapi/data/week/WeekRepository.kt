package de.romqu.schimmelhofapi.data.week

import org.springframework.stereotype.Repository

@Repository
class WeekRepository {

    companion object {
        const val NUMBER_OF_WEEK_DAYS = 7
    }

    private val weeks = mutableListOf<WeekEntity>()

    fun save(weekEntities: List<WeekEntity>): List<WeekEntity> {
        this.weeks.addAll(weekEntities)
        return weekEntities
    }

    fun delete(weekEntity: WeekEntity) {
        weeks.remove(weekEntity)
    }

    fun getAll() = weeks.toList()
}

