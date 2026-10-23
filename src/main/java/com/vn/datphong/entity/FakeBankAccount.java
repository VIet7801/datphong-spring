package com.vn.datphong.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fakebank_accounts")
public class FakeBankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "account_no", nullable = false, unique = true, length = 40)
    private String accountNo;
    @Column(name = "holder_name", nullable = false, length = 120)
    private String holderName;
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;
    @Column(nullable = false, length = 5)
    private String currency = "VND";
    @Version
    private Long version;
}
