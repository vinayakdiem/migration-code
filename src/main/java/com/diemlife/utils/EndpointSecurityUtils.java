/*package com.diemlife.utils;

import java.util.Optional;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Admin;

import com.diemlife.models.SecurityRole;
import com.diemlife.models.User;

public abstract class EndpointSecurityUtils {

    private EndpointSecurityUtils() {
        super();
    }

    public static boolean isUserAdmin(final User user) {
        return Optional.ofNullable(user)
                .map(User::getSecurityRoles)
                .filter(roles -> roles.stream().map(SecurityRole::getName).anyMatch(Admin.USER_ROLE::equalsIgnoreCase))
                .isPresent();
    }

}
*/