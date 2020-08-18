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
package nl.moj.server.config.properties;

import javax.validation.constraints.NotNull;
import java.nio.file.Path;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Data
@ConfigurationProperties(prefix = "moj.server")
public class MojServerProperties {

    @NotNull
    private Path assignmentRepo;

    private String authServerUrl;

    private boolean performanceValidation;

    public String getAuthServerUrl() {
        return StringUtils.isEmpty(authServerUrl)?"":authServerUrl;
    }
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
