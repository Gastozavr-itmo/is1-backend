package ru.se.ifmo.is1.dto.error;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
import ru.se.ifmo.is1.dto.imports.ValidationError;

import java.time.Instant;
import java.util.List;

@Value
@Builder
@Data
public class ExceptionResponse {

    int status;                     // HTTP статус (числом)
    String error;                   // краткое имя статуса: "Bad Request", "Conflict", ...
    String message;                 // основное сообщение для пользователя
    String path;                    // URI запроса
    Instant timestamp;              // когда произошло

    // Детализация по полям (для валидации, импортов и т.п.)
    List<ValidationError> details;
}
