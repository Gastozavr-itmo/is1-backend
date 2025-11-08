package ru.se.ifmo.is1.dto.imports;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ValidationError {
    private int index;         // индекс записи в массиве
    private String fieldPath;  // путь к полю (items[3].manufacturer.fullName)
    private String message;    // текст ошибки

    public ValidationError(int index, String fieldPath, String message) {
        this.index = index;
        this.fieldPath = fieldPath;
        this.message = message;
    }
}
