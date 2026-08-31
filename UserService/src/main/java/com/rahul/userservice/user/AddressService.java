package com.rahul.userservice.user;

import com.rahul.userservice.user.dto.AddressRequest;
import com.rahul.userservice.user.dto.AddressResponse;
import com.rahul.userservice.user.dto.*;
import java.util.List;
import java.util.UUID;

public interface AddressService {

    AddressResponse addAddress(String email, AddressRequest request);

    List<AddressResponse> getAddresses(String email);

    AddressResponse updateAddress(String email, UUID addressId, AddressRequest request);
    void deleteAddress(String email, UUID addressId);
    AddressResponse setDefaultAddress(String email, UUID addressId);

}
