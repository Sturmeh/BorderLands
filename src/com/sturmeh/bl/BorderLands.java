package com.sturmeh.bl;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Player;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockDamagedEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlacedEvent;
import org.bukkit.event.player.PlayerItemEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
/**
 * BorderLands.java - Plug-in for CraftBukkit.
 * @author Shaun (sturmeh)
 */
public class BorderLands extends JavaPlugin {
    //private final Listener listener = new Listener();
    private final BorderBlockListener blockListener = new BorderBlockListener();
    private final BorderPlayerListener playerListener = new BorderPlayerListener();
    
    private final String properName = "BorderLands";
    //private final String friendName = "borderlands";
    private final Float version = 2.6f;
    
    private String teleportQuote;
    private String borderQuote;
    private static boolean useRadius;
    private boolean hallPass;

    private ArrayList<String> immune;
    
    public World space;
    public static final Logger log = Logger.getLogger("Minecraft");

    public BorderLands(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File plugin, ClassLoader cLoader) {
        super(pluginLoader, instance, desc, plugin, cLoader);
        this.space = instance.getWorlds()[0];
        registerEvents();
    }
    
    @Override
    public void onDisable() {
        log.info(String.format("%s %.2f was disabled", properName, version));
    }

    @Override
    public void onEnable() {
        log.info(String.format("%s %.2f was enabled", properName, version));
    }
    /*
    public BorderLands() { 
        File oldFile = new File("borderLands.txt");

        if (oldFile.exists()) {
            PropertiesFile oldConfig = new PropertiesFile("borderLands.txt");
            double oldRadius = oldConfig.getDouble("distance-to-the-border");
            if (oldRadius > 0) Border.BORDER_DEFAULT_SIZE = oldRadius;
            teleportQuote = oldConfig.getString("teleport-blocked-msg");
            borderQuote = oldConfig.getString("border-hit-msg");
            useRadius = oldConfig.getBoolean("use-radius-instead");
            hallPass = oldConfig.getBoolean("admins-are-exempt");
            oldFile.deleteOnExit();
        } else {
            teleportQuote = "";
            borderQuote = "";
            useRadius = false;
            hallPass = false;
        }
        if (teleportQuote.isEmpty()) 
            teleportQuote = "You cannot teleport outside the map borders.";
        if (borderQuote.isEmpty()) 
            borderQuote = "You have reached the border.";

    }
     */
    private void registerEvents() {
        getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DAMAGED, null, Priority.Normal, this);
        
        getServer().getPluginManager().registerEvent(Event.Type.VEHICLE_MOVE, null, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Event.Type.CREATURE_SPAWN, null, Priority.Normal, this);
        //getServer().getPluginManager().registerEvent(Event.Type.PLAYER_DROPITEM, null, Priority.Normal, this);
        //getServer().getPluginManager().registerEvent(Event.Type.PLAYER_PICKUPITEM, null, Priority.Normal, this);
        //getServer().getPluginManager().registerEvent(Event.Type.ENTITY_EXPLODE, null, Priority.Normal, this);
        
        
        // BlockListener
        getServer().getPluginManager().registerEvent(Event.Type.BLOCK_PLACED, blockListener, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Event.Type.BLOCK_DAMAGED, blockListener, Priority.Normal, this);
        
