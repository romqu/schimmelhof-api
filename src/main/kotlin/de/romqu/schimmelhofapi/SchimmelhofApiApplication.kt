package de.romqu.schimmelhofapi

import de.romqu.schimmelhofapi.domain.StartupService
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter


const val SET_COOKIE_HEADER = "Set-Cookie"
const val COOKIE_HEADER = "Cookie"

@SpringBootApplication
class SchimmelhofApiApplication(
    startupService: StartupService,
) {
    init {
        startupService.execute()
    }

    @Bean
    fun protobufHttpMessageConverter(): ProtobufHttpMessageConverter =
        ProtobufHttpMessageConverter()
}

fun main(args: Array<String>) {
    runApplication<SchimmelhofApiApplication>(*args)
}