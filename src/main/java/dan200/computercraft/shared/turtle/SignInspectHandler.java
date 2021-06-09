/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle;

import java.util.HashMap;
import java.util.Map;

import com.google.common.eventbus.Subscribe;

import dan200.computercraft.api.turtle.event.TurtleBlockEvent;
import dan200.computercraft.fabric.mixin.SignBlockEntityAccess;

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
                textTable.put(k+1, ((SignBlockEntityAccess)sbe).getText()[k].asString());
            }
            event.getData().put("text", textTable);
        }
    }
}
