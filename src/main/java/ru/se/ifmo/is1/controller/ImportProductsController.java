package ru.se.ifmo.is1.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.se.ifmo.is1.dto.imports.ImportResponse;
import ru.se.ifmo.is1.dto.imports.ProductImportDTO;
import ru.se.ifmo.is1.dto.imports.ValidationError;
import ru.se.ifmo.is1.service.ProductImportService;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/import/products")
public class ImportProductsController {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final ProductImportService importService;

    public ImportProductsController(ObjectMapper objectMapper,
                                    Validator validator,
                                    ProductImportService importService) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.importService = importService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImportResponse> importJsonArray(
            @Valid @RequestBody List<ProductImportDTO> items) {

        var errors = validateAll(items);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(ImportResponse.failed(0, errors));
        }
        var result = importService.importAllTransactional(items);
        return ResponseEntity.status(result.getErrors().isEmpty() ? 200 : 400).body(result);
    }

    @PostMapping(path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImportResponse> importFromFile(@RequestPart("file") MultipartFile file) {

        String ct = file.getContentType() != null ? file.getContentType() : "";
        if (!ct.contains("json") && !MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE.equals(ct)) {
            return ResponseEntity.badRequest().body(
                    ImportResponse.failed(0, List.of(
                            new ValidationError(-1, "file", "Expected JSON file"))));
        }

        List<ProductImportDTO> items;
        try (InputStream is = file.getInputStream()) {
            items = objectMapper.readValue(is, new TypeReference<>() {});
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ImportResponse.failed(0, List.of(
                            new ValidationError(-1, "file", "Invalid JSON: " + e.getMessage()))));
        }

        var errors = validateAll(items);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(ImportResponse.failed(0, errors));
        }

        var result = importService.importAllTransactional(items);
        return ResponseEntity.status(result.getErrors().isEmpty() ? 200 : 400).body(result);
    }

    private List<ValidationError> validateAll(List<ProductImportDTO> items) {
        List<ValidationError> errors = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ProductImportDTO dto = items.get(i);
            Set<ConstraintViolation<ProductImportDTO>> v = validator.validate(dto);
            for (ConstraintViolation<?> cv : v) {
                String path = "items[" + i + "]." + cv.getPropertyPath();
                errors.add(new ValidationError(i, path, cv.getMessage()));
            }
        }
        return errors;
    }
}
