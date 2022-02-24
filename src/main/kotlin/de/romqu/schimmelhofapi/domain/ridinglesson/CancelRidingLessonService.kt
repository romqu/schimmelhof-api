package de.romqu.schimmelhofapi.domain.ridinglesson

import de.romqu.schimmelhofapi.data.ridinglesson.RidingLessonRepository
import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.data.shared.constant.INDEX_URL
import de.romqu.schimmelhofapi.domain.UpdateStateValuesTask
import de.romqu.schimmelhofapi.domain.ridinglesson.CancelRidingLessonService.Error.CouldNotCancelSession
import de.romqu.schimmelhofapi.domain.ridinglesson.CancelRidingLessonService.Error.CouldNotUpdateSession
import de.romqu.schimmelhofapi.shared.Result
import de.romqu.schimmelhofapi.shared.flatMap
import de.romqu.schimmelhofapi.shared.map
import de.romqu.schimmelhofapi.shared.mapError
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Service

@Service
class CancelRidingLessonService(
    private val ridingLessonRepository: RidingLessonRepository,
    private val updateStateValuesTask: UpdateStateValuesTask,
) {
    fun execute(
        session: SessionEntity,
        ridingLessonId: String,
    ): Result<Error, String> =
        cancelRidingLesson(ridingLessonId, session)
            .updateSession(session)


    private fun cancelRidingLesson(
        ridingLessonId: String,
        currentSession: SessionEntity
    ): Result<CouldNotCancelSession, CancelRidingLessonOut> =
        ridingLessonRepository.cancelRidingLesson(
            ridingLessonId = ridingLessonId,
            session = currentSession
        ).mapError(CouldNotCancelSession) { httpResponse ->
            val document = Jsoup.parse(
                httpResponse.responseBody.byteStream(),
                Charsets.UTF_8.name(),
                INDEX_URL.toString()
            )
            ridingLessonRepository.closeConnection(httpResponse.responseBody)
            CancelRidingLessonOut(document, ridingLessonId)
        }

    class CancelRidingLessonOut(
        val document: Document,
        val lessonId: String,
    )

    private fun Result<CouldNotCancelSession, CancelRidingLessonOut>.updateSession(
        session: SessionEntity,
    ): Result<Error, String> =
        flatMap { out ->
            updateStateValuesTask.execute(out.document, session)
                .map { out.lessonId }
        }.mapError(CouldNotUpdateSession)

    sealed class Error {
        object CouldNotCancelSession : Error()
        object CouldNotUpdateSession : Error()
    }
}