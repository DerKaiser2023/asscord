package cc.unilock.nilcord.discord;

import cc.unilock.nilcord.ChatFilter;
import cc.unilock.nilcord.util.TextUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.ForgeHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cc.unilock.nilcord.NilcordPremain.*;

public class Discord extends ListenerAdapter {
    private static final Pattern WEBHOOK_ID_REGEX = Pattern.compile("^https://discord\\.com/api/webhooks/(\\d+)/.+$");

    private final JDA jda;
    private final IncomingWebhookClient webhook;
    private final String webhookId;
    private final Map<String, String> perUserSkins;

    public Discord() {
        JDABuilder builder = JDABuilder.createDefault(CONFIG.discord.token.value())
                .addEventListeners(this)
                .setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);

        try {
            this.jda = builder.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to log into Discord!", e);
        }

        // Parse per-user skins config
        this.perUserSkins = parsePerUserSkins(CONFIG.formatting.discord.webhook.per_user_skins.value());

        if (CONFIG.discord.webhook.enabled.value()) {
            try {
                this.webhook = WebhookClient.createClient(jda, CONFIG.discord.webhook.url.value());
                Matcher matcher = WEBHOOK_ID_REGEX.matcher(CONFIG.discord.webhook.url.value());
                this.webhookId = matcher.find() ? matcher.group(1) : null;
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid webhook URL!");
            }
        } else {
            this.webhook = null;
            this.webhookId = null;
        }
    }

    private static Map<String, String> parsePerUserSkins(String config) {
        Map<String, String> map = new HashMap<>();
        if (config == null || config.trim().isEmpty()) {
            return map;
        }
        String[] pairs = config.split(";");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim().toLowerCase(Locale.ROOT), kv[1].trim());
            }
        }
        return map;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        LOGGER.info("Bot ready!");
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (server == null) return;

        if (CONFIG.formatting.minecraft.discord_message.value().isEmpty()) return;

        if (!event.isFromType(ChannelType.TEXT)) return;
        if (!event.getChannel().asTextChannel().getId().equals(CONFIG.discord.channel_id.value())) return;

        User author = event.getAuthor();
        if (!CONFIG.minecraft.show_bot_messages.value() && author.isBot()) return;
        if (author.getId().equals(this.jda.getSelfUser().getId()) || author.getId().equals(this.webhookId)) return;

        Message message = event.getMessage();
        MessageReference ref = message.getMessageReference();

        Member member = message.getMember();
        if (member == null) return;

        StringBuilder attachment_chunk = new StringBuilder(message.getContentDisplay().isEmpty() ? "" : " ");
        if (CONFIG.minecraft.show_attachments.value()) {
            for (Message.Attachment attachment : message.getAttachments()) {
                attachment_chunk.append(CONFIG.formatting.minecraft.attachment_format.value().replace("<attachment_url>", attachment.getUrl()));
            }
        }

        String reply_chunk = "";
        if (ref != null) {
            reply_chunk = TextUtils.parseDiscordReply(
                    CONFIG.formatting.minecraft.reply_format.value(),
                    ref.getMessage() == null ? ref.resolve().complete() : ref.getMessage()
            );
        }

        String msg = TextUtils.parseDiscordMessage(
                CONFIG.formatting.minecraft.discord_message.value(),
                attachment_chunk.toString(),
                reply_chunk,
                CONFIG.formatting.minecraft.username_format.value(),
                author,
                member,
                message
        );

        server.getConfigurationManager().sendChatMsg(ForgeHooks.newChatWithLinks(msg));
    }

    public void onPlayerChatMessage(EntityPlayerMP player, String message) {
        try {
            String msg = TextUtils.parseMessage(
                    CONFIG.discord.webhook.enabled.value() ? CONFIG.formatting.discord.webhook.chat_message.value() : CONFIG.formatting.discord.chat_message.value(),
                    player,
                    message
            );

            if (!CONFIG.minecraft.enable_everyone_and_here.value()) {
                msg = parseEveryoneAndHere(msg);
            }
            if (CONFIG.minecraft.enable_mentions.value()) {
                msg = parseMentions(msg);
            }

            // 🚫 Block if matches regex
            if (ChatFilter.isBlocked(message)) {
                LOGGER.debug("Chat message blocked by filter: {}", message);
                return;
            }

            sendMessageToDiscord(msg, player);
        } catch (Exception e) {
            LOGGER.error("Failed to send player chat message to Discord", e);
        }
    }



    public void sendMessageToDiscord(String message) {
        this.sendMessageToDiscord(message, null);
    }

    public void sendMessageToDiscord(String message, @Nullable EntityPlayerMP player) {
        if (!CONFIG.discord.webhook.enabled.value() || this.webhook == null || player == null) {
            sendBotMessageToDiscord(message);
        } else {
            sendWebhookMessageToDiscord(message, player);
        }
    }

    public void sendBotMessageToDiscord(String message) {
        try {
            TextChannel textChannel = this.jda.getTextChannelById(CONFIG.discord.channel_id.value());
            if (textChannel != null) {
                textChannel.sendMessage(message).queue(
                        success -> {},
                        error -> LOGGER.error("Failed to send bot message to Discord", error)
                );
            } else {
                LOGGER.error("Unable to find channel {}!", CONFIG.discord.channel_id.value());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to send bot message to Discord", e);
        }
    }

    public void sendWebhookMessageToDiscord(String message, EntityPlayerMP player) {
        try {
            String playerName = player.getGameProfile().getName();
            String avatarUrl = perUserSkins.get(playerName.toLowerCase(Locale.ROOT));
            if (avatarUrl == null || avatarUrl.isEmpty()) {
                avatarUrl = CONFIG.formatting.discord.webhook.tlauncher_avatar_url.value();
                if (avatarUrl.isEmpty()) {
                    avatarUrl = CONFIG.formatting.discord.webhook.avatar_url.value();
                }
            }
            String avatar = TextUtils.parsePlayer(avatarUrl, player);

            String username = TextUtils.parsePlayer(
                    CONFIG.formatting.discord.webhook.username.value(),
                    player
            );

            try (MessageCreateData data = new MessageCreateBuilder().setContent(message).build()) {
                webhook.sendMessage(data)
                        .setAvatarUrl(avatar)
                        .setUsername(username)
                        .queue(
                                success -> {},
                                error -> LOGGER.error("Failed to send webhook message to Discord", error)
                        );
            }
        } catch (Exception e) {
            LOGGER.error("Failed to prepare webhook message", e);
        }
    }

    private static final Pattern EVERYONE_AND_HERE_PATTERN = Pattern.compile("@(?<ping>everyone|here)");
    private String parseEveryoneAndHere(String message) {
        return EVERYONE_AND_HERE_PATTERN.matcher(message).replaceAll("@\u200B${ping}");
    }

    private String parseMentions(String message) {
        String msg = message;

        TextChannel textChannel = jda.getTextChannelById(CONFIG.discord.channel_id.value());
        if (textChannel != null) {
            for (Member member : textChannel.getMembers()) {
                message = Pattern.compile(Pattern.quote("@" + member.getUser().getName()), Pattern.CASE_INSENSITIVE).matcher(msg).replaceAll(member.getAsMention());
            }
        }

        return message;
    }

    public JDA getJda() {
        return this.jda;
    }

    public void shutdown() {
        this.jda.removeEventListener(this);
        this.jda.shutdown();
    }
}
