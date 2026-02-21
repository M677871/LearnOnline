package com.example.demo.model;

/**
 * Request body used to authenticate a user with username and password.
 *
 * @param username the username (or login identifier) entered on the login screen
 * @param password the raw password entered on the login screen
 */
public record LoginRequest(String username, String password) {}
