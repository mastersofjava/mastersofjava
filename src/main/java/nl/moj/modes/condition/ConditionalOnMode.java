package nl.moj.modes.condition;

import java.lang.annotation.*;

import nl.moj.modes.Mode;
import org.springframework.context.annotation.Conditional;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Conditional(IsModeCondition.class)
public @interface ConditionalOnMode {
    boolean matchIfMissing() default false;

    String property() default "moj.server.mode";

    Mode mode();
}
