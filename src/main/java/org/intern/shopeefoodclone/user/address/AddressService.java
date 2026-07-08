package org.intern.shopeefoodclone.user.address;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.intern.shopeefoodclone.shared.utils.SecurityUtils;
import org.intern.shopeefoodclone.user.User;
import org.intern.shopeefoodclone.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class AddressService {

    AddressRepository addressRepository;
    UserRepository userRepository;
    AddressMapper addressMapper;

    // ─── User address endpoints ───────────────────────────────────────────────

    /**
     * List all addresses belonging to the currently authenticated user.
     */
    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses() {
        UUID userId = currentUserId();
        return addressMapper.toResponseList(addressRepository.findByUserId(userId));
    }

    /**
     * Create a new address for the currently authenticated user.
     * If {@code isDefault} is true, any existing default address for this user is cleared first.
     */
    @Transactional
    public AddressResponse create(AddressCreateRequest request) {
        UUID userId = currentUserId();
        User user = findUser(userId);

        Address address = addressMapper.toEntity(request);
        address.setUser(user);

        address.setIsDefault(Boolean.TRUE.equals(request.isDefault()));

        Address saved = addressRepository.save(address);

        if (Boolean.TRUE.equals(saved.getIsDefault())) {
            addressRepository.clearDefaultForUser(userId, saved.getId());
        }

        return addressMapper.toResponse(saved);
    }

    /**
     * Get a single address by id — must belong to the authenticated user.
     */
    @Transactional(readOnly = true)
    public AddressResponse getMyAddressById(UUID addressId) {
        return addressMapper.toResponse(findOwnedAddress(addressId));
    }

    /**
     * Partial update of an address owned by the authenticated user.
     * If {@code isDefault} is set to true the other addresses are cleared.
     */
    @Transactional
    public AddressResponse update(UUID addressId, AddressUpdateRequest request) {
        UUID userId = currentUserId();
        Address address = findOwnedAddress(addressId);

        addressMapper.update(address, request);
        Address saved = addressRepository.save(address);

        if (Boolean.TRUE.equals(saved.getIsDefault())) {
            addressRepository.clearDefaultForUser(userId, saved.getId());
        }

        return addressMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID addressId) {
        Address address = findOwnedAddress(addressId);
        addressRepository.delete(address);
    }

    // ─── Restaurant address lookup ────────────────────────────────────────────

    /**
     * Fetch a single address by its UUID — used when looking up a restaurant's
     * address without tying it to a specific user owner.
     */
    @Transactional(readOnly = true)
    public AddressResponse getById(UUID addressId) {
        return addressMapper.toResponse(
                addressRepository.findById(addressId)
                        .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND,
                                "Address not found with id: " + addressId))
        );
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private UUID currentUserId() {
        return UUID.fromString(SecurityUtils.getCurrentUserId());
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND,
                        "User not found with id: " + userId));
    }

    private Address findOwnedAddress(UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND,
                        "Address not found with id: " + addressId));

        UUID userId = currentUserId();
        if (!userId.equals(address.getUser().getId())) {
            throw new AppException(ErrorCode.FORBIDDEN,
                    "You do not have permission to access this address");
        }
        return address;
    }
}
