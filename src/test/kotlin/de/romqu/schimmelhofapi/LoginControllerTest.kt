package de.romqu.schimmelhofapi

import de.romqu.schimmelhofapi.entrypoint.LoginController
import de.romqu.schimmelhofapi.entrypoint.LoginDtoIn
import de.romqu.schimmelhofapi.entrypoint.LoginDtoOut
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status


@ExtendWith(SpringExtension::class)
//@WebMvcTest(controllers = [LoginController::class])
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoginControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun test() {

        val loginDtoIn = LoginDtoIn.newBuilder().apply {
            username = "14394"
            passwordPlain = "J6WVh6ZHv7msMfMZLLWCSHzJMC6wkZeuqRWNis2WZBnhmvx5eskTN92"
        }.build()

        val bytes = mockMvc.perform(
            MockMvcRequestBuilders.post(LoginController.PATH_URL)
                .contentType(LoginController.PROTOBUF_MEDIA_TYPE)
                .content(loginDtoIn.toByteArray())
        ).andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsByteArray


        assertThat(LoginDtoOut.parseFrom(bytes)).isNotNull
    }
}