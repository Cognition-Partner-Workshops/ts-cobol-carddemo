package com.aws.carddemo.mapper;

import com.aws.carddemo.dto.CustomerDto;
import com.aws.carddemo.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "fullName", expression = "java(customer.getFullName())")
    @Mapping(target = "primaryCardHolder", expression = "java(customer.isPrimaryCardHolder())")
    CustomerDto toDto(Customer customer);

    @Mapping(target = "cardXrefs", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Customer toEntity(CustomerDto dto);

    List<CustomerDto> toDtoList(List<Customer> customers);

    @Mapping(target = "custId", ignore = true)
    @Mapping(target = "cardXrefs", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CustomerDto dto, @MappingTarget Customer customer);
}
