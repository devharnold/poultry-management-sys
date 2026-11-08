package com.poultry.pms.service;

import com.poultry.pms.dto.FarmRequest;
import com.poultry.pms.dto.FarmResponse;
import com.poultry.pms.entity.Farm;
import com.poultry.pms.exception.ConflictException;
import com.poultry.pms.exception.NotFoundException;
import com.poultry.pms.repository.FarmRepository;
import com.poultry.pms.repository.FlockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
public class FarmService {

    private final FarmRepository farms;
    private final FlockRepository flocks;

    public FarmService(FarmRepository farms, FlockRepository flocks) {
        this.farms = farms;
        this.flocks = flocks;
    }

    /** Optional search text matches the farm name or location (case-insensitive). */
    public List<FarmResponse> list(String search) {
        String needle = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return farms.findAll().stream()
                .filter(f -> needle.isEmpty() || contains(f.getName(), needle) || contains(f.getLocation(), needle))
                .sorted(Comparator.comparing((Farm f) -> f.getName().toLowerCase(Locale.ROOT)))
                .map(this::toResponse)
                .toList();
    }

    public FarmResponse get(Long id) {
        return toResponse(find(id));
    }

    public FarmResponse create(FarmRequest request) {
        String name = request.name().trim();
        if (farms.findByNameIgnoreCase(name).isPresent()) {
            throw new ConflictException("name", "A farm named '" + name + "' already exists.");
        }
        Farm farm = farms.save(new Farm(name, clean(request.location()), clean(request.contactNotes())));
        return toResponse(farm);
    }

    public FarmResponse update(Long id, FarmRequest request) {
        Farm farm = find(id);
        String name = request.name().trim();
        Optional<Farm> sameName = farms.findByNameIgnoreCase(name);
        if (sameName.isPresent() && !sameName.get().getId().equals(id)) {
            throw new ConflictException("name", "Another farm is already named '" + name + "'.");
        }
        farm.setName(name);
        farm.setLocation(clean(request.location()));
        farm.setContactNotes(clean(request.contactNotes()));
        return toResponse(farms.saveAndFlush(farm));
    }

    /** Deleting is only allowed when no flock belongs to the farm, so related records are never orphaned. */
    public void delete(Long id) {
        Farm farm = find(id);
        long flockCount = flocks.countByFarmId(id);
        if (flockCount > 0) {
            throw new ConflictException(null, "This farm cannot be deleted because it still has " + flockCount
                    + " flock(s). Move or remove the flocks first.");
        }
        farms.delete(farm);
    }

    Farm find(Long id) {
        return farms.findById(id).orElseThrow(() -> new NotFoundException("Farm " + id + " was not found."));
    }

    private FarmResponse toResponse(Farm farm) {
        return FarmResponse.from(farm, flocks.countByFarmId(farm.getId()));
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
