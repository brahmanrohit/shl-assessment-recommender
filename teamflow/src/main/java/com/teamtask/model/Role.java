package com.teamtask.model;

/**
 * What a user is allowed to do (authorization).
 *
 * ADMIN  - full control, including deleting tasks
 * MEMBER - normal user: create/read/update tasks
 *
 * The role travels inside the JWT (in the "groups" claim), and endpoints
 * declare what they need with @RolesAllowed("ADMIN") etc.
 */
public enum Role {
    ADMIN,
    MEMBER
}
