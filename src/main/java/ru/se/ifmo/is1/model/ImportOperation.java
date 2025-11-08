package ru.se.ifmo.is1.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportOperation {
    private Long id;
    private ImportStatus status;
    private Integer createdCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
