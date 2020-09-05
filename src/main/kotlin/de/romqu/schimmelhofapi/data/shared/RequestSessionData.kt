package de.romqu.schimmelhofapi.data.shared

class RequestSessionData(
    val cookie: String,
    val cookieWeb: String,
    val viewState: String,
    val viewStateGenerator: String,
    val eventValidation: String
) {
    val eventValidationEncoded: String
        get() = eventValidation
            .replace("/", "%2F")
            .replace("+", "%2B")
}