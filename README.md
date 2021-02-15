# asmp
Java class patcher using ASM and compatible with Minecraft Forge

## Todo List
- Shadow variables
- FIELD @At targeting
- Redirect
- Slice targeting
- Overwrite (easy)
- Gradle Plugin to automate annotation processing setup

## Forge Example (syntax is subject to change)
```
@Patch("net.minecraft.entity.EntityLivingBase")
public class InjectExamplePatch {
    ///////////////////
    // HEAD Examples //
    ///////////////////

    // makes it so the game thinks entities are always on a ladder
    @Inject(method = "Lnet/minecraft/entity/EntityLivingBase;isOnLadder()Z", at = @At(At.Target.HEAD))
    public void patch0(CallbackInfoReturnable<Boolean> cir) {
        cir.setValue(true);
        cir.cancel();
    }

    // makes it so entities can't heal
    @Inject(method = "Lnet/minecraft/entity/EntityLivingBase;heal(F)V", at = @At(At.Target.HEAD))
    public void patch1(CallbackInfo ci) {
        ci.cancel();
    }

    /////////////////////
    // RETURN Examples //
    /////////////////////

    // makes it so entities hands arent active
    @Inject(method = "Lnet/minecraft/entity/EntityLivingBase;isHandActive()Z", at = @At(At.Target.RETURN))
    public void patch2(CallbackInfoReturnable<Boolean> cir) {
        cir.setValue(false);
    }

    // called every travel tick
    @Inject(method = "Lnet/minecraft/entity/EntityLivingBase;travel(FFF)V", at = @At(At.Target.RETURN))
    public void patch3(CallbackInfo ci) {
        // Ares.EVENT_MANAGER.post(new UpdateMovementEvent());
    }

    /////////////////////
    // INVOKE Examples //
    /////////////////////

    // makes it so players can only take damage from fire
    @Inject(method = "Lnet/minecraft/entity/EntityLivingBase;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z", at = @At(value = At.Target.INVOKE,
            target = "Lnet/minecraft/entity/EntityLivingBase;isEntityInvulnerable(Lnet/minecraft/util/DamageSource;)Z"))
    public void patch4(CallbackInfoReturnable<Boolean> cir, DamageSource damageSource, float amount) {
        if((EntityLivingBase)((Object)this) instanceof EntityPlayer && !damageSource.isFireDamage()) cir.setValue(true);
    }

    // prevent player from moving when elytra flying
    @Inject(method = "Lnet/minecraft/entity/EntityLivingBase;travel(FFF)V", at = @At(value = At.Target.INVOKE,
            target = "Lnet/minecraft/entity/EntityLivingBase;isElytraFlying()Z"))
    public void patch5(CallbackInfoReturnable<Boolean> cir) {
        if(cir.getValue()) cir.cancel();
    }

    // called when move is called on player in travel method
    @Inject(method = "Lnet/minecraft/entity/EntityLivingBase;travel(FFF)V", at = @At(value = At.Target.INVOKE,
            target = "Lnet/minecraft/entity/Entity;move(Lnet/minecraft/entity/MoverType;DDD)V"))
    public void patch6(CallbackInfo ci) {
        // Ares.EVENT_MANAGER.post(new UpdateMovementEvent());
    }

    // prevents entities from swinging main hand
    // a better way would be to inject at HEAD, but this is just for testing
    @Inject(method = "Lnet/minecraft/entity/EntityLivingBase;swingArm(Lnet/minecraft/util/EnumHand;)V", at = @At(value = At.Target.INVOKE,
            target = "Lnet/minecraft/entity/EntityLivingBase;getHeldItem(Lnet/minecraft/util/EnumHand;)Lnet/minecraft/item/ItemStack;"))
    public void patch7(CallbackInfoReturnable<ItemStack> cir, EnumHand hand) {
        if(hand == EnumHand.MAIN_HAND) cir.cancel();
    }
}
```
