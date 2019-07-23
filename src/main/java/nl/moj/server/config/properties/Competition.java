package nl.moj.server.config.properties;

import javax.validation.constraints.NotNull;
import java.util.UUID;

import lombok.Data;

@Data
public class Competition {

    private int successBonus = 400;

    @NotNull
    private UUID uuid;

}
