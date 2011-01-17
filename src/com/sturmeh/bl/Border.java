package com.sturmeh.bl;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Player;
/**
 * Border.java - Add-in for BorderLands plug-in.
 * @author Shaun (sturmeh)
 */
public class Border {

    public enum Bounds {
        INSIDE,
        OUTSIDE,
        AWOL
    }

    private String name;
    private Position centre;
    private double radius;
    private String group;
    public Law rule;
    private static LinkedHashMap<String, Border> borders;
    private static final String BORDER_FILE = "borders.txt";
    public static double BORDER_DEFAULT_SIZE = 500;

    private Border(String name, Position centre, double radius, String group) {
        this.rule = new Law();
        this.name = name;
        this.centre = centre;
        this.radius = radius;
        this.group = group;
    }

    // Static content...

    public static void createNewBorder(String name, Position centre, double radius, String group) {
        borders.put(name, new Border(name, centre, radius, group));
        saveAllBorders();
    }

    public static boolean addNewBorder(String name, Player player, Location loc, String radius_, String group) {
        Position centre = new Position(loc);
        double radius = 0.0;

        if (!name.isEmpty() && name.matches("(?i)^[a-z]+$")) {
            if (Border.getBorder(name) != null) {
                player.sendMessage(ChatColor.DARK_RED+"Border already exists with this name.");
                return false;
            }
        } else {
            player.sendMessage(ChatColor.DARK_RED+"Invalid name.");
            return false;
        }

        try {
            radius = Double.parseDouble(radius_);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.DARK_RED+"Radius must be a number.");
            return false;
        }
        
        if (etc.getDataSource().getGroup(group) == null) {
            player.sendMessage(ChatColor.DARK_RED+"Invalid group.");
            return false;
        }

