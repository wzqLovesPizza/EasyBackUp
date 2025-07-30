package com.example.easybackup;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

public class EasyBackUp extends JavaPlugin {

    private BackupTask backupTask;
    private Logger log;

    @Override
    public void onEnable() {
        log = getLogger();
        saveDefaultConfig();
        int hours = getConfig().getInt("backup-interval-hours", 6);
        String targetDir = getConfig().getString("target-save-dir");
        long ticks = 20L * 60 * 60 * hours;

        // 初始化定时任务
        backupTask = new BackupTask(this);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, backupTask, 0L, ticks);
        log.info("EasyBackUp 插件已启用，每隔 " + hours + " 小时备份一次世界文件夹和 " + targetDir + " 文件夹");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("ebu") && args.length > 0 && args[0].equalsIgnoreCase("backup")) {
            if (sender.hasPermission("ebu.backup")) {
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    sender.sendMessage("§a开始手动备份 world 文件夹...");
                    backupTask.run();
                    sender.sendMessage("§a备份完成！");
                });
            } else {
                sender.sendMessage("§c你没有权限执行此命令！");
            }
            return true;
        }
        return false;
    }
}