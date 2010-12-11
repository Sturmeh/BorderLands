
public class Position {
    public double x;
    public double y;
    public double z;

    public Position(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Position(String x, String y, String z) {
        this.x = Double.parseDouble(x);
        this.y = Double.parseDouble(y);
        this.z = Double.parseDouble(z);
    }

    public Position(Location point) {
        this.x = point.x;
        this.y = point.y;
        this.z = point.z;
    }

    public double distanceBetween(double x, double z) {
        if (!BorderLands.usingRadius())
            return Math.max(Math.abs(this.x - x), Math.abs(this.z - z));
        return Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.z - z, 2));
    }

    public double distanceBetween(Location point) {
        return distanceBetween(point.x, point.z);
    }
    
    public double distanceBetween(Player player) {
        return distanceBetween(player.getLocation());
    }
    
    public Location toLocation() {
        return new Location(x, y, z);
    }
}
