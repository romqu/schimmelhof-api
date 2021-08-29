package de.romqu.schimmelhofapi.domain.ridinglesson

import de.romqu.schimmelhofapi.data.ridinglesson.RidingLessonRepository
import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.shared.Result
import de.romqu.schimmelhofapi.shared.mapError
import org.springframework.stereotype.Service

@Service
class BookRidingLessonService(
    private val ridingLessonRepository: RidingLessonRepository,
) {
    fun execute(
        currentSession: SessionEntity,
        ridingLessonId: String,
    ): Result<CouldNotBookSessionError, String> =
        bookRidingLesson(ridingLessonId, currentSession)

    private fun bookRidingLesson(
        ridingLessonId: String,
        currentSession: SessionEntity,
    ): Result<CouldNotBookSessionError, String> = ridingLessonRepository.bookRidingLesson(
        ridingLessonId = ridingLessonId,
        session = currentSession
    ).mapError(CouldNotBookSessionError) { httpResponse ->
        ridingLessonRepository.closeConnection(httpResponse.responseBody)
        ridingLessonId
    }

    object CouldNotBookSessionError
}