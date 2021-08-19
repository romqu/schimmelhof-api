package de.romqu.schimmelhofapi

import de.romqu.schimmelhofapi.entrypoint.getridinglessondays.GetRidingLessonsDaysController
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
class GetRidingLessonDaysControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun test() {

        val bytes = mockMvc.perform(
            MockMvcRequestBuilders.get(GetRidingLessonsDaysController.PATH_URL)
                .header("Authorization", "c99dd6a7-7e9a-438a-8dec-770ee0daf78c")
        ).andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsByteArray
    }
}