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

package com.azortis.protocolvanish.bukkit.storage;

import com.azortis.protocolvanish.bukkit.VanishPlayer;
import org.bukkit.entity.Player;

public class MySQLAdapter implements IDatabase {

    @Override
    public VanishPlayer getVanishPlayer(Player player) {
        return null;
    }

    @Override
    public VanishPlayer.PlayerSettings getPlayerSettings(Player player) {
        return null;
    }

    @Override
    public void saveVanishPlayer(VanishPlayer vanishPlayer) {

    }

    @Override
    public void savePlayerSettings(VanishPlayer.PlayerSettings playerSettings) {

    }

    @Override
    public void createVanishPlayer(VanishPlayer vanishPlayer) {

    }

    @Override
    public void deleteVanishPlayer(VanishPlayer vanishPlayer) {

    }

    @Override
    public void updateServerInfo() {

    }
}
