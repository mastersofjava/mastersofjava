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
package nl.moj.server.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import lombok.AllArgsConstructor;
import nl.moj.server.config.properties.MojServerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.PathMatcher;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@AllArgsConstructor
public class WebConfiguration {

    private MojServerProperties mojServerProperties;
    //private SessionRegistry sessionRegistry;

    @Configuration
    public class WebConfig implements WebMvcConfigurer {

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            Path path = Paths.get(mojServerProperties.getDirectories().getJavadocDirectory());
            if (!path.isAbsolute()) {
                path = mojServerProperties.getDirectories().getBaseDirectory()
                        .resolve(mojServerProperties.getDirectories().getJavadocDirectory());
            }
            registry.addResourceHandler("/javadoc/**").addResourceLocations(path.toAbsolutePath().toUri().toString());
        }
    }
}
