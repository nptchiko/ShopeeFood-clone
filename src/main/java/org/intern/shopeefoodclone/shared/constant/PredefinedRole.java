package org.intern.shopeefoodclone.shared.constant;

public enum PredefinedRole {
    USER("USER"),
    OWNER("OWNER"),
    ADMIN("ADMIN");

    private String roleName;

    PredefinedRole(String role) {
        this.roleName = role;
    }
}
