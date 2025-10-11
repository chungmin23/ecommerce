package org.shop.apiserver.domain.model.member;

import lombok.Data;

@Data
public class MemberJoinRequest {
    private String email;
    private String password;
    private String nickname;
    private String phone;
}