package com.nadra.ems.auth.domain.model;

import lombok.Data;

import java.time.Instant;

/**
 * Core domain model representing a role in the multi-role RBAC system.
 * Pure domain object — no framework annotations.
 */
@Data
public class Role {

    private Integer id;
    private String name;
    private String description;
    private Instant createdAt;

    public Role() {
    }

    public Role(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Role(Integer id, String name, String description, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }
}
