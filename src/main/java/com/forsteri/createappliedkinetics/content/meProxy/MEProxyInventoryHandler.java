package com.forsteri.createappliedkinetics.content.meProxy;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.EmptyFluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MEProxyInventoryHandler implements IItemHandler, IFluidHandler {
    IStorageService service;
    MEStorage storage;

    public MEProxyInventoryHandler(IStorageService storageService) {
        this.service = storageService;
        this.storage = storageService.getInventory();
    }

    List<AEFluidKey> getFluidKeys() {
        return service.getCachedInventory().keySet().stream().filter(aeKey -> aeKey instanceof AEFluidKey).map(aeKey -> ((AEFluidKey) aeKey)).toList();
    }

    List<AEItemKey> getItemKeys() {
        return service.getCachedInventory().keySet().stream().filter(aeKey -> aeKey instanceof AEItemKey).map(aeKey -> ((AEItemKey) aeKey)).toList();
    }

    @Override
    public int getTanks() {
        return getFluidKeys().size();
    }

    @NotNull
    @Override
    public FluidStack getFluidInTank(int tank) {
        // same changes as 1.18.2 (Also actually fixed now) --Alina
        List<AEFluidKey> fluidKeys = getFluidKeys();
        if (tank >= fluidKeys.size())
            return FluidStack.EMPTY;

        return fluidKeys.get(tank).toStack(((int) storage.extract(fluidKeys.get(tank), Integer.MAX_VALUE, Actionable.SIMULATE, IActionSource.empty())));
    }

    @Override
    public int getTankCapacity(int tank) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return storage.insert(AEFluidKey.of(stack), stack.getAmount(), Actionable.SIMULATE, IActionSource.empty()) > 0;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return (int) storage.insert(AEFluidKey.of(resource), resource.getAmount(), action == FluidAction.EXECUTE ? Actionable.MODULATE : Actionable.SIMULATE, IActionSource.empty());
    }

    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        FluidStack copied = resource.copy();

        if (copied.getFluid() instanceof EmptyFluid)
            return FluidStack.EMPTY;

        copied.setAmount(((int) storage.extract(AEFluidKey.of(resource), resource.getAmount(), action == FluidAction.EXECUTE ? Actionable.MODULATE : Actionable.SIMULATE, IActionSource.empty())));
        return copied.getAmount() > 0 ? copied : FluidStack.EMPTY;
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack copied = getFluidInTank(0).copy();

        if (copied.getFluid() instanceof EmptyFluid)
            return FluidStack.EMPTY;

        if (copied.getAmount() > maxDrain)
            copied.setAmount(maxDrain);

        return drain(copied, action);
    }

    @Override
    public int getSlots() {
        return getItemKeys().size() + 16; // Allocate 16 slots for input
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        // same changes as 1.18.2 (Also actually fixed now) --Alina
        List<AEItemKey> itemKeys = getItemKeys();
        if (slot >= itemKeys.size())
            return ItemStack.EMPTY;

        return itemKeys.get(slot).toStack(((int) storage.extract(itemKeys.get(slot), Integer.MAX_VALUE, Actionable.SIMULATE, IActionSource.empty())));
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        ItemStack copied = stack.copy();

        copied.setCount(copied.getCount() - (int) storage.insert(AEItemKey.of(stack), stack.getCount(), simulate ? Actionable.SIMULATE : Actionable.MODULATE, IActionSource.empty()));

        return copied;
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack stackInSlot = getStackInSlot(slot).copy();

        AEItemKey key = AEItemKey.of(stackInSlot);
        if (key == null) return ItemStack.EMPTY;

        stackInSlot.setCount((int) storage.extract(key, amount, simulate ? Actionable.SIMULATE : Actionable.MODULATE, IActionSource.empty()));

        return stackInSlot;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return insertItem(slot, stack, true).getCount() == 0;
    }
}
