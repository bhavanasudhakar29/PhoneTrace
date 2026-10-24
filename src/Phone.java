/**
 * MODEL CLASS - one row of the 'phones' table.
 * ENCAPSULATION: fields are private; setters validate before accepting data,
 * so an invalid Phone object can never exist.
 */
public class Phone {

    private String imei;
    private String brand;
    private String model;

    public Phone(String imei, String brand, String model) {
        setImei(imei);
        setBrand(brand);
        setModel(model);
    }

    /** An IMEI is valid only if it is exactly 15 digits. */
    public static boolean isValidImei(String imei) {
        return imei != null && imei.matches("\\d{15}");
    }

    public String getImei() { return imei; }

    public void setImei(String imei) {
        if (!isValidImei(imei)) {
            throw new IllegalArgumentException("IMEI must be exactly 15 digits.");
        }
        this.imei = imei;
    }

    public String getBrand() { return brand; }

    public void setBrand(String brand) {
        if (brand == null || brand.trim().isEmpty()) {
            throw new IllegalArgumentException("Brand cannot be empty.");
        }
        this.brand = brand.trim();
    }

    public String getModel() { return model; }

    public void setModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Model cannot be empty.");
        }
        this.model = model.trim();
    }

    @Override
    public String toString() {
        return imei + " | " + brand + " " + model;
    }
}
