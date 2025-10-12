package ru.se.ifmo.is1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.se.ifmo.is1.dto.paging.PageRequestDTO;
import ru.se.ifmo.is1.dto.paging.PageResponseDTO;
import ru.se.ifmo.is1.dto.product.ProductCreateDTO;
import ru.se.ifmo.is1.dto.product.ProductViewDTO;
import ru.se.ifmo.is1.mapper.ProductMapper;
import ru.se.ifmo.is1.model.Coordinates;
import ru.se.ifmo.is1.model.Product;
import ru.se.ifmo.is1.repository.ProductRepository;


@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository repo;
    private final ProductMapper mapper;

    @Transactional(readOnly = true)
    public ProductViewDTO get(Long id) {
        return mapper.toView(repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found")));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ru.se.ifmo.is1.dto.product.ProductViewDTO> list(
            int page, int size, String sort, String dir,
            String name, String partNumber, String unitOfMeasureLike,
            String organizationName, String personName
    ) {
        int offset = Math.max(page, 0) * Math.max(size, 1);

        var rows  = repo.findFiltered(name, partNumber, unitOfMeasureLike, organizationName, personName,
                offset, size, sort, dir);
        long total = repo.countFiltered(name, partNumber, unitOfMeasureLike, organizationName, personName);

        var items = rows.stream().map(mapper::toView).toList();
        return PageResponseDTO.of(items, page, size, total, sort, dir);
    }



    @Transactional
    public Long create(ProductCreateDTO dto) {
        Product p = mapper.toEntity(dto);
        validate(p);
        return repo.save(p);
    }

    @Transactional
    public void update(Long id, ProductCreateDTO dto) {
        Product p = mapper.toEntity(dto);
        p.setId(id);
        validate(p);
        repo.merge(p);
    }

    @Transactional
    public void delete(Long id) {
        var e = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        repo.delete(e);
    }

    private void validate(Product p) {
        if (p.getName() == null || p.getName().trim().isEmpty())
            throw new IllegalArgumentException("name required");
        Coordinates c = p.getCoordinates();
        if (c == null) throw new IllegalArgumentException("coordinates required");
        if (c.getX() == null || c.getX() > 450)
            throw new IllegalArgumentException("coordinates.x must be <= 450");
        if (c.getY() == null || c.getY() <= -422)
            throw new IllegalArgumentException("coordinates.y must be > -422");
        if (p.getUnitOfMeasure() == null)
            throw new IllegalArgumentException("unitOfMeasure required");
        if (p.getManufacturer() == null)
            throw new IllegalArgumentException("manufacturer.id required");
        if (p.getPrice() <= 0)
            throw new IllegalArgumentException("price > 0 required");
        if (p.getRating() <= 0)
            throw new IllegalArgumentException("rating > 0 required");
        if (p.getPartNumber() == null || p.getPartNumber().trim().isEmpty())
            throw new IllegalArgumentException("partNumber required");
    }
}
