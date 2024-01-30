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
package nl.moj.common.util;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import lombok.extern.slf4j.Slf4j;
import nl.moj.common.config.properties.Languages;

@Slf4j
public class JavaVersionUtil {

    private static final Pattern VERSION = Pattern.compile("^.+ version \"(.+)\"");

    public static Integer getRuntimeMajorVersion(Languages.JavaVersion javaVersion) {
        try {
            String e = runVersionCommand(javaVersion, "-fullversion");
            if (e != null) {
                Matcher m = VERSION.matcher(e);
                if (m.find()) {
                    return parseVersion(m.group(1));
                }
            }
            e = runVersionCommand(javaVersion, "-version");
            if (e != null) {
                Matcher m = VERSION.matcher(e);
                if (m.find()) {
                    return parseVersion(m.group(1));
                }
            }
        } catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace("Unable to retrieve JVM version, assuming 'null'", e);
            } else {
                log.warn("Unable to retrieve JVM version, assuming 'null'");
            }
        }
        return null;
    }

    private static String runVersionCommand(Languages.JavaVersion javaVersion, String option) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(javaVersion.getRuntime().toString(), option);
            Process p = pb.start();
            if (p.waitFor(2, TimeUnit.SECONDS)) {
                String e = IOUtils.toString(p.getErrorStream(), StandardCharsets.UTF_8);
                return e;
            }
        } catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace("Unable to retrieve JVM version, could not run configured {}, assuming 'null'",
                        javaVersion.getRuntime(), e);
            } else {
                log.warn("Unable to retrieve JVM version, could not run configured {}, assuming 'null'",
                        javaVersion.getRuntime());
            }
        }
        return null;
    }

    private static Integer parseVersion(String version) {
        try {
            return Runtime.Version.parse(version).feature();
        } catch (Throwable e) {
            return parsePre9Version(version);
        }
    }

    private static Integer parsePre9Version(String version) {
        if (version.length() >= 5 && Character.isDigit(version.charAt(0))
                && version.charAt(1) == '.' && Character.isDigit(version.charAt(2))
                && version.charAt(3) == '.' && Character.isDigit(version.charAt(4))) {
            return Character.digit(version.charAt(2), 10);
        }
        return null;
    }
}
