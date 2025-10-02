package org.shop.apiserver.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.util.CustomJWTException;
import org.shop.apiserver.util.JWTUtil;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

//refresh 토큰을 이용한 토큰 갱신 
@RestController
@RequiredArgsConstructor
@Log4j2
public class APIRefreshController {

  @RequestMapping("/api/member/refresh")
  public Map<String, Object> refresh(@RequestHeader("Authorization") String authHeader, String refreshToken){


    log.info("Try Refresh -------------------------------------------------");

    if(refreshToken == null) {
      throw new CustomJWTException("NULL_REFRASH");
    }
    
    if(authHeader == null || authHeader.length() < 7) {
      throw new CustomJWTException("INVALID_STRING");
    }

    String accessToken = authHeader.substring(7);

    log.info("Access token: " + accessToken);
    log.info("Refresh token: " + refreshToken);

    //Access 토큰이 만료되지 않았다면 
    if(checkExpiredToken(accessToken) == false ) {
      return Map.of("accessToken", accessToken, "refreshToken", refreshToken);
    }

    //Refresh토큰 검증 
    Map<String, Object> claims = JWTUtil.validateToken(refreshToken);

    log.info("refresh ... claims: " + claims);

    Map<String, Object> claimsCopy = new HashMap<>(claims);
    claimsCopy.remove("iat");
    claimsCopy.remove("exp");

    String newAccessToken = JWTUtil.generateToken(claimsCopy, 10);

    //log.info("-------------------");
    //log.info("New access token: " + newAccessToken);


    //24h
    String newRefreshToken =  checkTime((Long)claims.get("exp")) == true? JWTUtil.generateToken(claims, 60*24) : refreshToken;

    Map<String, Object> tokenResult =  Map.of("accessToken", newAccessToken, "refreshToken", newRefreshToken);

    log.info("token result: " + tokenResult);

    return tokenResult;

  }

  //시간이 5분 미만으로 남았다면
  private boolean checkTime(Long exp) {

    //JWT exp를 날짜로 변환
    java.util.Date expDate = new java.util.Date( (long)exp * (1000));

    //현재 시간과의 차이 계산 - 밀리세컨즈
    long gap   = expDate.getTime() - System.currentTimeMillis();


    log.info("-------------------------------------");
    log.info(gap);


    //분단위 계산 
    long leftMin = gap / (1000 * 60);

    //5분도 안남았는지..
    return leftMin < 5;
  }

  private boolean checkExpiredToken(String token) {

    try{
      JWTUtil.validateToken(token);

      log.info("token valid");

    }catch(Exception ex) {

      log.error(ex);

      if(ex.getClass() == CustomJWTException.class) {
        if(ex.getMessage().equals("Expired")) {
          return true;
        }
      }
    }
    return false;
  }
  
}
