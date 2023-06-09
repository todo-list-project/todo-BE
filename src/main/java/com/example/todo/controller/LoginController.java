package com.example.todo.controller;

import com.example.todo.dto.TokenDTO;
import com.example.todo.dto.UserDTO;
import com.example.todo.service.LoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "")
public class LoginController {

    @Autowired
    private LoginService loginService;


    // 회원가입
    @PostMapping("/join")
    public ResponseEntity<Void> join(@RequestBody UserDTO user) throws Exception {
        loginService.join(user);
        return ResponseEntity.ok(null);
    }

    // 로그인
    @PostMapping("/login")
    public List<TokenDTO> login(@RequestBody UserDTO user) throws Exception {
        return loginService.login(user);
    }

    // 로그아웃
    @PostMapping("/log-out")
    public ResponseEntity<String> logout(HttpServletRequest request) throws Exception {
        return ResponseEntity.ok(loginService.logout(request));
    }

    // 비밀번호 찾기
    @PostMapping("/findpw")
    public ResponseEntity<String> findPw(@RequestBody UserDTO user) throws Exception {
        return ResponseEntity.ok(loginService.findPw(user));
    }

    // Access 토큰 재발급 api
    @PostMapping("/accesstoken")
    public String getNewAccessToken(@RequestBody UserDTO user, HttpServletRequest request) throws Exception {
        return loginService.getNewAccessToken(user.getEmail(), request);
    }
}
