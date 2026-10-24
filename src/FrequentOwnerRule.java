import java.time.LocalDate;

/** RED FLAG: 3 or more owners in the last 12 months (possible reselling chain). */
public class FrequentOwnerRule extends RedFlagRule {

    private static final int LIMIT = 3;

    public FrequentOwnerRule() {
        super("Ownership frequency check");
    }

    private int countRecentOwners(PhoneHistory history) {
        LocalDate oneYearAgo = LocalDate.now().minusMonths(12);
        int count = 0;
        for (Ownership o : history.getOwnerships()) {
            if (o.getFromDate().isAfter(oneYearAgo)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean isTriggered(PhoneHistory history) {
        return countRecentOwners(history) >= LIMIT;
    }

    @Override
    public int getPenalty(PhoneHistory history) {
        return 30;
    }

    @Override
    public String getMessage(PhoneHistory history) {
        return countRecentOwners(history)
                + " owners in the last 12 months - possible reselling chain";
    }
}
