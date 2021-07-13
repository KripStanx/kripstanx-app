package com.kripstanx.service.dto;

import com.kripstanx.domain.User;

/**
 * A DTO representing a user, with only the public attributes.
 */
public class UserDTO {

    private Long id;

    private String username;

    public UserDTO() {
        // Empty constructor needed for Jackson.
    }

    public UserDTO(User user) {
        this.id = user.getId();
        // Customize it here if you need, or not, firstName/lastName/etc
        this.username = user.getUsername();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "UserDTO{" +
            "id='" + id + '\'' +
            ", username='" + username + '\'' +
            "}";
    }
}
