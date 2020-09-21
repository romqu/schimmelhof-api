package de.romqu.schimmelhofapi.data.ridinglesson

import de.romqu.schimmelhofapi.domain.ridinglesson.GetRidingLessonsTask
import java.time.LocalDate

class RidingLessonDayEntity(
    val weekday: GetRidingLessonsTask.Weekday,
    val date: LocalDate,
    val ridingLessons: List<RidingLessonEntity>,
)