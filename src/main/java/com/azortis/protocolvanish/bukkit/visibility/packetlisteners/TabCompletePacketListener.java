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

package com.azortis.protocolvanish.bukkit.visibility.packetlisteners;

import org.bukkit.entity.Player;

import com.azortis.protocolvanish.bukkit.ProtocolVanish;
import com.azortis.protocolvanish.bukkit.VanishPlayer;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;

public class TabCompletePacketListener extends PacketAdapter {

    private final ProtocolVanish plugin;

    public TabCompletePacketListener(ProtocolVanish plugin) {
        super(plugin, ListenerPriority.HIGH, PacketType.Play.Server.TAB_COMPLETE);
        this.plugin = plugin;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        VanishPlayer vanishPlayer = plugin.getVanishPlayer(player.getUniqueId());
        if (vanishPlayer == null) vanishPlayer = plugin.createVanishPlayer(player);
        if (!vanishPlayer.isInvisPotion() && plugin.getSettingsManager().getVisibilitySettings().getEnabledPacketListeners().contains("TabComplete")) {
            Suggestions suggestions = event.getPacket().getSpecificModifier(Suggestions.class).read(0);
            boolean writeChanges = false;
            for (Suggestion suggestion : suggestions.getList()){
                String suggestionString = suggestion.getText();
                if(!suggestionString.contains("/") && plugin.getVisibilityManager().isVanishedFrom(suggestionString, event.getPlayer())){
                    writeChanges = true;
                    suggestions.getList().remove(suggestion);
                }
            }
            if(writeChanges) {
                event.getPacket().getSpecificModifier(Suggestions.class).write(0, suggestions);
            }
        }
    }
}
