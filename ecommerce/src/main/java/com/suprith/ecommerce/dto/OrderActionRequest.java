package com.suprith.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderActionRequest {

    @NotBlank(message = "A reason is required")
    private String reason;
}
