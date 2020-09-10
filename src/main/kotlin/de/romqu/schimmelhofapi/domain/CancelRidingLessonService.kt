package de.romqu.schimmelhofapi.domain

import de.romqu.schimmelhofapi.data.RidingLessonRepository
import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.shared.mapError
import org.springframework.stereotype.Service

@Service
class CancelRidingLessonService(
    private val ridingLessonRepository: RidingLessonRepository,
) {
    fun execute(currentSession: SessionEntity, ridingLessonId: String) =
        ridingLessonRepository.cancelRidingLesson(
            ridingLessonId = ridingLessonId,
            cookieWeb = currentSession.cookieWeb,
            cookie = currentSession.cookie,
            viewState = currentSession.viewState,
            viewStateGenerator = currentSession.viewStateGenerator,
            eventValidation = currentSession.eventValidation
        ).mapError(CouldNotCancelSessionError) { it.response.close() }

    object CouldNotCancelSessionError
}