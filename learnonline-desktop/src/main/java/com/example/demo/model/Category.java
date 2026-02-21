package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Client-side representation of a course category.
 *
 * <p>Matches the structure of the backend {@code Category} entity and is used
 * when listing, creating or editing categories in the desktop client.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Category {
    private Long id;
    private String name;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
