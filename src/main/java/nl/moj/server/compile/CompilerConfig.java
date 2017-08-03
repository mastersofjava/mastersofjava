package nl.moj.server.compile;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Configuration
@EnableConfigurationProperties({CompilerProperties.class})
public class CompilerConfig {

    @Bean
    public JavaCompiler systemJavaCompiler() {
        return ToolProvider.getSystemJavaCompiler();
    }

    @Bean
    @Scope(SCOPE_PROTOTYPE)
    public DiagnosticCollector<JavaFileObject> diagnosticCollector() {
        return new DiagnosticCollector<>();
    }

    @Bean
    public StandardJavaFileManager standardJavaFileManager(JavaCompiler javaCompiler, DiagnosticCollector<JavaFileObject> diagnosticCollector) {
        final Charset charset = StandardCharsets.UTF_8;

        return javaCompiler.getStandardFileManager(diagnosticCollector, null, charset);
    }

}
