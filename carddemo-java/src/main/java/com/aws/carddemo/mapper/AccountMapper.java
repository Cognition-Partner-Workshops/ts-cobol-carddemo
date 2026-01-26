package com.aws.carddemo.mapper;

import com.aws.carddemo.dto.AccountDto;
import com.aws.carddemo.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "availableCredit", expression = "java(account.getAvailableCredit())")
    @Mapping(target = "active", expression = "java(account.isActive())")
    @Mapping(target = "expired", expression = "java(account.isExpired())")
    AccountDto toDto(Account account);

    @Mapping(target = "cards", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Account toEntity(AccountDto dto);

    List<AccountDto> toDtoList(List<Account> accounts);

    @Mapping(target = "acctId", ignore = true)
    @Mapping(target = "cards", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(AccountDto dto, @MappingTarget Account account);
}
