package de.romqu.schimmelhofapi.data


import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.data.shared.constant.LOGIN_URL
import de.romqu.schimmelhofapi.data.shared.httpcall.*
import de.romqu.schimmelhofapi.shared.Result
import okhttp3.Headers
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Repository

@Repository
class UserRepository(
    private val httpCallDelegate: HttpCallDelegate,
    private val postCallDelegate: HttpPostCallDelegate,
) : HttpCall by httpCallDelegate, HttpPostCall by postCallDelegate {

    fun login(
        username: String,
        password: String,
        session: SessionEntity,
    ): Result<HttpCall.Error, Headers> {

        val requestData = with(session) {
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
            addToRequestBody = "&login1%3AUserName=$username&login1%3APassword=$password&login1%3ALoginButton=Anmelden",
            httpCallRequest = requestData
        )

        return makeNullableBodyCall(request) { response ->
            // due to the redirect
            if (response.code == HttpStatus.FOUND.value()) {
                Result.Success(response.headers)
            } else {
                Result.Failure(HttpCall.Error.ResponseUnsuccessful(
                    response.code, response.message
                ))
            }
        }
    }
}


