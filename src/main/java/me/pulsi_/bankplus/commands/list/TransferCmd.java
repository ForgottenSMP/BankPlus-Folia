package me.pulsi_.bankplus.commands.list;

import me.pulsi_.bankplus.BankPlus;
import me.pulsi_.bankplus.commands.BPCmdExecution;
import me.pulsi_.bankplus.commands.BPCommand;
import me.pulsi_.bankplus.economy.BPEconomy;
import me.pulsi_.bankplus.sql.BPSQL;
import me.pulsi_.bankplus.utils.texts.BPArgs;
import me.pulsi_.bankplus.utils.texts.BPFormatter;
import me.pulsi_.bankplus.utils.texts.BPMessages;
import me.pulsi_.bankplus.values.ConfigValues;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TransferCmd extends BPCommand {

    public TransferCmd(FileConfiguration commandsConfig, String commandID) {
        super(commandsConfig, commandID);
    }

    public TransferCmd(FileConfiguration commandsConfig, String commandID, String... aliases) {
        super(commandsConfig, commandID, aliases);
    }

    @Override
    public List<String> defaultUsage() {
        return Arrays.asList(
                "%prefix% Usage: /bank transfer [mode]",
                "Specify a mode between <aqua>\"filesToDatabase\"</aqua> and <aqua>\"databaseToFiles\"</aqua>.",
                "Use this command to transfer the playerdata from a place to another in case you switch saving mode."
        );
    }

    @Override
    public int defaultConfirmCooldown() {
        return 5;
    }

    @Override
    public List<String> defaultConfirmMessage() {
        return Collections.singletonList("%prefix% <red>This command will overwrite the data from a place to another, type the command again within 5 seconds to confirm.");
    }

    @Override
    public int defaultCooldown() {
        return 0;
    }

    @Override
    public List<String> defaultCooldownMessage() {
        return Collections.emptyList();
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public boolean skipUsage() {
        return false;
    }

    @Override
    public BPCmdExecution onExecution(CommandSender s, String[] args) {
        String mode = args[1].toLowerCase();

        if (!mode.equals("filestodatabase") && !mode.equals("databasetofiles")) {
            BPMessages.sendIdentifier(s, "Invalid-Action");
            return BPCmdExecution.invalidExecution();
        }

        if (!ConfigValues.isMySqlEnabled()) {
            BPMessages.sendMessage(s, "%prefix% <red>Could not initialize the task, MySQL hasn't been enabled in the config file!");
            return BPCmdExecution.invalidExecution();
        }

        return new BPCmdExecution() {
            @Override
            public void execute() {
                BPMessages.sendMessage(s, "%prefix% Task initialized, wait a few moments...");

                Bukkit.getAsyncScheduler().runNow(BankPlus.INSTANCE(), (task) -> {
                    localToExternal();

                    BPMessages.sendMessage(s, "%prefix% Task finished!");
                });
            }
        };
    }

    @Override
    public List<String> tabCompletion(CommandSender s, String[] args) {
        if (args.length == 2)
            return BPArgs.getArgs(args, "databaseToFiles", "filesToDatabase");
        return null;
    }

    private void localToExternal() {
        List<BPEconomy> economies = BPEconomy.list();
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            BankPlus.INSTANCE().getLogger().info("Transferring data of " + p.getName() + "...");
            for (BPEconomy economy : economies) {
                BankPlus.INSTANCE().getLogger().info("-- Transferring " + economy.getOriginBank().getIdentifier());
                YamlConfiguration playerFile = YamlConfiguration.loadConfiguration(
                        BankPlus.INSTANCE().getDataFolder().toPath().resolve("playerdata").resolve(p.getUniqueId() + ".yml").toFile()
                );
                BPSQL.savePlayer(
                        p,
                        economy,
                        playerFile
                );
            }
            BankPlus.INSTANCE().getLogger().info("Finished transferring data of " + p.getName() + "!");
        }
    }
}