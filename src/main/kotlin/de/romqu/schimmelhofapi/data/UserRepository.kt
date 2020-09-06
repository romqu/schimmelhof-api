package de.romqu.schimmelhofapi.data


import de.romqu.schimmelhofapi.core.Result
import de.romqu.schimmelhofapi.data.shared.HttpCall
import de.romqu.schimmelhofapi.data.shared.HttpCallDelegate
import de.romqu.schimmelhofapi.data.shared.RequestSessionData
import de.romqu.schimmelhofapi.data.shared.constant.LOGIN_URL
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Repository

@Repository
class UserRepository(
    private val httpCallDelegate: HttpCallDelegate
) : HttpCall by httpCallDelegate {

    fun login(
        username: String,
        password: String,
        requestSessionData: RequestSessionData
    ): Result<HttpCall.Error, HttpCall.Response> {

        val request = createPostRequest(
            url = LOGIN_URL,
            addToRequestBody = "",
            requestSessionData = requestSessionData
        )

        return makeCall(request) { response, responseBody ->
            // due to the redirect
            if (response.code == HttpStatus.FOUND.value()) {
                Result.Success(HttpCall.Response(response.headers, responseBody))
            } else {
                Result.Failure(HttpCall.Error.ResponseUnsuccessful(
                    response.code, response.message
                ))
            }
        }
    }
}


