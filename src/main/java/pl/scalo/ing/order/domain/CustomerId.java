package pl.scalo.ing.order.domain;

public record CustomerId(String value) {
    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Customer id must not be blank");
        }
        value = value.trim();
    }

    @Override
    public String toString() {
        return value;
    }
}
