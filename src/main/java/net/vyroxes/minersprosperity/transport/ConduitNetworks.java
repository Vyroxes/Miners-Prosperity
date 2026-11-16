package net.vyroxes.minersprosperity.transport;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.*;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.items.*;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.vyroxes.minersprosperity.Tags;
import net.vyroxes.minersprosperity.config.ModConfig;
import net.vyroxes.minersprosperity.tile.TileEntityConduit;
import java.util.*;

@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class ConduitNetworks
{
    private static final int MAX_REDIRECTS = 8;
    private static final Map<World, State> WORLDS = new IdentityHashMap<>();

    private ConduitNetworks()
    {
    }

    public static void add(TileEntityConduit tile)
    {
        World world = tile.getWorld();
        if (world.isRemote) return;
        State state = WORLDS.computeIfAbsent(world, key -> new State());
        state.nodes.put(tile.getPos(), tile);
        for (ConduitPort[] ports : tile.ports) for (ConduitPort port : ports)
        {
            for (ConduitTransit transit : port.transits)
            {
                boolean ownsPosition = false;
                for (long position : transit.route) if (position == tile.getPos().toLong()) ownsPosition = true;
                if (!ownsPosition && transit.origin != null)
                {
                    transit.route = new long[]{tile.getPos().toLong()};
                    transit.durations = new int[]{1};
                    transit.index = 0;
                    transit.recovering = true;
                    transit.retryAt = world.getTotalWorldTime() + 1;
                    tile.markDirty();
                }
            }
            if (!port.transits.isEmpty()) state.inFlight.add(tile);
        }
        invalidate(world, tile.getPos());
        for (EnumFacing side : EnumFacing.VALUES) invalidate(world, tile.getPos().offset(side));
    }

    public static void remove(TileEntityConduit tile)
    {
        World world = tile.getWorld();
        State state = WORLDS.get(world);
        if (state == null) return;
        invalidate(world, tile.getPos());
        state.nodes.remove(tile.getPos());
        state.inFlight.remove(tile);
        state.dirty.remove(tile.getPos());
    }

    public static void invalidate(World world, BlockPos pos)
    {
        if (world == null || world.isRemote) return;
        State state = WORLDS.get(world);
        if (state == null) return;
        Network network = state.membership.get(pos);
        if (network != null)
        {
            state.networks.remove(network);
            for (BlockPos member : network.nodes.keySet())
            {
                state.membership.remove(member);
                state.dirty.add(member);
            }
        }
        if (state.nodes.containsKey(pos)) state.dirty.add(pos);
        if (state.building != null && (state.building.nodes.containsKey(pos) || state.queue.contains(pos)))
        {
            state.dirty.addAll(state.building.nodes.keySet());
            state.dirty.addAll(state.queue);
            state.building = null;
            state.queue.clear();
        }
    }

    @SubscribeEvent
    public static void unload(WorldEvent.Unload event)
    {
        WORLDS.remove(event.getWorld());
    }

    @SubscribeEvent
    public static void tick(TickEvent.WorldTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;
        State state = WORLDS.get(event.world);
        if (state == null) return;
        rebuild(event.world, state);
        List<Network> networks = new ArrayList<>(state.networks);
        state.animated.clear();
        state.animationBudget = Math.max(0, ModConfig.CONDUITS.animationPacketsPerTick);
        int routingBudget = Math.max(1, ModConfig.CONDUITS.routeNodesPerTick);
        int remaining = Math.max(1, ModConfig.CONDUITS.operationsPerTick);
        state.workBudget = remaining;
        deliver(event.world, state);
        int start = networks.isEmpty() ? 0 : Math.floorMod(state.networkCursor++, networks.size());
        for (int n = 0; n < networks.size() && remaining > 0 && state.workBudget > 0; n++)
        {
            Network network = networks.get((start + n) % networks.size());
            if (network.pendingRoute != null)
            {
                Route route = network.pendingRoute;
                while (routingBudget > 0 && !route.queue.isEmpty())
                {
                    BlockPos pos = route.queue.remove();
                    routingBudget--;
                    for (EnumFacing face : EnumFacing.VALUES)
                    {
                        BlockPos next = pos.offset(face);
                        if (network.nodes.containsKey(next) && !route.parents.containsKey(next))
                        {
                            route.parents.put(next, pos);
                            route.distances.put(next, route.distances.get(pos) + 1);
                            route.queue.add(next);
                        }
                    }
                }
                if (route.queue.isEmpty())
                {
                    if (network.routes.size() >= Math.max(1, ModConfig.CONDUITS.cachedRoutesPerNetwork)) network.routes.remove(network.routes.keySet().iterator().next());
                    network.routes.put(route.source, route);
                    network.pendingRoute = null;
                }
            }
            network.spent.clear();
            network.portSpent.clear();
            int count = Math.min(network.endpoints.size(), Math.min(remaining, ModConfig.CONDUITS.operationsPerNetwork));
            for (int i = 0; i < count && state.workBudget > 0; i++)
            {
                if (network.endpoints.isEmpty()) break;
                Endpoint source = network.endpoints.get(Math.floorMod(network.cursor++, network.endpoints.size()));
                for (int resource = 0; resource < 4; resource++)
                {
                    if (resource == 3 || !source.tile.conduit().type.supports(resource)) continue;
                    ConduitPort port = source.port(resource);
                    if (port.mode != 2 && port.mode != 3 || !source.tile.enabled(source.side, resource)) continue;
                    if (event.world.getTotalWorldTime() < port.nextOperation) continue;
                    port.nextOperation = event.world.getTotalWorldTime() + port.interval;
                    if (resource != 3) transfer(event.world, network, source, resource);
                }
                remaining--;
                state.workBudget--;
            }
            if (network.type.supports(3) && event.world.getTotalWorldTime() % network.redstoneInterval == 0) redstone(event.world, network);
        }
        for (TileEntityConduit tile : state.animated)
        {
            if (tile.isInvalid() || !event.world.isBlockLoaded(tile.getPos())) continue;
            tile.lastAnimation = event.world.getTotalWorldTime();
            IBlockState blockState = event.world.getBlockState(tile.getPos());
            event.world.notifyBlockUpdate(tile.getPos(), blockState, blockState, 2);
        }
        state.animated.clear();
        if (!ModConfig.CONDUITS.animations) state.itemVisuals.clear();
        Iterator<TileEntityConduit> items = state.itemVisuals.iterator();
        while (items.hasNext() && state.animationBudget > 0)
        {
            TileEntityConduit tile = items.next();
            items.remove();
            if (tile.isInvalid() || !event.world.isBlockLoaded(tile.getPos())) continue;
            IBlockState blockState = event.world.getBlockState(tile.getPos());
            event.world.notifyBlockUpdate(tile.getPos(), blockState, blockState, 2);
            tile.lastAnimation = event.world.getTotalWorldTime();
            state.animationBudget--;
        }
        if (!ModConfig.CONDUITS.animations) state.redstoneVisuals.removeIf(tile -> tile.conduit() == null || tile.conduit().type != ConduitType.UNIVERSAL);
        Iterator<TileEntityConduit> visuals = state.redstoneVisuals.iterator();
        while (visuals.hasNext() && state.animationBudget-- > 0)
        {
            TileEntityConduit tile = visuals.next();
            visuals.remove();
            if (tile.isInvalid() || !event.world.isBlockLoaded(tile.getPos())) continue;
            if (tile.conduit() != null && tile.conduit().type == ConduitType.UNIVERSAL
                    && tile.lastAnimation == event.world.getTotalWorldTime()) continue;
            tile.lastAnimation = event.world.getTotalWorldTime();
            IBlockState blockState = event.world.getBlockState(tile.getPos());
            event.world.notifyBlockUpdate(tile.getPos(), blockState, blockState, 2);
        }
    }

    private static void rebuild(World world, State state)
    {
        int budget = Math.max(1, ModConfig.CONDUITS.topologyNodesPerTick);
        while (budget-- > 0)
        {
            if (state.building == null)
            {
                if (state.dirty.isEmpty()) break;
                BlockPos seed = state.dirty.iterator().next();
                state.dirty.remove(seed);
                TileEntityConduit tile = state.nodes.get(seed);
                if (tile == null || tile.isInvalid() || state.membership.containsKey(seed) || tile.conduit() == null) continue;
                state.building = new Network(tile.conduit().type);
                state.queue.add(seed);
            }
            BlockPos pos = state.queue.poll();
            if (pos != null)
            {
                TileEntityConduit tile = state.nodes.get(pos);
                if (tile != null && !tile.isInvalid() && world.isBlockLoaded(pos) && tile.conduit() != null && tile.conduit().type == state.building.type && !state.building.nodes.containsKey(pos))
                {
                    state.building.nodes.put(pos, tile);
                    state.dirty.remove(pos);
                    for (EnumFacing side : EnumFacing.VALUES)
                    {
                        BlockPos neighbor = pos.offset(side);
                        TileEntityConduit next = state.nodes.get(neighbor);
                        if (next != null && next.conduit() != null && next.conduit().type == state.building.type)
                        {
                            if (!state.building.nodes.containsKey(neighbor)) state.queue.add(neighbor);
                        }
                        else if (world.isBlockLoaded(neighbor))
                        {
                            boolean pending = false;
                            for (ConduitPort port : tile.ports[side.getIndex()])
                                if (!port.pendingItem.isEmpty() || port.pendingFluid != null || port.pendingEnergy > 0) pending = true;
                            if (pending || tile.conduit().connects(world, pos, side)) state.building.endpoints.add(new Endpoint(tile, side));
                        }
                    }
                    state.building.redstoneInterval = 1;
                }
            }
            if (state.queue.isEmpty())
            {
                Network network = state.building;
                if (!network.nodes.isEmpty())
                {
                    state.networks.add(network);
                    for (Endpoint endpoint : network.endpoints) network.redstoneTiles.add(endpoint.tile);
                    for (TileEntityConduit tile : network.nodes.values()) for (int signal : tile.signals) if (signal != 0) network.redstoneTiles.add(tile);
                    for (BlockPos member : network.nodes.keySet()) state.membership.put(member, network);
                }
                state.building = null;
            }
        }
    }

    private static void transfer(World world, Network network, Endpoint source, int resource)
    {
        ConduitPort port = source.port(resource);
        TileEntity sourceTile = source.target(world);
        if ((resource == 1 || resource == 2) && port.transits.size() >= 128) return;
        if (sourceTile == null && port.pendingItem.isEmpty() && port.pendingFluid == null && port.pendingEnergy == 0) return;
        if (resource == 0 && port.pendingEnergy == 0)
        {
            IEnergyStorage energy = sourceTile == null ? null : sourceTile.getCapability(CapabilityEnergy.ENERGY, source.side.getOpposite());
            if (energy == null || !energy.canExtract() || energy.extractEnergy(1, true) == 0) return;
        }
        if (resource == 1 && port.pendingItem.isEmpty())
        {
            IItemHandler items = sourceTile == null ? null : sourceTile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, source.side.getOpposite());
            items = port.sourceInventory(items);
            if (items == null || items.getSlots() == 0) return;
            boolean available = false;
            for (int slot = 0; slot < Math.min(items.getSlots(), ModConfig.CONDUITS.maxInventorySlots); slot++)
            {
                if (!items.getStackInSlot(slot).isEmpty()) { available = true; break; }
            }
            if (!available && items.getSlots() <= ModConfig.CONDUITS.maxInventorySlots) return;
        }
        if (resource == 2 && port.pendingFluid == null)
        {
            IFluidHandler fluid = sourceTile == null ? null : sourceTile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, source.side.getOpposite());
            if (fluid == null || fluid.drain(1, false) == null) return;
        }
        Route route = network.routes.get(source.tile.getPos());
        if (route == null)
        {
            if (network.pendingRoute == null) network.pendingRoute = new Route(source.tile.getPos());
            port.nextOperation = Math.min(port.nextOperation, world.getTotalWorldTime() + 1);
            return;
        }
        Map<BlockPos, BlockPos> tree = route.parents;
        List<Endpoint> targets = new ArrayList<>();
        for (Endpoint target : network.endpoints)
        {
            ConduitPort other = target.port(resource);
            if (target == source || target.tile.getPos().offset(target.side).equals(source.tile.getPos().offset(source.side))) continue;
            if ((other.mode == 1 || other.mode == 3) && other.channel == port.channel && target.tile.enabled(target.side, resource)) targets.add(target);
        }
        Comparator<Endpoint> distance = Comparator.comparingInt(target -> route.distances.getOrDefault(target.tile.getPos(), Integer.MAX_VALUE));
        if (port.distribution == 0) targets.sort(distance);
        else if (port.distribution == 1) targets.sort(distance.reversed());
        else if (port.distribution == 2) targets.sort(Comparator.<Endpoint>comparingInt(target -> target.port(resource).priority).reversed().thenComparing(distance));
        else if (port.distribution == 5) targets.sort(Comparator.<Endpoint>comparingInt(target -> target.port(resource).priority).thenComparing(distance));
        else if (port.distribution == 3) Collections.shuffle(targets, world.rand);
        else if (!targets.isEmpty()) Collections.rotate(targets, -Math.floorMod(port.destination, targets.size()));
        int attempts = 0;
        if (!targets.isEmpty() && port.scanOffset > 0) Collections.rotate(targets, -Math.floorMod(port.scanOffset, targets.size()));
        for (Endpoint target : targets)
        {
            State scheduler = WORLDS.get(world);
            if (++attempts > ModConfig.CONDUITS.destinationsPerOperation || scheduler == null || scheduler.workBudget-- <= 0) break;
            TileEntity targetTile = target.target(world);
            if (targetTile == null) continue;
            if (resource == 0)
            {
                IEnergyStorage receiver = targetTile.getCapability(CapabilityEnergy.ENERGY, target.side.getOpposite());
                if (receiver == null || !receiver.canReceive() || receiver.receiveEnergy(1, true) == 0) continue;
            }
            List<TileEntityConduit> path = new ArrayList<>();
            BlockPos step = target.tile.getPos();
            while (step != null)
            {
                TileEntityConduit node = network.nodes.get(step);
                if (node == null || node.isInvalid() || !world.isBlockLoaded(step)) { path.clear(); break; }
                path.add(node);
                if (step.equals(source.tile.getPos())) break;
                step = tree.get(step);
            }
            if (path.isEmpty()) continue;
            int available = port.limit == 0 ? Integer.MAX_VALUE : port.limit;
            ConduitPort destination = target.port(resource);
            if (destination.limit > 0) available = Math.min(available, destination.limit - network.portSpent.getOrDefault(destination, 0));
            for (TileEntityConduit node : path) available = Math.min(available, node.conduit().tier.rate(resource) - network.spent.computeIfAbsent(node, key -> new int[3])[resource]);
            if (available <= 0) continue;
            int moved = resource == 0 ? energy(source, sourceTile, target, targetTile, available)
                    : resource == 1 ? items(source, sourceTile, target, targetTile, Math.min(available, port.batch), world)
                    : fluids(source, sourceTile, target, targetTile, available);
            if (moved == 0) continue;
            source.tile.markDirty();
            target.tile.markDirty();
            for (TileEntityConduit node : path) network.spent.get(node)[resource] += moved;
            network.portSpent.merge(destination, moved, Integer::sum);
            port.destination++;
            port.scanOffset = 0;
            if (resource == 1 || resource == 2) dispatch(world, scheduler, path, source, target, resource, moved);
            else animate(world, path, source, target, resource);
            return;
        }
        port.scanOffset += Math.min(attempts, ModConfig.CONDUITS.destinationsPerOperation);
    }

    private static int energy(Endpoint source, TileEntity from, Endpoint target, TileEntity to, int limit)
    {
        IEnergyStorage input = from == null ? null : from.getCapability(CapabilityEnergy.ENERGY, source.side.getOpposite());
        IEnergyStorage output = to.getCapability(CapabilityEnergy.ENERGY, target.side.getOpposite());
        if (output == null || !output.canReceive()) return 0;
        ConduitPort port = source.port(0);
        if (port.pendingEnergy == 0)
        {
            if (input == null || !input.canExtract()) return 0;
            int amount = output.receiveEnergy(Math.min(limit, input.extractEnergy(limit, true)), true);
            if (amount <= 0) return 0;
            port.pendingEnergy = input.extractEnergy(amount, false);
            source.tile.markDirty();
        }
        int received = output.receiveEnergy(Math.min(limit, port.pendingEnergy), false);
        port.pendingEnergy -= received;
        return received;
    }

    private static int items(Endpoint source, TileEntity from, Endpoint target, TileEntity to, int limit, World world)
    {
        IItemHandler input = from == null ? null : from.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, source.side.getOpposite());
        IItemHandler output = to.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, target.side.getOpposite());
        if (output == null || output.getSlots() > ModConfig.CONDUITS.maxInventorySlots) return 0;
        ConduitPort port = source.port(1);
        ConduitPort destination = target.port(1);
        input = port.sourceInventory(input);
        IItemHandler selectedOutput = destination.destinationInventory(output);
        int slots = input == null ? 0 : input.getSlots();
        for (int i = 0; i < Math.min(Math.max(1, slots), ModConfig.CONDUITS.maxInventorySlots); i++)
        {
            int slot = slots == 0 ? 0 : port.extraction == 1 ? slots - 1 - i : port.extraction == 2 ? world.rand.nextInt(slots)
                    : port.extraction == 3 ? Math.floorMod(port.cursor + i, slots) : i;
            ItemStack offered = !port.pendingItem.isEmpty() ? port.pendingItem.copy() : slots == 0 ? ItemStack.EMPTY : input.extractItem(slot, limit, true);
            if (offered.isEmpty() || !port.matches(offered) || !destination.matches(offered)) continue;
            offered = offered.copy();
            int amount = Math.min(limit, offered.getCount());
            if (destination.keep > 0)
            {
                long stored = 0;
                for (int j = 0; j < selectedOutput.getSlots(); j++)
                {
                    ItemStack existing = selectedOutput.getStackInSlot(j);
                    if (ItemHandlerHelper.canItemStacksStack(existing, offered)) stored += existing.getCount();
                }
                amount = (int) Math.min(amount, Math.max(0, destination.keep - stored));
            }
            if (amount == 0) continue;
            offered.setCount(amount);
            ItemStack simulated = insertItems(output, offered, destination, true);
            amount -= simulated.getCount();
            if (amount <= 0) continue;
            if (port.pendingItem.isEmpty()) port.pendingItem = input.extractItem(slot, amount, false);
            source.tile.markDirty();
            ItemStack actual = port.pendingItem.copy();
            actual.setCount(Math.min(amount, actual.getCount()));
            int moved = actual.getCount();
            if (moved > 0)
            {
                source.tile.animationItem = actual.copy();
                source.tile.animationItem.setCount(1);

                port.cursor = slots == 0 ? 0 : (slot + 1) % slots;
                return moved;
            }
            if (!port.pendingItem.isEmpty()) return 0;
        }
        return 0;
    }

    static ItemStack insertItems(IItemHandler output, ItemStack stack, ConduitPort destination, boolean simulate)
    {
        output = destination.destinationInventory(output);
        if (destination.insertion == 0) return ItemHandlerHelper.insertItemStacked(output, stack, simulate);
        if (destination.insertion == 4) return insertEvenly(output, stack, simulate);
        int slots = output.getSlots();
        ItemStack remainder = stack;
        int start = destination.insertionCursor;
        for (int i = 0; i < slots && !remainder.isEmpty(); i++)
        {
            int slot = switch (destination.insertion)
            {
                case 2 -> slots - 1 - i;
                case 3 -> (int) (((long) start + i) % slots);
                default -> i;
            };
            int before = remainder.getCount();
            remainder = output.insertItem(slot, remainder, simulate);
            if (!simulate && destination.insertion == 3 && remainder.getCount() < before)
                destination.insertionCursor = (slot + 1) % slots;
        }
        return remainder;
    }

    private static ItemStack insertEvenly(IItemHandler output, ItemStack stack, boolean simulate)
    {
        if (stack.isEmpty()) return stack;
        int slots = output.getSlots();
        int[] counts = new int[slots];
        int[] capacity = new int[slots];
        long lowest = Long.MAX_VALUE;
        for (int slot = 0; slot < slots; slot++)
        {
            if (!output.isItemValid(slot, stack)) continue;
            ItemStack existing = output.getStackInSlot(slot);
            if (!existing.isEmpty() && !ItemHandlerHelper.canItemStacksStack(existing, stack)) continue;
            capacity[slot] = stack.getCount() - output.insertItem(slot, stack, true).getCount();
            if (capacity[slot] <= 0) continue;
            counts[slot] = existing.getCount();
            lowest = Math.min(lowest, counts[slot]);
        }
        if (lowest == Long.MAX_VALUE) return stack;
        long upper = lowest + stack.getCount() + 1L;
        while (lowest + 1 < upper)
        {
            long level = (lowest + upper) / 2;
            long needed = 0;
            for (int slot = 0; slot < slots; slot++)
                needed += Math.min(capacity[slot], Math.max(0L, level - counts[slot]));
            if (needed <= stack.getCount()) lowest = level;
            else upper = level;
        }
        int extra = stack.getCount();
        for (int slot = 0; slot < slots; slot++)
            extra -= (int) Math.min(capacity[slot], Math.max(0L, lowest - counts[slot]));
        int remaining = stack.getCount();
        for (int slot = 0; slot < slots && remaining > 0; slot++)
        {
            int amount = (int) Math.min(capacity[slot], Math.max(0L, lowest - counts[slot]));
            if (extra > 0 && amount < capacity[slot] && (long) counts[slot] + amount <= lowest)
            {
                amount++;
                extra--;
            }
            if (amount <= 0) continue;
            ItemStack offered = stack.copy();
            offered.setCount(Math.min(amount, remaining));
            remaining -= offered.getCount() - output.insertItem(slot, offered, simulate).getCount();
        }
        if (remaining == 0) return ItemStack.EMPTY;
        ItemStack remainder = stack.copy();
        remainder.setCount(remaining);
        return remainder;
    }

    private static int fluids(Endpoint source, TileEntity from, Endpoint target, TileEntity to, int limit)
    {
        IFluidHandler input = from == null ? null : from.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, source.side.getOpposite());
        IFluidHandler output = to.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, target.side.getOpposite());
        if (output == null || input == null && source.port(2).pendingFluid == null) return 0;
        ConduitPort port = source.port(2);
        FluidStack offered = port.pendingFluid == null ? input.drain(limit, false) : port.pendingFluid.copy();
        if (!port.matches(offered) || !target.port(2).matches(offered)) return 0;
        offered = offered.copy();
        offered.amount = Math.min(limit, offered.amount);
        int accepted = output.fill(offered, false);
        if (accepted <= 0) return 0;
        if (port.pendingFluid == null)
        {
            offered.amount = accepted;
            port.pendingFluid = input.drain(offered, true);
            source.tile.markDirty();
        }
        if (port.pendingFluid == null) return 0;
        FluidStack actual = port.pendingFluid.copy();
        actual.amount = Math.min(accepted, actual.amount);
        int moved = actual.amount;
        source.tile.animationFluid = actual.getFluid().getName();

        return moved;
    }

    private static void redstone(World world, Network network)
    {
        Map<Integer, Integer> channels = new HashMap<>();
        for (Endpoint endpoint : network.endpoints)
        {
            ConduitPort port = endpoint.port(3);
            if ((port.mode != 2 && port.mode != 3) || !endpoint.tile.enabled(endpoint.side, 3))
            {
                network.sampledSignals.remove(port);
                continue;
            }
            BlockPos target = endpoint.tile.getPos().offset(endpoint.side);
            if (!world.isBlockLoaded(target)) continue;
            if (world.getTotalWorldTime() >= port.nextOperation)
            {
                network.sampledSignals.put(port, world.getRedstonePower(target, endpoint.side));
                port.nextOperation = world.getTotalWorldTime() + port.interval;
            }
            channels.merge(port.channel, network.sampledSignals.getOrDefault(port, 0), Math::max);
        }
        State scheduler = WORLDS.get(world);
        boolean active = channels.values().stream().anyMatch(value -> value > 0);
        if (network.activeState == null || network.activeState != active)
        {
            network.activeState = active;
            for (TileEntityConduit tile : network.nodes.values())
            {
                if (tile.redstoneActive == active) continue;
                tile.redstoneActive = active;
                if (scheduler != null && (ModConfig.CONDUITS.animations || tile.conduit().type == ConduitType.UNIVERSAL)) scheduler.redstoneVisuals.add(tile);
            }
        }
        for (TileEntityConduit tile : network.redstoneTiles)
        {
            boolean changed = false;
            for (EnumFacing side : EnumFacing.VALUES)
            {
                ConduitPort port = tile.ports[side.getIndex()][3];
                int value = (port.mode == 1 || port.mode == 3) && tile.enabled(side, 3) ? channels.getOrDefault(port.channel, 0) : 0;
                if (tile.signals[side.getIndex()] != value)
                {
                    tile.signals[side.getIndex()] = value;
                    changed = true;
                }
            }
            if (changed)
            {
                IBlockState state = world.getBlockState(tile.getPos());
                world.notifyNeighborsOfStateChange(tile.getPos(), state.getBlock(), false);
                if (scheduler != null && (ModConfig.CONDUITS.animations || tile.conduit().type == ConduitType.UNIVERSAL)) scheduler.redstoneVisuals.add(tile);
            }
        }
    }

    private static void dispatch(World world, State state, List<TileEntityConduit> path, Endpoint source, Endpoint target, int resource, int moved)
    {
        ConduitPort port = source.port(resource);
        ConduitTransit transit = new ConduitTransit(target.tile.getPos(), target.side, new long[0], 0);
        transit.origin = source.tile.getPos();
        transit.originSide = source.side;
        transit.channel = port.channel;
        transit.entrySide = source.side;
        if (resource == 1) transit.item = port.pendingItem.splitStack(moved);
        else
        {
            transit.fluid = port.pendingFluid.copy();
            transit.fluid.amount = moved;
            port.pendingFluid.amount -= moved;
            if (port.pendingFluid.amount <= 0) port.pendingFluid = null;
        }
        schedule(world, state, path, target, transit, resource);
        port.transits.addLast(transit);
        state.inFlight.add(source.tile);
        source.tile.markDirty();
    }

    private static void schedule(World world, State state, List<TileEntityConduit> path, Endpoint target, ConduitTransit transit, int resource)
    {
        transit.target = target.tile.getPos();
        transit.side = target.side;
        transit.started = world.getTotalWorldTime() + 2;
        transit.route = new long[path.size()];
        transit.durations = new int[path.size()];
        transit.index = path.size() - 1;
        transit.recovering = false;
        long arrival = transit.started;
        int budget = ModConfig.CONDUITS.animationNodesPerTransfer;
        if (budget < 0) budget = path.size();
        for (int i = path.size() - 1; i >= 0; i--)
        {
            TileEntityConduit tile = path.get(i);
            int duration = tile.conduit().tier.travelTicks();
            transit.route[i] = tile.getPos().toLong();
            transit.durations[i] = duration;
            if (ModConfig.CONDUITS.animations && budget-- > 0)
            {
                BlockPos from = i + 1 < path.size() ? path.get(i + 1).getPos() : tile.getPos().offset(transit.entrySide);
                BlockPos to = i > 0 ? path.get(i - 1).getPos() : tile.getPos().offset(target.side);
                int fromSide = direction(tile.getPos(), from).getIndex();
                int toSide = direction(tile.getPos(), to).getIndex();
                if (resource == 1) tile.recordItemTransit(transit.item, arrival, duration, fromSide, toSide, transit.visualId);
                else tile.recordFluidTransit(transit.fluid, arrival, duration, fromSide, toSide, transit.visualId);
                state.itemVisuals.add(tile);
            }
            arrival += duration;
        }
        transit.arrival = arrival;
        transit.nextStepAt = transit.started + transit.durations[transit.index];
        transit.retryAt = transit.nextStepAt;
        transit.exitSide = transit.index > 0 ? direction(path.get(transit.index).getPos(), path.get(transit.index - 1).getPos()) : target.side;
    }

    private static void deliver(World world, State state)
    {
        state.arrivalSpent.clear();
        long now = world.getTotalWorldTime();
        int deliveryBudget = Math.max(1, state.workBudget / 2);
        Iterator<TileEntityConduit> sources = state.inFlight.iterator();
        while (sources.hasNext() && state.workBudget > 0 && deliveryBudget > 0)
        {
            TileEntityConduit source = sources.next();
            if (source.isInvalid() || !world.isBlockLoaded(source.getPos()))
            {
                sources.remove();
                continue;
            }
            boolean pending = false;
            for (EnumFacing side : EnumFacing.VALUES) for (int resource = 1; resource <= 2; resource++)
            {
                ConduitPort port = source.ports[side.getIndex()][resource];
                Iterator<ConduitTransit> transits = port.transits.iterator();
                while (transits.hasNext() && state.workBudget > 0 && deliveryBudget > 0)
                {
                    ConduitTransit transit = transits.next();
                    initializeTransit(world, source, side, port, transit);
                    if (now < transit.retryAt) continue;
                    state.workBudget--;
                    deliveryBudget--;
                    transit.retryAt = now + 20;
                    if (!advance(world, state, transit, resource))
                    {
                        source.markDirty();
                        if (transit.item.isEmpty() && transit.fluid == null) transits.remove();
                        continue;
                    }
                    TileEntityConduit destination = (TileEntityConduit) world.getTileEntity(transit.target);
                    ConduitPort settings = destination.ports[transit.side.getIndex()][resource];
                    TileEntity target = world.getTileEntity(transit.target.offset(transit.side));
                    int limit = destination.conduit().tier.rate(resource);
                    if (settings.limit > 0) limit = Math.min(limit, settings.limit);
                    limit -= state.arrivalSpent.getOrDefault(settings, 0);
                    if (limit <= 0)
                    {
                        transit.retryAt = now + 1;
                        continue;
                    }
                    int moved = 0;
                    if (resource == 1 && (transit.returning || settings.matches(transit.item)))
                    {
                        IItemHandler output = target.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, transit.side.getOpposite());
                        if (output == null || output.getSlots() > ModConfig.CONDUITS.maxInventorySlots) continue;
                        ItemStack offered = transit.item.copy();
                        offered.setCount(Math.min(limit, offered.getCount()));
                        if (!transit.returning && settings.keep > 0)
                        {
                            long stored = 0;
                            IItemHandler selected = settings.destinationInventory(output);
                            for (int slot = 0; slot < selected.getSlots(); slot++)
                            {
                                ItemStack existing = selected.getStackInSlot(slot);
                                if (ItemHandlerHelper.canItemStacksStack(existing, offered)) stored += existing.getCount();
                            }
                            offered.setCount((int) Math.min(offered.getCount(), Math.max(0, settings.keep - stored)));
                        }
                        moved = offered.getCount() - insertItems(output, offered, settings, false).getCount();
                        transit.item.shrink(moved);
                    }
                    else if (resource == 2 && (transit.returning || settings.matches(transit.fluid)))
                    {
                        IFluidHandler output = target.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, transit.side.getOpposite());
                        if (output == null || transit.fluid == null) continue;
                        FluidStack offered = transit.fluid.copy();
                        offered.amount = Math.min(limit, offered.amount);
                        moved = output.fill(offered, true);
                        transit.fluid.amount -= moved;
                        if (transit.fluid.amount <= 0) transit.fluid = null;
                    }
                    if (moved > 0)
                    {
                        source.markDirty();
                        destination.markDirty();
                        state.arrivalSpent.merge(settings, moved, Integer::sum);
                        transit.retryAt = now + 1;
                    }
                    if (transit.item.isEmpty() && transit.fluid == null) transits.remove();
                }
                if (!port.transits.isEmpty()) pending = true;
            }
            if (!pending) sources.remove();
        }
    }

    private static EnumFacing direction(BlockPos from, BlockPos to)
    {
        return EnumFacing.getFacingFromVector(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
    }

    private static void initializeTransit(World world, TileEntityConduit owner, EnumFacing side, ConduitPort port, ConduitTransit transit)
    {
        if (transit.origin != null) return;
        transit.origin = transit.route.length == 0 ? owner.getPos() : BlockPos.fromLong(transit.route[transit.route.length - 1]);
        transit.originSide = side;
        transit.entrySide = side;
        transit.exitSide = transit.side;
        transit.channel = port.channel;
        transit.durations = new int[transit.route.length];
        long duration = 0;
        for (int i = 0; i < transit.route.length; i++)
        {
            BlockPos pos = BlockPos.fromLong(transit.route[i]);
            TileEntity tile = world.isBlockLoaded(pos) ? world.getTileEntity(pos) : null;
            transit.durations[i] = tile instanceof TileEntityConduit conduit && conduit.conduit() != null ? conduit.conduit().tier.travelTicks() : 1;
            duration += transit.durations[i];
        }
        transit.started = transit.arrival - duration;
        transit.index = Math.max(0, transit.route.length - 1);
        transit.nextStepAt = transit.started + (transit.route.length == 0 ? 0 : transit.durations[transit.index]);
        transit.retryAt = transit.nextStepAt;
    }

    private static boolean advance(World world, State state, ConduitTransit transit, int resource)
    {
        long now = world.getTotalWorldTime();
        if (transit.route.length == 0)
        {
            transit.recovering = true;
            return false;
        }
        BlockPos current = BlockPos.fromLong(transit.route[transit.index]);
        if (!world.isBlockLoaded(current)) return false;
        if (!(world.getTileEntity(current) instanceof TileEntityConduit))
        {
            cancelVisuals(world, state, transit);
            TileEntityConduit carrier = carrier(world, transit, null);
            if (carrier == null)
            {
                dropItem(world, current, transit);
                return false;
            }
            transit.route = new long[]{carrier.getPos().toLong()};
            transit.durations = new int[]{1};
            transit.index = 0;
            current = carrier.getPos();
            transit.recovering = true;
        }
        if (transit.recovering)
        {
            recover(world, state, transit, resource, current);
            return false;
        }
        while (now >= transit.nextStepAt && transit.index > 0)
        {
            BlockPos next = BlockPos.fromLong(transit.route[transit.index - 1]);
            if (!world.isBlockLoaded(next)) return false;
            TileEntity nextTile = world.getTileEntity(next);
            TileEntityConduit currentTile = (TileEntityConduit) world.getTileEntity(current);
            EnumFacing exit = direction(current, next);
            transit.exitSide = exit;
            if (!(nextTile instanceof TileEntityConduit conduit) || conduit.conduit() == null
                    || Objects.requireNonNull(currentTile).conduit() == null || currentTile.conduit().type != conduit.conduit().type
                    || !conduit.conduit().type.supports(resource)
                    || !currentTile.conduit().connects(world, current, exit) || !conduit.conduit().connects(world, next, exit.getOpposite()))
            {
                cancelVisuals(world, state, transit);
                transit.recovering = true;
                recover(world, state, transit, resource, current);
                return false;
            }
            transit.entrySide = exit.getOpposite();
            transit.index--;
            transit.nextStepAt += transit.durations[transit.index];
            current = next;
        }
        if (now < transit.nextStepAt)
        {
            transit.retryAt = transit.nextStepAt;
            return false;
        }
        transit.exitSide = transit.side;
        if (!world.isBlockLoaded(transit.target.offset(transit.side))) return false;
        if (receiver(world, new Endpoint((TileEntityConduit) world.getTileEntity(current), transit.side), transit, resource, transit.returning, false)) return true;
        cancelVisuals(world, state, transit);
        transit.recovering = true;
        recover(world, state, transit, resource, current);
        return false;
    }

    private static boolean receiver(World world, Endpoint endpoint, ConduitTransit transit, int resource, boolean returning, boolean requireSpace)
    {
        ConduitPort port = endpoint.port(resource);
        if (returning ? port.mode != 3 : port.mode != 1 && port.mode != 3) return false;
        if (!returning && port.channel != transit.channel || !endpoint.tile.enabled(endpoint.side, resource)) return false;
        TileEntity target = endpoint.target(world);
        if (target == null) return false;
        if (resource == 1)
        {
            if (!returning && !port.matches(transit.item)) return false;
            IItemHandler output = target.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, endpoint.side.getOpposite());
            return output != null && output.getSlots() <= ModConfig.CONDUITS.maxInventorySlots
                    && (!requireSpace || insertItems(output, transit.item, port, true).getCount() < transit.item.getCount());
        }
        if (!returning && !port.matches(transit.fluid)) return false;
        IFluidHandler output = target.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, endpoint.side.getOpposite());
        return output != null && transit.fluid != null && (!requireSpace || output.fill(transit.fluid, false) > 0);
    }

    private static void recover(World world, State state, ConduitTransit transit, int resource, BlockPos current)
    {
        if (!world.isBlockLoaded(transit.target.offset(transit.side))) return;
        Network network = state.membership.get(current);
        if (network == null) return;
        Route route = network.routes.get(current);
        if (route == null)
        {
            if (network.pendingRoute == null) network.pendingRoute = new Route(current);
            transit.retryAt = world.getTotalWorldTime() + 1;
            return;
        }
        Endpoint chosen = null;
        int distance = Integer.MAX_VALUE;
        if (!transit.returning && transit.redirects < MAX_REDIRECTS && route.parents.containsKey(transit.target))
        {
            TileEntity tile = world.getTileEntity(transit.target);
            if (tile instanceof TileEntityConduit destination)
            {
                Endpoint endpoint = new Endpoint(destination, transit.side);
                if (receiver(world, endpoint, transit, resource, false, false)) chosen = endpoint;
            }
        }
        if (chosen == null && !transit.returning && transit.redirects < MAX_REDIRECTS)
        {
            for (Endpoint endpoint : network.endpoints)
            {
                if (endpoint.tile.getPos().equals(transit.origin) && endpoint.side == transit.originSide) continue;
                int candidateDistance = route.distances.getOrDefault(endpoint.tile.getPos(), Integer.MAX_VALUE);
                if (candidateDistance >= distance || !receiver(world, endpoint, transit, resource, false, true)) continue;
                chosen = endpoint;
                distance = candidateDistance;
            }
        }
        if (chosen == null && route.parents.containsKey(transit.origin) && world.isBlockLoaded(transit.origin))
        {
            TileEntity tile = world.getTileEntity(transit.origin);
            if (tile instanceof TileEntityConduit origin)
            {
                Endpoint endpoint = new Endpoint(origin, transit.originSide);
                if (receiver(world, endpoint, transit, resource, true, false))
                {
                    chosen = endpoint;
                    transit.returning = true;
                }
            }
        }
        if (chosen == null)
        {
            for (BlockPos position : network.nodes.keySet()) if (!world.isBlockLoaded(position)) return;
            dropItem(world, current, transit);
            return;
        }
        List<TileEntityConduit> path = new ArrayList<>();
        BlockPos step = chosen.tile.getPos();
        while (step != null)
        {
            TileEntityConduit tile = network.nodes.get(step);
            if (tile == null) return;
            path.add(tile);
            if (step.equals(current)) break;
            BlockPos parent = route.parents.get(step);
            if (step.equals(parent)) return;
            step = parent;
        }
        if (path.isEmpty() || !path.get(path.size() - 1).getPos().equals(current)) return;
        if (!transit.returning) transit.redirects++;
        schedule(world, state, path, chosen, transit, resource);
    }

    private static void cancelVisuals(World world, State state, ConduitTransit transit)
    {
        for (long position : transit.route)
        {
            BlockPos pos = BlockPos.fromLong(position);
            if (!world.isBlockLoaded(pos) || !(world.getTileEntity(pos) instanceof TileEntityConduit tile)) continue;
            boolean changed = tile.itemTransits.removeIf(event -> event.getLong("TransitId") == transit.visualId);
            changed |= tile.fluidTransits.removeIf(event -> event.getLong("TransitId") == transit.visualId);
            if (changed) state.itemVisuals.add(tile);
        }
    }

    private static void dropItem(World world, BlockPos current, ConduitTransit transit)
    {
        if (transit.item.isEmpty()) return;
        EnumFacing exit = transit.exitSide == null ? transit.side : transit.exitSide;
        EntityItem item = new EntityItem(world,
                current.getX() + 0.5 + exit.getXOffset() * 0.6, current.getY() + 0.5 + exit.getYOffset() * 0.6,
                current.getZ() + 0.5 + exit.getZOffset() * 0.6, transit.item.copy());
        item.setDefaultPickupDelay();
        if (world.spawnEntity(item)) transit.item = ItemStack.EMPTY;
    }

    public static TileEntityConduit carrier(World world, ConduitTransit transit, TileEntityConduit excluded)
    {
        if (transit.route.length == 0) return null;
        int index = Math.min(transit.index, transit.route.length - 1);
        long next = transit.nextStepAt;
        if (!transit.recovering && transit.durations.length == transit.route.length)
            while (index > 0 && world.getTotalWorldTime() >= next) next += transit.durations[--index];
        for (int distance = 0; distance < transit.route.length; distance++) for (int direction = 1; direction >= -1; direction -= 2)
        {
            int candidate = index + distance * direction;
            if (candidate < 0 || candidate >= transit.route.length) continue;
            BlockPos pos = BlockPos.fromLong(transit.route[candidate]);
            if (world.isBlockLoaded(pos) && world.getTileEntity(pos) instanceof TileEntityConduit tile && tile != excluded) return tile;
        }
        return null;
    }

    public static void prepareDrop(NBTTagCompound data, TileEntityConduit owner)
    {
        World world = owner.getWorld();
        if (world.isRemote) return;
        NBTTagList ports = data.getTagList("Ports", 10);
        for (EnumFacing side : EnumFacing.VALUES) for (int resource = 1; resource <= 2; resource++)
        {
            NBTTagList retained = new NBTTagList();
            for (ConduitTransit transit : owner.ports[side.getIndex()][resource].transits)
            {
                if (transit.fluid != null && carrier(world, transit, owner) == null) retained.appendTag(transit.write());
            }
            ports.getCompoundTagAt(side.getIndex() * 4 + resource).setTag("Transits", retained);
        }
    }

    public static void broken(TileEntityConduit owner)
    {
        World world = owner.getWorld();
        if (world.isRemote) return;
        State state = WORLDS.computeIfAbsent(world, key -> new State());
        for (EnumFacing side : EnumFacing.VALUES) for (int resource = 1; resource <= 2; resource++)
        {
            ConduitPort port = owner.ports[side.getIndex()][resource];
            Iterator<ConduitTransit> transits = port.transits.iterator();
            while (transits.hasNext())
            {
                ConduitTransit transit = transits.next();
                initializeTransit(world, owner, side, port, transit);
                TileEntityConduit carrier = carrier(world, transit, owner);
                if (carrier == null)
                {
                    dropItem(world, owner.getPos(), transit);
                    continue;
                }
                cancelVisuals(world, state, transit);
                transit.route = new long[]{carrier.getPos().toLong()};
                transit.durations = new int[]{1};
                transit.index = 0;
                transit.recovering = true;
                transit.retryAt = world.getTotalWorldTime() + 1;
                carrier.ports[0][resource].transits.addLast(transit);
                carrier.markDirty();
                state.inFlight.add(carrier);
                transits.remove();
            }
        }
        state.inFlight.remove(owner);
    }

    private static void animate(World world, List<TileEntityConduit> path, Endpoint source, Endpoint target, int resource)
    {
        State state = WORLDS.get(world);
        if (state == null) return;
        long activityUntil = world.getTotalWorldTime() + 20;
        for (TileEntityConduit tile : path)
        {
            if (tile.conduit().type != ConduitType.UNIVERSAL) continue;
            tile.resourceUntil[resource] = activityUntil;
            if (world.getTotalWorldTime() - tile.lastAnimation >= 10) state.redstoneVisuals.add(tile);
        }
        if (!ModConfig.CONDUITS.animations) return;
        int budget = ModConfig.CONDUITS.animationNodesPerTransfer;
        if (budget < 0) budget = path.size();
        for (int i = 0; i < path.size() && budget > 0; i++)
        {
            TileEntityConduit tile = path.get(i);
            if (world.getTotalWorldTime() - tile.lastAnimation < 10) continue;
            if (!state.animated.contains(tile) && state.animationBudget <= 0) break;
            tile.activeUntil = world.getTotalWorldTime() + 20;
            tile.resourceUntil[resource] = tile.activeUntil;
            if (resource == 1) tile.animationItem = source.tile.animationItem.copy();
            if (resource == 2) tile.animationFluid = source.tile.animationFluid;
            BlockPos from = i + 1 < path.size() ? path.get(i + 1).getPos() : tile.getPos().offset(source.side);
            BlockPos to = i > 0 ? path.get(i - 1).getPos() : tile.getPos().offset(target.side);
            tile.animationFrom = EnumFacing.getFacingFromVector(from.getX() - tile.getPos().getX(), from.getY() - tile.getPos().getY(), from.getZ() - tile.getPos().getZ()).getIndex();
            tile.animationTo = EnumFacing.getFacingFromVector(to.getX() - tile.getPos().getX(), to.getY() - tile.getPos().getY(), to.getZ() - tile.getPos().getZ()).getIndex();
            tile.resourceFrom[resource] = tile.animationFrom;
            tile.resourceTo[resource] = tile.animationTo;
            if (state.animated.add(tile)) state.animationBudget--;
            budget--;
        }
    }

    private static final class State
    {
        final Map<BlockPos, TileEntityConduit> nodes = new HashMap<>();
        final Map<BlockPos, Network> membership = new HashMap<>();
        final Set<Network> networks = new LinkedHashSet<>();
        final Set<BlockPos> dirty = new LinkedHashSet<>();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Network building;
        int networkCursor;
        final Set<TileEntityConduit> itemVisuals = new LinkedHashSet<>();
        final Set<TileEntityConduit> inFlight = new LinkedHashSet<>();
        final Map<ConduitPort, Integer> arrivalSpent = new IdentityHashMap<>();
        final Set<TileEntityConduit> animated = new LinkedHashSet<>();
        final Set<TileEntityConduit> redstoneVisuals = new LinkedHashSet<>();
        int animationBudget;
        int workBudget;
    }

    private static final class Network
    {
        final ConduitType type;
        final Map<BlockPos, TileEntityConduit> nodes = new LinkedHashMap<>();
        final List<Endpoint> endpoints = new ArrayList<>();
        final Map<BlockPos, Route> routes = new LinkedHashMap<>();
        final Map<ConduitPort, Integer> sampledSignals = new IdentityHashMap<>();

        Route pendingRoute;
        final Set<TileEntityConduit> redstoneTiles = new LinkedHashSet<>();
        Boolean activeState;
        final Map<TileEntityConduit, int[]> spent = new IdentityHashMap<>();
        final Map<ConduitPort, Integer> portSpent = new IdentityHashMap<>();
        int cursor;
        int redstoneInterval = 1;

        Network(ConduitType type) { this.type = type; }
    }

    private static final class Route
    {
        final BlockPos source;
        final Map<BlockPos, BlockPos> parents = new HashMap<>();
        final Map<BlockPos, Integer> distances = new HashMap<>();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        Route(BlockPos source)
        {
            this.source = source;
            parents.put(source, source);
            distances.put(source, 0);
            queue.add(source);
        }
    }

    private static final class Endpoint
    {
        final TileEntityConduit tile;
        final EnumFacing side;

        Endpoint(TileEntityConduit tile, EnumFacing side) { this.tile = tile; this.side = side; }
        ConduitPort port(int resource) { return tile.ports[side.getIndex()][resource]; }
        TileEntity target(World world)
        {
            BlockPos pos = tile.getPos().offset(side);
            return world.isBlockLoaded(pos) ? world.getTileEntity(pos) : null;
        }
    }
}