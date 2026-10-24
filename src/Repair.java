import java.time.LocalDate;

/**
 * MODEL CLASS - one row of the 'repairs' table.
 * ENCAPSULATION: private fields + validating setters.
 */
public class Repair {

    private int id;
    private String imei;
    private String part;
    private String shopName;
    private LocalDate repairDate;
    private boolean genuine;

    public Repair(int id, String imei, String part, String shopName,
                  LocalDate repairDate, boolean genuine) {
        this.id = id;
        this.imei = imei;
        setPart(part);
        setShopName(shopName);
        setRepairDate(repairDate);
        this.genuine = genuine;
    }

    public int getId()              { return id; }
    public String getImei()         { return imei; }
    public String getPart()         { return part; }
    public String getShopName()     { return shopName; }
    public LocalDate getRepairDate(){ return repairDate; }
    public boolean isGenuine()      { return genuine; }

    public void setPart(String part) {
        if (part == null || part.trim().isEmpty()) {
            throw new IllegalArgumentException("Part cannot be empty.");
        }
        this.part = part.trim();
    }

    public void setShopName(String shopName) {
        if (shopName == null || shopName.trim().isEmpty()) {
            throw new IllegalArgumentException("Shop name cannot be empty.");
        }
        this.shopName = shopName.trim();
    }

    public void setRepairDate(LocalDate repairDate) {
        if (repairDate == null || repairDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Repair date cannot be empty or in the future.");
        }
        this.repairDate = repairDate;
    }

    public void setGenuine(boolean genuine) { this.genuine = genuine; }

    @Override
    public String toString() {
        return String.format("#%-3d %s | %-12s | %-24s | %s",
                id, repairDate, part, shopName, genuine ? "Genuine" : "NON-GENUINE");
    }
}
