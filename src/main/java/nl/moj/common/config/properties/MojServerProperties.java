/*
   Copyright 2020 First Eight BV (The Netherlands)
 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.moj.common.config.properties;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.Data;
import nl.moj.modes.Mode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Data
@ConfigurationProperties(prefix = MojServerProperties.PREFIX )
public class MojServerProperties {

    public static final String PREFIX = "moj.server";
    public static final String MODE_PROPERTY = PREFIX + ".mode";

    @NotNull
    private Mode mode = Mode.DEFAULT;

    private URI controllerEndpoint = null;

    private Path dataDirectory = Paths.get("data");

    @NestedConfigurationProperty
    private Limits limits = new Limits();
    @NestedConfigurationProperty
    private Languages languages = new Languages();
    @NestedConfigurationProperty
    private Runtime runtime = new Runtime();
    @NestedConfigurationProperty
    private Competition competition;

    public Path getDataDirectory() {
        if (dataDirectory.isAbsolute()) {
            return dataDirectory;
        }
        return Paths.get(System.getProperty("user.dir")).resolve(dataDirectory);
    }
}
