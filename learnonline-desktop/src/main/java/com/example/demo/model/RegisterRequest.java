package com.example.demo.model;

/**
 * Request body used by the desktop client to register a new user.
 *
 * <p>Most fields map directly to the backend's user entity. The {@code role}
 * field must contain one of the backend role codes such as {@code "STUDENT"}
 * or {@code "INSTRUCTOR"}.</p>
 *
 * @param username  unique username chosen by the user
 * @param email     e-mail address of the user
 * @param password  raw password chosen by the user
 * @param firstName optional first name
 * @param lastName  optional last name
 * @param phone     optional phone number
 * @param role      role code understood by the backend
 */
public record RegisterRequest(
        String username,
        String email,
        String password,
        String firstName,
        String lastName,
        String phone,
        String role
) {}
