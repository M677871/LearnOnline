package com.csis231.api.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @NotBlank
    @Size(min = 3, max = 50)
    @Column(unique = true, nullable = false)
    private String username;

    @Setter
    @NotBlank
    @Email
    @Column(unique = true, nullable = false)
    private String email;

    /** BCrypt hash, never the raw password. */
    @Setter
    @NotBlank
    @Size(min = 6)
    @Column(nullable = false)
    private String password;

    @Setter
    @Column(name = "first_name")
    private String firstName;

    @Setter
    @Column(name = "last_name")
    private String lastName;

    @Setter
    private String phone;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean emailVerified = false;

    @Builder.Default
    private Boolean twoFactorEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Role role = Role.STUDENT;

    public enum Role {
        ADMIN, INSTRUCTOR, STUDENT
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isInstructor() {
        return role == Role.INSTRUCTOR;
    }

    public boolean isStudent() {
        return role == Role.STUDENT;
    }
}
