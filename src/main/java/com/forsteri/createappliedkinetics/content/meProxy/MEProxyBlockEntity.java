package com.forsteri.createappliedkinetics.content.meProxy;

import appeng.api.networking.IGrid;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.me.storage.NetworkStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MEProxyBlockEntity extends AENetworkBlockEntity {
    MEProxyInventoryHandler handler;
    IGrid lastKnownGrid;

    public MEProxyBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState) {
        super(blockEntityType, pos, blockState);
        this.lastKnownGrid = getMainNode().getGrid(); // now should get grid only when it needs to
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        IGrid currentGrid = getMainNode().getGrid();
        if (this.lastKnownGrid == null || this.lastKnownGrid != currentGrid) {
            this.lastKnownGrid = currentGrid;
            if (this.handler != null && currentGrid != null) {
                // VERY UGLY HACK (auto-updates item handler's ME network references)
                this.handler.service = currentGrid.getStorageService();
                this.handler.storage = (NetworkStorage) this.handler.service.getInventory();
            }
        }

        if (this.lastKnownGrid != null && (cap == ForgeCapabilities.ITEM_HANDLER || cap == ForgeCapabilities.FLUID_HANDLER)) {
            if (handler == null) {
                handler = new MEProxyInventoryHandler((this.lastKnownGrid.getStorageService()));
            }
            return LazyOptional.of(() -> handler).cast();
        }

        return super.getCapability(cap, side);
    }
}
