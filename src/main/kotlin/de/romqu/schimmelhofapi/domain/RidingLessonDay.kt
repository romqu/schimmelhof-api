package de.romqu.schimmelhofapi.domain

import java.time.LocalDate

class RidingLessonDay(
    val weekday: GetRidingLessonsTask.Weekday,
    val date: LocalDate,
    val ridingLessonEntity: GetRidingLessonsTask.RidingLessonEntity,
)