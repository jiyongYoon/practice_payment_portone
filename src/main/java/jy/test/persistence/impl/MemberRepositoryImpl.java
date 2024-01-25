package jy.test.persistence.impl;

import jy.test.entity.Member;
import jy.test.persistence.MemberJpaRepository;
import jy.test.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {

    private final MemberJpaRepository jpaRepository;


    @Override
    public Member save(Member member) {
        return jpaRepository.save(member);
    }
}
