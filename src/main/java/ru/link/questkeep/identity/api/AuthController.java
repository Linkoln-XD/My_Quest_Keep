package ru.link.questkeep.identity.api;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ru.link.questkeep.identity.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
		AuthService.TokenPair pair = authService.register(request.email(), request.password());
		return TokenResponse.of(pair.accessToken(), pair.refreshToken());
	}

	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		AuthService.TokenPair pair = authService.login(request.email(), request.password());
		return TokenResponse.of(pair.accessToken(), pair.refreshToken());
	}

	@PostMapping("/refresh")
	public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
		AuthService.TokenPair pair = authService.refresh(request.refreshToken());
		return TokenResponse.of(pair.accessToken(), pair.refreshToken());
	}
}
