package com.kripstanx.config;

import com.kripstanx.domain.User;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "application", ignoreUnknownFields = true, ignoreInvalidFields = true)
public class ApplicationProperties {

    private Integer inactivityTimeoutMinutes = 30;

    private List<User> defaultUsers;

    public List<User> getDefaultUsers() {
        return defaultUsers;
    }

    public void setDefaultUsers(List<User> defaultUsers) {
        this.defaultUsers = defaultUsers;
    }

    public Integer getInactivityTimeoutMinutes() {
        return this.inactivityTimeoutMinutes;
    }

    public void setInactivityTimeoutMinutes(Integer inactivityTimeoutMinutes) {
        this.inactivityTimeoutMinutes = inactivityTimeoutMinutes;
    }

}
