/** RED FLAG: repairs done with non-genuine parts. -15 for EACH such repair. */
public class FakePartRule extends RedFlagRule {

    public FakePartRule() {
        super("Genuine parts check");
    }

    private int countFakeParts(PhoneHistory history) {
        int count = 0;
        for (Repair r : history.getRepairs()) {
            if (!r.isGenuine()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean isTriggered(PhoneHistory history) {
        return countFakeParts(history) > 0;
    }

    @Override
    public int getPenalty(PhoneHistory history) {
        return 15 * countFakeParts(history);
    }

    @Override
    public String getMessage(PhoneHistory history) {
        return countFakeParts(history) + " repair(s) used NON-GENUINE parts";
    }
}
