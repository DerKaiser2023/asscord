package cc.unilock.nilcord.util;

import cc.unilock.nilcord.mixin.early.minecraft.AchievementAccessor;
import cc.unilock.nilcord.mixin.early.minecraft.StatBaseAccessor;
import cc.unilock.nilcord.util.LangFileLoader;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.stats.Achievement;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;

import java.util.regex.Pattern;

public class TextUtils {
    private static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile('§' + "[0-9A-FK-OR]", Pattern.CASE_INSENSITIVE);

    public static String parseDiscordMessage(String template, String attachmentChunk, String replyChunk, String usernameChunk, User author, Member member, Message message) {
        return template
                .replace("<attachment_format>", attachmentChunk)
                .replace("<username_format>", usernameChunk)
                .replace("<role_color>", ColorUtils.getHexColor(member))
                .replace("<message_url>", message.getJumpUrl())
                .replace("<reply_format>", replyChunk)
                .replace("<username>", author.getName())
                .replace("<nickname>", member.getEffectiveName())
                .replace("<message>", message.getContentDisplay());
    }

    public static String parseDiscordReply(String template, Message refMessage) {
        User refAuthor = refMessage.getAuthor();
        Member refMember = refMessage.getMember();

        return template
                .replace("<reply_role_color>", refMember == null ? ColorUtils.WHITE : ColorUtils.getHexColor(refMember))
                .replace("<reply_url>", refMessage.getJumpUrl())
                .replace("<reply_username>", refAuthor.getName())
                .replace("<reply_nickname>", refMember == null ? refAuthor.getEffectiveName() : refMember.getEffectiveName())
                .replace("<reply_message>", refMessage.getContentDisplay());
    }

    public static String parsePlayer(String template, EntityPlayerMP player) {
        return template
                .replace("<displayname>", stripControlCodes(player.getDisplayName()))
                .replace("<username>", player.getGameProfile().getName())
                .replace("<uuid>", player.getGameProfile().getId().toString());
    }

    public static String parseMessage(String template, EntityPlayerMP player, String message) {
        return parsePlayer(template, player)
                .replace("<message>", message);
    }

    public static String parseAdvancement(String template, EntityPlayerMP player, Achievement achievement) {
        String titleKey = achievement.statId;
        String modId = extractModIdFromStatId(titleKey);
        String title = LangFileLoader.translate(titleKey, modId);

        String descriptionKey = ((AchievementAccessor) achievement).getAchievementDescription();
        String description = LangFileLoader.translate(descriptionKey, modId);

        if (achievement.statId.equals("achievement.openInventory")) {
            description = description.replace("%1$s", "E");
        }

        return parsePlayer(template, player)
                .replace("<achievement_title>", title)
                .replace("<achievement_description>", description);
    }

    private static String extractModIdFromStatId(String statId) {
        if (statId == null || statId.isEmpty()) {
            return null;
        }
        
        // Common patterns:
        // "achievement.modid.name" -> extract "modid"
        // "modid.achievement.name" -> extract "modid"
        // "modid.name" -> extract "modid"
        
        String[] parts = statId.split("\\.");
        if (parts.length >= 2) {
            // If it starts with "achievement.", the mod ID is the second part
            if ("achievement".equals(parts[0]) && parts.length >= 3) {
                return parts[1];
            }
            // Otherwise, the first part might be the mod ID
            return parts[0];
        }
        
        return null;
    }

    public static String parseDeath(String template, EntityPlayerMP player, DamageSource source) {
        return parsePlayer(template, player)
                .replace("<death_message>", source.func_151519_b(player).getUnformattedTextForChat());
    }

    /** Preserve the usual Nilcord template without ignoring edits to the final Forge component. */
    public static String chatBody(ChatComponentTranslation component) {
        Object[] args = component.getFormatArgs();
        if ("chat.type.text".equals(component.getKey()) && args.length == 2 && component.getSiblings().isEmpty()) {
            Object body = args[1];
            return body == null ? "" : stripControlCodes(body instanceof IChatComponent
                    ? ((IChatComponent) body).getUnformattedText() : body.toString());
        }
        return stripControlCodes(component.getUnformattedText());
    }

    public static String stripControlCodes(String text) {
        return FORMATTING_CODE_PATTERN.matcher(text).replaceAll("");
    }
}
