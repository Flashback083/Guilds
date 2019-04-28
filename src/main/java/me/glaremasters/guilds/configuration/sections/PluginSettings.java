/*
 * MIT License
 *
 * Copyright (c) 2018 Glare
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

package me.glaremasters.guilds.configuration.sections;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

/**
 * Created by GlareMasters
 * Date: 1/17/2019
 * Time: 2:29 PM
 */
public final class PluginSettings implements SettingsHolder {

    @Comment({"This is used for the Guild's Announcement System, which allow me (The Author) to communicate to you guys without updating.",
            "The way this works is very simple. If you have \"console\" set to \"true\", you will see the announcement when the server starts.",
            "If you have \"in-game\" set to \"true\", your OPed players will see it the first time they login to the server."
    })
    public static final Property<Boolean> ANNOUNCEMENTS_CONSOLE =
            newProperty("settings.announcements.console", true);

    public static final Property<Boolean> ANNOUNCEMENTS_IN_GAME =
            newProperty("settings.announcements.in-game", true);

    @Comment({"Choosing your language for the plugin couldn't be easier! The default language is english.",
            "If you speak another language but don't see it here, feel free to submit it via one of the links above to have it added to the plugin.",
            "If you try and use a different language than any in the list above, the plugin will not function in a normal manner.",
            "As you can see this is currently en-US, and there is a en-US.yml file in the language folder.",
            "If I wanted to switch to french, I would use fr-FR as the language instead."
    })
    public static final Property<String> MESSAGES_LANGUAGE =
            newProperty("settings.messagesLanguage", "en-US");

    @Comment({"Would you like to allow admin players to update the languages via command?",
    "If yes, set to true, and they will be able to run /guild admin update-languages",
    "If no, set to false, and the command will only run via console."})
    public static final Property<Boolean> UPDATE_LANGUAGES =
            newProperty("settings.player-update-languages", false);

    @Comment("Would you like to check for plugin updates on startup? It's highly suggested you keep this enabled!")
    public static final Property<Boolean> UPDATE_CHECK =
            newProperty("settings.update-check", true);

    @Comment("Which type or storage would you like to use? MySQL or JSON?")
    public static final Property<String> STORAGE_TYPE =
            newProperty("settings.storage-type", "json");

    @Comment("What is the IP / Host for the database?")
    public static final Property<String> SQL_HOST =
            newProperty("settings.mysql.host", "");

    @Comment("What is the username for the database?")
    public static final Property<String> SQL_USER =
            newProperty("settings.mysql.user", "");

    @Comment("What is the password for the database?")
    public static final Property<String> SQL_PASS =
            newProperty("settings.mysql.pass", "");

    @Comment("What is the name of the database?")
    public static final Property<String> SQL_DB =
            newProperty("settings.mysql.database", "");

    private PluginSettings() {
    }

    @Override
    public void registerComments(CommentsConfiguration conf) {
        String[] pluginHeader = {
                "Guilds",
                "Creator: Glare",
                "Contributors: https://github.com/guilds-plugin/Guilds/graphs/contributors",
                "Issues: https://github.com/guilds-plugin/Guilds/issues",
                "Spigot: https://www.spigotmc.org/resources/guilds.48920/",
                "Wiki: https://wiki.glaremasters.me/",
                "Discord: https://glaremasters.me/discord"
        };
        conf.setComment("settings", pluginHeader);
    }
}