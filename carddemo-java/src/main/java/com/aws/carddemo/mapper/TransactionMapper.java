package com.aws.carddemo.mapper;

import com.aws.carddemo.dto.TransactionDto;
import com.aws.carddemo.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "tranCardNum", source = "card.cardNum")
    @Mapping(target = "credit", expression = "java(transaction.isCredit())")
    @Mapping(target = "debit", expression = "java(transaction.isDebit())")
    TransactionDto toDto(Transaction transaction);

    @Mapping(target = "card", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Transaction toEntity(TransactionDto dto);

    List<TransactionDto> toDtoList(List<Transaction> transactions);
}
