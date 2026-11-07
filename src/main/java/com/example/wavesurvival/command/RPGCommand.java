package com.example.wavesurvival.command;

import com.example.wavesurvival.WaveSurvivalMod;
import com.example.wavesurvival.rpg.RPGManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * /rpg                - показать очки и цены
 * /rpg weapon|armor|sharpness|knockback|protection|resistance|speed|health - купить следующий уровень
 * /rpg scroll         - купить Свиток страха
 */
@Mod.EventBusSubscriber(modid = WaveSurvivalMod.MOD_ID)
public final class RPGCommand {

    private RPGCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("rpg")
                .executes(context -> stats(context.getSource()));

        for (RPGManager.Upgrade upgrade : RPGManager.Upgrade.values()) {
            root.then(Commands.literal(upgrade.getKey())
                    .executes(context -> upgrade(context.getSource(), upgrade)));
        }
        root.then(Commands.literal("scroll").executes(context -> scroll(context.getSource())));
        dispatcher.register(root);
    }

    private static int stats(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.sendSystemMessage(RPGManager.describeStats(player));
        return 1;
    }

    private static int upgrade(CommandSourceStack source, RPGManager.Upgrade upgrade) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.sendSystemMessage(RPGManager.tryUpgrade(player, upgrade));
        return 1;
    }

    private static int scroll(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.sendSystemMessage(RPGManager.tryBuyScroll(player));
        return 1;
    }
}
