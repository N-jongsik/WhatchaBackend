package com.example.whatcha.domain.interest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCarAlertRequestDto {
    private String modelName;
    private LocalDate alertExpirationDate;
}
