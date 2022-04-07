package com.github.rypengu23.autoworldtools.util;

import com.earth2me.essentials.User;
import com.earth2me.essentials.UserMap;
import com.github.rypengu23.autoworldtools.AutoWorldTools;
import org.bukkit.Location;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EssentialsUtil {

    private void deleteHome(Set<String> worldNameSet) {
        UserMap userMap = AutoWorldTools.essentials.getUserMap();
        Set<UUID> allUniqueUsers = userMap.getAllUniqueUsers();
        allUniqueUsers.parallelStream().forEach(
                uuid -> {
                    User user = userMap.getUser(uuid);
                    List<String> homeNameList = user.getHomes();
                    homeNameList.forEach(homeName -> {
                        Location homeLocation = user.getHome(homeName);
                        if (homeLocation.getWorld() != null &&
                                worldNameSet.stream()
                                        .anyMatch(worldName -> worldName.equalsIgnoreCase(homeLocation.getWorld().toString()))) {
                            try {
                                user.delHome(homeName);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
        );
    }
}
