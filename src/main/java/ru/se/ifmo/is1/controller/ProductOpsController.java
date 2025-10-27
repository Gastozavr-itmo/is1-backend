package ru.se.ifmo.is1.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ru.se.ifmo.is1.dto.ops.ManufactureCostGroupDTO;
import ru.se.ifmo.is1.dto.paging.PageRequestDTO;
import ru.se.ifmo.is1.dto.paging.PageResponseDTO;
import ru.se.ifmo.is1.service.ProductOpsService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/product-ops", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ProductOpsController {

    private final ProductOpsService service;
    private PageRequestDTO pr(Integer page, Integer size, String sort, String dir) {
        int p = page != null ? page : 0;
        int s = size != null ? size : 10;
        String so = (sort != null && !sort.isBlank()) ? sort : "id";
        String di = (dir != null && !dir.isBlank()) ? dir : "asc";
        return new PageRequestDTO(p, s, so, di);
    }

    @DeleteMapping("/delete-one-by-rating")
    public Map<String, Object> deleteOne(@RequestParam("rating") Number rating) {
        Long id = service.deleteOneByRating(rating);
        Map<String, Object> resp = new HashMap<>();
        resp.put("deletedId", id);
        return resp;
    }

    @GetMapping("/group-by-manufacture-cost")
    public PageResponseDTO<ManufactureCostGroupDTO> group(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "dir",  required = false) String dir
    ){
        return service.groupByManufactureCost(pr(page, size, sort, dir));
    }

    @GetMapping("/part-number-gt")
    public PageResponseDTO<?> partNumberGt(
            @RequestParam("pn") String pn,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "dir",  required = false) String dir
    ){
        return service.partNumberGt(pn, pr(page, size, sort, dir));
    }

    @GetMapping("/by-manufacturer/{orgId}")
    public PageResponseDTO<?> byManufacturer(
            @PathVariable("orgId") Integer orgId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "dir",  required = false) String dir
    ){
        return service.byManufacturer(orgId, pr(page, size, sort, dir));
    }

    @GetMapping("/by-price-range")
    public PageResponseDTO<?> byPriceRange(
            @RequestParam(value = "min", required = false) Number min,
            @RequestParam(value = "max", required = false) Number max,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "dir",  required = false) String dir
    ){
        return service.byPriceRange(min, max, pr(page, size, sort, dir));
    }
}
