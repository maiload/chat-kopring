package org.example.chatkopring.common.authority

import org.example.chatkopring.common.exception.CustomAccessDeniedHandler
import org.example.chatkopring.common.exception.CustomAuthenticationEntryPoint
import org.example.chatkopring.common.service.CustomOath2UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
@EnableWebSecurity
class SecurityConfig (
    private val jwtTokenProvider: JwtTokenProvider,
    private val customOath2UserService: CustomOath2UserService,
    private val oAuth2SuccessHandler: OAuth2SuccessHandler,
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
    private val customAccessDeniedHandler: CustomAccessDeniedHandler,
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .httpBasic{ it.disable() }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .formLogin { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/api/member/signup/**", "/api/member/login/**", "api/member/reissue-token").anonymous()
                    .requestMatchers("/api/member/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/member/**", "/api/chat/**").hasAnyRole("MEMBER", "OAUTH_MEMBER", "ADMIN")
                    .anyRequest().permitAll()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(customAuthenticationEntryPoint)
                it.accessDeniedHandler(customAccessDeniedHandler)
            }
            .addFilter(corsFilter())
            .addFilterBefore(
                JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter::class.java
            )
            .oauth2Login {
                it.userInfoEndpoint { u -> u.userService(customOath2UserService) }
                    .successHandler(oAuth2SuccessHandler)
            }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder =
        PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun corsFilter(): CorsFilter {
        val config = CorsConfiguration().apply {
            allowCredentials = true
            addAllowedOriginPattern("*")
            addAllowedHeader("*")
            addAllowedMethod("*")
        }

        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", config)
        }

        return CorsFilter(source)
    }
}