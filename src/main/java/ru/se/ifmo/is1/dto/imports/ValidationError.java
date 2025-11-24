package ru.se.ifmo.is1.dto.imports;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ValidationError {
    private int index;
    private String fieldPath;
    private String message;

    public ValidationError(int index, String fieldPath, String message) {
        this.index = index;
        this.fieldPath = fieldPath;
        this.message = message;
    }
}
