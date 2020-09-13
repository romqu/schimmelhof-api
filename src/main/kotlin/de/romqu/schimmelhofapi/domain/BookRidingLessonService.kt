package de.romqu.schimmelhofapi.domain

import de.romqu.schimmelhofapi.data.RidingLessonRepository
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
        cookieWeb = currentSession.cookieWeb,
        cookie = currentSession.cookie,
        viewState = currentSession.viewState,
        viewStateGenerator = currentSession.viewStateGenerator,
        eventValidation = currentSession.eventValidation
    ).mapError(CouldNotBookSessionError) {

        it.response.close()
    }

    object CouldNotBookSessionError
}