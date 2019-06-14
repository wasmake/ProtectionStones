/*
 * Copyright 2019 ProtectionStones team and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.espi.ProtectionStones;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import dev.espi.ProtectionStones.event.PSCreateEvent;
import dev.espi.ProtectionStones.utils.UUIDCache;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.tags.CustomItemTagContainer;
import org.bukkit.inventory.meta.tags.ItemTagType;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.HashMap;
import java.util.List;

public class ListenerClass implements Listener {
    private static HashMap<Player, Double> lastProtectStonePlaced = new HashMap<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        UUIDCache.uuidToName.remove(e.getPlayer().getUniqueId());
        UUIDCache.uuidToName.put(e.getPlayer().getUniqueId(), e.getPlayer().getName());
        UUIDCache.nameToUUID.remove(e.getPlayer().getName());
        UUIDCache.nameToUUID.put(e.getPlayer().getName(), e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (e.isCancelled()) return;

        Player p = e.getPlayer();
        Block b = e.getBlock();

        // check if the block is a protection stone
        if (!ProtectionStones.isProtectBlockType(b.getType().toString())) return;
        PSProtectBlock blockOptions = ProtectionStones.getBlockOptions(b.getType().toString());

        // check if the item was created by protection stones (stored in custom tag)
        // block must have restrictObtaining enabled for blocking place
        if (blockOptions.restrictObtaining && !ProtectionStones.isProtectBlockItem(e.getItemInHand(), true)) return;

        // check if player has toggled off placement of protection stones
        if (ProtectionStones.toggleList.contains(p.getUniqueId())) return;

        // check permission
        if (!p.hasPermission("protectionstones.create") || (!blockOptions.permission.equals("") && !p.hasPermission(blockOptions.permission))) {
            PSL.msg(p, PSL.NO_PERMISSION_CREATE.msg());
            e.setCancelled(true);
            return;
        }

        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(e.getPlayer().getWorld()));

        LocalPlayer lp = WorldGuardPlugin.inst().wrapPlayer(p);

        // check if player can place block in that area
        if (!WorldGuardPlugin.inst().createProtectionQuery().testBlockPlace(p, b.getLocation(), b.getType())) {
            PSL.msg(p, PSL.CANT_PROTECT_THAT.msg());
            e.setCancelled(true);
            return;
        }

        // check cooldown
        if (ProtectionStones.getInstance().getConfigOptions().placingCooldown != -1) {
            double currentTime = System.currentTimeMillis();
            if (lastProtectStonePlaced.containsKey(p)) {
                double cooldown = ProtectionStones.getInstance().getConfigOptions().placingCooldown; // seconds
                double lastPlace = lastProtectStonePlaced.get(p); // milliseconds

                if (lastPlace + cooldown * 1000 > currentTime) { // if cooldown has not been finished
                    e.setCancelled(true);
                    PSL.msg(p, PSL.COOLDOWN.msg().replace("%time%", String.format("%.1f", cooldown - ((currentTime - lastPlace) / 1000))));
                    return;
                }

                lastProtectStonePlaced.remove(p);
            }
            lastProtectStonePlaced.put(p, currentTime);
        }

        // non-admin checks
        if (!p.hasPermission("protectionstones.admin")) {

            // check if player has limit on protection stones
            HashMap<PSProtectBlock, Integer> regionLimits = ProtectionStones.getPlayerPSBlockLimits(p);
            int maxPS = ProtectionStones.getPlayerPSGlobalBlockLimits(p);

            if (maxPS != -1 || !regionLimits.isEmpty()) { // only check if limit was found
                // count player's protection stones
                HashMap<String, Integer> regionFound = new HashMap<>();
                int total = 0;
                for (World w : Bukkit.getWorlds()) {
                    RegionManager rgm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(w));
                    for (ProtectedRegion r : rgm.getRegions().values()) {
                        String f = r.getFlag(FlagHandler.PS_BLOCK_MATERIAL);
                        if (r.getOwners().contains(lp) && r.getId().startsWith("ps") && f != null) {
                            total++;
                            int num = regionFound.containsKey(ProtectionStones.getBlockOptions(f).alias) ? regionFound.get(ProtectionStones.getBlockOptions(f).alias) + 1 : 1;
                            regionFound.put(ProtectionStones.getBlockOptions(f).alias, num);
                        }
                    }
                }
                // check if player has passed region limit
                if (total >= maxPS && maxPS != 0) {
                    PSL.msg(p, PSL.REACHED_REGION_LIMIT.msg());
                    e.setCancelled(true);
                    return;
                }

                for (PSProtectBlock ps : regionLimits.keySet()) {
                    if (regionFound.containsKey(ps.type) && regionLimits.get(ps) <= regionFound.get(ps.type)) {
                        PSL.msg(p, PSL.REACHED_REGION_LIMIT.msg());
                        e.setCancelled(true);
                        return;
                    }
                }
            }

            // check if in world blacklist or not in world whitelist
            if (blockOptions.worldListType.equalsIgnoreCase("blacklist")) {
                for (String world : blockOptions.worlds) {
                    if (world.trim().equals(p.getLocation().getWorld().getName())) {
                        PSL.msg(p, PSL.WORLD_DENIED_CREATE.msg());
                        e.setCancelled(true);
                        return;
                    }
                }
            } else if (blockOptions.worldListType.equalsIgnoreCase("whitelist")) {
                boolean found = false;
                for (String world : blockOptions.worlds) {
                    if (world.trim().equals(p.getLocation().getWorld().getName())) {
                        found = true;
                    }
                }
                if (!found) {
                    PSL.msg(p, PSL.WORLD_DENIED_CREATE.msg());
                    e.setCancelled(true);
                    return;
                }
            }

        } // end of non-admin checks

        // create region
        double bx = b.getLocation().getX();
        double by = b.getLocation().getY();
        double bz = b.getLocation().getZ();
        BlockVector3 v1, v2;

        if (blockOptions.yRadius == -1) {
            v1 = BlockVector3.at(bx - blockOptions.xRadius, 0, bz - blockOptions.zRadius);
            v2 = BlockVector3.at(bx + blockOptions.xRadius, p.getWorld().getMaxHeight(), bz + blockOptions.zRadius);
        } else {
            v1 = BlockVector3.at(bx - blockOptions.xRadius, by - blockOptions.yRadius, bz - blockOptions.zRadius);
            v2 = BlockVector3.at(bx + blockOptions.xRadius, by + blockOptions.yRadius, bz + blockOptions.zRadius);
        }

        BlockVector3 min = v1;
        BlockVector3 max = v2;
        String id = "ps" + (long) bx + "x" + (long) by + "y" + (long) bz + "z";

        ProtectedRegion region = new ProtectedCuboidRegion(id, min, max);
        region.getOwners().addPlayer(p.getUniqueId());

        rm.addRegion(region);

        // fire event and check if cancelled
        PSCreateEvent event = new PSCreateEvent(ProtectionStones.getPSRegionFromWGRegion(p.getWorld(), region), p);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            e.setCancelled(true);
            rm.removeRegion(id);
            return;
        }

        // check if new region overlaps more powerful region
        if (rm.overlapsUnownedRegion(region, lp)) {
            ApplicableRegionSet rp = rm.getApplicableRegions(region);
            boolean powerfulOverLap = false;
            for (ProtectedRegion rg : rp) {
                if (!rg.isOwner(lp) && rg.getPriority() >= region.getPriority()) { // if protection priority < overlap priority
                    powerfulOverLap = true;
                    break;
                }
            }
            if (powerfulOverLap) { // if we overlap a more powerful region
                rm.removeRegion(id);
                p.updateInventory();
                PSL.msg(p, PSL.REGION_OVERLAP.msg());
                e.setCancelled(true);
                return;
            }
        }

        // add corresponding flags to new region by cloning blockOptions default flags
        HashMap<Flag<?>, Object> flags = new HashMap<>(blockOptions.regionFlags);

        // replace greeting and farewell messages with player name
        Flag<?> greeting = WorldGuard.getInstance().getFlagRegistry().get("greeting");
        Flag<?> farewell = WorldGuard.getInstance().getFlagRegistry().get("farewell");

        if (flags.containsKey(greeting)) {
            flags.put(greeting, ((String) flags.get(greeting)).replaceAll("%player%", p.getName()));
        }
        if (flags.containsKey(farewell)) {
            flags.put(farewell, ((String) flags.get(farewell)).replaceAll("%player%", p.getName()));
        }

        // set flags
        region.setFlags(flags);
        FlagHandler.initCustomFlagsForPS(region, b, blockOptions);

        region.setPriority(blockOptions.priority);
        p.sendMessage(PSL.PROTECTED.msg());

        // hide block if auto hide is enabled
        if (blockOptions.autoHide) b.setType(Material.AIR);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.isCancelled()) return;

        Player p = e.getPlayer();
        Block pb = e.getBlock();

        String blockType = pb.getType().toString();
        PSProtectBlock blockOptions = ProtectionStones.getBlockOptions(blockType);

        // check if block broken is protection stone
        if (blockOptions == null) return;

        WorldGuardPlugin wg = WorldGuardPlugin.inst();

        RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager rgm = regionContainer.get(BukkitAdapter.adapt(p.getWorld()));

        String id = "ps" + (long) pb.getLocation().getX() + "x" + (long) pb.getLocation().getY() + "y" + (long) pb.getLocation().getZ() + "z";

        // check if that is actually a protection stone block (owns a region)
        if (rgm.getRegion(id) == null) {

            // prevent silk touching of protection stone blocks (that aren't holding a region)
            if (blockOptions.preventSilkTouch) {
                ItemStack left = e.getPlayer().getInventory().getItemInMainHand();
                ItemStack right = e.getPlayer().getInventory().getItemInOffHand();
                if (!left.containsEnchantment(Enchantment.SILK_TOUCH) && !right.containsEnchantment(Enchantment.SILK_TOUCH)) {
                    return;
                }
                e.setDropItems(false);
            }

            return;
        }

        // check for destroy permission
        if (!p.hasPermission("protectionstones.destroy")) {
            PSL.msg(p, PSL.NO_PERMISSION_DESTROY.msg());
            e.setCancelled(true);
            return;
        }

        // check if player is owner of region
        if (!rgm.getRegion(id).isOwner(wg.wrapPlayer(p)) && !p.hasPermission("protectionstones.superowner")) {
            PSL.msg(p, PSL.NO_REGION_PERMISSION.msg());
            e.setCancelled(true);
            return;
        }

        // return protection stone if no drop option is off
        if (!blockOptions.noDrop) {
            if (!p.getInventory().addItem(ProtectionStones.createProtectBlockItem(blockOptions)).isEmpty()) {
                // method will return not empty if item couldn't be added
                PSL.msg(p, PSL.NO_ROOM_IN_INVENTORY.msg());
                e.setCancelled(true);
                return;
            }
        }

        // check if removing the region and firing region remove event blocked it
        if (!ProtectionStones.removePSRegion(p.getWorld(), id, p)) {
            return;
        }

        // remove block
        pb.setType(Material.AIR);

        PSL.msg(p, PSL.NO_LONGER_PROTECTED.msg());

        e.setDropItems(false);
        e.setExpToDrop(0);
    }

    private void pistonUtil(List<Block> pushedBlocks, BlockPistonEvent e) {
        RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager rgm = regionContainer.get(BukkitAdapter.adapt(e.getBlock().getWorld()));
        for (Block b : pushedBlocks) {
            PSProtectBlock cpb = ProtectionStones.getBlockOptions(b.getType().toString());
            if (cpb != null && rgm.getRegion("ps" + b.getX() + "x" + b.getY() + "y" + b.getZ() + "z") != null && cpb.preventPistonPush) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager rgm = regionContainer.get(BukkitAdapter.adapt(e.getEntity().getWorld()));

        // loop through exploded blocks
        for (int i = 0; i < e.blockList().size(); i++) {
            Block b = e.blockList().get(i);

            if (ProtectionStones.isProtectBlockType(b.getType().toString())) {
                String id = "ps" + b.getX() + "x" + b.getY() + "y" + b.getZ() + "z";
                if (rgm.getRegion(id) != null) {
                    if (ProtectionStones.getBlockOptions(b.getType().toString()).preventExplode) {
                        // remove block from exploded list if prevent_explode is enabled
                        e.blockList().remove(i);
                        i--;
                    } else if (ProtectionStones.getBlockOptions(b.getType().toString()).destroyRegionWhenExplode) {
                        // remove region from worldguard if destroy_region_when_explode is enabled
                        // check if removing the region and firing region remove event blocked it
                        if (!ProtectionStones.removePSRegion(e.getLocation().getWorld(), id)) {
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        pistonUtil(e.getBlocks(), e);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        pistonUtil(e.getBlocks(), e);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == TeleportCause.ENDER_PEARL || event.getCause() == TeleportCause.CHORUS_FRUIT) return;

        if (event.getPlayer().hasPermission("protectionstones.tp.bypassprevent")) return;

        WorldGuardPlugin wg = WorldGuardPlugin.inst();
        RegionManager rgm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(event.getTo().getWorld()));
        BlockVector3 v = BlockVector3.at(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());

        // check if player can teleport into region (no region with preventTeleportIn = true)
        ApplicableRegionSet regions = rgm.getApplicableRegions(v);
        if (regions.getRegions().isEmpty()) return;
        boolean foundNoTeleport = false;
        for (ProtectedRegion r : regions) {
            String f = r.getFlag(FlagHandler.PS_BLOCK_MATERIAL);
            if (f != null && ProtectionStones.getBlockOptions(f) != null && ProtectionStones.getBlockOptions(f).preventTeleportIn)
                foundNoTeleport = true;
            if (r.getOwners().contains(wg.wrapPlayer(event.getPlayer()))) return;
        }

        if (foundNoTeleport) {
            PSL.msg(event.getPlayer(), PSL.REGION_CANT_TELEPORT.msg());
            event.setCancelled(true);
        }
    }

}