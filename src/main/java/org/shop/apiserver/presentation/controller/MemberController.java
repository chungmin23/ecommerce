package org.shop.apiserver.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.domain.model.member.Member;
import org.shop.apiserver.domain.model.member.MemberJoinRequest;
import org.shop.apiserver.domain.model.member.MemberRole;
import org.shop.apiserver.infrastructure.persistence.jpa.MemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
@Log4j2
public class MemberController {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입
     * POST /api/member/join
     */
    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody MemberJoinRequest request) {

        log.info("회원가입 요청: " + request.getEmail());

        // 1. 이메일 중복 체크
        if (memberRepository.findById(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "이미 사용 중인 이메일입니다."));
        }

        // 2. 회원 생성
        Member member = Member.builder()
                .email(request.getEmail())
                .pw(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .social(false)
                .build();

        // 3. 기본 권한 부여
        member.addRole(MemberRole.USER);

        // 4. 저장
        memberRepository.save(member);

        log.info("회원가입 완료: " + member.getEmail());

        return ResponseEntity.ok(Map.of(
                "result", "SUCCESS",
                "email", member.getEmail()
        ));
    }

    /**
     * 이메일 중복 체크
     * GET /api/member/check-email?email=xxx
     */
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {

        boolean exists = memberRepository.findById(email).isPresent();

        return ResponseEntity.ok(Map.of(
                "available", !exists
        ));
    }
}