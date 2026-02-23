package com.carddemo.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User security entity mapped from COBOL copybook CSUSR01Y.
 * Original VSAM file: AWS.M2.CARDDEMO.USRSEC.PS (KSDS, 80-byte records)
 * Primary key: SEC-USR-ID PIC X(08)
 *
 * User types:
 *   'A' = Administrator (ROLE_ADMIN)
 *   'U' = Regular user  (ROLE_USER)
 */
@Entity
@Table(name = "user_security")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSecurity {

    @Id
    @Column(name = "usr_id", length = 8)
    private String usrId;

    @NotNull
    @Column(name = "usr_first_name", length = 20)
    private String usrFirstName;

    @NotNull
    @Column(name = "usr_last_name", length = 20)
    private String usrLastName;

    @NotNull
    @Column(name = "usr_password", length = 72)
    private String usrPassword;

    @NotNull
    @Column(name = "usr_type", length = 1)
    private String usrType;
}
