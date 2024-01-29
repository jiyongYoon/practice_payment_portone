package jy.test.service;

import jy.test.entity.Member;
import jy.test.persistence.impl.MemberRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepositoryImpl memberRepository;

    public Member autoRegister() {
        Member member = Member.builder()
                .username(UUID.randomUUID().toString())
                .email("example@example.com")
                .address("서울특별시 서초구 역삼동")
                .billingKey("billing_" + UUID.randomUUID().toString().substring(0, 8))
                .build();

        return memberRepository.save(member);
    }

    public Member getValidMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(NoSuchElementException::new);
    }
}
