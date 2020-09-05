package de.romqu.schimmelhofapi.data.shared

class RequestSessionValue(
    val viewState: String,
    val viewStateGenerator: String,
    val eventValidation: String,
    val cookie: String
) {
    val eventValidationEncoded: String
        get() = eventValidation
            .replace("/", "%2F")
            .replace("+", "%2B")
}