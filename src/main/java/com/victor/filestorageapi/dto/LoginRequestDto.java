package com.victor.filestorageapi.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
        @NotEmpty(message = "Username should not be empty")
        @Size(min = 3, max = 40, message = "Username length should be in between 3 and 40")
        String username,

        @NotEmpty(message = "Password should not be empty")
        @Size(min = 7, max = 40, message = "Password length should be in between 7 and 40")
        String password
) {
}
