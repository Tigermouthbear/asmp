package dev.tigr.asmp;

import dev.tigr.asmp.annotations.At;
import dev.tigr.asmp.annotations.Patch;
import dev.tigr.asmp.annotations.modifications.Inject;
import dev.tigr.asmp.annotations.modifications.Modify;
import dev.tigr.asmp.callback.CallbackInfoReturnable;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

// test patch for ASMP Forge Loader
@Patch("net.minecraft.entity.EntityLivingBase")
public class TestPatch {
	@Inject(method = "isPotionActive(Lnet/minecraft/potion/Potion;)Z", at = @At(At.Target.HEAD))
	public static void patch(CallbackInfoReturnable<Boolean> cir) {
		cir.setValue(false);
		cir.cancel();
	}
}