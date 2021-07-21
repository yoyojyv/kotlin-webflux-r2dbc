package me.jerry.example.webflux.config

import me.jerry.example.webflux.constant.SecurityConstants
import me.jerry.example.webflux.constant.SecurityConstants.Companion.PASSWORD_PREFIX_NOOP
import me.jerry.example.webflux.constant.SecurityConstants.Companion.PROP_USER_ACTUATOR
import me.jerry.example.webflux.constant.SecurityConstants.Companion.PROP_USER_ADMIN
import me.jerry.example.webflux.constant.SecurityConstants.Companion.ROLE_ACTUATOR
import me.jerry.example.webflux.constant.SecurityConstants.Companion.ROLE_ADMIN
import me.jerry.example.webflux.constant.SecurityConstants.Companion.ROLE_SYSTEM
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.actuate.info.InfoEndpoint
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean("adminUser")
    @ConfigurationProperties(prefix = PROP_USER_ADMIN)
    fun adminUser(): SecurityUserCredential {
        return SecurityUserCredential()
    }

    @Bean("actuatorUser")
    @ConfigurationProperties(prefix = PROP_USER_ACTUATOR)
    fun actuatorUser(): SecurityUserCredential {
        return SecurityUserCredential()
    }

    @Bean
    fun userDetailsService(@Qualifier("adminUser") adminUser: SecurityUserCredential,
                           @Qualifier("actuatorUser") actuatorUser: SecurityUserCredential): MapReactiveUserDetailsService {
        val admin = User.withUsername(adminUser.username)
                .password(PASSWORD_PREFIX_NOOP + adminUser.password)
                .roles(ROLE_ADMIN, ROLE_ACTUATOR, ROLE_SYSTEM)
                .build()
        val actuator = User.withUsername(actuatorUser.username)
                .password(PASSWORD_PREFIX_NOOP + actuatorUser.password)
                .roles(ROLE_ACTUATOR)
                .build()
        return MapReactiveUserDetailsService(admin, actuator)
    }

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain? {
        // @formatter:off
        http
                .httpBasic().and()
                .authorizeExchange()

                // actuator - health, info
                .matchers(EndpointRequest.to(HealthEndpoint::class.java, InfoEndpoint::class.java)).permitAll()

                // actuator - others
                .matchers(EndpointRequest.toAnyEndpoint()).hasRole(ROLE_ACTUATOR)

                // others - all open
                .anyExchange().permitAll()

        // csrf disabled
        http.csrf().disable()
        // @formatter:on
        return http.build()
    }

    class SecurityUserCredential {
        lateinit var username: String
        lateinit var password: String
    }

}