        // PlayerListener
        getServer().getPluginManager().registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Event.Type.PLAYER_TELEPORT, playerListener, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Event.Type.PLAYER_ITEM, playerListener, Priority.Normal, this);
    }
    
    private class BorderBlockListener extends BlockListener {
        @Override
        public void onBlockPlaced(BlockPlacedEvent event) {
            if (!Border.isSanctioned(Law.Rule.BUILD, event.getPlayer(), new Location(space, event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ()))) {
                event.setCancelled(true);
            }
        }
        
        @Override
        public void onBlockDamaged(BlockDamagedEvent event) {
            if (!Border.isSanctioned(Law.Rule.BREAK, event.getPlayer(), new Location(space, event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ()))) {
                event.setCancelled(true);
            }
        }
    }
    
    private class BorderPlayerListener extends PlayerListener {
        public void onPlayerMove(PlayerMoveEvent event) {
            event.setCancelled(applyReaction(event.getPlayer(), event.getFrom(), event.getTo(), false));
        }

        public void onPlayerTeleport(PlayerMoveEvent event) {
            event.setCancelled(applyReaction(event.getPlayer(), event.getFrom(), event.getTo(), true));
        }
        
        public void onPlayerItem(PlayerItemEvent event) {
            if (event.getItem().getType() == Material.FLINT_AND_STEEL) {
                if (!Border.isSanctioned(Law.Rule.IGNITE, event.getPlayer(), event.getPlayer().getLocation())) {
                    event.setCancelled(true);
                }
            }
        }

    }
    


    public void reloadConfig() {
        if (immune == null) {
            immune = new ArrayList<String>();
        } else {
            immune.clear();
        }
        teleportQuote = config.getString("teleport-blocked-msg", teleportQuote);
        borderQuote = config.getString("border-hit-msg", borderQuote);
        useRadius = config.getBoolean("use-radius-instead", useRadius);
        hallPass = config.getBoolean("enable-blexempt-permission", hallPass);
        Border.loadAllBorders();
    }

    public boolean extraCommand(Player player, String[] split) {
        if (player.canUseCommand("/border") && 
                (split[0].equalsIgnoreCase("/border") || split[0].equalsIgnoreCase("/bl"))) {
            if (split.length > 1) {
                String command = split[1];
                if (command.equalsIgnoreCase("add")) {
                    if (split.length == 4 || split.length == 5) {
                        String bname = split[2];
                        String radius = split[3];
                        String group;

                        if (split.length == 4 || split[4].isEmpty()) {
                            group = etc.getDataSource().getDefaultGroup().Name;
                        } else {
                            group = split[4];
                        }

                        if (Border.addNewBorder(bname, player, player.getLocation(), radius, group)) {
                            player.sendMessage(ChatColor.GREEN+"Border "+bname+" was successfully created.");
                        } else {
                            player.sendMessage(ChatColor.GRAY+"Border could not be created...");
                        }
                    } else {
                        player.sendMessage(ChatColor.DARK_RED+"Usage: /border add name size [group]");
                    }
                } else if (command.equalsIgnoreCase("del")) {
                    if (split.length == 3) {
                        if (Border.borderCount() <= 1) {
                            player.sendMessage(ChatColor.DARK_RED+"You cannot remove the last remaining border!");
                        } else if (Border.removeBorder(split[2])) {
                            player.sendMessage(ChatColor.GREEN+"Border "+split[2]+" was successfully deleted.");
                        } else {
                            player.sendMessage(ChatColor.DARK_RED+"Could not find such a border.");
                        }
                    } else {
                        player.sendMessage(ChatColor.DARK_RED+"Usage: /border del name");
                    }
                } else if (command.equalsIgnoreCase("edit")) {
                    if (split.length == 4 || split.length == 5) {
                        Border toEdit = Border.getBorder(split[2]);

                        if (toEdit != null) {
                            String radius = split[3];
                            String group;

                            if (split.length == 4 || split[4].isEmpty()) {
                                group = etc.getDataSource().getDefaultGroup().Name;
                            } else {
                                group = split[4];
                            }

                            if (toEdit.modify(player, radius, group)) {
                                player.sendMessage(ChatColor.GREEN+"Border "+split[2]+" was successfully edited.");
                            } else {
                                player.sendMessage(ChatColor.GRAY+"Border could not be edited...");
                            }
                        } else {
                            player.sendMessage(ChatColor.DARK_RED+"Could not find such a border.");
                        }
                    } else {
                        player.sendMessage(ChatColor.DARK_RED+"Usage: /border edit name size [group]");
                    }
                } else if (command.equalsIgnoreCase("import")) {
                    if (split.length == 4) {
                        Warp toImport = etc.getDataSource().getWarp(split[2]);
                        if (toImport != null) {
                            String radius = split[3];
                            if (Border.addNewBorder(toImport, player, radius)) {
                                player.sendMessage(ChatColor.GREEN+"Border "+toImport.Name+" was successfully imported.");
                            } else {
                                player.sendMessage(ChatColor.GRAY+"Border could not be created...");
                            }
                        } else {
                            player.sendMessage(ChatColor.DARK_RED+"Could not find such a warp point.");
                        }
                    } else {
                        player.sendMessage(ChatColor.DARK_RED+"Usage: /border import warpname size");
                    }
                } else if (command.equalsIgnoreCase("list")) {
                    if (split.length == 2) {
                        Border.listBorders(player);
                    } else {
                        player.sendMessage(ChatColor.DARK_RED+"Usage: /border list");
                    }
                } else if (command.equalsIgnoreCase("exempt")) {
                    if (split.length == 2 || split.length == 3) {
                        Player target = player;
                        boolean extra = false;
                        if (split.length == 3) {
                            target = etc.getServer().matchPlayer(split[2]);
                            if (target == null) {
                                player.sendMessage(ChatColor.RED+"Invalid player name...");
                                player.sendMessage(ChatColor.DARK_RED+"Usage: /border exempt [player]");
                                return true;
                            }
                            extra = true;
                        }

                        if (immune.contains(target.getName())) {
                            immune.remove(target.getName());
                            target.sendMessage(ChatColor.YELLOW+"You are now bounded once more...");
                            if (extra) player.sendMessage(ChatColor.BLUE+"You bound "+target.getName()+"...");
                        } else {
                            immune.add(target.getName());
                            target.sendMessage(ChatColor.YELLOW+"You are now fully unbounded...");
                            if (extra) player.sendMessage(ChatColor.BLUE+"You unbound "+target.getName()+"...");
                        }
                    } else {
                        player.sendMessage(ChatColor.DARK_RED+"Usage: /border exempt [player]");
                    }
                } else if (command.equalsIgnoreCase("goto")) {
                    if (split.length == 3) {
                        Border toGo = Border.getBorder(split[2]);
                        if (toGo != null) {
                            toGo.swallow(player);
                        } else {
                            player.sendMessage(ChatColor.DARK_RED+"Could not find such a border.");
                        }
                    } else {
                        player.sendMessage(ChatColor.DARK_RED+"Usage: /border goto name");
                    }
                } else if (command.equalsIgnoreCase("law")) {
                    if (split.length == 5) {
                        Border toSet = Border.getBorder(split[2]);
                        if (toSet != null) {
                            if (toSet.rule.setPolicy(split[3].toUpperCase(), split[4])) {
                                Border.saveAllBorders();
                                player.sendMessage(ChatColor.GREEN+"Law set!");
                            } else {
                                player.sendMessage(ChatColor.DARK_RED+"No such law.");
                            }
                        } else {
                            player.sendMessage(ChatColor.DARK_RED+"Could not find such a border.");
                        }
                    } else {
                        player.sendMessage(ChatColor.DARK_RED+"Usage: /border law name NAMEOFLAW true/false");
                    }
                }
            } else {
                player.sendMessage(ChatColor.DARK_RED+"Usage: /border [add|del|edit|import|list|exempt|goto|law]");
            }
            return true;
        }
        return false;
    }

    private boolean isImmune(Player player) {
        return ((hallPass && player.canUseCommand("/blexempt")) || immune.contains(player.getName()));
    }

    private boolean applyReaction(Player player, Location from, Location to, boolean teleport) {
        if (isImmune(player)) return false;

        Law.Rule action = null;
        if (teleport)
            action = Law.Rule.TELEPORT;
        else
            action = Law.Rule.MOVE;

        if (!hallPass || !player.canUseCommand("/blexempt")) {
            Border.Bounds verdict = Border.outOfBounds(player, from, to, action);
            if (verdict == Border.Bounds.OUTSIDE) {
                if (teleport) {
                    if (!teleportQuote.isEmpty())
                        player.sendMessage(ChatColor.YELLOW + teleportQuote);
                    return true;
                } else {
                    if (!borderQuote.isEmpty())
                        player.sendMessage(ChatColor.YELLOW + borderQuote);
                    player.teleportTo(centralize(from));
                }
            } else if (verdict == Border.Bounds.AWOL) {
                Border.closestBorder(player).swallow(player);
                return true;
            }
        }

        return false;
    }

    private Location centralize(Location edge) {
        return new Location(space, edge.getX() +.5, edge.getY(), edge.getZ() +.5, edge.getPitch(), edge.getYaw());
    }

    public static boolean usingRadius() {
        return useRadius;
    }
    


    private class Listener extends PluginListener {

        public boolean onDamage(PluginLoader.DamageType type, BaseEntity attacker, BaseEntity defender, int amount) {
            if (defender != null && defender.isPlayer()) {
                if (!Border.isSanctioned(Law.Rule.DAMAGE, defender.getPlayer()))
                    return true;
                if (attacker != null && attacker.isPlayer()) {
                    if (!Border.isSanctioned(Law.Rule.PVP, attacker.getPlayer()) ||
                            !Border.isSanctioned(Law.Rule.PVP, defender.getPlayer()))
                        return true;
                }
            }

            return false;
        }

        public void onVehiclePositionChange(BaseVehicle vehicle, int x, int y, int z) {
            Border.outOfBounds(vehicle, x, y, z);
        }

        public boolean onMobSpawn(Mob mob) {
            return (!Border.isSanctioned(Law.Rule.MOBSPAWN, new Location(mob.getX(),mob.getY(),mob.getZ())));
        }
/*
        public boolean onItemDrop(Player player, Item item) {
            return (!Border.isSanctioned(Law.Rule.LITTER, player));
        }

        public boolean onItemPickUp(Player player, Item item) {
            return (!Border.isSanctioned(Law.Rule.SCAVENGE, player));
        }

        public boolean onExplode(Block block) {
            return (!Border.isSanctioned(Law.Rule.EXPLODE, new Location(block.getX(),block.getY(),block.getZ())));
        }
*/

    }
}