package com.aws.cardemo.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "user_id", length = 8)
    private String userId;

    @NotNull
    @Column(name = "password", length = 8)
    private String password;

    @NotNull
    @Column(name = "first_name", length = 20)
    private String firstName;

    @NotNull
    @Column(name = "last_name", length = 20)
    private String lastName;

    @NotNull
    @Column(name = "user_type", length = 1)
    private String userType;
}
