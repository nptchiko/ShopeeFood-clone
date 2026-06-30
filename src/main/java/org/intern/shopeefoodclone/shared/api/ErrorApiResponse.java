package org.intern.shopeefoodclone.shared.api;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorApiResponse(
    String message,
    int status,
    String detailMessage
) { }
