package org.shop.apiserver.application.service;

import org.shop.apiserver.domain.model.member.Member;
import org.shop.apiserver.domain.model.member.MemberModifyDTO;
import org.shop.apiserver.application.dto.MemberDTO;
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
