package com.artezo.service;

import com.artezo.dto.request.ShippingAddressRequestDTO;
import com.artezo.dto.response.ShippingAddressResponseDTO;

import java.util.List;

public interface ShippingAddressService {

    ShippingAddressResponseDTO addAddress(Long userId, ShippingAddressRequestDTO dto);

    List<ShippingAddressResponseDTO> getAllAddresses(Long userId);

//    ShippingAddressResponseDTO getDefaultAddress(Long userId);

    ShippingAddressResponseDTO getAddressById(Long userId, Long shippingId);

    ShippingAddressResponseDTO patchAddress(Long userId, Long shippingId, ShippingAddressRequestDTO dto);

    void setDefaultAddress(Long userId, Long shippingId);

    void deleteAddress(Long userId, Long shippingId);
}