package rip.wiped.permissions.essentials;

import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.registry.RegistryKey;

public final class GameCommands {

    private static final String PERM_GAMEMODE = "essentials.gamemode";
    private static final String PERM_GIVE = "essentials.give";
    private static final String PERM_CLEAR = "essentials.clear";
    private static final String PERM_ENCHANT = "essentials.enchant";
    private static final String PERM_EFFECT = "essentials.effect";
    private static final String PERM_HEAL = "essentials.heal";
    private static final String PERM_FEED = "essentials.feed";
    private static final String PERM_GOD = "essentials.god";
    private static final String PERM_KILL = "essentials.kill";

    private GameCommands() {}

    public static void register(Essentials essentials) {
        registerGamemode(essentials);
        registerGive(essentials);
        registerClear(essentials);
        registerEnchant(essentials);
        registerEffect(essentials);
        registerHeal(essentials);
        registerFeed(essentials);
        registerGod(essentials);
        registerKill(essentials);
    }

    private static void registerGamemode(Essentials essentials) {
        var gamemode = new Command("gamemode", "gm");
        var mode = ArgumentType.Enum("mode", GameMode.class);
        var target = playerArg("target");
        gamemode.addSyntax((sender, context) -> handleGamemode(essentials, sender, context.get(mode), context.get(target)), mode, target);
        gamemode.addSyntax((sender, context) -> handleGamemode(essentials, sender, context.get(mode), null), mode);
        Essentials.registerCommand(gamemode);

        registerGamemodeAlias(essentials, "gmc", GameMode.CREATIVE);
        registerGamemodeAlias(essentials, "gms", GameMode.SURVIVAL);
        registerGamemodeAlias(essentials, "gma", GameMode.ADVENTURE);
        registerGamemodeAlias(essentials, "gsp", GameMode.SPECTATOR);
    }

    private static void registerGamemodeAlias(Essentials essentials, String name, GameMode mode) {
        Command alias = new Command(name);
        alias.setDefaultExecutor((sender, context) -> handleGamemode(essentials, sender, mode, null));
        Essentials.registerCommand(alias);
    }

