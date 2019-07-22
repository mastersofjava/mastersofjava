package nl.moj.server.config.properties;

import lombok.Data;
import org.aspectj.weaver.ast.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
public class Runtime {

    private int gameThreads = 10;
    private boolean playSounds = true;

}
