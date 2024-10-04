package github.kasuminova.stellarcore.common.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class CommandStellarCoreClient extends CommandBase {

    public static final CommandStellarCoreClient INSTANCE = new CommandStellarCoreClient();

    private CommandStellarCoreClient() {
    }

    @Nonnull
    @Override
    public String getName() {
        return "stellarcore_client";
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
        return "Usage: /stellarcore_client <ItemStackCapInitStatus>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(@Nonnull final MinecraftServer server, @Nonnull final ICommandSender sender, @Nonnull final String[] args) {
        if (args.length == 1 && args[0].equals("ItemStackCapInitStatus")) {
            CommandStellarCore.printItemStackCapInitStatus(sender);
        }
    }

}
