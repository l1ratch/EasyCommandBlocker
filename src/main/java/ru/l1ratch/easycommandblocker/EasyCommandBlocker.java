package ru.l1ratch.easycommandblocker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

public class EasyCommandBlocker extends JavaPlugin implements Listener, TabCompleter, CommandExecutor {

    private class BlockedCommand {
        String command;
        String message;
        boolean hideFromTab;

        BlockedCommand(String command, String message, boolean hideFromTab) {
            this.command = command;
            this.message = translateColors(message);
            this.hideFromTab = hideFromTab;
        }
    }

    private Map<String, BlockedCommand> blockedCommands;
    private String defaultText;
    private String blockColonCommands;

    @Override
    public void onEnable() {
        // Сообщение о запуске плагина
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "----------------------------------------");
        getServer().getConsoleSender().sendMessage(ChatColor.AQUA + " EasyCommandBlocker " + ChatColor.YELLOW + "v" + getDescription().getVersion() + ChatColor.GREEN + " has been enabled!");
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "----------------------------------------");

        saveDefaultConfig();
        ensureDefaultFilesExist(); // Обеспечить наличие конфигурации и файла миграции

        // Регистрация команды для миграции и других команд
        PluginCommand ecbCommand = getCommand("ecb");
        if (ecbCommand != null) {
            ecbCommand.setExecutor(this);
            ecbCommand.setTabCompleter(this);
        }

        // Загрузка текста по умолчанию из конфигурации и преобразование цветов
        defaultText = translateColors(getConfig().getString("deftxt"));

        // Загрузка настройки блокировки/скрытия команд с двоеточием
        blockColonCommands = getConfig().getString("blockColonCommands", "false").toUpperCase();

        // Загрузка заблокированных команд и их сообщений
        blockedCommands = new HashMap<>();
        List<Map<?, ?>> configCommands = getConfig().getMapList("BlockCMD");
        for (Map<?, ?> entry : configCommands) {
            String cmd = entry.get("command").toString().toLowerCase();
            String message = entry.get("message").toString();
            boolean hideFromTab = entry.get("tabCompleter") != null && Boolean.parseBoolean(entry.get("tabCompleter").toString());
            blockedCommands.put(cmd, new BlockedCommand(cmd, message, hideFromTab));
        }

        getServer().getPluginManager().registerEvents(this, this);

        // Проверка зарегистрированных команд на сервере
        CommandMap commandMap = getCommandMap();
        if (commandMap != null) {
            List<String> unregisteredCommands = new ArrayList<>();
            for (String command : blockedCommands.keySet()) {
                Command registeredCommand = commandMap.getCommand(command);
                if (registeredCommand instanceof PluginCommand) {
                    ((PluginCommand) registeredCommand).setTabCompleter(this);
                } else if (blockedCommands.get(command).hideFromTab) {
                    unregisteredCommands.add(command);
                }
            }

            // Вывод списка незарегистрированных команд в консоль
            if (!unregisteredCommands.isEmpty()) {
                getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "EasyCommandBlocker: The following commands are set to be hidden in TabCompleter, but are not registered on the server:");
                for (String cmd : unregisteredCommands) {
                    getServer().getConsoleSender().sendMessage(ChatColor.RED + " - " + cmd);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        // Сообщение о выключении плагина
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "----------------------------------------");
        getServer().getConsoleSender().sendMessage(ChatColor.AQUA + " EasyCommandBlocker " + ChatColor.YELLOW + "v" + getDescription().getVersion() + ChatColor.RED + " has been disabled!");
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "----------------------------------------");
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase();
        Player player = event.getPlayer();

        // Проверка на разрешение полного обхода всех ограничений
        if (player.hasPermission("EasyCommandBlocker.bypass-all")) {
            return; // Игрок имеет право полного обхода всех блокировок
        }

        // Проверка на разрешение обхода блокировки команд с двоеточием
        if (message.contains(":")) {
            if (player.hasPermission("EasyCommandBlocker.bypass-colon")) {
                return; // Игрок имеет право обхода блокировок команд с двоеточием
            }

            switch (blockColonCommands) {
                case "BLOCK":
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(translateColors("&cЭта команда заблокирована."));
                    return;
                case "HIDE":
                    // Ничего не делаем, только скрываем команду из таб-комплитера
                    break;
                case "TRUE":
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(translateColors("&cЭта команда заблокирована."));
                    return;
                case "FALSE":
                    // Ничего не делаем, команда разрешена
                    break;
                default:
                    break;
            }
        }

        // Проверка на разрешение обхода блокировки обычных команд
        if (player.hasPermission("EasyCommandBlocker.bypass-command")) {
            return; // Игрок имеет право обхода блокировок команд
        }

        // Проверяем, начинается ли команда с любой из заблокированных команд
        for (BlockedCommand blockedCommand : blockedCommands.values()) {
            if (message.equalsIgnoreCase("/" + blockedCommand.command)) {
                event.setCancelled(true);
                if (!blockedCommand.message.isEmpty()) {
                    // Если текст сообщения "deftxt", подставляем текст по умолчанию
                    String blockMessage = blockedCommand.message.equalsIgnoreCase("deftxt")
                            ? defaultText
                            : blockedCommand.message;
                    event.getPlayer().sendMessage(blockMessage);
                }
                return;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender.hasPermission("EasyCommandBlocker.admin")) {
            if (args.length == 1) {
                return Arrays.asList("help", "reload", "migrate", "cmd");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("cmd")) {
                return Arrays.asList("add", "del", "edit");
            }
        }
        return null;
    }

    // Обработка всех команд плагина /ecb
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender.hasPermission("EasyCommandBlocker.admin"))) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Configuration reloaded.");
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("migrate")) {
            String fileName = args[1];
            File file = new File(getDataFolder(), fileName);

            if (!file.exists()) {
                sender.sendMessage(ChatColor.RED + "File " + fileName + " does not exist.");
                return true;
            }

            FileConfiguration externalConfig = YamlConfiguration.loadConfiguration(file);
            List<String> externalCommands = externalConfig.getStringList("BlockCMD");

            if (externalCommands.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "No commands found in the specified file.");
                return true;
            }

            // Миграция команд в конфигурацию плагина
            List<Map<String, Object>> migratedCommands = new ArrayList<>();
            for (String commandName : externalCommands) {
                Map<String, Object> newCommand = new HashMap<>();
                newCommand.put("command", commandName.toLowerCase());
                newCommand.put("message", "deftxt");  // Сообщение по умолчанию
                newCommand.put("tabCompleter", false); // Видимость в TabCompleter по умолчанию
                migratedCommands.add(newCommand);
            }

            getConfig().set("BlockCMD", migratedCommands);
            saveConfig();
            reloadConfig(); // Перезагрузка конфигурации после миграции

            sender.sendMessage(ChatColor.GREEN + "Commands from " + fileName + " have been migrated to the main configuration.");
            return true;
        }

        if (args.length >= 4 && args[0].equalsIgnoreCase("cmd")) {
            String subCommand = args[1];
            String cmdName = args[2];
            String blockMessage = args[3];
            boolean tabCompleter = args.length > 4 && Boolean.parseBoolean(args[4]);

            if (subCommand.equalsIgnoreCase("add")) {
                addCommand(cmdName, blockMessage, tabCompleter);
                sender.sendMessage(ChatColor.GREEN + "Command " + cmdName + " added.");
                return true;
            }

            if (subCommand.equalsIgnoreCase("del")) {
                removeCommand(cmdName);
                sender.sendMessage(ChatColor.GREEN + "Command " + cmdName + " removed.");
                return true;
            }

            if (subCommand.equalsIgnoreCase("edit")) {
                editCommand(cmdName, blockMessage, tabCompleter);
                sender.sendMessage(ChatColor.GREEN + "Command " + cmdName + " updated.");
                return true;
            }
        }

        return false;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "EasyCommandBlocker Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/ecb help" + ChatColor.WHITE + " - Show this help message.");
        sender.sendMessage(ChatColor.YELLOW + "/ecb reload" + ChatColor.WHITE + " - Reload the configuration.");
        sender.sendMessage(ChatColor.YELLOW + "/ecb migrate <file.yml>" + ChatColor.WHITE + " - Migrate commands from a specified file.");
        sender.sendMessage(ChatColor.YELLOW + "/ecb cmd add <command> <'block message'> <true/false>" + ChatColor.WHITE + " - Add a command to the block list.");
        sender.sendMessage(ChatColor.YELLOW + "/ecb cmd del <command>" + ChatColor.WHITE + " - Remove a command from the block list.");
        sender.sendMessage(ChatColor.YELLOW + "/ecb cmd edit <command> <'block message'> <true/false>" + ChatColor.WHITE + " - Edit a command in the block list.");
    }

    // Добавление команды в конфигурацию
    private void addCommand(String command, String message, boolean tabCompleter) {
        List<Map<String, Object>> configCommands = (List<Map<String, Object>>) getConfig().getList("BlockCMD");
        if (configCommands == null) configCommands = new ArrayList<>();

        Map<String, Object> newCommand = new HashMap<>();
        newCommand.put("command", command.toLowerCase());
        newCommand.put("message", message);
        newCommand.put("tabCompleter", tabCompleter);
        configCommands.add(newCommand);

        getConfig().set("BlockCMD", configCommands);
        saveConfig();
        reloadConfig();
    }

    // Удаление команды из конфигурации
    private void removeCommand(String command) {
        List<Map<String, Object>> configCommands = (List<Map<String, Object>>) getConfig().getList("BlockCMD");
        if (configCommands != null) {
            configCommands.removeIf(cmd -> cmd.get("command").equals(command.toLowerCase()));
            getConfig().set("BlockCMD", configCommands);
            saveConfig();
            reloadConfig();
        }
    }

    // Изменение команды в конфигурации
    private void editCommand(String command, String message, boolean tabCompleter) {
        List<Map<String, Object>> configCommands = (List<Map<String, Object>>) getConfig().getList("BlockCMD");
        if (configCommands != null) {
            for (Map<String, Object> cmd : configCommands) {
                if (cmd.get("command").equals(command.toLowerCase())) {
                    cmd.put("message", message);
                    cmd.put("tabCompleter", tabCompleter);
                    break;
                }
            }
            getConfig().set("BlockCMD", configCommands);
            saveConfig();
            reloadConfig();
        }
    }

    // Обеспечить наличие конфигурации и файла миграции
    private void ensureDefaultFilesExist() {
        saveDefaultConfig();

        File migrateFile = new File(getDataFolder(), "migrate.yml");
        if (!migrateFile.exists()) {
            saveResource("migrate.yml", false);
        }
    }

    // Метод для преобразования цветовых кодов
    private String translateColors(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    // Получение CommandMap для проверки зарегистрированных команд
    private CommandMap getCommandMap() {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(Bukkit.getServer());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
