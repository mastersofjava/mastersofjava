package nl.moj.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
public class Competition {

	private int successBonus = 400;

	@NotNull
	private UUID uuid;
	
}
