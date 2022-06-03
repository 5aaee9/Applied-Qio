package me.indexyz.minecraft.ae2qio;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.storage.*;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.me.storage.ITickingMonitor;
import appeng.util.item.AEItemStack;
import mekanism.common.content.qio.IQIOFrequencyHolder;
import mekanism.common.content.qio.QIOFrequency;
import mekanism.common.lib.frequency.Frequency;
import mekanism.common.lib.inventory.HashedItem;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class QioStorageAdapter<DASHBOARD extends TileEntityMekanism & IQIOFrequencyHolder> implements IMEInventory<IAEItemStack>, IBaseMonitor<IAEItemStack>, ITickingMonitor {
    private final DASHBOARD dashboard;
    @Nullable
    private final Direction queriedSide;

    public QioStorageAdapter(DASHBOARD dashboard, @Nullable Direction queriedSide) {
        this.dashboard = dashboard;
        this.queriedSide = queriedSide;
    }

    @Nullable
    public QIOFrequency getFrequency() {
        // Check dashboard facing.
        if (dashboard.getBlockState().getValue(BlockStateProperties.FACING).getOpposite() != queriedSide) {
            return null;
        }
        // Check that it has a frequency.
        var freq = dashboard.getQIOFrequency();
        if (freq == null || !freq.isValid()) {
            return null;
        }
        // Check security.
        if (!freq.isPublic()) {
            // Private or trusted: the player who placed the storage bus must have dashboard access.
            return null;
        }

        return freq;
    }

    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable type, IActionSource src) {
        var freq = this.getFrequency();

        if (freq == null) {
            return input;
        }

        var stack = input.createItemStack();

        if (type == Actionable.SIMULATE) {
            // Check insert count
            var availCount = freq.getTotalItemCountCapacity() - freq.getTotalItemCount();
            if (availCount < stack.getCount()) {
                var ret = AEItemStack.fromItemStack(stack);
                assert ret != null;
                ret.setStackSize(availCount);
                return ret;
            }

            // Check insert type limit
            if (freq.getStored(HashedItem.raw(stack)) == 0) {
                if (freq.getTotalItemCount() == freq.getTotalItemCountCapacity()) {
                    return input;
                }
            }

            return null;
        }

        return AEItemStack.fromItemStack(freq.addItem(stack));
    }

    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, IActionSource src) {
        var freq = this.getFrequency();

        if (freq == null) {
            return request;
        }

        var requestStack = request.createItemStack();
        var storedItems = freq.getStored(HashedItem.raw(requestStack));
        var outSize = Math.min(request.getStackSize(), storedItems);

        if (outSize == 0) {
            return null;
        }

        if (mode == Actionable.SIMULATE) {
            return request.setStackSize(outSize);
        }

        return AEItemStack.fromItemStack(freq.removeItem(requestStack, (int) outSize));
    }


    @Override
    public IStorageChannel<IAEItemStack> getChannel() {
        return null;
    }

    private final Map<IMEMonitorHandlerReceiver<IAEItemStack>, Object> listeners = new HashMap<>();

    @Override
    public void addListener(final IMEMonitorHandlerReceiver<IAEItemStack> l, final Object verificationToken) {
        this.listeners.put(l, verificationToken);
    }

    @Override
    public void removeListener(final IMEMonitorHandlerReceiver<IAEItemStack> l) {
        this.listeners.remove(l);
    }

    @Override
    public TickRateModulation onTick() {
        var items = this.update();
        if (items == null) {
            return TickRateModulation.SLOWER;
        }

        final Iterator<Map.Entry<IMEMonitorHandlerReceiver<IAEItemStack>, Object>> i = this.listeners.entrySet()
                .iterator();

        while (i.hasNext()) {
            final Map.Entry<IMEMonitorHandlerReceiver<IAEItemStack>, Object> l = i.next();
            final IMEMonitorHandlerReceiver<IAEItemStack> key = l.getKey();
            if (key.isValid(l.getValue())) {
                key.postChange(this, items, this.actionSource);
            } else {
                i.remove();
            }
        }

        return TickRateModulation.URGENT;
    }

    private IActionSource actionSource;
    @Override
    public void setActionSource(final IActionSource mySource) {
        this.actionSource = mySource;
    }

    private ArrayList<IAEItemStack> cachedAeStacks = new ArrayList<IAEItemStack>();

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
        this.cachedAeStacks.forEach((out::add));
        return out;
    }

    private List<IAEItemStack> update() {
        var frequency = this.getFrequency();
        if (frequency == null) {
            return null;
        }

        final var items = new ArrayList<IAEItemStack>();
        frequency.getItemDataMap().forEach(((hashedItem, qioItemTypeData) -> {
            var stack = AEItemStack.fromItemStack(hashedItem.getStack());
            stack.setStackSize(qioItemTypeData.getCount());

            items.add(stack);
        }));

        this.cachedAeStacks = items;
        return items;
    }
}