package de.romqu.schimmelhofapi.data


import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.data.shared.constant.LOGIN_URL
import de.romqu.schimmelhofapi.data.shared.httpcall.HttpCall
import de.romqu.schimmelhofapi.data.shared.httpcall.HttpCallRequest
import de.romqu.schimmelhofapi.data.shared.httpcall.HttpPostCall
import de.romqu.schimmelhofapi.data.shared.httpcall.HttpPostCallDelegate
import de.romqu.schimmelhofapi.shared.Result
import okhttp3.Headers
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Repository

@Repository
class UserRepository(
    private val postCallDelegate: HttpPostCallDelegate,
) : HttpPostCall by postCallDelegate {

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
            addToRequestBody = "&login1%24UserName=$username&login1%24Password=$password&login1%24LoginButton=Anmelden",
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


