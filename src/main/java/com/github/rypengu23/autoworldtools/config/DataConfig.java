package com.github.rypengu23.autoworldtools.config;

import jp.jyn.jbukkitlib.config.YamlLoader;
import org.bukkit.configuration.Configuration;

import java.util.Arrays;
import java.util.List;

public class DataConfig {
    private final MainConfig mainConfig;

    private static final String CURRENT_WORLD_NAME_OF_NORMAL = "currentWorldNameOfNormal";
    private static final String CURRENT_WORLD_NAME_OF_NETHER = "currentWorldNameOfNether";
    private static final String CURRENT_WORLD_NAME_OF_END = "currentWorldNameOfEnd";

    private final Configuration config;
    private final YamlLoader dataLoader;


    public DataConfig(Configuration config, YamlLoader dataLoader) {
        ConfigLoader configLoader = new ConfigLoader();
        this.config = config;
        this.mainConfig = configLoader.getMainConfig();
        this.dataLoader = dataLoader;
    }

    public String getCurrentWorldNameOfNormal() {
        return config.getString(CURRENT_WORLD_NAME_OF_NORMAL);
    }

    private void saveCurrentWorldNameOfNormal(String worldName) {
        config.set(CURRENT_WORLD_NAME_OF_NORMAL, worldName);
        dataLoader.saveConfig();
    }


    public String getNextNormalWorld() {
        List<String> worlds = Arrays.asList(mainConfig.getResetWorldNameOfNormal());
        String currentWorldName = getCurrentWorldNameOfNormal();
        return getNextWorldName(worlds, currentWorldName);
    }

    public String toNextNormalWorld() {
        String nextWorldName = getNextNormalWorld();
        saveCurrentWorldNameOfNormal(nextWorldName);
        return nextWorldName;
    }


    public String getCurrentWorldNameOfNether() {
        return config.getString(CURRENT_WORLD_NAME_OF_NETHER);
    }

    private void saveCurrentWorldNameOfNether(String worldName) {
        config.set(CURRENT_WORLD_NAME_OF_NETHER, worldName);
        dataLoader.saveConfig();
    }


    public String getNextNetherWorld() {
        List<String> worlds = Arrays.asList(mainConfig.getResetWorldNameOfNether());
        String currentWorldName = getCurrentWorldNameOfNether();
        return getNextWorldName(worlds, currentWorldName);
    }

    public String toNextNetherWorld() {
        String nextWorldName = getNextNetherWorld();
        saveCurrentWorldNameOfNether(nextWorldName);
        return nextWorldName;
    }


    public String getCurrentWorldNameOfEnd() {
        return config.getString(CURRENT_WORLD_NAME_OF_END);
    }

    private void saveCurrentWorldNameOfEnd(String worldName) {
        config.set(CURRENT_WORLD_NAME_OF_END, worldName);
        dataLoader.saveConfig();
    }


    public String getNextEndWorld() {
        List<String> worlds = Arrays.asList(mainConfig.getResetWorldNameOfEnd());
        String currentWorldName = getCurrentWorldNameOfEnd();
        return getNextWorldName(worlds, currentWorldName);
    }

    public String toNextEndWorld() {
        String nextWorldName = getNextEndWorld();
        saveCurrentWorldNameOfEnd(nextWorldName);
        return nextWorldName;
    }

    private String getNextWorldName(List<String> worlds, String worldName) {
        if (worldName == null) {
            return worlds.get(0);
        }

        int currentWorldIndex = worlds.indexOf(worldName);
        if (currentWorldIndex < 0) {
            throw new RuntimeException();
        }
        int nextWorldIndex = currentWorldIndex + 1;
        int maxWorldIndex = worlds.size() - 1;
        if (maxWorldIndex < nextWorldIndex) {
            return worlds.get(0);
        } else {
            return worlds.get(nextWorldIndex);
        }
    }


}
