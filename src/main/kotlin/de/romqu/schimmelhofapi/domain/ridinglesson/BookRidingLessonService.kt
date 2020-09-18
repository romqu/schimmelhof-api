package de.romqu.schimmelhofapi.domain.ridinglesson

import de.romqu.schimmelhofapi.data.ridinglesson.RidingLessonRepository
import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.shared.mapError
import org.springframework.stereotype.Service

@Service
class BookRidingLessonService(
    private val ridingLessonRepository: RidingLessonRepository,
) {
    fun execute(currentSession: SessionEntity, ridingLessonId: String) =
        bookRidingLesson(ridingLessonId, currentSession)

    private fun bookRidingLesson(
        ridingLessonId: String,
        currentSession: SessionEntity,
    ) = ridingLessonRepository.bookRidingLesson(
        ridingLessonId = ridingLessonId,
        session = currentSession
    ).mapError(CouldNotBookSessionError) {
        it.responseBody.close()
    }

    object CouldNotBookSessionError
}