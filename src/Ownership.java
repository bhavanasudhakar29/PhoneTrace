import java.time.LocalDate;

/**
 * MODEL CLASS - one row of the 'ownerships' table, plus the owner's
 * name and phone (fetched with a JOIN on the owners table).
 *
 * Ownership records are a history log, so they have no setters:
 * once written, history should not be edited.
 */
public class Ownership {

    private final int id;
    private final String imei;
    private final int ownerId;
    private final String ownerName;
    private final String ownerPhone;
    private final LocalDate fromDate;
    private final LocalDate toDate;     // null = current owner

    public Ownership(int id, String imei, int ownerId, String ownerName,
                     String ownerPhone, LocalDate fromDate, LocalDate toDate) {
        this.id = id;
        this.imei = imei;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.ownerPhone = ownerPhone;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public int getId()             { return id; }
    public String getImei()        { return imei; }
    public int getOwnerId()        { return ownerId; }
    public String getOwnerName()   { return ownerName; }
    public String getOwnerPhone()  { return ownerPhone; }
    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getToDate()   { return toDate; }

    public boolean isCurrent() {
        return toDate == null;
    }
}
