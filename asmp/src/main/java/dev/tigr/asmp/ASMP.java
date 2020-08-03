package dev.tigr.asmp;

import dev.tigr.asmp.annotations.Patch;
import dev.tigr.asmp.modification.Modificate;
import dev.tigr.asmp.modification.Modification;
import dev.tigr.asmp.obfuscation.IObfuscationMapper;
import dev.tigr.asmp.obfuscation.NoObfuscationMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

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
	private final IObfuscationMapper obfuscationMapper;

	public ASMP(String identifier) {
		this(identifier, new NoObfuscationMapper());
	}

	public ASMP(String identifier, IObfuscationMapper obfuscationMapper) {
		this.identifier = identifier;
		this.obfuscationMapper = obfuscationMapper;

		LOGGER.info("Starting ASMP with id " + identifier);
	}

	public void register(Class<?>... clazzes) {
		for(Class<?> clazz: clazzes) register(clazz);
	}

	public void register(Class<?> clazz) {
		Patch patch = clazz.getAnnotation(Patch.class);
		if(patch != null) patches.put(getObfuscationMapper().unmapClass(patch.value()), clazz);
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
						Modification<?> modification = modificate.value().getConstructor(ASMP.class, annotation.annotationType()).newInstance(this, annotation);
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
		} catch(IllegalAccessException e) {
			LOGGER.error("[" + identifier + "] Failed to transform class " + name + "! IllegalAccessException");
			e.printStackTrace();
			return bytes;
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		// write classnode to classwriter
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(classWriter);

		return classWriter.toByteArray();
	}

	public IObfuscationMapper getObfuscationMapper() {
		return obfuscationMapper;
	}
}
