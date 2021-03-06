/*
 * Hides you completely from players on your servers by using packets!
 *     Copyright (C) 2019  Azortis
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.azortis.protocolvanish.bukkit;

import com.azortis.protocolvanish.bukkit.api.VanishAPI;
import com.azortis.protocolvanish.bukkit.listeners.*;
import com.azortis.protocolvanish.bukkit.settings.SettingsManager;
import com.azortis.protocolvanish.bukkit.command.VanishCommand;
import com.azortis.protocolvanish.bukkit.hooks.HookManager;
import com.azortis.protocolvanish.bukkit.storage.StorageManager;
import com.azortis.protocolvanish.bukkit.visibility.VisibilityManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.UUID;

public final class ProtocolVanish extends JavaPlugin {

    private Metrics metrics;
    private SettingsManager settingsManager;
    private PermissionManager permissionManager;
    private VisibilityManager visibilityManager;
    private StorageManager storageManager;
    private HookManager hookManager;
    private UpdateChecker updateChecker;

    private VanishCommand vanishCommand;

    private HashMap<UUID, VanishPlayer> vanishPlayerMap = new HashMap<>();

    @Override
    public void onEnable() {
        if (!Bukkit.getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            this.getLogger().severe("ProtocolLib isn't present, please install ProtocolLib! Shutting down...");
            Bukkit.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.updateChecker = new UpdateChecker(this);
        this.settingsManager = new SettingsManager(this);
        if(!settingsManager.areFilesUpToDate())return;

        this.metrics = new Metrics(this);
        this.storageManager = new StorageManager(this);
        this.permissionManager = new PermissionManager(this);
        this.visibilityManager = new VisibilityManager(this);
        this.hookManager = new HookManager(this);
//        this.vanishCommand = new VanishCommand(this);

        this.getLogger().info("Registering events...");
        new PlayerLoginListener(this);
        new PlayerJoinListener(this);
        new PlayerQuitListener(this);
        new EntityDamageListener(this);
        new FoodLevelChangeListener(this);
        new EntityTargetLivingEntityListener(this);
        new EntityPickupItemListener(this);
        new PlayerToggleSneakListener(this);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    VanishPlayer vanishPlayer = getVanishPlayer(player.getUniqueId());
                    if (vanishPlayer == null) vanishPlayer = createVanishPlayer(player);
                    PotionEffect potionEffect = player.getPotionEffect(PotionEffectType.INVISIBILITY);
                    if (!vanishPlayer.isVanished() && potionEffect != null && !vanishPlayer.isInvisPotion()) {
                        vanishPlayer.setInvisPotion(true);
                        vanishPlayer.getPlayerSettings().setNightVision(false);
                        visibilityManager.setVanished(player.getUniqueId(), true);
                    } else if (potionEffect == null && vanishPlayer.isInvisPotion()) {
                        if (vanishPlayer.isVanished()) {
                            visibilityManager.setVanished(player.getUniqueId(), false);
                        }
                        vanishPlayer.getPlayerSettings().setNightVision(
                                getSettingsManager().getInvisibilitySettings().getNightVisionEffect());
                        vanishPlayer.setInvisPotion(false);
                    } else if (potionEffect != null && vanishPlayer.isInvisPotion()) {
                        boolean shouldBeVanished = shouldPlayerBeVanished(player);
                        if (vanishPlayer.isVanished() && !shouldBeVanished) {
                            visibilityManager.setVanished(player.getUniqueId(), false);
                        } else if (!vanishPlayer.isVanished() && shouldBeVanished) {
                            visibilityManager.setVanished(player.getUniqueId(), true);
                        }
                        if (vanishPlayer.isVanished()) {
                            spawnParticles(player);
                        }
                    }
                }
            }
        },  100L, 20L);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.isSprinting()) {
                        continue;
                    }
                    VanishPlayer vanishPlayer = getVanishPlayer(player.getUniqueId());
                    if (vanishPlayer == null) vanishPlayer = createVanishPlayer(player);
                    if (vanishPlayer.isVanished() && vanishPlayer.isInvisPotion()) {
                        Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
                        if (block.getType() != Material.AIR) {
                            BlockData blockData = block.getBlockData();
                            player.getWorld().spawnParticle(Particle.BLOCK_DUST, player.getLocation(), 10, blockData);
                        }
                    }
                }
            }
        }, 100L, 4L);

        VanishAPI.setPlugin(this);
    }

    private void spawnParticles(Player player) {
        Location spellParticleLocation = new Location(player.getWorld(), player.getLocation().getX(),
                player.getLocation().getY() + 1, player.getLocation().getZ());
        player.getWorld().spawnParticle(Particle.SPELL, spellParticleLocation, 1);
    }

    private boolean shouldPlayerBeVanished(Player player) {
        boolean isOnFire = player.getFireTicks() > 0;
        if (isOnFire) {
            return false;
        }
        boolean isNaked = isItemSlotEmpty(player.getInventory().getItemInMainHand()) &&
                isItemSlotEmpty(player.getInventory().getHelmet()) &&
                isItemSlotEmpty(player.getInventory().getChestplate()) &&
                isItemSlotEmpty(player.getInventory().getLeggings()) &&
                isItemSlotEmpty(player.getInventory().getBoots());
        if (!isNaked) {
            return false;
        }

        boolean isTooCloseToAnotherPlayer = false;
        for (Player player1 : Bukkit.getOnlinePlayers()) {
            if (player.equals(player1) || !player.getWorld().equals(player1.getWorld())) {
                continue;
            }
            if (player.getLocation().distanceSquared(player1.getLocation()) < 16) {
                isTooCloseToAnotherPlayer = true;
                break;
            }
        }
        return !isTooCloseToAnotherPlayer;
    }

    private boolean isItemSlotEmpty(ItemStack itemStack) {
        return itemStack == null || itemStack.getType() == Material.AIR;
    }

    public UpdateChecker getUpdateChecker(){
        return updateChecker;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public VisibilityManager getVisibilityManager() {
        return visibilityManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public HookManager getHookManager() {
        return hookManager;
    }

    public VanishCommand getVanishCommand() {
        return vanishCommand;
    }

    /**
     * Gets the vanishPlayer of a specific player.
     *
     * @param uuid The {@link UUID} of the player.
     * @return The {@link VanishPlayer} of the player, null if have no permission.
     */
    public VanishPlayer getVanishPlayer(UUID uuid) {
        if (!vanishPlayerMap.containsKey(uuid)) return null;
        return vanishPlayerMap.get(uuid);
    }

    /**
     * Called to load the {@link VanishPlayer} from storage.
     * Should only be called on join.
     *
     * @param player The player of which instance has to be loaded.
     * @return The {@link VanishPlayer} that has been loaded.
     */
    public VanishPlayer loadVanishPlayer(Player player) {
        VanishPlayer vanishPlayer = storageManager.getVanishPlayer(player);
        if (vanishPlayer == null) return null;
        vanishPlayerMap.put(player.getUniqueId(), vanishPlayer);
        return vanishPlayer;
    }

    /**
     * Called to unload the {@link VanishPlayer} instance.
     * Should only be called on leave.
     *
     * @param player The player of which instance has to be unloaded.
     */
    public void unloadVanishPlayer(Player player) {
        vanishPlayerMap.remove(player.getUniqueId());
    }

    /**
     * Called to create an new instance of {@link VanishPlayer}
     * should only be called if there doesn't exist an entry,
     * and if the player has the permission in order to keep the database clean.
     *
     * @param player The {@link Player} of which to create a vanishPlayer instance.
     * @return The created vanishPlayer instance.
     */
    public VanishPlayer createVanishPlayer(Player player) {
        VanishPlayer vanishPlayer = storageManager.createVanishPlayer(player);
        vanishPlayerMap.put(player.getUniqueId(), vanishPlayer);
        return vanishPlayer;
    }

    /**
     * Sends message to specified {@link Player}.
     * Message will be processed, to replace all placeholders.
     *
     * @param receiver The player that should receive the message.
     * @param messagePath The path of the message in the MessageSettings.
     */
    public void sendPlayerMessage(Player receiver, Player placeholderPlayer,  String messagePath){
        return;
//        String message = settingsManager.getMessageSettings().getMessage(messagePath);
//        message = message.replace("%playername%", placeholderPlayer.getDisplayName());
////        String message = PlaceholderAPI.setPlaceholders(placeholderPlayer, rawMessage);
//        if(message.startsWith("[JSON]")){
//            try {
//                String jsonString = message.replace("[JSON]", "").trim();
//                WrappedChatComponent chatComponent = WrappedChatComponent.fromJson(jsonString);
//                PacketContainer packet = new PacketContainer(PacketType.Play.Server.CHAT);
//                packet.getChatTypes().write(0, EnumWrappers.ChatType.CHAT);
//                packet.getChatComponents().write(0, chatComponent);
//                ProtocolLibrary.getProtocolManager().sendServerPacket(receiver, packet);
//            }catch (InvocationTargetException ex){
//                ex.printStackTrace();
//            }
//        }
//        receiver.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

}
