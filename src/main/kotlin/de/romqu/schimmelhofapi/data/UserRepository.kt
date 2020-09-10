package de.romqu.schimmelhofapi.data


import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.data.shared.constant.LOGIN_URL
import de.romqu.schimmelhofapi.data.shared.httpcall.*
import de.romqu.schimmelhofapi.shared.Result
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Repository

@Repository
class UserRepository(
    private val httpCallDelegate: HttpCallDelegate,
    private val postCallDelegate: HttpPostCallDelegate
) : HttpCall by httpCallDelegate, HttpPostCall by postCallDelegate {

    fun login(
        username: String,
        password: String,
        sessionEntity: SessionEntity
    ): Result<HttpCall.Error, HttpCall.Response> {

        val requestData = with(sessionEntity) {
            HttpCallRequest(
                cookie = cookie,
                cookieWeb = cookieWeb,
                viewState = viewState,
                viewStateGenerator = viewStateGenerator,
                eventValidation = eventValidation
            )
        }

        val request = createPostRequest(
            url = LOGIN_URL,
            addToRequestBody = "",
            httpCallRequest = requestData
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


