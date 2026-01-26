package com.aws.carddemo.mapper;

import com.aws.carddemo.dto.UserDto;
import com.aws.carddemo.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    @Mapping(target = "admin", expression = "java(user.isAdmin())")
    @Mapping(target = "userPassword", ignore = true)
    UserDto toDto(User user);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(UserDto dto);

    List<UserDto> toDtoList(List<User> users);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "userPassword", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UserDto dto, @MappingTarget User user);
}
