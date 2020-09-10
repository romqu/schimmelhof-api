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
    private val postCallDelegate: PostCallDelegate
) : HttpCall by httpCallDelegate, PostCall by postCallDelegate {

    fun login(
        username: String,
        password: String,
        sessionEntity: SessionEntity
    ): Result<HttpCall.Error, HttpCall.Response> {

        val requestData = with(sessionEntity) {
            HttpCallRequestData(
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
            httpCallRequestData = requestData
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


