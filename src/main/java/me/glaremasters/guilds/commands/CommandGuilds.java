package me.glaremasters.guilds.commands;

import co.aikar.commands.ACFBukkitUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import me.glaremasters.guilds.Guilds;
import me.glaremasters.guilds.api.events.GuildCreateEvent;
import me.glaremasters.guilds.api.events.GuildLeaveEvent;
import me.glaremasters.guilds.api.events.GuildRemoveEvent;
import me.glaremasters.guilds.guild.Guild;
import me.glaremasters.guilds.guild.GuildMember;
import me.glaremasters.guilds.guild.GuildRole;
import me.glaremasters.guilds.messages.Messages;
import me.glaremasters.guilds.updater.SpigotUpdater;
import me.glaremasters.guilds.utils.ConfirmAction;
import me.glaremasters.guilds.utils.Serialization;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.PluginDescriptionFile;

import static me.glaremasters.guilds.utils.ConfigUtils.color;
import static me.glaremasters.guilds.utils.ConfigUtils.getInt;

/**
 * Created by GlareMasters
 * Date: 9/9/2018
 * Time: 4:57 PM
 */
@CommandAlias("guild|guilds")
public class CommandGuilds extends BaseCommand {

    @Dependency private Guilds guilds;

    @Subcommand("create")
    @Description("{@@descriptions.create}")
    @CommandPermission("guilds.command.create")
    @Syntax("<name>")
    public void onCreate(Player player, String name) {
        if (Guild.getGuild(player.getUniqueId()) != null) {
            getCurrentCommandIssuer().sendInfo(Messages.ERROR__ALREADY_IN_GUILD);
            return;
        }

        for (String guildName : guilds.getGuildHandler().getGuilds().keySet()) {
            if (guildName.equalsIgnoreCase(name)) {
                getCurrentCommandIssuer().sendInfo(Messages.CREATE__GUILD_NAME_TAKEN);
                return;
            }
        }
        getCurrentCommandIssuer().sendInfo(Messages.CREATE__WARNING);

        guilds.getActionHandler().addAction(player, new ConfirmAction() {
            @Override
            public void accept() {
                Guild guild = new Guild(color(name), player.getUniqueId());
                GuildCreateEvent event = new GuildCreateEvent(player, guild);
                if (event.isCancelled()) return;
                guilds.getDatabase().createGuild(guild);
                getCurrentCommandIssuer().sendInfo(Messages.CREATE__SUCCESSFUL, "{guild}", guild.getName());
                guilds.getActionHandler().removeAction(player);
            }

            @Override
            public void decline() {
                getCurrentCommandIssuer().sendInfo(Messages.CREATE__CANCELLED);
                guilds.getActionHandler().removeAction(player);
            }
        });
    }

    @Subcommand("confirm")
    @Description("{@@descriptions.confirm}")
    @CommandPermission("guilds.command.confirm")
    public void onConfirm(Player player) {
        ConfirmAction action = guilds.getActionHandler().getActions().get(player);
        if (action != null) action.accept();
    }

    @Subcommand("cancel")
    @Description("{@@descriptions.cancel}")
    @CommandPermission("guilds.command.cancel")
    public void onCancel(Player player) {
        ConfirmAction action = guilds.getActionHandler().getActions().get(player);
        if (action != null) action.decline();
    }

    @Subcommand("reload")
    @Description("{@@descriptions.reload}")
    @CommandPermission("guilds.command.reload")
    public void onReload(CommandSender sender) {
        sender.sendMessage("Reloading config");
        guilds.reloadConfig();
    }

    @Subcommand("sethome")
    @Description("{@@descriptions.sethome}")
    @CommandPermission("guilds.command.sethome")
    public void onSetHome(Player player, Guild guild, GuildRole role) {
        if (!role.canChangeHome()) {
            getCurrentCommandIssuer().sendInfo(Messages.ERROR__ROLE_NO_PERMISSION);
            return;
        }
        guild.updateHome(ACFBukkitUtil.fullLocationToString(player.getLocation()));
    }

    @Subcommand("home")
    @Description("{@@descriptions.home}")
    @CommandPermission("guilds.command.home")
    public void onHome(Player player, Guild guild) {
        if (guild.getHome().equals("")) return;
        player.teleport(ACFBukkitUtil.stringToLocation(guild.getHome()));
    }

    @Subcommand("rename")
    @Description("{@@descriptions.rename}")
    @CommandPermission("guilds.command.rename")
    @Syntax("<name>")
    public void onRename(Player player, Guild guild, GuildRole role, String name) {
        if (!role.canChangeName()) {
            getCurrentCommandIssuer().sendInfo(Messages.ERROR__ROLE_NO_PERMISSION);
            return;
        }
        String oldName = guild.getName();
        guilds.getDatabase().removeGuild(Guild.getGuild(oldName));
        guild.updateName(color(name));
    }

