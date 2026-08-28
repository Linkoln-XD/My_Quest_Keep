package ru.link.questkeep.shared.security;

import java.util.UUID;

import ru.link.questkeep.identity.Role;

public record AuthenticatedUser(UUID id, String email, Role role) {

	public boolean staff() {
		return role == Role.STAFF;
	}
}
