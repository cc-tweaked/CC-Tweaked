// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.shared.computer.core.ComputerFamily;

import javax.annotation.Nullable;

public interface IComputerBlockEntity {
    int getComputerID();

    void setComputerID(int id);

    @Nullable
    String getLabel();

    void setLabel(@Nullable String label);

    ComputerFamily getFamily();
}
