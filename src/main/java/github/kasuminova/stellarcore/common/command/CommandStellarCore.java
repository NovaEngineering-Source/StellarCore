package github.kasuminova.stellarcore.common.command;

import github.kasuminova.stellarcore.common.itemstack.ItemStackCapInitializer;
import github.kasuminova.stellarcore.common.mod.Mods;
import github.kasuminova.stellarcore.common.util.NumberUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommandStellarCore extends CommandBase {

    public static final CommandStellarCore INSTANCE = new CommandStellarCore();

    private CommandStellarCore() {
    }

    @Nonnull
    @Override
    public String getName() {
        return "stellarcore";
    }

    @Nonnull
    @Override
    public List<String> getTabCompletions(@Nonnull final MinecraftServer server,
                                          @Nonnull final ICommandSender sender,
                                          @Nonnull final String[] args,
                                          @Nullable final BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "ItemStackCapInitStatus");
        }
        return super.getTabCompletions(server, sender, args, targetPos);
    }

    @Nonnull
    @Override
    public String getUsage(@Nonnull final ICommandSender sender) {
        return "Usage: /stellarcore <ItemStackCapInitStatus>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(@Nonnull final MinecraftServer server, @Nonnull final ICommandSender sender, @Nonnull final String[] args) {
        if (args.length == 1 && args[0].equals("ItemStackCapInitStatus")) {
            printItemStackCapInitStatus(sender);
        }
    }

    protected static void printItemStackCapInitStatus(final ICommandSender sender) {
        final long existedMillis = ItemStackCapInitializer.getExistedMillis();
        final long completedTasks = ItemStackCapInitializer.getCompletedTasks();
        final int queueSize = ItemStackCapInitializer.getQueueSize();
        final int maxQueueSize = ItemStackCapInitializer.getMaxQueueSize();

        sender.sendMessage(new TextComponentString(String.format("%s<Stellar%sCore>%s - ItemStack Cap Initializer Status:",
                Mods.RGB_CHAT.loaded() ? "#66CCFF-FF99CC" : TextFormatting.AQUA,
                Mods.RGB_CHAT.loaded() ? "" : TextFormatting.LIGHT_PURPLE,
                TextFormatting.RESET
        )));
        sender.sendMessage(new TextComponentString(String.format("Existed Time: %s%ss",
                TextFormatting.GREEN,
                TimeUnit.MILLISECONDS.toSeconds(existedMillis)
        )));
        sender.sendMessage(new TextComponentString(String.format("Completed Tasks: %s%s", TextFormatting.GREEN,
                NumberUtils.formatDecimal(completedTasks)
        )));
        sender.sendMessage(new TextComponentString(String.format("Workers: %s%s%s / %s%s",
                TextFormatting.GREEN, ItemStackCapInitializer.getWorkers(), TextFormatting.RESET,
                TextFormatting.YELLOW, ItemStackCapInitializer.getMaxWorkers()
        )));
        sender.sendMessage(new TextComponentString(String.format("Queue Status: %s%s%s / %s%s%s (%s%s%s)",
                TextFormatting.YELLOW, NumberUtils.formatDecimal(queueSize), TextFormatting.RESET,
                TextFormatting.GOLD, NumberUtils.formatDecimal(maxQueueSize), TextFormatting.RESET,
                TextFormatting.YELLOW, NumberUtils.formatPercent(queueSize, maxQueueSize), TextFormatting.RESET
        )));
        sender.sendMessage(new TextComponentString(String.format("Task Execution Per Second: %s%s/s",
                TextFormatting.AQUA, NumberUtils.formatDecimal(completedTasks > 0 ? completedTasks / ((double) existedMillis / 1000) : completedTasks)
        )));
    }

}
