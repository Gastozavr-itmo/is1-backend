package ru.se.ifmo.is1.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.se.ifmo.is1.dto.ops.ManufactureCostGroupDTO;
import ru.se.ifmo.is1.dto.paging.PageRequestDTO;
import ru.se.ifmo.is1.dto.paging.PageResponseDTO;
import ru.se.ifmo.is1.mapper.ProductMapper;
import ru.se.ifmo.is1.model.Product;
import ru.se.ifmo.is1.repository.ProductOpsRepository;
import ru.se.ifmo.is1.ws.ChangePublisher;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductOpsService {

    private final ProductOpsRepository repo;
    private final ProductMapper mapper;

    private final ChangePublisher changesBroadcaster;

    @Transactional
    public Long deleteOneByRating(Number rating){
        Long id = repo.deleteOneByRating(rating);
        if (id != null && changesBroadcaster != null) {
            changesBroadcaster.broadcast("product", "deleted", id);
        }
        return id;
    }

    @Transactional
    public PageResponseDTO<ManufactureCostGroupDTO> groupByManufactureCost(PageRequestDTO pr){
        int page = Math.max(0, pr.getPage());
        int size = Math.max(1, pr.getSize());
        boolean asc = !"desc".equalsIgnoreCase(pr.getDir());
        long total = repo.countGroupByManufactureCost();

        List<Object[]> rows = repo.pageGroupByManufactureCost(pr.getSort(), asc, size, page * size);
        List<ManufactureCostGroupDTO> items = rows.stream()
                .map(r -> new ManufactureCostGroupDTO((Number) r[0], ((Number) r[1]).longValue()))
                .toList();

        return PageResponseDTO.of(items, page, size, total, pr.getSort(), pr.getDir());
    }

    @Transactional
    public PageResponseDTO<?> partNumberGt(String pn, PageRequestDTO pr){
        int page = Math.max(0, pr.getPage());
        int size = Math.max(1, pr.getSize());
        boolean asc = !"desc".equalsIgnoreCase(pr.getDir());
        long total = repo.countPartNumberGt(pn);

        List<Product> entities = repo.pagePartNumberGt(pn, pr.getSort(), asc, size, page * size);
        var items = entities.stream().map(mapper::toView).toList();

        return PageResponseDTO.of(items, page, size, total, pr.getSort(), pr.getDir());
    }

    @Transactional
    public PageResponseDTO<?> byManufacturer(Integer orgId, PageRequestDTO pr){
        int page = Math.max(0, pr.getPage());
        int size = Math.max(1, pr.getSize());
        boolean asc = !"desc".equalsIgnoreCase(pr.getDir());
        long total = repo.countByManufacturer(orgId);

        List<Product> entities = repo.pageByManufacturer(orgId, pr.getSort(), asc, size, page * size);
        var items = entities.stream().map(mapper::toView).toList();

        return PageResponseDTO.of(items, page, size, total, pr.getSort(), pr.getDir());
    }

    @Transactional
    public PageResponseDTO<?> byPriceRange(Number min, Number max, PageRequestDTO pr){
        int page = Math.max(0, pr.getPage());
        int size = Math.max(1, pr.getSize());
        boolean asc = !"desc".equalsIgnoreCase(pr.getDir());

        Number lo = Objects.requireNonNullElse(min, 0);
        Number hi = Objects.requireNonNullElse(max, Integer.MAX_VALUE);

        long total = repo.countByPriceRange(lo, hi);
        List<Product> entities = repo.pageByPriceRange(lo, hi, pr.getSort(), asc, size, page * size);
        var items = entities.stream().map(mapper::toView).toList();

        return PageResponseDTO.of(items, page, size, total, pr.getSort(), pr.getDir());
    }

}
