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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.util.NamedThreadFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
@AllArgsConstructor
public class AsyncConfiguration implements AsyncConfigurer {

    private MojServerProperties mojServerProperties;

    @Bean(name = "parallel")
    public ExecutorService parallelExecutor() {
        return Executors.newFixedThreadPool(mojServerProperties.getRuntime()
                .getGameThreads(), new NamedThreadFactory("parallel"));
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error("Uncaught async error", ex);
    }

    @Bean(name = "sequential")
    public ExecutorService sequentialExecutor() {
        return Executors.newFixedThreadPool(1, new NamedThreadFactory("sequential"));
    }
}
