package nl.moj.modes.condition;

import java.lang.annotation.*;

import org.springframework.context.annotation.Conditional;

import nl.moj.modes.Mode;

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
