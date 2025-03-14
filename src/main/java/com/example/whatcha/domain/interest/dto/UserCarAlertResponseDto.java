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
public class UserCarAlertResponseDto {
    private Long userCarAlertId;
    private Long userId;
    private Long modelId;
    private String modelName;
    private LocalDate alertExpirationDate;
}
