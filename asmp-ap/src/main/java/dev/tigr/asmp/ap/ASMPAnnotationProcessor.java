package dev.tigr.asmp.ap;

import dev.tigr.asmp.annotations.Patch;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * @author Tigermouthbear
 */
@SupportedAnnotationTypes({
    "dev.tigr.asmp.annotations.Patch",
    "dev.tigr.asmp.annotations.At",
    "dev.tigr.asmp.annotations.Inject",
    "dev.tigr.asmp.annotations.Modify"
})
public class ASMPAnnotationProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        for(Element element: roundEnvironment.getElementsAnnotatedWith(Patch.class)) {
            Patch patch = element.getAnnotation(Patch.class);
            // wow patch!
        }

        return true;
    }
}
