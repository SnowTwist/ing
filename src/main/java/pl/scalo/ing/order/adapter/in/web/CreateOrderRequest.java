package pl.scalo.ing.order.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotBlank(message = "must not be blank") @Size(max = 64, message = "must be at most 64 characters")
        String customerId) {}
