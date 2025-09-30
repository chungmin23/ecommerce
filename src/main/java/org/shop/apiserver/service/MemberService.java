package org.shop.apiserver.service;

import org.shop.apiserver.domain.Member;
import org.shop.apiserver.domain.MemberModifyDTO;
import org.shop.apiserver.dto.MemberDTO;
import org.springframework.transaction.annotation.Transactional;


import java.util.stream.Collectors;

@Transactional
public interface MemberService {
  
  MemberDTO getKakaoMember(String accessToken);

  void modifyMember(MemberModifyDTO memberModifyDTO);

  default MemberDTO entityToDTO(Member member) {

    MemberDTO dto = new MemberDTO(
      member.getEmail(), 
      member.getPw(), 
      member.getNickname(), 
      member.isSocial(), 
      member.getMemberRoleList().stream().map(memberRole -> memberRole.name()).collect(Collectors.toList()));

    return dto;
  }

}
