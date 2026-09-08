package dream.maven.demoProject.controller;

import dream.maven.demoProject.common.Result;
import dream.maven.demoProject.dto.auth.*;
import dream.maven.demoProject.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success("登录成功", authService.login(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("退出成功", null);
    }

    @GetMapping("/userinfo")
    public Result<UserInfoResponse> getUserInfo() {
        return Result.success(authService.getUserInfo());
    }

    @PutMapping("/profile")
    public Result<UserInfoResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
        return Result.success("更新成功", authService.updateProfile(request));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return Result.success("修改成功", null);
    }
}
