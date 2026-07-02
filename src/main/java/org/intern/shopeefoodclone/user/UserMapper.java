package org.intern.shopeefoodclone.user;

import org.intern.shopeefoodclone.auth.RegisterRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", constant = "USER")
    User toEntity(RegisterRequest request);
    
    UserResponse toResponse(User user);
}
