package com.github.rypengu23.autoworldtools.util;

import com.github.rypengu23.autoworldtools.AutoWorldTools;
import com.github.rypengu23.autoworldtools.config.*;
import org.apache.commons.io.FileUtils;
import com.github.rypengu23.autoworldtools.config.ConfigLoader;
import com.github.rypengu23.autoworldtools.config.ConsoleMessage;
import com.github.rypengu23.autoworldtools.config.MainConfig;
import com.github.rypengu23.autoworldtools.config.MessageConfig;
import com.github.rypengu23.autoworldtools.model.ResetWorldModel;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;

import javax.annotation.CheckReturnValue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

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
        MultiversePortalsUtil multiversePortalsUtil = new MultiversePortalsUtil();

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
        CompletableFuture.allOf(IntStream
            .rangeClosed(0, 2)
            .mapToObj(this::regenerateWorld)
            .toArray(CompletableFuture[]::new)
        ).thenRun(() -> {
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
        });
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
    @CheckReturnValue
    public CompletableFuture<Void> regenerateWorld(int worldType) {
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
        if (resetWorld == null) {
            Bukkit.getLogger().warning("[AutoWorldTools] " + ConsoleMessage.ResetUtil_resetFailure + worldName);
            return CompletableFuture.completedFuture(null);
        }

        //プレイヤー退避
        movePlayer(resetWorld);

        // 先にアンロード
        resetWorld.save();
        Bukkit.unloadWorld(resetWorld, false);

        CompletableFuture<Void> cf;
        if (mainConfig.isBackupBeforeDeleteWorld()) {
            BackupUtil backupUtil = new BackupUtil();
            cf = backupUtil.createWorldFileZip(resetWorld, false)
                    .thenRunAsync(() -> backupUtil.deleteOldFile(worldName));
        } else {
            cf = CompletableFuture.completedFuture(null);
        }

        return cf.thenRunAsync(() -> {
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

            // ポータルを再生成
            try {
                MultiversePortalsUtil multiversePortalsUtil = new MultiversePortalsUtil();
                if (mainConfig.isUseMultiversePortals()) {
                    if ((worldType == 0 && mainConfig.isGateAutoBuildOfNormal()) || (worldType == 1 && mainConfig.isGateAutoBuildOfNether()) || (worldType == 2 && mainConfig.isGateAutoBuildOfEnd())) {
                        multiversePortalsUtil.createWarpGateUtil(worldType);
                    }
                }
            } catch (NoClassDefFoundError e) {
                Bukkit.getLogger().warning("[AutoWorldTools] " + ConsoleMessage.ResetUtil_resetFailureNotConnectedMultiverseCore);
            }

            if (mainConfig.isUseDynmap()) {
                DynmapUtil dynmapUtil = new DynmapUtil();
                dynmapUtil.deleteMapDataOfWorldName(worldName);
            }

            CommandUtil commandUtil = new CommandUtil();
            commandUtil.executeCommands(worldType);

            Bukkit.getLogger().info("[AutoWorldTools] " + ConsoleMessage.ResetUtil_resetComp + worldName);
        }, task -> Bukkit.getScheduler().runTask(AutoWorldTools.getInstance(), task));
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