    private static void handleGamemode(Essentials essentials, CommandSender sender, GameMode mode, String targetName) {
        if (!essentials.requirePermission(sender, PERM_GAMEMODE)) return;
        Player target = targetName != null ? Essentials.findOnline(targetName) : Essentials.asPlayer(sender);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: §f" + targetName);
            return;
        }
        target.setGameMode(mode);
        sender.sendMessage("§aSet §f" + target.getUsername() + "§a's gamemode to §f" + mode.name().toLowerCase());
    }

    private static void registerGive(Essentials essentials) {
        var give = new Command("give");
        var target = playerArg("target");
        var item = ArgumentType.String("item");
        var count = ArgumentType.Integer("count");
        give.addSyntax((sender, context) -> handleGive(essentials, sender, context.get(target), context.get(item), context.get(count)), target, item, count);
        give.addSyntax((sender, context) -> handleGive(essentials, sender, context.get(target), context.get(item), 1), target, item);
        Essentials.registerCommand(give);
    }

    private static void handleGive(Essentials essentials, CommandSender sender, String targetName, String itemName, int count) {
        if (!essentials.requirePermission(sender, PERM_GIVE)) return;
        Player target = Essentials.findOnline(targetName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: §f" + targetName);
            return;
        }
        String namespaced = itemName.contains(":") ? itemName : "minecraft:" + itemName;
        Material material = Material.fromKey(namespaced);
        if (material == null) {
            sender.sendMessage("§cUnknown item: §f" + itemName);
            return;
        }
        target.getInventory().addItemStack(ItemStack.of(material, count));
        sender.sendMessage("§aGave §f" + target.getUsername() + " §a" + count + "x §f" + material.key().asString());
    }

    private static void registerClear(Essentials essentials) {
        var clear = new Command("clear");
        var target = playerArg("target");
        clear.addSyntax((sender, context) -> handleClear(essentials, sender, context.get(target)), target);
        clear.setDefaultExecutor((sender, context) -> handleClear(essentials, sender, null));
        Essentials.registerCommand(clear);
    }

    private static void handleClear(Essentials essentials, CommandSender sender, String targetName) {
        if (!essentials.requirePermission(sender, PERM_CLEAR)) return;
        Player target = targetName != null ? Essentials.findOnline(targetName) : Essentials.asPlayer(sender);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: §f" + targetName);
            return;
        }
        target.getInventory().clear();
        sender.sendMessage("§aCleared §f" + target.getUsername() + "§a's inventory.");
    }

    private static void registerEnchant(Essentials essentials) {
        var enchant = new Command("enchant");
        var name = ArgumentType.String("enchant");
        var level = ArgumentType.Integer("level");
        enchant.addSyntax((sender, context) -> handleEnchant(essentials, sender, context.get(name), context.get(level)), name, level);
        enchant.addSyntax((sender, context) -> handleEnchant(essentials, sender, context.get(name), 1), name);
        Essentials.registerCommand(enchant);
    }

    private static void handleEnchant(Essentials essentials, CommandSender sender, String enchantName, int level) {
        if (!essentials.requirePermission(sender, PERM_ENCHANT)) return;
        Player player = Essentials.asPlayer(sender);
        if (player == null) return;
        DynamicRegistry<Enchantment> registry = MinecraftServer.getEnchantmentRegistry();
        Key key = Key.key(enchantName);
        Enchantment enchantment = registry.get(key);
        if (enchantment == null) {
            sender.sendMessage("§cUnknown enchantment: §f" + enchantName);
            return;
        }
        int slot = player.getHeldSlot();
        ItemStack held = player.getInventory().getItemStack(slot);
        if (held.isAir()) {
            sender.sendMessage("§cYou need to hold an item to enchant.");
            return;
        }
        RegistryKey<Enchantment> enchantKey = registry.getKey(key);
        EnchantmentList list = held.get(DataComponents.ENCHANTMENTS, EnchantmentList.EMPTY).with(enchantKey, level);
        player.getInventory().setItemStack(slot, held.with(DataComponents.ENCHANTMENTS, list));
        sender.sendMessage("§aEnchanted your held item with §f" + enchantName + " §e" + level);
    }

    private static void registerEffect(Essentials essentials) {
        var effect = new Command("effect");
        var target = playerArg("target");
        var effectName = ArgumentType.String("effect");
        var seconds = ArgumentType.Integer("seconds");
        var amplifier = ArgumentType.Integer("amplifier");
        effect.addSyntax((sender, context) -> handleEffect(essentials, sender, context.get(target), context.get(effectName), context.get(seconds), context.get(amplifier)), target, effectName, seconds, amplifier);
        effect.addSyntax((sender, context) -> handleEffect(essentials, sender, context.get(target), context.get(effectName), context.get(seconds), 0), target, effectName, seconds);
        effect.addSyntax((sender, context) -> handleEffect(essentials, sender, context.get(target), context.get(effectName), 30, 0), target, effectName);
        Essentials.registerCommand(effect);
    }

    private static void handleEffect(Essentials essentials, CommandSender sender, String targetName, String effectName, int seconds, int amplifier) {
        if (!essentials.requirePermission(sender, PERM_EFFECT)) return;
        Player target = Essentials.findOnline(targetName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: §f" + targetName);
            return;
        }
        String namespaced = effectName.contains(":") ? effectName : "minecraft:" + effectName;
        PotionEffect potionEffect = PotionEffect.fromKey(namespaced);
        if (potionEffect == null) {
            sender.sendMessage("§cUnknown effect: §f" + effectName);
            return;
        }
        target.addEffect(new Potion(potionEffect, amplifier, seconds * 20));
        sender.sendMessage("§aApplied §f" + effectName + " §afor §f" + seconds + "s");
    }

    private static void registerHeal(Essentials essentials) {
        Command heal = new Command("heal");
        heal.setDefaultExecutor((sender, context) -> handleHeal(essentials, sender));
        Essentials.registerCommand(heal);
    }

    private static void handleHeal(Essentials essentials, CommandSender sender) {
        Player player = Essentials.asPlayer(sender);
        if (player == null || !essentials.requirePermission(sender, PERM_HEAL)) return;
        player.setHealth(20f);
        player.setFood(20);
        sender.sendMessage("§aYou have been healed.");
    }

    private static void registerFeed(Essentials essentials) {
        Command feed = new Command("feed");
        feed.setDefaultExecutor((sender, context) -> handleFeed(essentials, sender));
        Essentials.registerCommand(feed);
    }

    private static void handleFeed(Essentials essentials, CommandSender sender) {
        Player player = Essentials.asPlayer(sender);
        if (player == null || !essentials.requirePermission(sender, PERM_FEED)) return;
        player.setFood(20);
        sender.sendMessage("§aYou have been fed.");
    }

    private static void registerGod(Essentials essentials) {
        Command god = new Command("god");
        god.setDefaultExecutor((sender, context) -> handleGod(essentials, sender));
        Essentials.registerCommand(god);
    }

    private static void handleGod(Essentials essentials, CommandSender sender) {
        Player player = Essentials.asPlayer(sender);
        if (player == null || !essentials.requirePermission(sender, PERM_GOD)) return;
        boolean god = !player.isInvulnerable();
        player.setInvulnerable(god);
        sender.sendMessage(god ? "§aGod mode enabled." : "§7God mode disabled.");
    }

    private static void registerKill(Essentials essentials) {
        var kill = new Command("kill");
        var target = playerArg("target");
        kill.addSyntax((sender, context) -> handleKill(essentials, sender, context.get(target)), target);
        kill.setDefaultExecutor((sender, context) -> handleKill(essentials, sender, null));
        Essentials.registerCommand(kill);
    }

    private static void handleKill(Essentials essentials, CommandSender sender, String targetName) {
        if (!essentials.requirePermission(sender, PERM_KILL)) return;
        Player target = targetName != null ? Essentials.findOnline(targetName) : Essentials.asPlayer(sender);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: §f" + targetName);
            return;
        }
        target.kill();
        sender.sendMessage("§cKilled §f" + target.getUsername());
    }

    private static Argument<String> playerArg(String id) {
        Argument<String> arg = ArgumentType.String(id);
        arg.setSuggestionCallback((sender, context, suggestion) -> suggestPlayers(suggestion));
        return arg;
    }

    private static void suggestPlayers(Suggestion suggestion) {
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            suggestion.addEntry(new SuggestionEntry(player.getUsername()));
        }
    }
}