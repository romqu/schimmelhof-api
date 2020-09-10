package de.romqu.schimmelhofapi.data.session

import java.util.*

data class SessionEntity(
    val uuid: UUID,
    val cookie: String,
    val cookieWeb: String,
    val viewState: String,
    val viewStateGenerator: String,
    val eventValidation: String,
)