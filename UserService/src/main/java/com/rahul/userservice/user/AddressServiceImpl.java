package com.rahul.userservice.user;

import com.rahul.userservice.common.exception.InvalidCredentialsException;
import com.rahul.userservice.common.exception.ResourceNotFoundException;
import com.rahul.userservice.user.dto.AddressRequest;
import com.rahul.userservice.user.dto.AddressResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;



@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService{

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    public AddressResponse addAddress(String email, AddressRequest request){

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));

        if (request.isDefault()) {
            List<Address> existingAddresses = addressRepository.findByUserId(user.getId());
            existingAddresses.forEach(addr -> addr.setDefault(false));
            addressRepository.saveAll(existingAddresses);
        }

        Address address = Address.builder()
                .street(request.getStreet())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .country(request.getCountry())
                .user(user)
                .isDefault(request.isDefault())
                .build();

        Address savedAddress = addressRepository.save(address);

        return AddressResponse.builder()
                .id(savedAddress.getId())
                .street(savedAddress.getStreet())
                .city(savedAddress.getCity())
                .state(savedAddress.getState())
                .pincode(savedAddress.getPincode())
                .country(savedAddress.getCountry())
                .isDefault(savedAddress.isDefault())
                .createdAt(savedAddress.getCreatedAt())
                .build();
    }

    @Override
    public List<AddressResponse> getAddresses(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));

        List<Address> addresses = addressRepository.findByUserId(user.getId());

        return addresses.stream()
                .map(addr -> AddressResponse.builder()
                    .id(addr.getId())
                    .street(addr.getStreet())
                    .city(addr.getCity())
                    .state(addr.getState())
                    .pincode(addr.getPincode())
                    .country(addr.getCountry())
                    .isDefault(addr.isDefault())
                    .createdAt(addr.getCreatedAt())
                    .build())

                .collect(Collectors.toList());

    }

    @Override
    public AddressResponse updateAddress(String email, UUID addressId, AddressRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new InvalidCredentialsException("You are not allowed to update this address");
        }

        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setCountry(request.getCountry());

        Address updatedAddress = addressRepository.save(address);

        return AddressResponse.builder()
                .id(updatedAddress.getId())
                .street(updatedAddress.getStreet())
                .city(updatedAddress.getCity())
                .state(updatedAddress.getState())
                .pincode(updatedAddress.getPincode())
                .country(updatedAddress.getCountry())
                .isDefault(updatedAddress.isDefault())
                .createdAt(updatedAddress.getCreatedAt())
                .build();
    }

    @Override
    public void deleteAddress(String email, UUID addressId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new InvalidCredentialsException("You are not allowed to delete this address");
        }

        addressRepository.delete(address);
    }

    @Override
    public AddressResponse setDefaultAddress(String email, UUID addressId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new InvalidCredentialsException("You are not allowed to modify this address");
        }

        List<Address> existingAddresses = addressRepository.findByUserId(user.getId());
        existingAddresses.forEach(addr -> addr.setDefault(false));
        addressRepository.saveAll(existingAddresses);

        address.setDefault(true);
        Address updatedAddress = addressRepository.save(address);

        return AddressResponse.builder()
                .id(updatedAddress.getId())
                .street(updatedAddress.getStreet())
                .city(updatedAddress.getCity())
                .state(updatedAddress.getState())
                .pincode(updatedAddress.getPincode())
                .country(updatedAddress.getCountry())
                .isDefault(updatedAddress.isDefault())
                .createdAt(updatedAddress.getCreatedAt())
                .build();
    }

}
