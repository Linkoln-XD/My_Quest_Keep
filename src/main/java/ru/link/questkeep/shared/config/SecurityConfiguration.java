package ru.link.questkeep.shared.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import ru.link.questkeep.identity.JwtProperties;
import ru.link.questkeep.identity.StaffSeedProperties;
import ru.link.questkeep.shared.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({ JwtProperties.class, StaffSeedProperties.class, CorsProperties.class })
public class SecurityConfiguration {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(corsProperties.allowedOriginPatterns());
		configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(unauthorizedEntryPoint())
						.accessDeniedHandler(forbiddenHandler()))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/v1/auth/**").permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/bookings/me").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/bookings").hasAuthority("STAFF")
						.requestMatchers(HttpMethod.GET, "/api/v1/waitlist/me").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/waitlist").hasAuthority("STAFF")
						.requestMatchers("/api/v1/waitlist/**").authenticated()
						.requestMatchers("/api/v1/bookings/**").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/tables/**", "/api/v1/games/**", "/api/v1/game-copies/**")
						.authenticated()
						.requestMatchers("/api/v1/tables/**", "/api/v1/games/**", "/api/v1/game-copies/**")
						.hasAuthority("STAFF")
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	private static AuthenticationEntryPoint unauthorizedEntryPoint() {
		return (request, response, exception) -> {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			response.getWriter().write("""
					{"type":"about:blank","title":"Unauthorized","status":401,"detail":"Authentication is required"}
					""");
		};
	}

	private static AccessDeniedHandler forbiddenHandler() {
		return (request, response, exception) -> {
			response.setStatus(HttpStatus.FORBIDDEN.value());
			response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			response.getWriter().write("""
					{"type":"about:blank","title":"Forbidden","status":403,"detail":"Insufficient permissions"}
					""");
		};
	}
}
