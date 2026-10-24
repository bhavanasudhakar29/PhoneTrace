import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** RED FLAG: the same part was replaced 2 or more times (recurring hardware fault). */
public class RepeatedRepairRule extends RedFlagRule {

    public RepeatedRepairRule() {
        super("Repeated repair check");
    }

    // Returns parts replaced 2+ times, e.g. ["display (2x)"]
    private List<String> findRepeatedParts(PhoneHistory history) {
        Map<String, Integer> counts = new HashMap<>();
        for (Repair r : history.getRepairs()) {
            String part = r.getPart().toLowerCase();
            counts.put(part, counts.getOrDefault(part, 0) + 1);
        }
        List<String> repeated = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() >= 2) {
                repeated.add(entry.getKey() + " (" + entry.getValue() + "x)");
            }
        }
        return repeated;
    }

    @Override
    public boolean isTriggered(PhoneHistory history) {
        return !findRepeatedParts(history).isEmpty();
    }

    @Override
    public int getPenalty(PhoneHistory history) {
        return 20;
    }

    @Override
    public String getMessage(PhoneHistory history) {
        return "Same part replaced repeatedly: " + String.join(", ", findRepeatedParts(history))
                + " - recurring hardware fault";
    }
}
