package com.github.rypengu23.autoworldtools.util;

import com.github.rypengu23.autoworldtools.AutoWorldTools;
import com.github.rypengu23.autoworldtools.config.ConfigLoader;
import com.github.rypengu23.autoworldtools.config.ConsoleMessage;
import com.github.rypengu23.autoworldtools.config.DataConfig;
import com.github.rypengu23.autoworldtools.config.MainConfig;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.onarandombox.MultiversePortals.MVPortal;
import com.onarandombox.MultiversePortals.PortalLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreateWarpGateUtil {

    private final ConfigLoader configLoader;
    private final MainConfig mainConfig;
    private final DataConfig dataConfig;

    public CreateWarpGateUtil() {
        configLoader = new ConfigLoader();
        mainConfig = configLoader.getMainConfig();
        dataConfig = configLoader.getDataConfig();
    }

    /**
     * 指定されたワールドにゲートを生成
     *
     * @param worldType
     */
    public boolean createWarpGateUtil(int worldType) {

        ConvertUtil convertUtil = new ConvertUtil();

        //Multiverse-Portals 再起動
        //Restart Multiverse-Portals
        if (mainConfig.isUseMultiversePortals()) {
            try {
                Bukkit.getLogger().info("[AutoWorldTools] " + ConsoleMessage.CreateWarpGateUtil_RestartMultiversePortals);
                Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Portals");
                plugin.onDisable();
                plugin.onEnable();
                Bukkit.getLogger().info("[AutoWorldTools] " + ConsoleMessage.CreateWarpGateUtil_RestartCompMultiversePortals);
            } catch (NoClassDefFoundError e) {
                Bukkit.getLogger().warning("[AutoWorldTools] " + ConsoleMessage.CreateWarpGateUtil_RestartFailureMultiversePortals);
            }
        }

        //ワールド名リストとポータル名リストの取得
        //Get worlds list & portals list
        List<String> worldNameList;
        List<String> portalNameList;
        String worldName;
        if (worldType == 0) {
            worldName = dataConfig.getCurrentWorldNameOfNormal();
            worldNameList = new ArrayList<>(Arrays.asList(mainConfig.getResetWorldNameOfNormal()));
            portalNameList = new ArrayList<>(Arrays.asList(mainConfig.getPortalNameOfNormal()));
        } else if (worldType == 1) {
            worldName = dataConfig.getCurrentWorldNameOfNether();
            worldNameList = new ArrayList<>(Arrays.asList(mainConfig.getResetWorldNameOfNether()));
            portalNameList = new ArrayList<>(Arrays.asList(mainConfig.getPortalNameOfNether()));
        } else {
            worldName = dataConfig.getCurrentWorldNameOfEnd();
            worldNameList = new ArrayList<>(Arrays.asList(mainConfig.getResetWorldNameOfEnd()));
            portalNameList = new ArrayList<>(Arrays.asList(mainConfig.getPortalNameOfEnd()));
        }

        //ワールド名とポータル名の数が一致しない場合、終了
        if (worldNameList.size() != portalNameList.size()) {
            Bukkit.getLogger().info("[AutoWorldTools] " + ConsoleMessage.CreateWarpGateUtil_WorldNameGateNameAmountMismatch);
            return false;
        }

        //ゲート生成処理開始
        Bukkit.getLogger().info("[AutoWorldTools] " + ConsoleMessage.CreateWarpGateUtil_GateGenerateStart);

        MVPortal portal = null;
        for (String portalName : portalNameList) {
            //ポータル情報取得
            //ポータルの取得に失敗した場合、次のポータルへ
            MVPortal tempPortal = AutoWorldTools.multiversePortals.getPortalManager().getPortal(portalName);
            if (tempPortal != null && worldName.equals(tempPortal.getWorld().getName())) {
                portal = tempPortal;
            }
        }

        if (portal == null) {
            // Bukkit.getLogger().info("[AutoWorldTools] " + convertUtil.placeholderUtil("{portalname}", portalName, ConsoleMessage.CreateWarpGateUtil_PortalNotFound));
            // Bukkit.getLogger().info("[AutoWorldTools] " + convertUtil.placeholderUtil("{worldname}", worldName, "{portalname}", portalName, ConsoleMessage.CreateWarpGateUtil_PortalNotGenerateInfo));
            throw new RuntimeException();
        }


        Bukkit.getLogger().info("[AutoWorldTools] " + convertUtil.placeholderUtil("{worldname}", worldName, "{portalname}", portal.getName(), ConsoleMessage.CreateWarpGateUtil_GateGenerateInfo));

        //ワープゲートの位置
        int portalX = 0;
        int portalY = 0;
        int portalZ = 0;


        //ポータルの大きさを1×1×2に設定
        PortalLocation portalLocation = portal.getLocation();

        Vector pos1 = portal.getLocation().getRegion().getMaximumPoint();
        Vector pos2 = portal.getLocation().getRegion().getMinimumPoint();

        pos1.setX((pos1.getBlockX() + pos2.getBlockX()) / 2);
        pos1.setY(pos2.getBlockY());
        pos1.setZ((pos1.getBlockX() + pos2.getBlockX()) / 2);

        pos2.setX(pos1.getBlockX());
        pos2.setY(pos2.getBlockY() + 1);
        pos2.setZ(pos1.getBlockZ());

        portalLocation.setLocation(pos1, pos2, portalLocation.getMVWorld());
        portal.setPortalLocation(portalLocation);

        portalX = pos1.getBlockX();
        portalY = pos1.getBlockY();
        portalZ = pos1.getBlockZ();

        //スポーン地点変更
        Bukkit.getWorld(worldName).setSpawnLocation(portalX, portalY, portalZ + 2);

        //ワープゲート作成
        //土台
        World world = Bukkit.getWorld(worldName);
        Location setLocation = world.getSpawnLocation();

        int setX = portalX - 3;
        int setY = portalY - 1;
        int setZ = portalZ - 3;

        //ゲートを生成する領域内をクリア
        for (int j = 0; j < 7; j++) {
            for (int k = 0; k < 7; k++) {
                for (int l = 0; l < 5; l++) {
                    setLocation.setX(setX);
                    setLocation.setY(setY);
                    setLocation.setZ(setZ);
                    Block block = setLocation.getBlock();
                    block.setType(Material.AIR);
                    setY++;
                }
                setX++;
                setY = portalY - 1;
            }
            setZ++;
            setX = portalX - 3;
        }

        setX = portalX - 2;
        setY = portalY - 1;
        setZ = portalZ - 2;
        for (int j = 0; j < 5; j++) {
            for (int k = 0; k < 5; k++) {
                setLocation.setX(setX);
                setLocation.setY(setY);
                setLocation.setZ(setZ);
                Block block = setLocation.getBlock();
                block.setType(Material.BEDROCK);
                setX++;
            }
            setZ++;
            setX = portalX - 2;
        }

        setY = portalY;
        for (int j = 0; j < 3; j++) {
            setLocation.setX(portalX + 1);
            setLocation.setY(setY);
            setLocation.setZ(portalZ);
            Block block = setLocation.getBlock();
            block.setType(Material.BEDROCK);
            setY++;
        }

        setY = portalY;
        for (int j = 0; j < 3; j++) {
            setLocation.setX(portalX - 1);
            setLocation.setY(setY);
            setLocation.setZ(portalZ);
            Block block = setLocation.getBlock();
            block.setType(Material.BEDROCK);
            setY++;
        }

        setLocation.setX(portalX);
        setLocation.setY(portalY + 2);
        setLocation.setZ(portalZ);
        Block gateBlock = setLocation.getBlock();
        gateBlock.setType(Material.BEDROCK);

        //ワープ地点に松明設置
        setLocation.setX(portalX);
        setLocation.setY(portalY);
        setLocation.setZ(portalZ);
        gateBlock = setLocation.getBlock();
        gateBlock.setType(Material.TORCH);
        //土台・ゲート完成

        //ゲートを囲うガラス生成
        //天井
        setX = portalX - 3;
        setY = portalY + 3;
        setZ = portalZ - 3;
        for (int j = 0; j < 7; j++) {
            for (int k = 0; k < 7; k++) {
                setLocation.setX(setX);
                setLocation.setY(setY);
                setLocation.setZ(setZ);
                Block block = setLocation.getBlock();
                block.setType(Material.GLASS);
                setX++;
            }
            setZ++;
            setX = portalX - 3;
        }

        //側面1
        setX = portalX - 3;
        setY = portalY - 1;
        setZ = portalZ + 3;
        for (int j = 0; j < 5; j++) {
            for (int k = 0; k < 7; k++) {
                setLocation.setX(setX);
                setLocation.setY(setY);
                setLocation.setZ(setZ);
                Block block = setLocation.getBlock();
                block.setType(Material.GLASS);
                setX++;
            }
            setY++;
            setX = portalX - 3;
        }
        //側面2
        setX = portalX - 3;
        setY = portalY - 1;
        setZ = portalZ - 3;
        for (int j = 0; j < 5; j++) {
            for (int k = 0; k < 7; k++) {
                setLocation.setX(setX);
                setLocation.setY(setY);
                setLocation.setZ(setZ);
                Block block = setLocation.getBlock();
                block.setType(Material.GLASS);
                setX++;
            }
            setY++;
            setX = portalX - 3;
        }
        //側面3
        setX = portalX + 3;
        setY = portalY - 1;
        setZ = portalZ - 3;
        for (int j = 0; j < 5; j++) {
            for (int k = 0; k < 7; k++) {
                setLocation.setX(setX);
                setLocation.setY(setY);
                setLocation.setZ(setZ);
                Block block = setLocation.getBlock();
                block.setType(Material.GLASS);
                setZ++;
            }
            setY++;
            setZ = portalZ - 3;
        }
        //側面4
        setX = portalX - 3;
        setY = portalY - 1;
        setZ = portalZ - 3;
        for (int j = 0; j < 5; j++) {
            for (int k = 0; k < 7; k++) {
                setLocation.setX(setX);
                setLocation.setY(setY);
                setLocation.setZ(setZ);
                Block block = setLocation.getBlock();
                block.setType(Material.GLASS);
                setZ++;
            }
            setY++;
            setZ = portalZ - 3;
        }
        Bukkit.getLogger().info("[AutoWorldTools] " + ConsoleMessage.CreateWarpGateUtil_GateGenerateComp);
        return true;
    }
}
