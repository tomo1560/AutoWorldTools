package com.github.rypengu23.autoworldtools.config;

import com.github.rypengu23.autoworldtools.AutoWorldTools;
import jp.jyn.jbukkitlib.config.YamlLoader;
import org.bukkit.plugin.Plugin;

import javax.xml.crypto.Data;

public class ConfigLoader {

    private Plugin plugin;

    private final YamlLoader mainLoader;
    private MainConfig mainConfig;
    static MainConfig mainConfigMemory;

    private YamlLoader messageLoader;
    private MessageConfig messageConfig;
    static MessageConfig messageConfigMemory;

    private final YamlLoader dataLoader;
    private DataConfig dataConfig;
    static DataConfig dataConfigMemory;

    public ConfigLoader() {
        this.plugin = AutoWorldTools.getInstance();
        this.mainLoader = new YamlLoader(plugin, "config.yml");
        this.mainConfig = mainConfigMemory;
        this.messageLoader = new YamlLoader(plugin, "message_en.yml");
        this.messageConfig = messageConfigMemory;
        this.dataLoader = new YamlLoader(plugin, "data.yml");
        this.dataConfig = dataConfigMemory;
    }

    public void reloadConfig() {

        //MainConfig
        mainLoader.saveDefaultConfig();
        if (mainConfig != null) {
            mainLoader.reloadConfig();
        }
        mainConfig = new MainConfig(mainLoader.getConfig());
        mainConfigMemory = mainConfig;

        //MessageConfig
        String[] messageConfigList = {"message_ja.yml", "message_en.yml"};
        for (String fileName : messageConfigList) {
            YamlLoader initLoader = new YamlLoader(plugin, fileName);
            initLoader.saveDefaultConfig();
        }

        messageLoader = new YamlLoader(plugin, "message_" + mainConfig.getLanguage() + ".yml");
        messageLoader.reloadConfig();

        messageConfig = new MessageConfig(messageLoader.getConfig());
        messageConfigMemory = messageConfig;

        dataLoader.saveDefaultConfig();
        if (dataConfig != null){
            dataLoader.reloadConfig();

        }

        dataConfig = new DataConfig(dataLoader.getConfig(), dataLoader);
        dataConfigMemory = dataConfig;

        //CommandMessage
        CommandMessage commandMessage = new CommandMessage(mainConfig);
        commandMessage.changeLanguageCommandMessages();

        //ConsoleMessage
        ConsoleMessage consoleMessage = new ConsoleMessage(mainConfig);
        consoleMessage.changeLanguageConsoleMessages();
    }

    public MainConfig getMainConfig() {
        return mainConfig;
    }

    public MessageConfig getMessageConfig() {
        return messageConfig;
    }

    public DataConfig getDataConfig() {
        return dataConfig;
    }

}
