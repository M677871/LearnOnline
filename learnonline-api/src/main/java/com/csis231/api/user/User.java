package com.csis231.api.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * JPA entity representing a user of the online learning platform.
 *
 * <p>Stores login credentials, basic profile information, activation flags
 * and the application role (ADMIN, INSTRUCTOR or STUDENT).</p>
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /** Primary key of the user. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique username used for login. */
    @NotBlank
    @Size(min = 3, max = 50)
    @Column(unique = true, nullable = false)
    private String username;

    /** Unique e-mail address of the user. */
    @NotBlank
    @Email
    @Column(unique = true, nullable = false)
    private String email;

    /** Hashed password stored in the database. */
    @NotBlank
    @Size(min = 6)
    @Column(nullable = false)
    private String password;

    /** Optional first name of the user. */
    @Column(name = "first_name")
    private String firstName;

    /** Optional last name of the user. */
    @Column(name = "last_name")
    private String lastName;

    /** Optional phone number of the user. */
    private String phone;

    /** Indicates whether the account is active and allowed to sign in. */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /** Indicates whether the user's e-mail address has been verified. */
    @Builder.Default
    private Boolean emailVerified = false;

    /** Indicates whether two-factor authentication via OTP is enabled. */
    @Builder.Default
    private Boolean twoFactorEnabled = false;

    /**
     * High-level role of the user, used for authorization in the platform.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Role role = Role.STUDENT;  // default

    /**
     * Application roles available in the platform.
     */
    public enum Role {
        ADMIN, INSTRUCTOR, STUDENT
    }
}