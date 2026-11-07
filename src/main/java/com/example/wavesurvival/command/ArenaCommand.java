package com.example.wavesurvival.command;

import com.example.wavesurvival.WaveSurvivalMod;
import com.example.wavesurvival.wave.WaveManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * /wavearena              - построить арену 51x51 вокруг игрока (только операторы)
 * /wavearena build        - то же самое
 * /wavearena start        - запустить волны (на построенной арене или вокруг игрока)
 * /wavearena stop         - остановить волны и убрать врагов
 * /wavearena wave &lt;N&gt;     - перейти к волне N (например 10 для проверки босса)
 */
@Mod.EventBusSubscriber(modid = WaveSurvivalMod.MOD_ID)
public final class ArenaCommand {
    /** Внутренняя площадь = (2*25+1)^2 = 51x51. Стена стоит на расстоянии 26 от центра. */
    private static final int HALF = WaveManager.ARENA_HALF;
    private static final int WALL = HALF + 1;
    private static final int TOWER_OUTER = WALL + 4;
    /** Высота стен над полом. */
    private static final int WALL_HEIGHT = 10;
    private static final int TOWER_HEIGHT = WALL_HEIGHT + 4;
    private static final int CLEAR_EXTRA = 8;

    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private ArenaCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("wavearena")
                .requires(source -> source.hasPermission(2))
                .executes(context -> build(context.getSource()))
                .then(Commands.literal("build").executes(context -> build(context.getSource())))
                .then(Commands.literal("start").executes(context -> start(context.getSource())))
                .then(Commands.literal("stop").executes(context -> stop(context.getSource())))
                .then(Commands.literal("wave")
                        .then(Commands.argument("number", IntegerArgumentType.integer(1, 1000))
                                .executes(context -> setWave(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "number"))))));
    }

    private static int build(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        BlockPos floor = player.blockPosition().below();

        if (floor.getY() - 3 < level.getMinBuildHeight() || floor.getY() + TOWER_HEIGHT + CLEAR_EXTRA >= level.getMaxBuildHeight()) {
            source.sendFailure(Component.literal("Здесь не хватает высоты для арены. Встаньте ниже/выше."));
            return 0;
        }

        int placed = buildArena(level, floor);
        WaveManager.registerArena(level, floor);
        player.teleportTo(floor.getX() + 0.5D, floor.getY() + 1.0D, floor.getZ() + 0.5D);
        source.sendSuccess(() -> Component.literal("Арена 51x51 построена (" + placed + " блоков). Запуск волн: /wavearena start"), true);
        return 1;
    }

    private static int start(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        WaveManager.start(player);
        source.sendSuccess(() -> Component.literal("Волны запущены."), true);
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        WaveManager.stop(true);
        source.sendSuccess(() -> Component.literal("Волны остановлены."), true);
        return 1;
    }

    private static int setWave(CommandSourceStack source, int number) {
        if (!WaveManager.setWave(number)) {
            source.sendFailure(Component.literal("Волны не запущены. Сначала /wavearena start"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Следующая волна: " + number), true);
        return 1;
    }

    /**
     * Строит арену. floor - блок пола в центре.
     * Материалы: пол - серый/светло-серый бетон (шахматка), стены - серый бетон,
     * колонны и верхний ряд - железные блоки, освещение - морские фонари.
     */
    public static int buildArena(ServerLevel level, BlockPos floor) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState concreteDark = Blocks.GRAY_CONCRETE.defaultBlockState();
        BlockState concreteLight = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState iron = Blocks.IRON_BLOCK.defaultBlockState();
        BlockState lantern = Blocks.SEA_LANTERN.defaultBlockState();

        int cx = floor.getX();
        int cz = floor.getZ();
        int fy = floor.getY();
        int placed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -TOWER_OUTER; dx <= TOWER_OUTER; dx++) {
            for (int dz = -TOWER_OUTER; dz <= TOWER_OUTER; dz++) {
                int ax = Math.abs(dx);
                int az = Math.abs(dz);
                boolean inTower = ax >= WALL && az >= WALL;
                boolean inArena = !inTower && ax <= WALL && az <= WALL;
                if (!inTower && !inArena) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;

                // 1) очистка объёма над полом
                for (int y = fy + 1; y <= fy + TOWER_HEIGHT + CLEAR_EXTRA; y++) {
                    level.setBlock(pos.set(x, y, z), air, FLAGS);
                    placed++;
                }
                // 2) фундамент (чтобы не было дыр под полом)
                for (int y = fy - 3; y < fy; y++) {
                    level.setBlock(pos.set(x, y, z), concreteDark, FLAGS);
                    placed++;
                }

                if (inTower) {
                    placed += buildTowerColumn(level, pos, x, fy, z, ax - WALL, az - WALL, concreteDark, iron, lantern);
                    continue;
                }

                // 3) пол: шахматка + фонари каждые 8 блоков
                BlockState floorState = ((dx + dz) & 1) == 0 ? concreteDark : concreteLight;
                if (ax <= HALF && az <= HALF && dx % 8 == 0 && dz % 8 == 0) {
                    floorState = lantern;
                }
                level.setBlock(pos.set(x, fy, z), floorState, FLAGS);
                placed++;

                // 4) стены по периметру
                if (ax == WALL || az == WALL) {
                    int along = ax == WALL ? dz : dx;
                    boolean pillar = along % 8 == 0;
                    for (int y = fy + 1; y <= fy + WALL_HEIGHT; y++) {
                        BlockState wallState = concreteDark;
                        if (pillar) {
                            wallState = y == fy + 6 ? lantern : iron;
                        } else if (y == fy + WALL_HEIGHT) {
                            wallState = iron;
                        }
                        level.setBlock(pos.set(x, y, z), wallState, FLAGS);
                        placed++;
                    }
                }
            }
        }
        return placed;
    }

    /** Одна колонна угловой башни 5x5: полая коробка из железа с крышей, зубцами и фонарём. */
    private static int buildTowerColumn(ServerLevel level, BlockPos.MutableBlockPos pos, int x, int fy, int z,
                                        int tx, int tz, BlockState concrete, BlockState iron, BlockState lantern) {
        int placed = 0;
        boolean edge = tx == 0 || tx == 4 || tz == 0 || tz == 4;

        level.setBlock(pos.set(x, fy, z), concrete, FLAGS);
        placed++;

        for (int y = fy + 1; y < fy + TOWER_HEIGHT; y++) {
            if (edge) {
                level.setBlock(pos.set(x, y, z), iron, FLAGS);
                placed++;
            }
        }
        // крыша (в центре - фонарь)
        boolean centre = tx == 2 && tz == 2;
        level.setBlock(pos.set(x, fy + TOWER_HEIGHT, z), centre ? lantern : iron, FLAGS);
        placed++;
        // зубцы по краю
        if (edge && ((tx + tz) & 1) == 0) {
            level.setBlock(pos.set(x, fy + TOWER_HEIGHT + 1, z), iron, FLAGS);
            placed++;
        }
        return placed;
    }
}
