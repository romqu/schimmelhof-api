package de.romqu.schimmelhofapi.domain.ridinglesson

import de.romqu.schimmelhofapi.data.ridinglesson.RidingLessonRepository
import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.shared.Result
import de.romqu.schimmelhofapi.shared.mapError
import org.springframework.stereotype.Service

@Service
class CancelRidingLessonService(
    private val ridingLessonRepository: RidingLessonRepository,
) {
    fun execute(
        currentSession: SessionEntity,
        ridingLessonId: String,
    ): Result<CouldNotCancelSessionError, Unit> =
        ridingLessonRepository.cancelRidingLesson(
            ridingLessonId = ridingLessonId,
            session = currentSession
        ).mapError(CouldNotCancelSessionError) { httpResponse ->
            ridingLessonRepository.closeConnection(httpResponse.responseBody)
        }

    object CouldNotCancelSessionError
}