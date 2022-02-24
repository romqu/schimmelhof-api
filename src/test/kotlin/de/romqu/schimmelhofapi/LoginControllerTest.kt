package de.romqu.schimmelhofapi

import de.romqu.schimmelhofapi.data.session.SessionRepository
import de.romqu.schimmelhofapi.domain.GetSessionByAuthHeaderTask
import de.romqu.schimmelhofapi.entrypoint.getridinglessondays.GetRidingLessonsDaysController
import de.romqu.schimmelhofapi.entrypoint.login.*
import de.romqu.schimmelhofapi.shared.map
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import redis.clients.jedis.Jedis
import java.util.*


@ExtendWith(SpringExtension::class)
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class LoginControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var sessionRepository: SessionRepository

    @Autowired
    lateinit var getSessionByAuthHeaderTask: GetSessionByAuthHeaderTask

    @Autowired
    lateinit var jedis: Jedis

    lateinit var token: String

    @Test
    @Order(0)
    fun logoutAll() {
        jedis.keys("*")
            .mapNotNull {
                try {
                    UUID.fromString(it)
                } catch (ex: Exception) {
                    null
                }
            }
            .map { token ->
                sessionRepository.getBy(token)
            }.map { result ->
                result.map { session ->
                    doLogout(session.uuid.toString())
                }
            }
    }

    @Test
    @Order(1)
    fun login() {

        val loginDtoIn = LoginDtoIn.newBuilder().apply {
            username = "15411"
            passwordPlain = "nEVd64RoOWdV6NPmAyFjoqL0IR3itWPtVOW"
        }.build()

        val response = mockMvc.perform(
            MockMvcRequestBuilders.post(LoginController.PATH_URL)
                .contentType(LoginController.PROTOBUF_MEDIA_TYPE)
                .content(loginDtoIn.toByteArray())
        ).andExpect(status().isOk)
            .andReturn()
            .response

        val dto = LoginDtoOut.parseFrom(response.contentAsByteArray)

        token = getSessionByAuthHeaderTask.execute(
            response.getHeaderValue(HttpHeaders.AUTHORIZATION)?.toString()
        ).unwrapOrNull()?.uuid?.toString() ?: ""

        assertThat(token).isNotEmpty
        assertThat(dto).isNotNull
    }

    @Test
    @Order(2)
    fun getLessons() {

        val response = mockMvc.perform(
            MockMvcRequestBuilders.get(GetRidingLessonsDaysController.PATH_URL)
                .header("Authorization", "$BEARER $token")
        ).andExpect(status().isOk)
            .andReturn()
            .response

        val dto = GetRidingLessonDaysOutDto.parseFrom(response.contentAsByteArray)

        dto.ridingLessonDayDtosList

        assertThat(dto).isNotNull
    }

/*    @Test
    @Order(3)
    fun bookLesson() {

        val response = mockMvc.perform(
            MockMvcRequestBuilders.post(BookRidingLessonController.PATH_URL, 12121)
                .header("Authorization", "$BEARER $token")
        ).andExpect(status().isOk)
            .andReturn()
            .response
    }

    @Test
    @Order(4)
    fun cancelLesson() {

        val response = mockMvc.perform(
            MockMvcRequestBuilders.delete(BookRidingLessonController.PATH_URL, 12121)
                .header("Authorization", "$BEARER $token")
        ).andExpect(status().isOk)
            .andReturn()
            .response
    }*/

    @Test
    @Order(5)
    fun logout() {
        doLogout()
    }

    private fun doLogout(token1: String = token) {
        mockMvc.perform(
            MockMvcRequestBuilders.delete(LogoutController.PATH_URL)
                .header("Authorization", "$BEARER $token1")
        ).andExpect(status().isOk)
            .andReturn()
            .response
    }
}
