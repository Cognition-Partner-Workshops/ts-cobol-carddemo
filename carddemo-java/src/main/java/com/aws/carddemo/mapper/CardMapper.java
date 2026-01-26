package com.aws.carddemo.mapper;

import com.aws.carddemo.dto.CardDto;
import com.aws.carddemo.entity.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardMapper {

    @Mapping(target = "cardAcctId", source = "account.acctId")
    @Mapping(target = "maskedCardNumber", expression = "java(card.getMaskedCardNumber())")
    @Mapping(target = "active", expression = "java(card.isActive())")
    @Mapping(target = "expired", expression = "java(card.isExpired())")
    CardDto toDto(Card card);

    @Mapping(target = "account", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "cardXref", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Card toEntity(CardDto dto);

    List<CardDto> toDtoList(List<Card> cards);

    @Mapping(target = "cardNum", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "cardXref", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CardDto dto, @MappingTarget Card card);
}
