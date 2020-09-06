package dan200.computercraft.shared.turtle;

import java.util.HashMap;
import java.util.Map;

import com.google.common.eventbus.Subscribe;

import dan200.computercraft.api.turtle.event.TurtleBlockEvent;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;

public class SignInspectHandler {
    @Subscribe
    public void onTurtleInspect(TurtleBlockEvent.Inspect event) {
        BlockEntity be = event.getWorld().getBlockEntity(event.getPos());
        if (be instanceof SignBlockEntity) {
            SignBlockEntity sbe = (SignBlockEntity)be;
            Map<Integer, String> textTable = new HashMap<>();
            for(int k = 0; k < 4; k++) {
                textTable.put(k+1, sbe.getTextOnRow(k).asString());
            }
            event.getData().put("text", textTable);
        }
    }
}
