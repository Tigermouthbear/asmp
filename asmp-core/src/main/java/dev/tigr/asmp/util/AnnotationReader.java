package dev.tigr.asmp.util;

import org.objectweb.asm.tree.AnnotationNode;

import java.util.HashMap;

/**
 * reads all annotation values into HashMap
 * @author Tigermouthbear 10/1/21
 */
public class AnnotationReader {
    private final HashMap<String, Object> values = new HashMap<>();

    public AnnotationReader(AnnotationNode annotationNode) {
        if(annotationNode.values != null) {
            boolean first = true;
            String last = null;
            for(Object object: annotationNode.values) {
                if(first) {
                    last = (String) object;
                    first = false;
                } else {
                    values.put(last, object);
                    first = true;
                }
            }
        }
    }

    public HashMap<String, Object> getValues() {
        return values;
    }
}