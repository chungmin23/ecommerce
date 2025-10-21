package org.shop.apiserver.infrastructure.security.filter;

import com.google.gson.Gson;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.application.dto.MemberDTO;
import org.shop.apiserver.util.JWTUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@Log4j2
public class JWTCheckFilter extends OncePerRequestFilter {

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

    // Preflight요청은 체크하지 않음
    if(request.getMethod().equals("OPTIONS")){
      return true;
    }



    String path = request.getRequestURI();

    log.info("check uri.............." + path);


    if(path.startsWith("/api/recommendations")) {
      return true;  // ← 이 부분 추가 필요!
    }


    //api/member/ 경로의 호출은 체크하지 않음
    if(path.startsWith("/api/member/")) {
      return true;
    }

    //이미지 조회 경로는 체크하지 않는다면
    if(path.startsWith("/api/products/view/")) {
      return true;
    }

    // 상품 목록/상세 조회는 인증 없이 허용
    if(path.startsWith("/api/products/list") || path.matches("/api/products/\\d+.*")) {
      return true;
    }

    // 활성 쿠폰 목록 조회는 인증 없이 허용
    if(path.equals("/api/coupons/active")) {
      return true;
    }

    return false;
  }



  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {

    log.info("------------------------JWTCheckFilter.......................");

    String authHeaderStr = request.getHeader("Authorization");

    // Authorization 헤더 검증
    if(authHeaderStr == null || !authHeaderStr.startsWith("Bearer ")) {
      log.error("Authorization header is missing or invalid");

      Gson gson = new Gson();
      String msg = gson.toJson(Map.of("error", "ERROR_ACCESS_TOKEN"));

      response.setContentType("application/json");
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      PrintWriter printWriter = response.getWriter();
      printWriter.println(msg);
      printWriter.close();
      return;
    }

    try {
      //Bearer accestoken...
      String accessToken = authHeaderStr.substring(7);
      Map<String, Object> claims = JWTUtil.validateToken(accessToken);

      log.info("JWT claims: " + claims);

      String email = (String) claims.get("email");
      String pw = (String) claims.get("pw");
      String nickname = (String) claims.get("nickname");
      Boolean social = (Boolean) claims.get("social");
      List<String> roleNames = (List<String>) claims.get("roleNames");

      MemberDTO memberDTO = new MemberDTO(email, pw, nickname, social.booleanValue(), roleNames);

      log.info("-----------------------------------");
      log.info(memberDTO);
      log.info(memberDTO.getAuthorities());

      UsernamePasswordAuthenticationToken authenticationToken
              = new UsernamePasswordAuthenticationToken(memberDTO, pw, memberDTO.getAuthorities());

      SecurityContextHolder.getContext().setAuthentication(authenticationToken);

      filterChain.doFilter(request, response);

    }catch(Exception e){

      log.error("JWT Check Error..............");
      log.error(e.getMessage());

      Gson gson = new Gson();
      String msg = gson.toJson(Map.of("error", "ERROR_ACCESS_TOKEN"));

      response.setContentType("application/json");
      PrintWriter printWriter = response.getWriter();
      printWriter.println(msg);
      printWriter.close();
    }
  }
}