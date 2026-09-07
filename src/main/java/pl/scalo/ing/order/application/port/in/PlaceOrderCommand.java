package pl.scalo.ing.order.application.port.in;

public record PlaceOrderCommand(String customerId) {
    public PlaceOrderCommand {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }
    }
}
