# asmp
Java class patcher using ASM and compatible with Minecraft Forge. This is a work in progress, more details on use will be provided after the first release.

## Todo List
- Fix RETURN crash after try catch
- Shadow variables
- FIELD @At targeting
- Slice targeting

## Using (0.2-SNAPSHOT) with forgegradle
### First add the maven repo
```
maven {
    name = "tigr.dev"
    url = "https://maven.tigr.dev"
}
```

### Next apply plugin
```
buildscript {
    repositories {
        jcenter()
        maven {
            name = "forge"
            url = "http://files.minecraftforge.net/maven"
        }
        maven {
            name = "tigr.dev"
            url = "https://maven.tigr.dev"
        }
    }
    dependencies {
        classpath "net.minecraftforge.gradle:ForgeGradle:3+"
        classpath "dev.tigr.asmp:asmp-plugin:0.2-SNAPSHOT"
    }
}
apply plugin: "net.minecraftforge.gradle"
apply plugin: "dev.tigr.asmp"
```

### Then configure the plugin
```
asmp {
    input = "${buildDir.path}/extractSrg/output.srg"
    inputFormat = "TSRG"
    intermediaryInput = "${buildDir.path}/createMcpToSrg/output.tsrg"
    intermediaryInputFormat = "TSRG"
    mappingsName = "asmp.modid.notch.srg"
    intermediaryMappingsName = "asmp.modid.searge.srg"
    tasks = ["jar"]
}
```

### Then add the dependencies
```
dependencies {
    implementation "dev.tigr.asmp:asmp-core:0.2-SNAPSHOT"
    implementation "dev.tigr.asmp:asmp-forge:0.2-SNAPSHOT"
    annotationProcessor "dev.tigr.asmp:asmp-ap:0.2-SNAPSHOT"
}
```

### Then add a Loading Plugin
```
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class ASMPLoader extends ASMPForgeLoader {
    public ASMPLoader() {
        super("modid", Transformer.class);
        register(ExamplePatch.class);
    }

    public static class Transformer extends ASMPForgeTransformer {
        public Transformer() {
            super(ASMPForgeLoader.get("modid"));
        }
    }
}
```

### Then add a patch (make sure its registered in the loading plugin)
```
@Patch("net.minecraft.entity.EntityLivingBase")
public class ExamplePatch {
    // makes it so entities can't heal
    @Inject(method = "Lnet/minecraft/entity/EntityLivingBase;heal(F)V", at = @At("HEAD"))
    public void patch1(CallbackInfo ci) {
        ci.cancel();
    }
}
```

### Add loading plugin class to jar manifest
```
jar {
    manifest {
        attributes(
                "FMLCorePluginContainsFMLMod": "true",
                "FMLCorePlugin": "com.example.ASMPLoader",
                "ForceLoadAsMod": "true"
        )
    }
}
```

### You should be all set!
For a working example (with mixin integration too), see https://github.com/AresClient/ares/tree/asmp/ares-forge

## Examples patching minecraft (syntax is subject to change)
```
@Patch("net.minecraft.entity.EntityLivingBase")
public class InjectExamplePatch {
    ///////////////////
    // HEAD Examples //
    ///////////////////

    // makes it so the game thinks entities are always on a ladder
    @Inject(method = "Lnet/minecraft/entity/EntityLivingBase;isOnLadder()Z", at = @At("HEAD"))
    public void patch0(CallbackInfoReturnable<Boolean> cir) {
        cir.setValue(true);
        cir.cancel();
    }

    // makes it so entities can't heal
    @Inject(method = "Lnet/minecraft/entity/EntityLivingBase;heal(F)V", at = @At("HEAD"))
    public void patch1(CallbackInfo ci) {
        ci.cancel();
    }

    /////////////////////
    // RETURN Examples //
    /////////////////////

    // makes it so entities hands arent active
    @Inject(method = "Lnet/minecraft/entity/EntityLivingBase;isHandActive()Z", at = @At("RETURN"))
    public void patch2(CallbackInfoReturnable<Boolean> cir) {
        cir.setValue(false);
    }

    // called every travel tick
    @Inject(method = "Lnet/minecraft/entity/EntityLivingBase;travel(FFF)V", at = @At("RETURN"))
    public void patch3(CallbackInfo ci) {
        // Ares.EVENT_MANAGER.post(new UpdateMovementEvent());
    }

    /////////////////////
    // INVOKE Examples //
    /////////////////////

    // makes it so players can only take damage from fire
    @Inject(method = "Lnet/minecraft/entity/EntityLivingBase;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityLivingBase;isEntityInvulnerable(Lnet/minecraft/util/DamageSource;)Z"))
    public void patch4(CallbackInfoReturnable<Boolean> cir, DamageSource damageSource, float amount) {
        if((EntityLivingBase)((Object)this) instanceof EntityPlayer && !damageSource.isFireDamage()) cir.setValue(true);
    }

    // prevent player from moving when elytra flying
    @Inject(method = "Lnet/minecraft/entity/EntityLivingBase;travel(FFF)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityLivingBase;isElytraFlying()Z"))
    public void patch5(CallbackInfoReturnable<Boolean> cir) {
        if(cir.getValue()) cir.cancel();
    }

    // called when move is called on player in travel method
    @Inject(method = "Lnet/minecraft/entity/EntityLivingBase;travel(FFF)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;move(Lnet/minecraft/entity/MoverType;DDD)V"))
    public void patch6(CallbackInfo ci) {
        // Ares.EVENT_MANAGER.post(new UpdateMovementEvent());
    }

    // prevents entities from swinging main hand
    // a better way would be to inject at HEAD, but this is just for testing
    @Inject(method = "Lnet/minecraft/entity/EntityLivingBase;swingArm(Lnet/minecraft/util/EnumHand;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityLivingBase;getHeldItem(Lnet/minecraft/util/EnumHand;)Lnet/minecraft/item/ItemStack;"))
    public void patch7(CallbackInfoReturnable<ItemStack> cir, EnumHand hand) {
        if(hand == EnumHand.MAIN_HAND) cir.cancel();
    }
}
```
```
@Patch("net.minecraft.entity.Entity")
public class ModifyExamplePatch {
    // patch whole classNode
    @Modify
    public void patch0(ClassNode classNode) {
        ASMP.LOGGER.info("we patched class " + classNode.name + "!");
    }

    // patch methodNode
    @Modify("Lnet/minecraft/entity/Entity;<init>(Lnet/minecraft/world/World;)V")
    public void patch1(MethodNode methodNode) {
        ASMP.LOGGER.info("we patched method " + methodNode.name + "!");
    }

    // prints 'new entity created!' every time an entity is created
    // insert instruction list before all return instructions in method
    @Modify(value = "Lnet/minecraft/entity/Entity;<init>(Lnet/minecraft/world/World;)V", at = @At("RETURN"))
    public void patch2(InsnModifier insnModifier) {
        insnModifier.insertBefore(
            new FieldInsnNode(Opcodes.GETSTATIC, "dev/tigr/asmp/ASMP", "LOGGER",
                    "Lorg/apache/logging/log4j/Logger;"),
            new LdcInsnNode("new entity created!"),
            new MethodInsnNode(Opcodes.INVOKEINTERFACE, "org/apache/logging/log4j/Logger", "info",
                    "(Ljava/lang/String;)V", true)
        );
    }
}
```
