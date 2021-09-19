package com.github.rypengu23.autoworldtools.util;

import com.github.rypengu23.autoworldtools.AutoWorldTools;
import com.github.rypengu23.autoworldtools.config.*;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class ResetUtil {

    private final ConfigLoader configLoader;
    private final MainConfig mainConfig;
    private final MessageConfig messageConfig;
    private final DataConfig dataConfig;


    public ResetUtil() {
        this.configLoader = new ConfigLoader();
        this.mainConfig = configLoader.getMainConfig();
        this.messageConfig = configLoader.getMessageConfig();
        this.dataConfig = configLoader.getDataConfig();
    }

    /**
     * 現在時刻がリセット実行時刻か判定
     *
     * @param nowCalendar
     * @return
     */
    public boolean checkResetTime(Calendar nowCalendar) {

        CheckUtil checkUtil = new CheckUtil();
        ConvertUtil convertUtil = new ConvertUtil();

        //リセット時刻リストを取得
        ArrayList<Calendar> resetTimeList = convertUtil.convertCalendar(mainConfig.getResetDayOfTheWeekList(), mainConfig.getResetTimeList());

        //比較
        if (resetTimeList != null) {
            for (Calendar resetTime : resetTimeList) {
                if (checkUtil.checkComparisonTime(nowCalendar, resetTime)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 現在時刻がリセット前アナウンス時刻か判定。
     * 戻り地が-1の場合、アナウンス時刻ではない。
     *
     * @param nowCalendar
     * @return
     */
    public int checkAnnounceBeforeResetTime(Calendar nowCalendar) {

        CheckUtil checkUtil = new CheckUtil();
        ConvertUtil convertUtil = new ConvertUtil();

        //リセット時刻リストを取得
        ArrayList<Calendar> resetTimeList = convertUtil.convertCalendar(mainConfig.getResetDayOfTheWeekList(), mainConfig.getResetTimeList());

        //比較
        if (resetTimeList != null) {
            for (Calendar resetTime : resetTimeList) {
                int result = checkUtil.checkComparisonTimeOfList(nowCalendar, resetTime, mainConfig.getResetNotifyTimeList());
                if (result != -1) {
                    return result;
                }
            }
        }
        return -1;
    }

    /**
     * Configに登録された全ワールドをリセット・ゲート生成する。
     * メッセージ等も送信
     */
    public void autoReset() {

        CheckUtil checkUtil = new CheckUtil();
        CreateWarpGateUtil createWarpGateUtil = new CreateWarpGateUtil();

        //メッセージが空白で無ければ送信
        //リセット開始メッセージ
        if (!checkUtil.checkNullOrBlank(messageConfig.getResetStart())) {
            Bukkit.getServer().broadcastMessage("§a" + messageConfig.getPrefix() + " §f" + messageConfig.getResetStart());
        }

        //メッセージが空白で無ければ送信
        //リセット開始メッセージ(Discord)
        if (mainConfig.isUseDiscordSRV() && !checkUtil.checkNullOrBlank(messageConfig.getResetStartOfDiscord())) {
            DiscordUtil discordUtil = new DiscordUtil();
            discordUtil.sendMessageMainChannel(messageConfig.getResetStartOfDiscord());
        }

        //全てのワールドのリセット
        for (int i = 0; i <= 2; i++) {
            regenerateWorld(i);
        }

        //全てのワールドへゲートを再生成
        if (mainConfig.isUseMultiversePortals()) {
            for (int i = 0; i <= 2; i++) {
                if ((i == 0 && mainConfig.isGateAutoBuildOfNormal()) || (i == 1 && mainConfig.isGateAutoBuildOfNether()) || (i == 2 && mainConfig.isGateAutoBuildOfEnd())) {
                    createWarpGateUtil.createWarpGateUtil(i);
                }
            }
        }

        //メッセージが空白で無ければ送信
        //リセット完了メッセージ
        if (!checkUtil.checkNullOrBlank(messageConfig.getResetComplete())) {
            Bukkit.getServer().broadcastMessage("§a" + messageConfig.getPrefix() + " §f" + messageConfig.getResetComplete());
        }

        //メッセージが空白で無ければ送信
        //リセット完了メッセージ(Discord)
        if (mainConfig.isUseDiscordSRV() && !checkUtil.checkNullOrBlank(messageConfig.getResetCompleteOfDiscord())) {
            DiscordUtil discordUtil = new DiscordUtil();
            discordUtil.sendMessageMainChannel(messageConfig.getResetCompleteOfDiscord());
        }
    }

    /**
     * 引数のカウントダウン秒数をもとに、メッセージを送信。
     *
     * @param second
     */
    public void sendNotify(int second) {

        CheckUtil checkUtil = new CheckUtil();
        ConvertUtil convertUtil = new ConvertUtil();

        if (second <= 0) {
            return;
        }

        String countdownStr = convertUtil.createCountdown(second, messageConfig);

        //メッセージが空白で無ければ送信
        //カウントダウンメッセージ
        if (!checkUtil.checkNullOrBlank(messageConfig.getResetCountdown())) {
            Bukkit.getServer().broadcastMessage("§a" + messageConfig.getPrefix() + " §f" + convertUtil.placeholderUtil("{countdown}", countdownStr, messageConfig.getResetCountdown()));
        }
    }

    /**
     * 指定されたタイプの素材世界を再生成
     * 0:ノーマル 1:ネザー 2:ジエンド
     *
     * @param worldType
     */
    public void regenerateWorld(int worldType) {

        try {
            //ワールド名リストの取得
            String worldName;
            if (worldType == 0) {
                worldName = dataConfig.toNextNormalWorld();
            } else if (worldType == 1) {
                worldName = dataConfig.toNextNetherWorld();
            } else {
                worldName = dataConfig.toNextEndWorld();
            }

            //ワールド再生成
            Bukkit.getLogger().info("[AutoWorldTools] " + ConsoleMessage.ResetUtil_resetStart + worldName);
            World resetWorld = Bukkit.getWorld(worldName);
            if (resetWorld != null) {

                //プレイヤー退避
                movePlayer(resetWorld);

                // 先にアンロード
                resetWorld.save();
                Bukkit.unloadWorld(resetWorld, false);

                CompletableFuture.runAsync(() -> {
                    if (mainConfig.isBackupBeforeDeleteWorld()) {
                        BackupUtil backupUtil = new BackupUtil();
                        backupUtil.createWorldFileZip(resetWorld, false).join();
                        backupUtil.deleteOldFile(worldName);
                    }
                }).thenRunAsync(() -> {
                    //ワールド削除
                    deleteDirectory(resetWorld.getWorldFolder());
                }).thenRunAsync(() -> {
                    //ワールド生成
                    WorldCreator worldCreator = new WorldCreator(worldName);
                    int worldSize;
                    switch (worldType) {
                        case 0:
                            worldCreator.environment(World.Environment.NORMAL);
                            worldSize = mainConfig.getWorldOfNormalSize();
                            break;
                        case 1:
                            worldCreator.environment(World.Environment.NETHER);
                            worldSize = mainConfig.getWorldOfNetherSize();
                            break;
                        case 2:
                            worldCreator.environment(World.Environment.THE_END);
                            worldSize = mainConfig.getWorldOfEndSize();
                            break;
                        default:
                            throw new IllegalArgumentException("worldType " + worldType + " is not valid. Must be [0,2]");
                    }
                    World world = worldCreator.createWorld();
                    if (world == null) {
                        throw new IllegalStateException("Couldn't to create new world");
                    }

                    //ワールドボーダーをセット
                    WorldBorder worldBorder = world.getWorldBorder();
                    worldBorder.setCenter(0.0, 0.0);
                    worldBorder.setSize(worldSize);


                    CommandUtil commandUtil = new CommandUtil();
                    commandUtil.executeCommands(worldType);

                    Bukkit.getLogger().info("[AutoWorldTools] " + ConsoleMessage.ResetUtil_resetComp + worldName);
                }, task -> Bukkit.getScheduler().runTask(AutoWorldTools.getInstance(), task));
            } else {
                Bukkit.getLogger().warning("[AutoWorldTools] " + ConsoleMessage.ResetUtil_resetFailure + worldName);
            }

        } catch (NoClassDefFoundError e) {
            Bukkit.getLogger().warning("[AutoWorldTools] " + ConsoleMessage.ResetUtil_resetFailureNotConnectedMultiverseCore);
        }
    }

    public void deleteDirectory(File file) {
        try {
            FileUtils.deleteDirectory(file);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void movePlayer(World world) {
        List<Player> playerList = world.getPlayers();

        for (Player player : playerList) {
            Location respawnLocation = Bukkit.getWorld("world").getSpawnLocation();
            player.teleport(respawnLocation);
        }
    }
}
