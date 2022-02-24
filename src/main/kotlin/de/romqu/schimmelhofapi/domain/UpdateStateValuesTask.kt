package de.romqu.schimmelhofapi.domain

import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.data.session.SessionRepository
import de.romqu.schimmelhofapi.shared.Result
import de.romqu.schimmelhofapi.shared.map
import org.jsoup.nodes.Document
import org.springframework.stereotype.Service

@Service
class UpdateStateValuesTask(
    private val getStateValuesTask: GetStateValuesTask,
    private val sessionRepository: SessionRepository,
) {
    fun execute(
        document: Document,
        session: SessionEntity
    ): Result<GetStateValuesTask.CouldNotParseStateValuesFromHtmlDocumentError, SessionEntity> =
        getStateValuesTask.execute(document)
            .map { sessionRepository.saveOrUpdate(session) }

}