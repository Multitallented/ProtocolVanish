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

package com.azortis.protocolvanish.visibility;

import com.azortis.protocolvanish.ProtocolVanish;
import com.azortis.protocolvanish.api.PlayerReappearEvent;
import com.azortis.protocolvanish.api.PlayerVanishEvent;
import com.azortis.protocolvanish.settings.VisibilitySettingsWrapper;
import com.azortis.protocolvanish.visibility.packetlisteners.*;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

public class VisibilityManager {

    private ProtocolVanish plugin;
    private VisibilityChanger visibilityChanger;

    private Collection<UUID> vanishedPlayers = new ArrayList<>();
    private HashMap<UUID, VanishPlayer> vanishPlayerMap = new HashMap<>();

    public VisibilityManager(ProtocolVanish plugin){
        this.plugin = plugin;
        this.visibilityChanger = new VisibilityChanger(plugin);
        validateSettings();
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new ServerInfoPacketListener(plugin));
        protocolManager.addPacketListener(new PlayerInfoPacketListener(plugin));
        protocolManager.addPacketListener(new TabCompletePacketListener(plugin));
        protocolManager.addPacketListener(new GeneralEntityPacketListener(plugin));
        protocolManager.addPacketListener(new NamedSoundEffectPacketListener(plugin));
        protocolManager.addPacketListener(new WorldParticlesPacketListener(plugin));
        new ActionBarRunnable(plugin);
    }

    private void validateSettings(){
        boolean valid = true;
        VisibilitySettingsWrapper visibilitySettings = plugin.getSettingsManager().getVisibilitySettings();
        List<String> enabledPacketListeners = visibilitySettings.getEnabledPacketListeners();
        if(!enabledPacketListeners.contains("GeneralEntity") && enabledPacketListeners.contains("PlayerInfo")){
            enabledPacketListeners.add("GeneralEntity");
            valid = false;
        }
        if(!enabledPacketListeners.contains("ServerInfo") && (visibilitySettings.getAdjustOnlinePlayerCount() || visibilitySettings.getAdjustOnlinePlayerList())){
            enabledPacketListeners.add("ServerInfo");
            valid = false;
        }
        if((!visibilitySettings.getAdjustOnlinePlayerList() || !visibilitySettings.getAdjustOnlinePlayerCount()) && enabledPacketListeners.contains("ServerInfo")){
            enabledPacketListeners.remove("ServerInfo");
            valid = false;
        }
        if(!enabledPacketListeners.contains("PlayerInfo") && enabledPacketListeners.contains("TabComplete")){
            enabledPacketListeners.remove("TabComplete");
            valid = false;
        }
        if(!valid){
            plugin.getAzortisLib().getLogger().warning("You're invisibility settings are invalid, changing some values...");
            visibilitySettings.setEnabledPacketListeners(enabledPacketListeners);
            visibilitySettings.save();
            plugin.getSettingsManager().saveSettingsFile();
        }
    }

    public void setVanished(UUID uuid, boolean vanished){
        if(vanishedPlayers.contains(uuid) && vanished)return;
        if(vanished){
            PlayerVanishEvent playerVanishEvent = new PlayerVanishEvent(Bukkit.getPlayer(uuid));
            Bukkit.getServer().getPluginManager().callEvent(playerVanishEvent);
            if(!playerVanishEvent.isCancelled()) {
                vanishedPlayers.add(uuid);
                if(!vanishPlayerMap.containsKey(uuid))vanishPlayerMap.put(uuid, new VanishPlayer(Bukkit.getPlayer(uuid), true, plugin));
                else vanishPlayerMap.get(uuid).setVanishState(true);
                plugin.getStorageManager().setVanished(uuid, true);
                visibilityChanger.vanishPlayer(uuid);
            }
        }else{
            PlayerReappearEvent playerReappearEvent = new PlayerReappearEvent(Bukkit.getPlayer(uuid));
            Bukkit.getServer().getPluginManager().callEvent(playerReappearEvent);
            if(!playerReappearEvent.isCancelled()) {
                vanishedPlayers.remove(uuid);
                plugin.getStorageManager().setVanished(uuid, false);
                vanishPlayerMap.get(uuid).setVanishState(false);
                visibilityChanger.showPlayer(uuid);
            }
        }
    }

    public Player getPlayerFromEntityID(int entityId, World world){
        Entity entity = ProtocolLibrary.getProtocolManager().getEntityFromID(world, entityId);

        if(entity instanceof Player){
            return (Player)entity;
        }
        return null;
    }

    public boolean isVanished(UUID uuid){
        if(!vanishedPlayers.contains(uuid) && plugin.getStorageManager().isVanished(uuid)){
            vanishedPlayers.add(uuid);
            vanishPlayerMap.put(uuid, new VanishPlayer(Bukkit.getPlayer(uuid), true, plugin));
        }
        return vanishedPlayers.contains(uuid);
    }

    public Collection<UUID> getVanishedPlayers() {
        return vanishedPlayers;
    }

    public Collection<UUID> getOnlineVanishedPlayers(){
        Collection<UUID> onlineVanishedPlayers = new ArrayList<>(vanishedPlayers);
        onlineVanishedPlayers.removeIf((UUID uuid) -> Bukkit.getPlayer(uuid) == null);
        return onlineVanishedPlayers;
    }

    public VanishPlayer getVanishPlayer(UUID uuid){
        if(!vanishPlayerMap.containsKey(uuid))vanishPlayerMap.put(uuid, new VanishPlayer(Bukkit.getPlayer(uuid), false, plugin));
        return vanishPlayerMap.get(uuid);
    }

}
