package nl.moj.modes.condition;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;

import nl.moj.modes.Mode;

public class IsModeCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        List<AnnotationAttributes> allAnnotationAttributes = metadata.getAnnotations()
                .stream(ConditionalOnMode.class.getName())
                .filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
                .map(MergedAnnotation::asAnnotationAttributes).toList();

        List<ConditionMessage> noMatch = new ArrayList<>();
        List<ConditionMessage> match = new ArrayList<>();
        for (AnnotationAttributes annotationAttributes : allAnnotationAttributes) {
            ConditionOutcome outcome = determineOutcome(annotationAttributes, context.getEnvironment());
            (outcome.isMatch() ? match : noMatch).add(outcome.getConditionMessage());
        }
        if (!noMatch.isEmpty()) {
            return ConditionOutcome.noMatch(ConditionMessage.of(noMatch));
        }
        return ConditionOutcome.match(ConditionMessage.of(match));
    }

    private ConditionOutcome determineOutcome(AnnotationAttributes annotationAttributes, PropertyResolver resolver) {
        Spec spec = new Spec(annotationAttributes);
        List<Mode> missingModes = new ArrayList<>();
        List<Mode> nonMatchingModes = new ArrayList<>();
        if (spec.matches(resolver)) {
            return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnMode.class, spec)
                    .because("matched"));
        } else {
            return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnProperty.class, spec)
                    .because("not matched"));
        }
    }

    private static class Spec {
        private final boolean matchIfMissing;
        private final Mode mode;
        private final String property;

        Spec(AnnotationAttributes annotationAttributes) {
            this.mode = annotationAttributes.getEnum("mode");
            this.property = annotationAttributes.getString("property");
            this.matchIfMissing = annotationAttributes.getBoolean("matchIfMissing");
        }

        public boolean matches(PropertyResolver resolver) {
            Mode m = resolver.getProperty(this.property, Mode.class);
            return (m == null && matchIfMissing) || m == this.mode;
        }
    }
}
