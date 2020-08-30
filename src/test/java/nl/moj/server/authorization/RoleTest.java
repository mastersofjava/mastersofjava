package nl.moj.server.authorization;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class RoleTest {

	@Test
	void testIsWithControleRole() {
		assertTrue(Role.isWithControleRole(List.of(Role.ADMIN)));
		assertTrue(Role.isWithControleRole(List.of(Role.GAME_MASTER)));
	}

	@Test
	void testIsWithoutControleRole() {
		assertFalse(Role.isWithControleRole(List.of(Role.USER)));
		assertFalse(Role.isWithControleRole(List.of(Role.ANONYMOUS)));
	}
	
	@Test
	void testISWithControleRoleMixedRoles() {
		assertTrue(Role.isWithControleRole(List.of(Role.USER, Role.ADMIN)));
	}
}
