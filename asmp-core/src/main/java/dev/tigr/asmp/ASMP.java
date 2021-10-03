package dev.tigr.asmp;

import dev.tigr.asmp.annotations.Annotations;
import dev.tigr.asmp.annotations.Patch;
import dev.tigr.asmp.modification.modifications.*;
import dev.tigr.asmp.obfuscation.IObfuscationMapper;
import dev.tigr.asmp.obfuscation.NoObfuscationMapper;
import dev.tigr.asmp.util.ASMPClassLoader;
import dev.tigr.asmp.util.NodeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ASMP patch manager and administrator
 * @author Tigermouthbear 7/30/20
 */
public class ASMP {
	public static final Logger LOGGER = LogManager.getLogger("ASMP");
	private static final ASMPClassLoader CLASS_LOADER = new ASMPClassLoader(ASMP.class.getClassLoader());

	private final Map<String, List<Class<?>>> patches = new HashMap<>();
	private final Map<String, List<Class<?>>> accessors = new HashMap<>();
	protected final String identifier;
	protected IObfuscationMapper obfuscationMapper;

	public ASMP(String identifier) {
		this(identifier, new NoObfuscationMapper());
	}

	public ASMP(String identifier, IObfuscationMapper obfuscationMapper) {
		this.identifier = identifier;
		this.obfuscationMapper = obfuscationMapper;

		LOGGER.info("Starting ASMP with ID: " + identifier);
	}

	public void register(Class<?>... clazzes) {
		for(Class<?> clazz: clazzes) register(clazz);
	}

	public void register(Class<?> clazz) {
		Patch patch = clazz.getAnnotation(Patch.class);
		if(patch != null) {
			if(clazz.isInterface()) accessors.computeIfAbsent(getObfuscationMapper().unmapClass(patch.value()), k -> new ArrayList<>()).add(clazz);
			else patches.computeIfAbsent(getObfuscationMapper().unmapClass(patch.value()), k -> new ArrayList<>()).add(clazz);
		}
		else LOGGER.error("[" + identifier + "] Failed to add patch " + clazz.getName() + ", Patch annotation missing");
	}

	public boolean shouldTransform(String name) {
		return patches.containsKey(name) || accessors.containsKey(name);
	}

	public byte[] transform(String name, byte[] bytes) {
		if(!shouldTransform(name)) return bytes;

		// read class
		ClassReader classReader = new ClassReader(bytes);
		ClassNode classNode = new ClassNode();
		classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

		transform(name, classNode);

		// write classnode to classwriter and return
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		classNode.accept(classWriter);
		return classWriter.toByteArray();
	}

	public ClassNode transform(String name, ClassNode classNode) {
		if(patches.containsKey(name)) {
			// patch the classnode
			List<Class<?>> patchClasses = patches.get(name);

			for(Class<?> patchClass: patchClasses) {
				ClassNode patchNode = NodeUtils.readClassNode(patchClass);

				// make generated node and add all methods which need to be called at transform time (Modify patches)
				ClassNode generatedNode = null;
				for(MethodNode methodNode: patchNode.methods) {
					if(methodNode.visibleAnnotations == null) continue;
					for(AnnotationNode annotationNode: methodNode.visibleAnnotations) {
						if(annotationNode.desc.equals("Ldev/tigr/asmp/annotations/modifications/Modify;")) {
							if(generatedNode == null) {
								generatedNode = new ClassNode();
								generatedNode.version = Opcodes.V1_8; // TODO: MAKE THIS CONFIGURABLE
								generatedNode.access = Opcodes.ACC_PUBLIC;
								generatedNode.name = "dev/tigr/asmp/generated/" + patchClass.getSimpleName();
								generatedNode.superName = "java/lang/Object";

								MethodNode init = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
								init.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
								init.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
								init.instructions.add(new InsnNode(Opcodes.RETURN));
								generatedNode.methods.add(init);
							}
							generatedNode.methods.add(methodNode);
							break;
						}
					}
				}

				// load generated node into classpath
				Object generated = null;
				if(generatedNode != null) {
					// load actual class
					Class<?> clazz = CLASS_LOADER.defineClass(generatedNode);
					try {
						generated = clazz.newInstance();
					} catch(InstantiationException | IllegalAccessException e) {
						e.printStackTrace();
					}
				}

				// find the modifications and run them
				for(MethodNode methodNode: patchNode.methods) {
					if(methodNode.visibleAnnotations == null) continue;
					label:
					for(AnnotationNode annotationNode: methodNode.visibleAnnotations) {
						switch(annotationNode.desc) {
							case "Ldev/tigr/asmp/annotations/modifications/Inject;":
								InjectModification injectModification = new InjectModification(this, Annotations.readInject(annotationNode));
								injectModification.invoke(patchNode, classNode, methodNode);
								break label;
							case "Ldev/tigr/asmp/annotations/modifications/Modify;":
								if(generated != null) {
									ModifyModification modifyModification = new ModifyModification(this, Annotations.readModify(annotationNode));
									modifyModification.invoke(patchNode, classNode, methodNode, generated);
								}
								break label;
							case "Ldev/tigr/asmp/annotations/modifications/Overwrite;":
								OverwriteModification overwriteModification = new OverwriteModification(this, Annotations.readOverwrite(annotationNode));
								overwriteModification.invoke(patchNode, classNode, methodNode);
								break label;
							case "Ldev/tigr/asmp/annotations/modifications/Redirect;":
								RedirectModification redirectModification = new RedirectModification(this, Annotations.readRedirect(annotationNode));
								redirectModification.invoke(patchNode, classNode, methodNode);
								break label;
						}
					}
				}
			}
		}

		if(accessors.containsKey(name)) {
			// patch the classnode
			List<Class<?>> accessorClasses = accessors.get(name);

			for(Class<?> accessorClass: accessorClasses) {
				ClassNode accessorNode = NodeUtils.readClassNode(accessorClass);

				// find the modifications and run them
				for(MethodNode methodNode: accessorNode.methods) {
					if(methodNode.visibleAnnotations == null) continue;
					label:
					for(AnnotationNode annotationNode: methodNode.visibleAnnotations) {
						switch(annotationNode.desc) {
							case "Ldev/tigr/asmp/annotations/modifications/Getter;":
								GetterModification getterModification = new GetterModification(this, Annotations.readGetter(annotationNode));
								getterModification.invoke(accessorNode, classNode, methodNode);
								break label;
							case "Ldev/tigr/asmp/annotations/modifications/Setter;":
								SetterModification setterModification = new SetterModification(this, Annotations.readSetter(annotationNode));
								setterModification.invoke(accessorNode, classNode, methodNode);
								break label;
							case "Ldev/tigr/asmp/annotations/modifications/Invoker;":
								InvokerModification invokerModification = new InvokerModification(this, Annotations.readInvoker(annotationNode));
								invokerModification.invoke(accessorNode, classNode, methodNode);
								break label;
						}
					}
				}
			}
		}

		return classNode;
	}

	public IObfuscationMapper getObfuscationMapper() {
		return obfuscationMapper;
	}

	public String getIdentifier() {
		return identifier;
	}
}
