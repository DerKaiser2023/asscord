package cc.unilock.nilcord.compat;

import cc.unilock.nilcord.NilcordPremain;
import cc.unilock.nilcord.util.TextUtils;
import cpw.mods.fml.common.Loader;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.function.BiConsumer;

/** Optional finalized-public-chat API, resolved without any Xenofactions class references. */
public final class XenofactionsChatBridge {
    private static boolean ownsChat;
    private static final BiConsumer<EntityPlayerMP, String> LISTENER = (player, message) -> {
        if (player.worldObj.isRemote || message == null || message.isEmpty()) return;
        NilcordPremain.listener.playerChatMessage(player, TextUtils.stripControlCodes(message));
    };

    private XenofactionsChatBridge() { }

    public static void init() {
        ownsChat = Loader.isModLoaded("hfr");
        if (!ownsChat) return;
        try {
            Class.forName("com.hfr.api.PublicChat").getMethod("addListener", BiConsumer.class).invoke(null, LISTENER);
            NilcordPremain.LOGGER.info("Xenofactions finalized public chat bridge enabled");
        } catch (ReflectiveOperationException | LinkageError failure) {
            // Older Xenofactions has no approved result. Never fall back to its canceled raw input.
            NilcordPremain.LOGGER.error("Xenofactions public chat API unavailable; Minecraft chat relay is disabled. Update Xenofactions.", failure);
        }
    }

    public static boolean ownsChat() {
        return ownsChat;
    }
}