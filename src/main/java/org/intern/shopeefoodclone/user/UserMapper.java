package org.intern.shopeefoodclone.user;

import org.intern.shopeefoodclone.auth.RegisterRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", constant = "USER")
    User toEntity(RegisterRequest request);
    
    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "verifiedAt", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    void update(@MappingTarget User existing, User updated);
}
