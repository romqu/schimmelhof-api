package de.romqu.schimmelhofapi.data.ridinglesson

import de.romqu.schimmelhofapi.domain.ridinglesson.GetRidingLessonDaysTask
import java.time.LocalDate
import java.time.LocalTime


data class RidingLessonEntity(
    val weekday: GetRidingLessonDaysTask.Weekday,
    val date: LocalDate,
    val title: String,
    val from: LocalTime,
    val to: LocalTime,
    val teacher: String,
    val place: String,
    val lessonCmd: String = "",
    val lessonId: String = "",
    val state: RidingLessonState = RidingLessonState.EXPIRED,
    val action: RidingLessonAction = RidingLessonAction.NONE,
)
