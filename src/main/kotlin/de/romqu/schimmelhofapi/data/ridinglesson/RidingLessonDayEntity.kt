package de.romqu.schimmelhofapi.data.ridinglesson

import de.romqu.schimmelhofapi.domain.ridinglesson.GetRidingLessonDaysTask
import java.time.LocalDate

class RidingLessonDayEntity(
    val weekday: GetRidingLessonDaysTask.Weekday,
    val date: LocalDate,
    val ridingLessons: List<RidingLessonEntity>,
)