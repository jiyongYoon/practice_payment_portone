package jy.test.repository;

import jy.test.entity.Member;

import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(Long memberId);

    Optional<Member> findByBillingKey(String billingKey);
}
