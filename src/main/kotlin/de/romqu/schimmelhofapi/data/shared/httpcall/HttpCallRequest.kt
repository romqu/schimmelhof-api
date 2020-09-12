package de.romqu.schimmelhofapi.data.shared.httpcall

data class HttpCallRequest(
    val cookie: String,
    val cookieWeb: String,
    val viewState: String,
    val viewStateGenerator: String,
    val eventValidation: String,
    val eventArgument: String = "",
    val eventTarget: String = "",
) {
    val eventValidationEncoded: String
        get() = eventValidation
            .replace("/", "%2F")
            .replace("+", "%2B")
}