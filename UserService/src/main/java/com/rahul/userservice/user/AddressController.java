package com.rahul.userservice.user;

import com.rahul.userservice.common.ApiResponse;
import com.rahul.userservice.user.dto.AddressRequest;
import com.rahul.userservice.user.dto.AddressResponse;
import com.rahul.userservice.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(@Valid @RequestBody AddressRequest request) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        AddressResponse addressResponse = addressService.addAddress(email, request);

        ApiResponse<AddressResponse> response = ApiResponse.<AddressResponse>builder()
                .success(true)
                .message("Address added successfully")
                .data(addressResponse)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        List<AddressResponse> addressResponses = addressService.getAddresses(email);

        ApiResponse<List<AddressResponse>> response = ApiResponse.<List<AddressResponse>>builder()
                .success(true)
                .message("Addresses fetched successfully")
                .data(addressResponses)
                .build();
        return ResponseEntity.ok(response);

    }

    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(@PathVariable UUID addressId, @Valid @RequestBody AddressRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        AddressResponse addressResponse = addressService.updateAddress(email, addressId, request);

        ApiResponse<AddressResponse> response = ApiResponse.<AddressResponse>builder()
                .success(true)
                .message("Addresses updated successfully")
                .data(addressResponse)
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable UUID addressId) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        addressService.deleteAddress(email, addressId);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Address deleted")
                .data(null)
                .build();
        return ResponseEntity.ok(response);

    }

    @PutMapping("/{addressId}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(@PathVariable UUID addressId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        AddressResponse addressResponse = addressService.setDefaultAddress(email, addressId);

        ApiResponse<AddressResponse> response = ApiResponse.<AddressResponse>builder()
                .success(true)
                .message("Default address updated successfully")
                .data(addressResponse)
                .build();
        return ResponseEntity.ok(response);
    }
}