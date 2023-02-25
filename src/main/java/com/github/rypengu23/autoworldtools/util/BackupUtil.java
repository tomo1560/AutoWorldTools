package com.github.rypengu23.autoworldtools.util;

import com.github.rypengu23.autoworldtools.AutoWorldTools;
import com.github.rypengu23.autoworldtools.config.ConfigLoader;
import com.github.rypengu23.autoworldtools.config.ConsoleMessage;
import com.github.rypengu23.autoworldtools.config.MainConfig;
import com.github.rypengu23.autoworldtools.config.MessageConfig;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class BackupUtil {

    private final ConfigLoader configLoader;
    private final MainConfig mainConfig;
    private final MessageConfig messageConfig;
    //private String worldName;

    public BackupUtil() {
        this.configLoader = new ConfigLoader();
        this.mainConfig = configLoader.getMainConfig();
        this.messageConfig = configLoader.getMessageConfig();
    }

    /**
     * 現在時刻がバックアップ実行時刻か判定
     *
     * @param nowCalendar
     * @return
     */
    public boolean checkBackupTime(Calendar nowCalendar) {

        CheckUtil checkUtil = new CheckUtil();
        ConvertUtil convertUtil = new ConvertUtil();

        //バックアップ時刻リストを取得
        ArrayList<Calendar> backupTimeList = convertUtil.convertCalendar(mainConfig.getBackupDayOfTheWeekList(), mainConfig.getBackupTimeList());

        //比較
        if (backupTimeList != null) {
            for (Calendar backupTime : backupTimeList) {
                if (checkUtil.checkComparisonTime(nowCalendar, backupTime)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 現在時刻がバックアップ前アナウンス時刻か判定。
     * 戻り地が-1の場合、アナウンス時刻ではない。
     *
     * @param nowCalendar
     * @return
     */
    public int checkAnnounceBeforeBackupTime(Calendar nowCalendar) {

        CheckUtil checkUtil = new CheckUtil();
        ConvertUtil convertUtil = new ConvertUtil();

        //リセット時刻リストを取得
        ArrayList<Calendar> backupTimeList = convertUtil.convertCalendar(mainConfig.getBackupDayOfTheWeekList(), mainConfig.getBackupTimeList());

        //比較
        if (backupTimeList != null) {
            for (Calendar backupTime : backupTimeList) {
                int result = checkUtil.checkComparisonTimeOfList(nowCalendar, backupTime, mainConfig.getBackupNotifyTimeList());
                if (result != -1) {
                    return result;
                }
            }
        }
        return -1;
    }

    /**
     * Configに記載された全ワールドをバックアップ
     * メッセージ等も送信
     */
    public void autoBackup() {

        CheckUtil checkUtil = new CheckUtil();



        Runnable runnable = new BukkitRunnable() {
            @Override
            public void run() {

                //メッセージが空白で無ければ送信
                //バックアップ開始メッセージ(Discord)
                if (mainConfig.isUseDiscordSRV() && !checkUtil.checkNullOrBlank(messageConfig.getBackupStartOfDiscord())) {
                    DiscordUtil discordUtil = new DiscordUtil();
                    discordUtil.sendMessageMainChannel(messageConfig.getBackupStartOfDiscord());
                }

                //全ワールドバックアップ
                // TODO: CompletableFuture.allOf
                for (String worldName : mainConfig.getBackupWorldName()) {
                    createWorldFileZip(worldName, true).join();
                    deleteOldFile(worldName);
                }

                //メッセージが空白で無ければ送信
                //バックアップ完了メッセージ
                if (!checkUtil.checkNullOrBlank(messageConfig.getBackupComplete())) {
                    Bukkit.getServer().broadcastMessage("§a" + messageConfig.getPrefix() + " §f" + messageConfig.getBackupComplete());
                }

                //メッセージが空白で無ければ送信
                //バックアップ完了メッセージ(Discord)
                if (mainConfig.isUseDiscordSRV() && !checkUtil.checkNullOrBlank(messageConfig.getBackupCompleteOfDiscord())) {
                    DiscordUtil discordUtil = new DiscordUtil();
                    discordUtil.sendMessageMainChannel(messageConfig.getBackupCompleteOfDiscord());
                }
                AutoWorldTools.backupTask = null;

            }
        };

        AutoWorldTools.backupTask = Bukkit.getServer().getScheduler().runTaskAsynchronously(AutoWorldTools.getInstance(), runnable);

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
            Bukkit.getServer().broadcastMessage("§a" + messageConfig.getPrefix() + " §f" + convertUtil.placeholderUtil("{countdown}", countdownStr, messageConfig.getBackupCountdown()));
        }
    }

    /**
     * 引数のワールド名のデータをZIPに圧縮後、保存する。
     *
     * @param worldName
     */
    @CheckReturnValue
    public CompletableFuture<Void> createWorldFileZip(String worldName, boolean flush) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            throw new IllegalArgumentException("World '" + worldName + "' is not found.");
        }
        return createWorldFileZip(world, flush);
    }

    @CheckReturnValue
    public CompletableFuture<Void> createWorldFileZip(World world, boolean flush) {
        String worldName = world.getName();
        Bukkit.getLogger().info("[AutoWorldTools] " + ConsoleMessage.BackupUtil_startZip + worldName);

        //ファイル名用日付取得
        String zipFileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        Path saveDirectory = Paths.get(mainConfig.getBackupLocation()).resolve(worldName);
        Path workDirectory = saveDirectory.resolve("work" + zipFileName);
        Path perWorldWorkDirectory = workDirectory.resolve(worldName);
        Path zipPath = saveDirectory.resolve(worldName + zipFileName + ".zip");
        //バックアップするワールドをセーブ
        return CompletableFuture.runAsync(() -> {
                //バックアップ保存先フォルダを作成
                //一時ファイル保存先フォルダを作成
                try {
                    try {
                        Files.createDirectories(saveDirectory);
                    } catch (FileAlreadyExistsException ignored) {
                    }
                    try {
                        Files.createDirectories(perWorldWorkDirectory);
                    } catch (FileAlreadyExistsException ignored) {
                    }
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            })
            // サーバースレッドに再入し、ワールドを保存 (フラッシュ)
            .thenRunAsync(flush ? world::save : () -> {}, task -> Bukkit.getScheduler().runTask(AutoWorldTools.getInstance(), task))
            .thenApplyAsync(__ -> {
                Path worldDirectory = world.getWorldFolder().toPath();
                //バックアップするファイルをworkにコピー
                try {
                    FileUtils.copyDirectory(worldDirectory.toFile(), perWorldWorkDirectory.toFile());
                } catch (FileSystemException e){
                    if(!e.getFile().contains("session.lock")) {
                        System.out.println(e.getFile());
                        Bukkit.getLogger().warning("[AutoWorldTools] " + ConsoleMessage.BackupUtil_backupFailure);
                    }
                } catch (IOException exception) {
                    Bukkit.getLogger().warning("[AutoWorldTools] " + ConsoleMessage.BackupUtil_backupFailure);
                    throw new UncheckedIOException(exception);
                }
                //ZIP化
                try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + zipPath.toUri()), ImmutableMap.of("create", "true"))) {
                    Files.walk(perWorldWorkDirectory).forEach(file -> {
                        try {
                            Path destination = fs.getPath("/").resolve(perWorldWorkDirectory.relativize(file).toString());
                            Path parent = destination.getParent();
                            if (parent != null && !Files.isDirectory(parent)) {
                                try {
                                    Files.createDirectories(parent);
                                } catch (FileAlreadyExistsException ignored) {
                                }
                            }
                            Files.copy(file, destination);
                        } catch (FileAlreadyExistsException ignored) {
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    });
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
                return true;
            })
            .exceptionally(exception -> {
                Bukkit.getLogger().log(Level.WARNING, "[AutoWorldTools] " + ConsoleMessage.BackupUtil_backupFailure + worldName, exception);
                return false;
            })
            .thenAcceptAsync(succeeded -> {
                if (succeeded) {
                    Bukkit.getLogger().info("[AutoWorldTools] " + ConsoleMessage.BackupUtil_compZip + worldName);
                }
                //一時ディレクトリを削除
                try {
                    if (Files.isDirectory(workDirectory)) {
                        FileUtils.deleteDirectory(workDirectory.toFile());
                    }
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            });
    }

    /**
     * Configで設定されている保存上限を超えた場合に古いファイルを削除する。
     *
     * @param worldName
     */
    public void deleteOldFile(String worldName) {
        //バックアップロケーション内のファイル一覧取得
        //ZIPファイルリスト作成
        List<Path> backupFileList;
        try {
            backupFileList = Files.list(Paths.get(mainConfig.getBackupLocation()).resolve(worldName))
                .filter(p -> p.getFileName().toString().endsWith(".zip"))
                .filter(Files::isRegularFile)
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException exception) {
            Bukkit.getLogger().log(Level.WARNING, "Couldn't retrieve backup list", exception);
            return;
        }
        //上限を超えたファイル削除
        for (int i = 0; i < backupFileList.size() - mainConfig.getBackupLimit(); i++) {
            try {
                Files.deleteIfExists(backupFileList.get(i));
            } catch (IOException exception) {
                Bukkit.getLogger().log(Level.WARNING, "Couldn't delete old backup", exception);
            }
        }
    }

}
