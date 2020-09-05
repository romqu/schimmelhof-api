package de.romqu.schimmelhofapi.data


import de.romqu.schimmelhofapi.core.Result
import de.romqu.schimmelhofapi.data.shared.HttpCall
import de.romqu.schimmelhofapi.data.shared.HttpCallDelegate
import de.romqu.schimmelhofapi.data.shared.RequestSessionData
import de.romqu.schimmelhofapi.data.shared.constant.LOGIN_URL
import okhttp3.Headers
import org.springframework.stereotype.Repository

@Repository
class UserRepository(
    private val httpCallDelegate: HttpCallDelegate
) : HttpCall by httpCallDelegate {

    fun login(
        username: String,
        password: String,
        requestSessionData: RequestSessionData
    ): Result<HttpCall.Error, Headers> {

        val request = createPostRequest(
            url = LOGIN_URL,
            addToRequestBody = "",
            requestSessionData = requestSessionData
        )

        return makeNoBodyCall(request)
    }
}


