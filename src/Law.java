import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;


public class Law {
    private HashMap<Rule, Policy> laws;
    public enum Rule {
        BUILD,
        BREAK,
        MOVE,
        TELEPORT,
        DAMAGE,
        PVP,
        MOBSPAWN,
        LITTER,
        SCAVENGE,
        EXPLODE,
        IGNITE
    }

    private class Policy {
        public Rule type;
        public boolean allow = true;

        public Policy (Rule type) {
            this.type = type;
        }

        public String toString() {
            return type + "=" + String.valueOf(allow);
        }
    }

    public Law() {
        laws = new HashMap<Rule, Policy>();
        for (Rule type : Rule.values()) {
            laws.put(type, new Policy(type));
        }
    }

    public boolean setPolicy(String name, String key) {
        Policy toSet = laws.get(Rule.valueOf(name));
        if (toSet != null)
            toSet.allow = Boolean.parseBoolean(key);
        else
            return false;
        return true;
    }

    public boolean getPolicy(Rule type) {
        return laws.get(type).allow;
    }

    public void loadFromString (String word) {
        if (word.isEmpty()) return;
        String[] props = word.split(",");
        if (props.length < 1) return;
        for (String setting : props) {
            if (setting.isEmpty() || !setting.contains("=")) continue;
            String[] prop = setting.split("=");
            if (prop.length != 2 || prop[0].isEmpty() || prop[1].isEmpty()) continue;
            setPolicy(prop[0], prop[1]);
        }
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
