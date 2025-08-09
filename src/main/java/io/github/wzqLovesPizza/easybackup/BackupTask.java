package io.github.wzqLovesPizza.easybackup;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class BackupTask implements Runnable {

    private final JavaPlugin plugin;
    private final Map<File, File> backupMVFolderHash;
    private final FileConfiguration config;


    public BackupTask(JavaPlugin plugin) {
        this.plugin = plugin;
        this.backupMVFolderHash = new HashMap<>();
        this.config = plugin.getConfig();
        File serverRoot = plugin.getDataFolder().getParentFile().getParentFile();

        List<String> mvFolderList = config.getStringList("target-save-dir");
        if (!mvFolderList.isEmpty()) {
            for (String mvFolder : mvFolderList) {
                File worldMVFolder = new File(serverRoot, mvFolder);
                if (worldMVFolder.exists()) {
                    File backupMVFolder = new File(plugin.getDataFolder(), "BackupWorldsOf" + mvFolder);
                    if (!backupMVFolder.exists()) backupMVFolder.mkdirs();
                    backupMVFolderHash.put(worldMVFolder, backupMVFolder);

                } else {
                    plugin.getLogger().warning("在服务器根路径找不到 " + mvFolder + " 文件夹");
                }
            }
        } else {
            plugin.getLogger().warning("配置文件target-save-dir值为空，无需另外备份文件，请确认");
        }
    }


    @Override
    public void run() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        if (!backupMVFolderHash.isEmpty()) {

            boolean ifBroadcastMessage = config.getBoolean("notify-players", true);
            if (ifBroadcastMessage) {
                String startMessage = ChatColor.translateAlternateColorCodes('&', "&a[EasyBackUp] &3正在备份，可能引起短时间卡顿.");
                Bukkit.broadcastMessage(startMessage);
            }

            // 开始根据哈希来将文件夹备份到相应的备份文件夹
            for (Map.Entry<File, File> entry : backupMVFolderHash.entrySet()) {
                File worldDir = entry.getKey();
                File backupMVDir = entry.getValue();
                File zipFile = new File(backupMVDir, worldDir.getName() + "_" + timestamp + ".zip");
                try{
                    zipFolder(worldDir, zipFile, Arrays.asList("plugins", backupMVDir.getName()));
                } catch (Exception e) {
                    plugin.getLogger().severe(worldDir.getName() + " 文件夹备份失败: " + e.getMessage());
                    e.printStackTrace();
                }
                plugin.getLogger().info("已成功备份文件夹 " + worldDir.getName() + " 为 " + zipFile.getName());
                cleanOldBackups(backupMVDir);
            }
            if (ifBroadcastMessage) {
                String endMessage = ChatColor.translateAlternateColorCodes('&', "&a[EasyBackUp] &3备份成功.");
                Bukkit.broadcastMessage(endMessage);
            }
        }
    }

    private void zipFolder(File sourceFolder, File zipFile, List<String> excludeDirs) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zipFiles(sourceFolder, sourceFolder, zos, excludeDirs);
        }
    }

    private void zipFiles(File rootFolder, File currentFile, ZipOutputStream zos, List<String> excludeDirs) throws IOException {
        if (excludeDirs.contains(currentFile.getName())) return;

        if (currentFile.isDirectory()) {
            for (File file : Objects.requireNonNull(currentFile.listFiles())) {
                zipFiles(rootFolder, file, zos, excludeDirs);
            }
        } else {
            String entryName = rootFolder.toURI().relativize(currentFile.toURI()).getPath();
            try {
                zos.putNextEntry(new ZipEntry(entryName));
                try (FileInputStream fis = new FileInputStream(currentFile)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            } catch (IOException e) {
                // 捕获并记录文件锁定错误，但继续备份其他文件,session.lock跳过，不影响
                if (!currentFile.getName().equals("session.lock")) {
                    plugin.getLogger().warning("跳过文件 " + currentFile.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    private void cleanOldBackups(File BACKUPFOLDER) {
        File[] files = BACKUPFOLDER.listFiles((dir, name) -> name.endsWith(".zip"));

        int maxBackups = config.getInt("max-backups");

        if (files != null && files.length > maxBackups) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
            for (int i = 0; i < files.length - maxBackups; i++) {
                if (!files[i].delete()) {
                    plugin.getLogger().warning("无法删除旧备份：" + files[i].getName());
                    continue;
                }
                plugin.getLogger().info("已删除旧备份：" + files[i].getName());
            }
        }
    }
}
