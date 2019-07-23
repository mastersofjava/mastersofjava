package nl.moj.server.config.properties;

import lombok.Data;

@Data
public class Runtime {

    private int gameThreads = 10;
    private boolean playSounds = true;

}
