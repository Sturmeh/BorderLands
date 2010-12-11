import java.io.File;
import java.util.ArrayList;
import java.util.Random;
/**
 * BorderLands.java - Plug-in for hey0's minecraft mod.
 * @author Shaun (sturmeh)
 */
public class BorderLands extends SuperPlugin {
    public final Listener listener = new Listener();
    private static Random generator = new Random(etc.getServer().getTime());
    private static String teleportQuote;
    private static String borderQuote;
    private static boolean useRadius;
    private static boolean hallPass;

    private ArrayList<String> immune;

    //public BorderLands() { super("borderLands"); }
    public BorderLands() { 
        super("BorderLands", 2.3f, "borderlands");
        
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
    
    public void initializeExtra() {
        etc.getLoader().addListener(PluginLoader.Hook.DAMAGE, listener, this, PluginListener.Priority.HIGH);
        etc.getLoader().addListener(PluginLoader.Hook.PLAYER_MOVE, listener, this, PluginListener.Priority.HIGH);
        etc.getLoader().addListener(PluginLoader.Hook.TELEPORT, listener, this, PluginListener.Priority.HIGH);
        etc.getLoader().addListener(PluginLoader.Hook.VEHICLE_POSITIONCHANGE, listener, this, PluginListener.Priority.HIGH);
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
                    if (split.length == 5 || split.length == 6) {
                        String bname = split[2];
                        String radius = split[3];
                        String pvp = split [4];
                        String group;

                        if (split.length == 5 || split[5].isEmpty()) {
                            group = etc.getDataSource().getDefaultGroup().Name;
                        } else {
                            group = split[5];
                        }

                        if (Border.addNewBorder(bname, player, player.getLocation(), radius, group, pvp)) {
                            player.sendMessage(Colors.Green+"Border "+bname+" was successfully created.");
                        } else {
                            player.sendMessage(Colors.Gray+"Border could not be created...");
                        }
                    } else {
                        player.sendMessage(Colors.Rose+"Usage: /border add name size pvp [group]");
                    }
                } else if (command.equalsIgnoreCase("del")) {
                    if (split.length == 3) {
                        if (Border.borderCount() <= 1) {
                            player.sendMessage(Colors.Rose+"You cannot remove the last remaining border!");
                        } else if (Border.removeBorder(split[2])) {
                            player.sendMessage(Colors.Green+"Border "+split[2]+" was successfully deleted.");
                        } else {
                            player.sendMessage(Colors.Rose+"Could not find such a border.");
                        }
                    } else {
                        player.sendMessage(Colors.Rose+"Usage: /border del name");
                    }
                } else if (command.equalsIgnoreCase("edit")) {
                    if (split.length == 5 || split.length == 6) {
                        Border toEdit = Border.getBorder(split[2]);
                        
                        if (toEdit != null) {
                            String radius = split[3];
                            String pvp = split[4];
                            String group;

                            if (split.length == 5 || split[5].isEmpty()) {
                                group = etc.getDataSource().getDefaultGroup().Name;
                            } else {
                                group = split[5];
                            }
                            
                            if (toEdit.modify(player, radius, group, pvp)) {
                                player.sendMessage(Colors.Green+"Border "+split[2]+" was successfully edited.");
                            } else {
                                player.sendMessage(Colors.Gray+"Border could not be edited...");
                            }
                        } else {
                            player.sendMessage(Colors.Rose+"Could not find such a border.");
                        }
                    } else {
                        player.sendMessage(Colors.Rose+"Usage: /border edit name size pvp [group]");
                    }
                } else if (command.equalsIgnoreCase("import")) {
                    if (split.length == 5) {
                        Warp toImport = etc.getDataSource().getWarp(split[2]);
                        if (toImport != null) {
                            String radius = split[3];
                            String pvp = split[4];
                            if (Border.addNewBorder(toImport, player, radius, pvp)) {
                                player.sendMessage(Colors.Green+"Border "+toImport.Name+" was successfully imported.");
                            } else {
                                player.sendMessage(Colors.Gray+"Border could not be created...");
                            }
                        } else {
                            player.sendMessage(Colors.Rose+"Could not find such a warp point.");
                        }
                    } else {
                        player.sendMessage(Colors.Rose+"Usage: /border import warpname size pvp");
                    }
                } else if (command.equalsIgnoreCase("list")) {
                    if (split.length == 2) {
                        Border.listBorders(player);
                    } else {
                        player.sendMessage(Colors.Rose+"Usage: /border list");
                    }
                } else if (command.equalsIgnoreCase("exempt")) {
                    if (split.length == 2 || split.length == 3) {
                        Player target = player;
                        boolean extra = false;
                        if (split.length == 3) {
                            target = etc.getServer().matchPlayer(split[2]);
                            if (target == null) {
                                player.sendMessage(Colors.Red+"Invalid player name...");
                                player.sendMessage(Colors.Rose+"Usage: /border exempt [player]");
                                return true;
                            }
                            extra = true;
                        }
                        
                        if (immune.contains(target.getName())) {
                            immune.remove(target.getName());
                            target.sendMessage(Colors.Yellow+"You are now bounded once more...");
                            if (extra) player.sendMessage(Colors.Blue+"You bound "+target.getName()+"...");
                        } else {
                            immune.add(target.getName());
                            target.sendMessage(Colors.Yellow+"You are now fully unbounded...");
                            if (extra) player.sendMessage(Colors.Blue+"You unbound "+target.getName()+"...");
                        }
                    } else {
                        player.sendMessage(Colors.Rose+"Usage: /border exempt [player]");
                    }
                } else if (command.equalsIgnoreCase("goto")) {
                    if (split.length == 3) {
                        Border toGo = Border.getBorder(split[2]);
                        if (toGo != null) {
                            toGo.swallow(player);
                        } else {
                            player.sendMessage(Colors.Rose+"Could not find such a border.");
                        }
                    } else {
                        player.sendMessage(Colors.Rose+"Usage: /border goto name");
                    }
                }
            } else {
                player.sendMessage(Colors.Rose+"Usage: /border [add|del|edit|import|list|exempt|goto]");
            }
            return true;
        }
        return false;
    }

    private boolean applyReaction(Player player, Location from, Location to, boolean teleport) {
        if (immune.contains(player.getName())) return false;
        
        if (!hallPass || !player.canUseCommand("/blexempt")) {
            Border.Bounds verdict = Border.outOfBounds(player, from, to);
            if (verdict == Border.Bounds.OUTSIDE) {
                if (teleport) {
                    if (!teleportQuote.isEmpty())
                        player.sendMessage(Colors.Yellow + teleportQuote);
                    return true;
                } else {
                    if (generator.nextInt(5) == 3)
                        if (!borderQuote.isEmpty())
                            player.sendMessage(Colors.Yellow + borderQuote);
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
        return new Location(edge.x +.5, edge.y, edge.z +.5, edge.rotX, edge.rotY);
    }
    
    public static boolean usingRadius() {
        return useRadius;
    }
    
    

    private class Listener extends PluginListener {
        public void onPlayerMove(Player player, Location from, Location to) { applyReaction(player, from, to, false); }
        
        public boolean onDamage(PluginLoader.DamageType type, BaseEntity attacker, BaseEntity defender, int amount) {
            if ((attacker != null && attacker.isPlayer()) && (defender != null && defender.isPlayer())) {
                return !Border.pvpSanctioned(attacker.getPlayer(), defender.getPlayer());
            }
            return false;
        }
        
        public boolean onTeleport(Player player, Location from, Location to) {
            StackTraceElement[] trace = Thread.currentThread().getStackTrace();
            for (StackTraceElement tracy : trace) if (tracy.getMethodName() == "applyReaction") return false;
            return applyReaction(player, from, to, true);
        }
        
        public void onVehiclePositionChange(BaseVehicle vehicle, int x, int y, int z) {
            Border.outOfBounds(vehicle, x, y, z);
        }
        
    }
}