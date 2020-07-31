package dev.tigr.asmp.api;

import dev.tigr.asmp.api.annotations.Modify;
import dev.tigr.asmp.api.annotations.Patch;
import dev.tigr.asmp.api.modification.Modificate;
import dev.tigr.asmp.api.modification.Modification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * ASMP patch manager and administrator
 * @author Tigermouthbear 7/30/20
 */
public class ASMP {
	public static final Logger LOGGER = LogManager.getLogger("ASMP");

	private final Map<String, Class<?>> patches = new HashMap<>();
	private final String identifier;

	public ASMP(String identifier) {
		this.identifier = identifier;

		LOGGER.info("Starting ASMP with id " + identifier);
	}

	public void register(Class<?>... clazzes) {
		for(Class<?> clazz: clazzes) register(clazz);
	}

	public void register(Class<?> clazz) {
		Patch patch = clazz.getAnnotation(Patch.class);
		if(patch != null) patches.put(unmap(patch.value()), clazz);
		else LOGGER.error("[" + identifier + "] Failed to add patch " + clazz.getName() + ", Patch annotation missing");
	}

	public byte[] transform(String name, byte[] bytes) {
		if(!patches.containsKey(name)) return bytes;

		// read class
		ClassReader classReader = new ClassReader(bytes);
		ClassNode classNode = new ClassNode();
		classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

		// patch the classnode
		try {
			Class<?> patchClass = patches.get(name);

			// create new instance of patch
			Object patch;
			Constructor<?> constructor;
			try {
				constructor = patchClass.getConstructor(ClassNode.class);
				patch = constructor.newInstance(classNode);
			} catch(NoSuchMethodException e) {
				constructor = patchClass.getConstructor();
				patch = constructor.newInstance();
			}

			// find the modification and run it
			for(Method method: patchClass.getMethods()) {
				for(Annotation annotation: method.getAnnotations()) {
					Modificate modificate = annotation.annotationType().getAnnotation(Modificate.class);
					if(modificate != null) {
						Modification modification = modificate.value().getConstructor(annotation.annotationType()).newInstance(annotation);
						modification.invoke(classNode, patch, method);
					}
				}
			}
			//TODO: Make this error messages better
		} catch(InstantiationException e) {
			LOGGER.error("[" + identifier + "] Failed to transform class " + name + "! InstantiationException");
			e.printStackTrace();
			return bytes;
		} catch(InvocationTargetException e) {
			LOGGER.error("[" + identifier + "] Failed to transform class " + name + "! InvocationTargetException");
			e.printStackTrace();
			return bytes;
		} catch(NoSuchMethodException e) {
			LOGGER.error("[" + identifier + "] Failed to transform class " + name + "! NoSuchMethodException");
			e.printStackTrace();
			return bytes;
		} catch(IllegalAccessException e) {
			LOGGER.error("[" + identifier + "] Failed to transform class " + name + "! IllegalAccessException");
			e.printStackTrace();
			return bytes;
		}

		// write classnode to classwriter
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(classWriter);

		return classWriter.toByteArray();
	}

	protected String unmap(String clazz) {
		return clazz;
	}
}
