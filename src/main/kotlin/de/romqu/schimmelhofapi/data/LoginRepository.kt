package de.romqu.schimmelhofapi.data


import de.romqu.schimmelhofapi.core.Result
import de.romqu.schimmelhofapi.data.shared.HttpCall
import de.romqu.schimmelhofapi.data.shared.HttpCallDelegate
import de.romqu.schimmelhofapi.data.shared.LOGIN_URL
import de.romqu.schimmelhofapi.data.shared.RequestSessionData
import okhttp3.Headers
import okhttp3.Request
import org.springframework.stereotype.Repository

@Repository
class LoginRepository(
    private val httpCallDelegate: HttpCallDelegate
) : HttpCall by httpCallDelegate {


    fun getInitialResponse(): Result<HttpCall.Error, HttpCall.Response> {
        val request = Request.Builder().url(LOGIN_URL).get().build()

        return makeCall(request) { response, responseBody ->
            // due to the redirect
            if (response.code == 302) {
                Result.Success(HttpCall.Response(response.headers, responseBody))
            } else {
                Result.Failure(HttpCall.Error.ResponseUnsuccessful(
                    response.code, response.message
                ))
            }
        }
    }

    fun getLoginHeadersResponse(
        username: String,
        password: String,
        requestSessionData: RequestSessionData
    ): Result<HttpCall.Error, Headers> {

        val request = with(requestSessionData) {
            createPostRequest(
                url = LOGIN_URL,
                addToRequestBody = "",
                requestSessionData = requestSessionData
            )
        }

        return makeNoBodyCall(request)
    }
}


