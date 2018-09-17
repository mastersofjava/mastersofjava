package nl.moj.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

import java.util.UUID;

@Data
@ConfigurationProperties(prefix = "moj.server.competition")
public class Competition {

	private int successBonus = 400;

	@NonNull
	private UUID uuid;
	
}
