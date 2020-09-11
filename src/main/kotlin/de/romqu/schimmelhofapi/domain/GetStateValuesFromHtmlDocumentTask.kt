package de.romqu.schimmelhofapi.domain

import de.romqu.schimmelhofapi.shared.Result
import org.jsoup.nodes.Document
import org.springframework.stereotype.Service

@Service
class GetStateValuesFromHtmlDocumentTask {

    companion object {
        const val VIEW_STATE = "__VIEWSTATE"
        const val VIEW_STATE_GENERATOR = "__VIEWSTATEGENERATOR"
        const val EVENT_VALIDATION = "__EVENTVALIDATION"
    }

    private val sessionValueKeys = mapOf(
        VIEW_STATE to "",
        VIEW_STATE_GENERATOR to "",
        EVENT_VALIDATION to ""
    )

    class Out(
        val viewState: String,
        val viewStateGenerator: String,
        val eventValidation: String,
    )

    fun execute(htmlDocument: Document): Result<CouldNotParseStateValuesFromHtmlDocumentError, Out> =
        getStateValuesFromHtml(htmlDocument)

    private fun getStateValuesFromHtml(htmlDocument: Document): Result<CouldNotParseStateValuesFromHtmlDocumentError, Out> {

        val sessionValuesMap = sessionValueKeys.mapValues { entry: Map.Entry<String, String> ->
            val element = htmlDocument.getElementById(entry.key)

            if (element == null) return@getStateValuesFromHtml Result.Failure(
                CouldNotParseStateValuesFromHtmlDocumentError
            )
            else element.`val`()
        }

        return Result.Success(
            Out(
                viewState = sessionValuesMap.getValue(VIEW_STATE),
                viewStateGenerator = sessionValuesMap.getValue(VIEW_STATE_GENERATOR),
                eventValidation = sessionValuesMap.getValue(EVENT_VALIDATION)
            )
        )
    }

    object CouldNotParseStateValuesFromHtmlDocumentError
}