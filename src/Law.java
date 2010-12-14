import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;


public class Law {
    private HashMap<Rule, Policy> laws;
    public enum Rule {
        BUILD,
        DESTROY,
        MOVEMENT,
        TELEPORT,
        HEALTH,
        PVP,
        MOBSPAWN,
        ITEMDROP,
        ITEMFIND,
        EXPLODE,
        IGNITE
    }
    
    private class Policy {
        public Rule type;
        public boolean allow = true;
        
        public Policy (Rule type) {
            this.type = type;
        }
        /*
        public Policy (Rule type) {
            this.type = type;
           
            switch (type) {
            case CREATE:
                name = "build";
                allow = true;
                break;
            case DESTROY:
                name = "destroy";
                allow = true;
                break;
            case PLAYERMOVE:
                name = "movement";
                allow = true;
                break;
            case TELEPORT:
                name = "teleport";
                allow = true;
                break;
            case DAMAGE:
                name = "health";
                allow = true;
                break;
            case PVP:
                name = "pvp";
                allow = true;
                break;
            case MOBSPAWN:
                name = "mobspawn";
                allow = true;
                break;
            case ITEMDROP:
                name = "litter";
                allow = true;
                break;
            case ITEMPICK:
                name = "scavenge";
                allow = true;
                break;
            case EXPLODE:
                name = "explode";
                allow = true;
                break;
            case IGNITE:
                name = "ignite";
                allow = true;
                break;
            }
            
        }
        */
        public String toString() {
            return type + "=" + String.valueOf(allow);
        }
    }
    
    public Law () {
        laws = new HashMap<Rule, Policy>();
        for (Rule type : Rule.values()) {
            laws.put(type, new Policy(type));
        }
    }
    
    public void setLaws (String word) {
        if (word.isEmpty()) return;
        String[] props = word.split(",");
        if (props.length < 1) return;
        for (String setting : props) {
            if (setting.isEmpty() || !setting.contains("=")) continue;
            String[] prop = word.split("=");
            if (prop.length != 2 || prop[0].isEmpty() || prop[1].isEmpty()) continue;
            changeLaw(prop[0], prop[1]);
        }
    }

    
    private boolean changeLaw(String name, String key) {
        Policy toSet = laws.get(Rule.valueOf(name));
        if (toSet != null)
            toSet.allow = Boolean.parseBoolean(key);
        else
            return false;
        return true;
    }

    public String toString() {
        Collection<Policy> l = laws.values();
        Iterator<Policy> all = l.iterator();
        
        String constructor = "";
        
        while (all.hasNext()) {
            Policy policy = all.next();
            constructor += policy;
            if (all.hasNext()) constructor += ",";
        }
        
        return constructor;
    }
}
