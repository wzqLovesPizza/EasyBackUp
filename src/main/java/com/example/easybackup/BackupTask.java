package com.example.easybackup;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupTask implements Runnable {

    private final JavaPlugin plugin;
    private final File worldFolder;
    private final File backupFolder;
    private final File worldMVFolder;
    private final File backupMVFolder;
    private final String mvFolderName;


    public BackupTask(JavaPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        File serverRoot = plugin.getDataFolder().getParentFile().getParentFile();
        this.worldFolder = new File(serverRoot, "world");
        this.backupFolder = new File(plugin.getDataFolder().getParentFile(), "BackupWorlds");

        if (!backupFolder.exists()) backupFolder.mkdirs();

        this.mvFolderName = config.getString("target-save-dir");
        if (mvFolderName != null  && !mvFolderName.isEmpty()) {
            this.worldMVFolder = new File(serverRoot, mvFolderName);
            this.backupMVFolder = new File(plugin.getDataFolder().getParentFile(), "BackupMVWorlds");

            if (!backupMVFolder.exists()) backupMVFolder.mkdirs();
        } else {
            this.worldMVFolder = null;
            this.backupMVFolder = null;
        }
    }

    @Override
    public void run() {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File zipFile = new File(backupFolder, "worlds_" + timestamp + ".zip");
            zipFolder(worldFolder, zipFile, Arrays.asList("plugins", "BackupWorlds"));

            cleanOldBackups(backupFolder);

            plugin.getLogger().info("已成功备份 worlds 文件夹为：" + zipFile.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("备份失败: " + e.getMessage());
            e.printStackTrace();
        }
        if (worldMVFolder != null && backupMVFolder != null) {
            try {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                File zipFile2 = new File(backupMVFolder, mvFolderName + "_" + timestamp + ".zip");
                zipFolder(worldMVFolder, zipFile2, Arrays.asList("plugins", "BackupMVWorlds"));

                cleanOldBackups(backupMVFolder);

                plugin.getLogger().info("已成功备份 " + mvFolderName + " 文件夹为：" + zipFile2.getName());
            } catch (Exception e) {
                plugin.getLogger().severe("备份失败: " + e.getMessage());
                e.printStackTrace();
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
                // 捕获并记录文件锁定错误，但继续备份其他文件
                plugin.getLogger().warning("跳过文件 " + currentFile.getName() + ": " + e.getMessage());
            }
        }
    }

    private void cleanOldBackups(File BACKUPFOLDER) {
        File[] files = BACKUPFOLDER.listFiles((dir, name) -> name.endsWith(".zip"));
        if (files != null && files.length > 10) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
            for (int i = 0; i < files.length - 10; i++) {
                files[i].delete();
                plugin.getLogger().info("已删除旧备份：" + files[i].getName());
            }
        }
    }
}
