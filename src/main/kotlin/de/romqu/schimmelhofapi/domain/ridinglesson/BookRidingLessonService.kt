package de.romqu.schimmelhofapi.domain.ridinglesson

import de.romqu.schimmelhofapi.data.ridinglesson.RidingLessonRepository
import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.data.shared.constant.INDEX_URL
import de.romqu.schimmelhofapi.domain.UpdateStateValuesTask
import de.romqu.schimmelhofapi.domain.ridinglesson.BookRidingLessonService.Error.CouldNotBookSessionError
import de.romqu.schimmelhofapi.domain.ridinglesson.BookRidingLessonService.Error.CouldNotUpdateSession
import de.romqu.schimmelhofapi.shared.Result
import de.romqu.schimmelhofapi.shared.flatMap
import de.romqu.schimmelhofapi.shared.map
import de.romqu.schimmelhofapi.shared.mapError
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Service

@Service
class BookRidingLessonService(
    private val ridingLessonRepository: RidingLessonRepository,
    private val updateStateValuesTask: UpdateStateValuesTask
) {
    fun execute(
        session: SessionEntity,
        ridingLessonId: String,
    ): Result<Error, String> =
        bookRidingLesson(ridingLessonId, session)
            .updateSession(session)

    private fun bookRidingLesson(
        ridingLessonId: String,
        currentSession: SessionEntity,
    ): Result<CouldNotBookSessionError, BookRidingLessonOut> = ridingLessonRepository.bookRidingLesson(
        ridingLessonId = ridingLessonId,
        session = currentSession
    ).mapError(CouldNotBookSessionError) { httpResponse ->
        val document = Jsoup.parse(
            httpResponse.responseBody.byteStream(),
            Charsets.UTF_8.name(),
            INDEX_URL.toString()
        )
        ridingLessonRepository.closeConnection(httpResponse.responseBody)
        BookRidingLessonOut(document, ridingLessonId)
    }

    class BookRidingLessonOut(
        val document: Document,
        val lessonId: String,
    )

    private fun Result<CouldNotBookSessionError, BookRidingLessonOut>.updateSession(
        session: SessionEntity,
    ): Result<CouldNotUpdateSession, String> =
        flatMap { out ->
            updateStateValuesTask.execute(out.document, session)
                .map { out.lessonId }
        }.mapError(CouldNotUpdateSession)

    sealed class Error {
        object CouldNotBookSessionError : Error()
        object CouldNotUpdateSession : Error()

    }
}

