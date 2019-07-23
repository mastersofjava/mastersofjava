package nl.moj.server.config.properties;

import javax.validation.constraints.NotNull;
import java.nio.file.Path;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Data
@ConfigurationProperties(prefix = "moj.server")
public class MojServerProperties {

    @NotNull
    private Path assignmentRepo;
    @NestedConfigurationProperty
    private Limits limits = new Limits();
    @NestedConfigurationProperty
    private Directories directories = new Directories();
    @NestedConfigurationProperty
    private Languages languages = new Languages();
    @NestedConfigurationProperty
    private Runtime runtime = new Runtime();
    @NestedConfigurationProperty
    private Competition competition;

}
