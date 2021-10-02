package dev.tigr.asmp.annotations;

import dev.tigr.asmp.util.AnnotationReader;
import org.objectweb.asm.tree.AnnotationNode;

/**
 * @author Tigermouthbear 10/1/21
 */
public class Annotations {
    public static class At {
        private final String value;
        private final String target;
        private final int ordinal;

        public At(String value, String target, int ordinal) {
            this.value = value;
            this.target = target;
            this.ordinal = ordinal;
        }

        public String getValue() {
            return value;
        }

        public String getTarget() {
            return target;
        }

        public int getOrdinal() {
            return ordinal;
        }
    }

    public static At readAt(AnnotationNode annotationNode) {
        AnnotationReader annotationReader = new AnnotationReader(annotationNode);
        return new At((String) annotationReader.getValues().getOrDefault("value", "NONE"), (String) annotationReader.getValues().getOrDefault("target", ""), (int) annotationReader.getValues().getOrDefault("ordinal", -1));
    }

    public static class Getter {
        private final String value;

        public Getter(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static Getter readGetter(AnnotationNode annotationNode) {
        AnnotationReader annotationReader = new AnnotationReader(annotationNode);
        return new Getter((String) annotationReader.getValues().get("value"));
    }

    public static class Inject {
        private final String method;
        private final At at;

        public Inject(String method, At at) {
            this.method = method;
            this.at = at;
        }

        public String getMethod() {
            return method;
        }

        public At getAt() {
            return at;
        }
    }

    public static Inject readInject(AnnotationNode annotationNode) {
        AnnotationReader annotationReader = new AnnotationReader(annotationNode);
        return new Inject((String) annotationReader.getValues().get("method"), readAt((AnnotationNode) annotationReader.getValues().get("at")));
    }

    public static class Invoker {
        private final String value;

        public Invoker(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static Invoker readInvoker(AnnotationNode annotationNode) {
        AnnotationReader annotationReader = new AnnotationReader(annotationNode);
        return new Invoker((String) annotationReader.getValues().get("value"));
    }

    public static class Modify {
        private final String value;
        private final At at;

        public Modify(String value, At at) {
            this.value = value;
            this.at = at;
        }

        public String getValue() {
            return value;
        }

        public At getAt() {
            return at;
        }
    }

    public static Modify readModify(AnnotationNode annotationNode) {
        AnnotationReader annotationReader = new AnnotationReader(annotationNode);
        Object at = annotationReader.getValues().get("at");
        return new Modify((String) annotationReader.getValues().getOrDefault("value", ""), at == null ? new At("NONE", "", -1) : readAt((AnnotationNode) at));
    }

    public static class Overwrite {
        private final String value;

        public Overwrite(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static Overwrite readOverwrite(AnnotationNode annotationNode) {
        AnnotationReader annotationReader = new AnnotationReader(annotationNode);
        return new Overwrite((String) annotationReader.getValues().get("value"));
    }

    public static class Redirect {
        private final String method;
        private final At at;

        public Redirect(String method, At at) {
            this.method = method;
            this.at = at;
        }

        public String getMethod() {
            return method;
        }

        public At getAt() {
            return at;
        }
    }

    public static Redirect readRedirect(AnnotationNode annotationNode) {
        AnnotationReader annotationReader = new AnnotationReader(annotationNode);
        return new Redirect((String) annotationReader.getValues().get("method"), readAt((AnnotationNode) annotationReader.getValues().get("at")));
    }

    public static class Setter {
        private final String value;

        public Setter(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static Setter readSetter(AnnotationNode annotationNode) {
        AnnotationReader annotationReader = new AnnotationReader(annotationNode);
        return new Setter((String) annotationReader.getValues().get("value"));
    }
}
