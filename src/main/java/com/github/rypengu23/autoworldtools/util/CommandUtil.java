package com.github.rypengu23.autoworldtools.util;

import com.github.rypengu23.autoworldtools.config.ConfigLoader;
import com.github.rypengu23.autoworldtools.config.DataConfig;
import com.github.rypengu23.autoworldtools.config.MainConfig;
import com.github.rypengu23.autoworldtools.config.MessageConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

import java.util.List;

public class CommandUtil {

    private final ConfigLoader configLoader;
    private final MainConfig mainConfig;
    private final DataConfig dataConfig;

    public CommandUtil() {
        this.configLoader = new ConfigLoader();
        this.mainConfig = configLoader.getMainConfig();
        this.dataConfig = configLoader.getDataConfig();
    }


    public static final String CURRENT_WORLD_NAME = "{currentWorldName}";
    public static final String NEXT_WORLD_NAME = "{nextWorldName}";

    public void executeCommands(int worldType) {
        String currentWorldName ;
        String nextWorldName ;


        if (worldType == 0) {
            currentWorldName = dataConfig.getCurrentWorldNameOfNormal();
            nextWorldName = dataConfig.getNextNormalWorld();
        } else if (worldType == 1) {
            currentWorldName = dataConfig.getCurrentWorldNameOfNether();
            nextWorldName = dataConfig.getNextNetherWorld();
        } else {
            currentWorldName = dataConfig.getCurrentWorldNameOfEnd();
            nextWorldName = dataConfig.getNextEndWorld();
        }


        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        List<String> executeCommandsAfterBackup = mainConfig.getExecuteCommandsAfterBackup();
        for (String command : executeCommandsAfterBackup) {
            String replacedCommand = ConvertUtil.placeholderUtil(CURRENT_WORLD_NAME, currentWorldName, NEXT_WORLD_NAME, nextWorldName, command);
            Bukkit.dispatchCommand(console, replacedCommand);
        }
    }
}
