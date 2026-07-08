package org.intern.shopeefoodclone.user.address;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.intern.shopeefoodclone.shared.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class AddressController {

    AddressService addressService;

    // ─── User-scoped address endpoints (/api/users/me/addresses) ─────────────

    @GetMapping("/api/users/me/addresses")
    public ApiResponse<List<AddressResponse>> getMyAddresses() {
        return ApiResponse.success(addressService.getMyAddresses(), "Addresses retrieved successfully");
    }

    @PostMapping("/api/users/me/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AddressResponse> create(@Valid @RequestBody AddressCreateRequest request) {
        return ApiResponse.created(addressService.create(request), "Address created successfully");
    }

    @GetMapping("/api/users/me/addresses/{addressId}")
    public ApiResponse<AddressResponse> getMyAddressById(@PathVariable UUID addressId) {
        return ApiResponse.success(addressService.getMyAddressById(addressId), "Address retrieved successfully");
    }

    @PutMapping("/api/users/me/addresses/{addressId}")
    public ApiResponse<AddressResponse> update(
            @PathVariable UUID addressId,
            @Valid @RequestBody AddressUpdateRequest request) {
        return ApiResponse.success(addressService.update(addressId, request), "Address updated successfully");
    }

    @DeleteMapping("/api/users/me/addresses/{addressId}")
    public ApiResponse<Void> delete(@PathVariable UUID addressId) {
        addressService.delete(addressId);
        return ApiResponse.success("Address deleted successfully");
    }

    // ─── Generic address lookup (used by restaurants) ────────────────────────

    @GetMapping("/api/addresses/{id}")
    public ApiResponse<AddressResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(addressService.getById(id), "Address retrieved successfully");
    }
}
