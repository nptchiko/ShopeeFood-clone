package org.intern.shopeefoodclone.shared.utils;

import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;


public class SecurityUtils {

    private SecurityUtils() {
    }

    public static String getCurrentUserId(){
        SecurityContext context = SecurityContextHolder.getContext();
        return extractPrincipal(context.getAuthentication());
    }

    // retrieve id
    private static String extractPrincipal(Authentication authentication){
        if (authentication == null)
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        else if (authentication.getPrincipal() instanceof UserDetails userDetails){
            return userDetails.getUsername();
        } else if (authentication.getPrincipal() instanceof String s){
            return s;
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }


}