        createNewBorder(name, centre, radius, group);
        return true;
    }

    public static boolean removeBorder(String name) {
        if (borders.containsKey(name)) {
            borders.remove(name);
            saveAllBorders();
            return true;
        }

        return false;
    }

    public static Border getBorder(String name) {
        return borders.get(name);
    }

    public static void loadAllBorders() {
        if (borders == null) {
            borders = new LinkedHashMap<String, Border>();
        } else {
            borders.clear();
        }

        if (new File(BORDER_FILE).exists()) {
            try {
                Scanner scanner = new Scanner(new File(BORDER_FILE));
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (line.startsWith("#") || line.isEmpty()) continue;

                    String[] split = line.split(":");
                    if (split.length < 6) continue;

                    String name = split[0];
                    Position centre = new Position(split[1], split[2], split[3]);
                    double radius = Double.parseDouble(split[4]);
                    String group = split[5];

                    Border toAdd = new Border(name, centre, radius, group);
                    borders.put(name, toAdd);

                    if (split.length == 7) {
                        if (split[6].contains("="))
                            toAdd.rule.loadFromString(split[6]);
                        else
                            toAdd.rule.loadFromString(Law.Rule.PVP.toString() + "=" + split[6]);
                    }
                }
                scanner.close();
            } catch (Exception e) {
                BorderLands.log.log(Level.SEVERE, "Exception while reading borders from " + BORDER_FILE + ": " + e);
            }
        } else {
            Position centre = new Position(etc.getServer().getSpawnLocation());
            Border border = new Border("spawn", centre, BORDER_DEFAULT_SIZE, etc.getDataSource().getDefaultGroup().Name);
            borders.put(border.name, border);
            saveAllBorders();
        }
    }

    public static void saveAllBorders() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(BORDER_FILE, false));
            Collection<Border> b = borders.values();
            Iterator<Border> all = b.iterator();
            while (all.hasNext()) {
                Border toSave = all.next();

                StringBuilder builder = new StringBuilder();

                builder.append(toSave.name);
                builder.append(':');
                builder.append(toSave.centre.x);
                builder.append(':');
                builder.append(toSave.centre.y);
                builder.append(':');
                builder.append(toSave.centre.z);
                builder.append(':');
                builder.append(toSave.radius);
                builder.append(':');
                builder.append(toSave.group);
                builder.append(':');
                builder.append(toSave.rule);

                bw.append(builder.toString());
                bw.newLine();
            }

            bw.close();
        } catch (Exception e) {
            BorderLands.log.log(Level.SEVERE, "Exception while writing borders to " + BORDER_FILE + ": " + e);
        }
    }

    public static boolean isSanctioned(Law.Rule action, Location point) {
        Border local = smallestBorder(point, null);
        if (local == null) return false;
        return smallestBorder(point, null).rule.getPolicy(action);
    }

    public static boolean isSanctioned(Law.Rule action, Player player, Location point) {
        Border local = smallestBorder(point, player);
        if (local == null || !player.isInGroup(local.group)) return false;
        return smallestBorder(point, player).rule.getPolicy(action);
    }

    public static boolean isSanctioned(Law.Rule action, Player player) {
        return isSanctioned(action, player, player.getLocation());
    }

    public static Border.Bounds outOfBounds(Player player, Location from, Location to, Law.Rule action) {
        boolean AWOL = true;

        if (isSanctioned(action, player, from)) AWOL = false;
        if (isSanctioned(action, player, to)) return Border.Bounds.INSIDE;

        if (AWOL) return Border.Bounds.AWOL;
        return Border.Bounds.OUTSIDE;
    }

    public static void outOfBounds(BaseVehicle vehicle, int x, int y, int z) {
        boolean permit;
        if (vehicle.isEmpty() || vehicle.getPassenger() == null) {
            permit = isSanctioned(Law.Rule.MOVE, new Location(BorderLands.space, x, y, z));
        } else {
            permit = isSanctioned(Law.Rule.MOVE, vehicle.getPassenger(), new Location(BorderLands.space, x, y, z));
        }

        if (permit) return;
        
        if (isSanctioned(Law.Rule.MOVE, new Location(BorderLands.space, x+1, y, z))) {
            vehicle.teleportTo(x+1.5, y, z, 0, 0);
        } else if (isSanctioned(Law.Rule.MOVE, new Location(BorderLands.space, x, y, z+1))) {
            vehicle.teleportTo(x, y, z+1.5, 0, 0);
        } else if (isSanctioned(Law.Rule.MOVE, new Location(BorderLands.space, x-2, y, z))) {
            vehicle.teleportTo(x-1.5, y, z, 0, 0);
        } else if (isSanctioned(Law.Rule.MOVE, new Location(BorderLands.space, x, y, z-2))) {
            vehicle.teleportTo(x, y, z-1.5, 0, 0);
        } else {
            vehicle.destroy();
        }
    }

    public static Border closestBorder(Player player) {
        double range = -1;
        Border solution = null;

        Collection<Border> b = borders.values();
        Iterator<Border> all = b.iterator();

        while (all.hasNext()) {
            Border border = all.next();
            if (player.isInGroup(border.group)) {
                if (range == -1 || border.centre.distanceBetween(player) > range) {
                    solution = border;
                    range = border.centre.distanceBetween(player);
                }
            }
        }

        return solution;
    }

    public static Border smallestBorder(Location point, Player player) {
        double size = -1;
        Border solution = null;

        Collection<Border> b = borders.values();
        Iterator<Border> all = b.iterator();

        while (all.hasNext()) {
            Border border = all.next();
            if (border.contains(point)) {
                if (size == -1 || border.radius < size) {
                    solution = border;
                    size = border.radius;
                } else if (player != null && size == border.radius && player.isInGroup(border.group)) {
                    solution = border;
                }
            }
        }

        return solution;
    }
    /*
    public static Border smallestBorder(Player player) {
        double size = -1;
        Border solution = null;

        Collection<Border> b = borders.values();
        Iterator<Border> all = b.iterator();

        while (all.hasNext()) {
            Border border = all.next();
            if (border.contains(player)) {
                if (size == -1 || border.radius < size) {
                    solution = border;
                    size = border.radius;
                } else if (size == border.radius && player.isInGroup(border.group)) {
                    solution = border;
                }
            }
        }

        return solution;
    }
     */
    public static void listBorders(Player player) {
        Collection<Border> b = borders.values();
        Iterator<Border> all = b.iterator();
        player.sendMessage(ChatColor.YELLOW+"Borders:");
        player.sendMessage(ChatColor.YELLOW+"Name  | Size | Group | Distance");
        player.sendMessage(ChatColor.YELLOW+"----------------------");
        while (all.hasNext()) {
            Border border = all.next();
            player.sendMessage(ChatColor.YELLOW+border.name+" | "+
                    (int)border.radius+" | "+border.group+" | "+
                    (int)border.centre.distanceBetween(player));

        }
    }

    public static int borderCount() {
        return borders.size();
    }

    public static boolean containsAll(double x, double z) {
        Collection<Border> b = borders.values();
        Iterator<Border> all = b.iterator();

        while (all.hasNext()) {
            Border border = all.next();
            if (border.contains(x, z)) return true;
        }

        return false;
    }

    public static boolean containsAll(Location point) {
        return containsAll(point.getX(), point.getY());
    }

    public static boolean containsAll(Player player) {
        return containsAll(player.getLocation());
    }

    // Specific content...
    public boolean modify(Player player, String radius_, String group) {
        double radius;
        try {
            radius = Double.parseDouble(radius_);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.DARK_RED+"Radius must be a number.");
            return false;
        }

        if (etc.getDataSource().getGroup(group) == null) {
            player.sendMessage(ChatColor.DARK_RED+"Invalid group.");
            return false;
        }

        this.radius = radius;
        this.group = group;

        saveAllBorders();

        return true;
    }

    public void swallow(Player player) {
        player.teleportTo(centre.toLocation());
        player.sendMessage(ChatColor.YELLOW + "You were stranded, and thus returned to "+name+".");
    }

    public boolean contains(double x, double z) {
        return (centre.distanceBetween(x, z) < radius);
    }

    public boolean contains(Location point) {
        return (centre.distanceBetween(point) < radius);
    }

    public boolean contains(Player player) {
        return (centre.distanceBetween(player) < radius);
    }
}
