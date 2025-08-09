package io.github.wzqLovesPizza.easybackup;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;
import java.util.List;

public class EasyBackUp extends JavaPlugin {

    private BackupTask backupTask;
    private Logger log;

    @Override
    public void onEnable() {
        log = getLogger();
        saveDefaultConfig();
        int hours = getConfig().getInt("backup-interval-hours", 6);
        List<String> targetDir = getConfig().getStringList("target-save-dir");
        long ticks = 20L * 60 * 60 * hours;

        // 初始化定时任务
        if (hours != 0) {
            backupTask = new BackupTask(this);
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, backupTask, 0L, ticks);
            log.info("EasyBackUp 插件已启用，每隔 " + hours + " 小时备份一次 " + targetDir + " 文件夹");
        } else {
            log.warning("EasyBackUp 插件已启用，但备份已关闭, 如需启用备份，请将配置文件的backup-interval-hours设置为0以外的值");
        }

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