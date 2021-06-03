package dan200.computercraft.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;

@Mixin (ChatHud.class)
public interface ChatHudAccess {
	@Invoker
	void callAddMessage(Text text, int messageId);

    @Invoker
    void callRemoveMessage(int messageId);
}
