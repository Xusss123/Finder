package karm.van.controller;

import karm.van.exception.EmailNotFoundException;
import karm.van.service.MyUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${microservices.auth.endpoints.recovery-password}")
@RequiredArgsConstructor
@CrossOrigin
public class PasswordRecoveryController {
    private final MyUserService myUserService;
    @GetMapping("/{recoveryKey}")
    public ResponseEntity<?> updatePassword(@PathVariable String recoveryKey) {
        try {
            myUserService.updatePassword(recoveryKey);
            return ResponseEntity.ok("The password has been successfully changed, you can close this window");
        } catch (EmailNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
