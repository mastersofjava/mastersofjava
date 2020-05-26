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

import javax.validation.constraints.NotEmpty;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.util.JavaVersionUtil;
import org.apache.commons.lang3.StringUtils;

@Data
@Slf4j
public class Languages {

    @NotEmpty
    private List<JavaVersion> javaVersions = new ArrayList<>();

    public JavaVersion getJavaVersion(Integer version) {

        log.debug("Configured versions: ");
        javaVersions.forEach(jv -> {
            if (jv.getVersion()==version) {
                StringBuilder sb = new StringBuilder();
                sb.append("Version " + jv.getVersion());
                sb.append("  Compiler " + jv.getCompiler());
                sb.append("  Runtime " + jv.getRuntime());
                sb.append("  -> available =  " + isAvailable(jv));
                sb.append("  -> version ok =  " + (jv.getVersion() >= version));
                sb.append("  -> version runtime = " + JavaVersionUtil.getRuntimeMajorVersion(jv));
                log.debug(sb.toString());
            }
        });

        return javaVersions.stream()
                .filter(this::isAvailable)
                .filter(jv -> jv.getVersion() >= version)
                .findFirst()
                .orElseGet(() -> tryJavaHomeFallback(version));
    }

    private boolean isAvailable(JavaVersion javaVersion) {
        return javaVersion.getCompiler().toFile().exists() &&
                javaVersion.getRuntime().toFile().exists() &&
                javaVersion.getVersion().equals(JavaVersionUtil.getRuntimeMajorVersion(javaVersion));
    }

    private JavaVersion tryJavaHomeFallback(Integer version) {
        // we should still check if the specified version is available
        // on the fallback as source and target version
        String javaHome = System.getenv("JAVA_HOME");
        if (StringUtils.isNotBlank(javaHome)) {
            JavaVersion v = new JavaVersion();
            v.setCompiler(Paths.get(javaHome, "bin", "javac"));
            v.setRuntime(Paths.get(javaHome, "bin", "java"));
            v.setName("fallback");
            v.setVersion(JavaVersionUtil.getRuntimeMajorVersion(v));

            if (version != null && version >= 1 && version <= v.getVersion()) {
                log.debug("Using JAVA_HOME since it is an appropriate version");
            } else {
                throw new IllegalArgumentException("No java runtime available for version " + version);
            }
            return v;
        } else {
            throw new IllegalArgumentException("No java version defined and no JAVA_HOME specified, cannot run without a javac/java...");
        }
    }

    @Data
    public static class JavaVersion {

        private Integer version;
        private String name;
        private Path compiler;
        private Path runtime;

    }

}
