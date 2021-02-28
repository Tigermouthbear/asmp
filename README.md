# asmp
Java class patcher using ASM and compatible with Minecraft Forge. This is a work in progress, more details on use will be provided after the first release.

## Todo List
- Shadow variables
- FIELD @At targeting
- Redirect
- Slice targeting
- Overwrite (easy)

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
