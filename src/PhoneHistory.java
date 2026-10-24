import java.util.List;

/**
 * Bundles everything known about one phone into a single object,
 * so the red flag rules can examine the complete history at once.
 */
public class PhoneHistory {

    private final Phone phone;
    private final List<Ownership> ownerships;
    private final List<Repair> repairs;
    private final List<TheftReport> theftReports;
    private final String sellerPhone;   // optional: the mobile number of the person selling

    public PhoneHistory(Phone phone, List<Ownership> ownerships, List<Repair> repairs,
                        List<TheftReport> theftReports, String sellerPhone) {
        this.phone = phone;
        this.ownerships = ownerships;
        this.repairs = repairs;
        this.theftReports = theftReports;
        this.sellerPhone = sellerPhone;
    }

    public Phone getPhone()                     { return phone; }
    public List<Ownership> getOwnerships()      { return ownerships; }
    public List<Repair> getRepairs()            { return repairs; }
    public List<TheftReport> getTheftReports()  { return theftReports; }
    public String getSellerPhone()              { return sellerPhone; }

    /** Returns the current owner, or null if none is recorded. */
    public Ownership getCurrentOwner() {
        for (Ownership o : ownerships) {
            if (o.isCurrent()) {
                return o;
            }
        }
        return null;
    }
}
