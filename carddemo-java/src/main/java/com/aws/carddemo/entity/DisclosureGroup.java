package com.aws.carddemo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "disclosure_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisclosureGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Integer groupId;

    @Column(name = "group_code", nullable = false, unique = true, length = 10)
    private String groupCode;

    @Column(name = "group_name", nullable = false, length = 50)
    private String groupName;

    @Column(name = "group_desc", length = 200)
    private String groupDesc;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isEffective() {
        LocalDate today = LocalDate.now();
        boolean afterEffective = !today.isBefore(effectiveDate);
        boolean beforeExpiration = expirationDate == null || !today.isAfter(expirationDate);
        return active && afterEffective && beforeExpiration;
    }
}