    @Subcommand("status")
    @Description("{@@descriptions.status}")
    @CommandPermission("guilds.command.status")
    @Syntax("<private/public>")
    public void onStatus(Player player, Guild guild, GuildRole role, String status) {
        if (!role.canChangeStatus()) {
            getCurrentCommandIssuer().sendInfo(Messages.ERROR__ROLE_NO_PERMISSION);
            return;
        }
        boolean argCheck = !status.equalsIgnoreCase("private") && !status.equalsIgnoreCase("public");
        if (argCheck) return;
        String updatedStatus = StringUtils.capitalize(status);
        guild.updateStatus(updatedStatus);
    }

    @Subcommand("prefix")
    @Description("{@@descriptions.prefix}")
    @CommandPermission("guilds.command.prefix")
    @Syntax("<prefix>")
    public void onPrefix(Player player, Guild guild, GuildRole role, String prefix) {
        if (!role.canChangePrefix()) {
            getCurrentCommandIssuer().sendInfo(Messages.ERROR__ROLE_NO_PERMISSION);
            return;
        }
        guild.updatePrefix(color(prefix));
    }

    @Subcommand("version|v|ver")
    @Description("{@@descriptions.version}")
    public void onVersion(CommandSender sender) {
        SpigotUpdater updater = new SpigotUpdater(guilds, 48920);
        PluginDescriptionFile pdf = guilds.getDescription();
        try {
            String message;
            if (updater.getLatestVersion().equalsIgnoreCase(pdf.getVersion())) {
                message = "";
            } else {
                message = "\n&8» &7An update has been found! &f- " + updater.getResourceURL();
            }
            sender.sendMessage(
                    color("&8&m--------------------------------------------------"
                            + "\n&8» &7Name - &a"
                            + pdf.getName() + "\n&8» &7Version - &a" + pdf.getVersion()
                            + "\n&8» &7Author - &a" + pdf.getAuthors() + "\n&8» &7Support - &a"
                            + pdf.getWebsite() + message
                            + "\n&8&m--------------------------------------------------"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Subcommand("upgrade")
    @Description("{@@descriptions.upgrade}")
    @CommandPermission("guilds.command.upgrade")
    public void onUpgrade(Player player, Guild guild, GuildRole role) {
        if (!role.canUpgradeGuild()) {
            getCurrentCommandIssuer().sendInfo(Messages.ERROR__ROLE_NO_PERMISSION);
            return;
        }
        int tier = guild.getTier();
        if (tier >= getInt("max-number-of-tiers")) return;
        if (guild.getMembersToRankup() != 0 && guild.getMembers().size() < guild.getMembersToRankup()) return;
        double balance = guild.getBalance();
        double upgradeCost = guild.getTierCost();
        if (balance < upgradeCost) return;
        guilds.getActionHandler().addAction(player, new ConfirmAction() {
            @Override
            public void accept() {
                if (balance < upgradeCost) return;
                // Carry over perms check
                guild.updateTier(tier + 1);
                // Add new perms
            }

            @Override
            public void decline() {
                guilds.getActionHandler().removeAction(player);
            }
        });
    }

    @Subcommand("transfer")
    @Description("{@@descriptions.transfer}")
    @CommandPermission("guilds.command.transfer")
    @Syntax("<player>")
    public void onTransfer(Player player, Guild guild, GuildRole role, String target) {
        if (!role.canTransfer()) {
            getCurrentCommandIssuer().sendInfo(Messages.ERROR__ROLE_NO_PERMISSION);
            return;
        }
        Player transferPlayer = Bukkit.getPlayerExact(target);
        if (transferPlayer == null) return;

        GuildMember oldMaster = guild.getMember(player.getUniqueId());
        GuildMember newMaster = guild.getMember(transferPlayer.getUniqueId());

        if (newMaster == null) return;
        if (newMaster.getRole() != 1) return;

        GuildRole newRole, oldRole;
        int currentLevel = newMaster.getRole();
        newRole = GuildRole.getRole(currentLevel - 1);
        oldRole = GuildRole.getRole(currentLevel + 1);

        if (oldMaster.getRole() == 0) {
            oldMaster.setRole(oldRole);
            newMaster.setRole(newRole);
            guild.updateGuild("", guild.getName(), Guild.getGuild(guild.getName()).getName());
        }

    }

    @Subcommand("leave|exit")
    @Description("{@@descriptions.transfer}")
    @CommandPermission("guilds.command.leave")
    public void onLeave(Player player, Guild guild) {

        if (guild.getGuildMaster().getUniqueId().equals(player.getUniqueId())) {
            getCurrentCommandIssuer().sendInfo(Messages.LEAVE__WARNING_GUILDMASTER);
        } else {
            getCurrentCommandIssuer().sendInfo(Messages.LEAVE__WARNING);
        }

        guilds.getActionHandler().addAction(player, new ConfirmAction() {
            @Override
            public void accept() {
                GuildLeaveEvent event = new GuildLeaveEvent(player, guild);
                guilds.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) return;
                // If guild master remove perms from all members
                if (guild.getGuildMaster().getUniqueId().equals(player.getUniqueId())) {
                    GuildRemoveEvent removeEvent = new GuildRemoveEvent(player, guild, GuildRemoveEvent.RemoveCause.REMOVED);
                    guilds.getServer().getPluginManager().callEvent(removeEvent);
                    if (removeEvent.isCancelled()) return;
                    guilds.getDatabase().removeGuild(guild);
                    guild.removeMember(player.getUniqueId());
                    getCurrentCommandIssuer().sendInfo(Messages.LEAVE__SUCCESSFUL);
                    guilds.getActionHandler().removeAction(player);
                } else {
                    // Send message player left guild
                }
            }

            @Override
            public void decline() {
                getCurrentCommandIssuer().sendInfo(Messages.LEAVE__CANCELLED);
                guilds.getActionHandler().removeAction(player);
            }
        });
    }

    @Subcommand("delete")
    @Description("{@@descriptions.transfer}")
    @CommandPermission("guilds.command.delete")
    public void onDelete(Player player, Guild guild, GuildRole role) {
        if (!role.canRemoveGuild()) {
            getCurrentCommandIssuer().sendInfo(Messages.ERROR__ROLE_NO_PERMISSION);
            return;
        }
        getCurrentCommandIssuer().sendInfo(Messages.DELETE__WARNING);
        guilds.getActionHandler().addAction(player, new ConfirmAction() {
            @Override
            public void accept() {
                GuildRemoveEvent event = new GuildRemoveEvent(player, guild, GuildRemoveEvent.RemoveCause.REMOVED);
                guilds.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) return;

                getCurrentCommandIssuer().sendInfo(Messages.DELETE__SUCCESS, "{guild}", guild.getName());
                // Todo - Something about perms
                guilds.getDatabase().removeGuild(guild);
                guilds.getActionHandler().removeAction(player);
            }

            @Override
            public void decline() {
                getCurrentCommandIssuer().sendInfo(Messages.DELETE__CANCELLED);
                guilds.getActionHandler().removeAction(player);
            }
        });
    }

    @Subcommand("decline")
    @Description("{@@descriptions.decline}")
    @CommandPermission("guilds.command.decline")
    @Syntax("<guild name>")
    public void onDecline(Player player, String name) {
        Guild guild = Guild.getGuild(name);
        if (Guild.getGuild(player.getUniqueId()) != null) return;
        if (guild == null) return;
        if (!guild.getInvitedMembers().contains(player.getUniqueId())) return;
        getCurrentCommandIssuer().sendInfo(Messages.DECLINE__SUCCESS);
        guild.removeInvitedPlayer(player.getUniqueId());
    }

    @Subcommand("boot|kick")
    @Description("Kick someone from your Guild")
    @CommandPermission("guilds.command.boot")
    @Syntax("<name>")
    public void onKick(Player player, Guild guild, GuildRole role, String name) {
        if (!role.canKick()) {
            getCurrentCommandIssuer().sendInfo(Messages.ERROR__ROLE_NO_PERMISSION);
            return;
        }
        OfflinePlayer bootedPlayer = Bukkit.getOfflinePlayer(name);
        if (bootedPlayer == null) return;
        if (bootedPlayer.getUniqueId() == null) return;

        GuildMember kickedPlayer = guild.getMember(bootedPlayer.getUniqueId());
        if (kickedPlayer == null) {
            getCurrentCommandIssuer().sendInfo(Messages.ERROR__PLAYER_NOT_FOUND, "{player}", name);
            return;
        }

        Guild targetGuild = Guild.getGuild(kickedPlayer.getUniqueId());
        if (targetGuild == null) return;
        if (!guild.getName().equals(targetGuild.getName())) return;

        if (kickedPlayer.equals(guild.getGuildMaster())) {
            getCurrentCommandIssuer().sendInfo(Messages.ERROR__ROLE_NO_PERMISSION);
            return;
        }
        guild.removeMember(kickedPlayer.getUniqueId());
        getCurrentCommandIssuer().sendInfo(Messages.BOOT__SUCCESSFUL, "{player}", bootedPlayer.getName());
        // Todo - Send message to all online members saying <user> was kicked.
        // Todo - Send message to user if they are online that they were kicked by <user>
    }

    @Subcommand("vault")
    @Description("{@@descriptions.vault}")
    @CommandPermission("guilds.command.vault")
    public void onVault(Player player, Guild guild, GuildRole role) {
        if (!role.canOpenVault()) {
            getCurrentCommandIssuer().sendInfo(Messages.ERROR__ROLE_NO_PERMISSION);
            return;
        }
        if (guild.getInventory().equalsIgnoreCase("")) {
            Inventory inv = Bukkit.createInventory(null, 54, guild.getName() + "'s Guild Vault");
            player.openInventory(inv);
            return;
        }
        try {
            player.openInventory(Serialization.deserializeInventory(guild.getInventory()));
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    @HelpCommand
    @CommandPermission("guilds.command.help")
    @Syntax("")
    @Description("{@@descriptions.help}")
    public static void onHelp(CommandHelp help) {
        help.showHelp();
    }


}
