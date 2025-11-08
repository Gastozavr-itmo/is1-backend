package ru.se.ifmo.is1.dto.imports;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImportResponse {
    private int createdCount;
    private List<ValidationError> errors = new ArrayList<>();

    public static ImportResponse ok(int created) {
        ImportResponse r = new ImportResponse();
        r.setCreatedCount(created);
        return r;
    }

    public static ImportResponse failed(int created, List<ValidationError> errors) {
        ImportResponse r = new ImportResponse();
        r.setCreatedCount(created);
        r.setErrors(errors);
        return r;
    }
}
