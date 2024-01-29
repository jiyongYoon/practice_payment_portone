package jy.test.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String email;
    private String address;
    private String billingKey; // 카드가 1개만 있다고 가정 -> 여러개라면 table 분리하여 관리 필요
    private boolean billingIssuance;

    @Builder
    public Member(String username, String email, String address, String billingKey) {
        this.username = username;
        this.email = email;
        this.address = address;
        this.billingKey = billingKey;
        this.billingIssuance = false;
    }

    public void billingIssued() {
        this.billingIssuance = true;
    }

    public void isBillingIssued() {
         if (!this.billingIssuance) {
             throw new RuntimeException("빌링키 발급 안됨");
         }
    }
}
