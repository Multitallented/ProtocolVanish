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

package com.azortis.protocolvanish.common;

import java.util.UUID;

public class VanishPlayer {

    private final UUID uuid;
    private boolean vanished;
    private final PlayerSettings playerSettings;

    public VanishPlayer(UUID uuid, boolean vanished, PlayerSettings playerSettings){
        this.uuid = uuid;
        this.vanished = vanished;
        this.playerSettings = playerSettings;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isVanished() {
        return vanished;
    }

    public void setVanished(boolean vanished) {
        this.vanished = vanished;
    }

    public PlayerSettings getPlayerSettings() {
        return playerSettings;
    }
}
