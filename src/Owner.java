/**
 * MODEL CLASS - one row of the 'owners' table.
 * ENCAPSULATION: private fields + validating setters.
 */
public class Owner {

    private int ownerId;
    private String name;
    private String phone;

    public Owner(int ownerId, String name, String phone) {
        this.ownerId = ownerId;
        setName(name);
        setPhone(phone);
    }

    public int getOwnerId() { return ownerId; }

    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public String getName() { return name; }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        this.name = name.trim();
    }

    public String getPhone() { return phone; }

    public void setPhone(String phone) {
        if (phone == null || !phone.matches("\\d{10}")) {
            throw new IllegalArgumentException("Mobile number must be exactly 10 digits.");
        }
        this.phone = phone;
    }

    @Override
    public String toString() {
        return String.format("ID %-3d | %-15s | %s", ownerId, name, phone);
    }
}
