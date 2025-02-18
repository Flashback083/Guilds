/*
 * MIT License
 *
 * Copyright (c) 2019 Glare
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.glaremasters.guilds.commands.codes;

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.ACFUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import me.glaremasters.guilds.configuration.sections.CodeSettings;
import me.glaremasters.guilds.exceptions.ExpectationNotMet;
import me.glaremasters.guilds.exceptions.InvalidPermissionException;
import me.glaremasters.guilds.guild.Guild;
import me.glaremasters.guilds.guild.GuildRole;
import me.glaremasters.guilds.messages.Messages;
import me.glaremasters.guilds.utils.Constants;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.entity.Player;

/**
 * Created by GlareMasters
 * Date: 4/4/2019
 * Time: 4:51 PM
 */
@CommandAlias(Constants.ROOT_ALIAS)
public class CommandCodeCreate extends BaseCommand {

    @Dependency private SettingsManager settingsManager;

    /**
     * Create an invite code for your guild
     * @param player the player creating the invite code
     * @param guild the guild the invite is being created for
     * @param role the guild role of the user
     */
    @Subcommand("code create")
    @Description("{@@descriptions.code-create}")
    @Syntax("<uses>")
    @CommandPermission(Constants.CODE_PERM + "create")
    public void execute(Player player, Guild guild, GuildRole role, @Default("1") Integer uses) {
        if (!role.isCreateCode())
            ACFUtil.sneaky(new InvalidPermissionException());

        if (guild.getActiveCheck(settingsManager.getProperty(CodeSettings.ACTIVE_CODE_AMOUNT)))
            ACFUtil.sneaky(new ExpectationNotMet(Messages.CODES__MAX));

        String code = RandomStringUtils.randomAlphabetic(settingsManager.getProperty(CodeSettings.CODE_LENGTH));

        guild.addCode(code, uses, player);

        getCurrentCommandIssuer().sendInfo(Messages.CODES__CREATED,
                "{code}", code,
                "{amount}", String.valueOf(uses));
    }
}
