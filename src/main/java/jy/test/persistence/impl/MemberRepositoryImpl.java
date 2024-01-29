package jy.test.persistence.impl;

import jy.test.entity.Member;
import jy.test.persistence.MemberJpaRepository;
import jy.test.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {

    private final MemberJpaRepository jpaRepository;


    @Override
    public Member save(Member member) {
        return jpaRepository.save(member);
    }

    @Override
    public Optional<Member> findById(Long memberId) {
        return jpaRepository.findById(memberId);
    }

    @Override
    public Optional<Member> findByBillingKey(String billingKey) {
        return jpaRepository.findByBillingKey(billingKey);
    }
}
