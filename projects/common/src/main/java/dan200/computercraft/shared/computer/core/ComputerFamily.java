// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringRepresentable;

public enum ComputerFamily implements StringRepresentable {
    NORMAL("normal"),
    ADVANCED("advanced"),
    COMMAND("command");

    private final String name;

    ComputerFamily(String name) {
        this.name = name;
    }

    public static ComputerFamily getFamily(JsonObject json, String name) {
        var familyName = GsonHelper.getAsString(json, name);
        for (var family : values()) {
            if (family.getSerializedName().equalsIgnoreCase(familyName)) return family;
        }

        throw new JsonSyntaxException("Unknown computer family '" + familyName + "' for field " + name);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
