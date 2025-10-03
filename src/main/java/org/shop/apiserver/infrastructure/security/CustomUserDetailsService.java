package org.shop.apiserver.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.domain.model.member.Member;
import org.shop.apiserver.application.dto.MemberDTO;
import org.shop.apiserver.infrastructure.persistence.jpa.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * CustomUSerDetailsService
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService{

  private final MemberRepository memberRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    
    log.info("----------------loadUserByUsername-----------------------------");

    Member member = memberRepository.getWithRoles(username);

    if(member == null){
      throw new UsernameNotFoundException("Not Found");
    }

    MemberDTO memberDTO = new MemberDTO(
            member.getEmail(),
            member.getPw(),
            member.getNickname(),
            member.isSocial(),
            member.getMemberRoleList()
                  .stream()
                  .map(memberRole -> memberRole.name()).collect(Collectors.toList()));

    log.info(memberDTO);

    return memberDTO;

  }
  
}
