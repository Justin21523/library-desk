package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.model.Branch;
import com.justin.libradesk.domain.model.ShelfLocation;
import com.justin.libradesk.repository.BranchRepository;
import com.justin.libradesk.repository.LocationRepository;
import com.justin.libradesk.validation.ValidationException;

import java.util.List;

/**
 * Manages branches and their shelving locations. Used by the Reference Data
 * screen and to populate the location picker when adding copies.
 */
public class LocationService {

    private final BranchRepository branchRepository;
    private final LocationRepository locationRepository;
    private final AuditLogService auditLogService;

    public LocationService(BranchRepository branchRepository, LocationRepository locationRepository,
                           AuditLogService auditLogService) {
        this.branchRepository = branchRepository;
        this.locationRepository = locationRepository;
        this.auditLogService = auditLogService;
    }

    public List<Branch> listBranches() {
        return branchRepository.findAll();
    }

    public Branch addBranch(String code, String name, String actor) {
        if (code == null || code.isBlank() || name == null || name.isBlank()) {
            throw new ValidationException("Branch code and name are required");
        }
        Branch saved = branchRepository.save(new Branch(null, code.trim(), name.trim()));
        auditLogService.record(actor, "BRANCH_ADDED", "Branch", saved.id(), saved.code());
        return saved;
    }

    public void deleteBranch(Long id, String actor) {
        branchRepository.deleteById(id);
        auditLogService.record(actor, "BRANCH_DELETED", "Branch", id, null);
    }

    public List<ShelfLocation> listLocations() {
        return locationRepository.findAll();
    }

    public List<ShelfLocation> listLocations(Long branchId) {
        return locationRepository.findByBranch(branchId);
    }

    public ShelfLocation addLocation(Long branchId, String name, String actor) {
        if (branchId == null) {
            throw new ValidationException("Select a branch first");
        }
        if (name == null || name.isBlank()) {
            throw new ValidationException("Location name is required");
        }
        ShelfLocation saved = locationRepository.save(new ShelfLocation(null, branchId, name.trim()));
        auditLogService.record(actor, "LOCATION_ADDED", "Location", saved.id(), saved.name());
        return saved;
    }

    public void deleteLocation(Long id, String actor) {
        locationRepository.deleteById(id);
        auditLogService.record(actor, "LOCATION_DELETED", "Location", id, null);
    }
}
