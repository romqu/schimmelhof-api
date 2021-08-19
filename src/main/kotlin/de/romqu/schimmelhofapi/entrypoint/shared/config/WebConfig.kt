package de.romqu.schimmelhofapi.entrypoint.shared.config

import de.romqu.schimmelhofapi.entrypoint.shared.HeaderSessionMethodArgumentResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
class WebConfig : WebMvcConfigurer {

    @Autowired
    lateinit var headerSessionMethodArgumentResolver: HeaderSessionMethodArgumentResolver

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(headerSessionMethodArgumentResolver)
        super.addArgumentResolvers(resolvers)
    }
}


