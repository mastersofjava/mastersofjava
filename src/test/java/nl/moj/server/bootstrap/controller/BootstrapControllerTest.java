package nl.moj.server.bootstrap.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import nl.moj.server.bootstrap.service.BootstrapService;

@ExtendWith(MockitoExtension.class)
class BootstrapControllerTest {

	@Mock
	private BootstrapService bootstrapService;
	
	@InjectMocks
	private BootstrapController controller;
	
	@Test
	void testShouldBootstrap() {
		when(bootstrapService.isBootstrapNeeded()).thenReturn(true);
		
		assertEquals("bootstrap", controller.bootstrap(null));
	}

	@Test
	void testIsAlreadyBootstrapped() {
		when(bootstrapService.isBootstrapNeeded()).thenReturn(false);
		
		assertEquals("redirect:/", controller.bootstrap(mock(HttpServletRequest.class)));
	}

	@Test
	void testIsAlreadyBootstrappedDoBootstrap() {
		when(bootstrapService.isBootstrapNeeded()).thenReturn(false);
		
		assertEquals("redirect:/control", controller.doBootstrap(null));
	}
	
	@Test
	void testDoBootstrap() throws IOException {
		when(bootstrapService.isBootstrapNeeded()).thenReturn(true);
		assertEquals("redirect:/control", controller.doBootstrap(mock(RedirectAttributes.class)));
	}
	
	@Test
	void testDoBootstrapFailure() throws IOException {
		when(bootstrapService.isBootstrapNeeded()).thenReturn(true);
		doThrow(IOException.class)
			.when(bootstrapService)
			.bootstrap();
		
		assertEquals("redirect:/bootstrap", controller.doBootstrap(mock(RedirectAttributes.class)));
	}
}
