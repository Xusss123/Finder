package karm.van.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import karm.van.dto.request.AuthRequest;
import karm.van.dto.request.UserDtoRequest;
import karm.van.dto.response.AuthResponse;
import karm.van.exception.*;
import karm.van.service.JwtService;
import karm.van.service.MyUserDetailsService;
import karm.van.service.MyUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final MyUserDetailsService myUserDetailsService;
    private final MyUserService myUserService;
    private final JwtService jwtService;

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok(Map.of("valid", true));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
    }

    @GetMapping("/get-user")
    public ResponseEntity<?> getUserDto(@RequestParam(name = "userId",required = false) Optional<Long> userId,
                                        @RequestHeader(name = "x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }
            return ResponseEntity.ok(myUserService.getUser(SecurityContextHolder.getContext().getAuthentication(),userId));
        }catch (UsernameNotFoundException | InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (BadCredentialsException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @GetMapping("/profile/{userName}")
    public ResponseEntity<?> getFullUserData(HttpServletRequest request,
                                             @PathVariable String userName) {
        try {
            return ResponseEntity.ok(myUserService.getFullUserData(request,userName));
        }catch (CardsNotGetedException | ImageNotGetedException | JsonProcessingException e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred on the server side");
        }
    }

    @PatchMapping("/user/patch")
    public ResponseEntity<?> patchUser(@RequestParam(required = false) Optional<String> name,
                                       @RequestParam(required = false) Optional<String> email,
                                       @RequestParam(required = false) Optional<String> firstName,
                                       @RequestParam(required = false) Optional<String> lastName,
                                       @RequestParam(required = false) Optional<String> description,
                                       @RequestParam(required = false) Optional<String> country,
                                       @RequestParam(required = false) Optional<String> roleInCommand,
                                       @RequestParam(required = false) Optional<String> skills){


        try {
            myUserService.patchUser(
                    SecurityContextHolder.getContext().getAuthentication(),
                    name,
                    email,
                    firstName,
                    lastName,
                    description,
                    country,
                    roleInCommand,
                    skills
            );
            return ResponseEntity.ok("User successfully changed");
        }catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/user/addCard/{cardId}")
    public ResponseEntity<?> addCardToUser(@PathVariable("cardId") Long cardId,
                                           @RequestHeader("x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }
            myUserService.addCardToUser(SecurityContextHolder.getContext().getAuthentication(),cardId);
            return ResponseEntity.ok("Card added successfully");
        }catch (UsernameNotFoundException | InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));
        }catch (BadCredentialsException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body("Refresh token is missing");
        }

        // Обновление Access Token с использованием Refresh Token
        String newAccessToken = jwtService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequest authRequest) throws Exception {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.username(), authRequest.password())
            );
        } catch (BadCredentialsException e) {
            throw new Exception("Incorrect username or password", e);
        }

        // Загружаем UserDetails
        UserDetails userDetails = myUserDetailsService.loadUserByUsername(authRequest.username());

        // Генерируем Access Token и Refresh Token
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Возвращаем оба токена в ответе
        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
    }

    @PostMapping(value = "/register")
    public ResponseEntity<?> register(@RequestBody UserDtoRequest userDtoRequest) {
        try {
            myUserService.registerUser(userDtoRequest);
            return ResponseEntity.ok("User registered successfully");
        } catch (UserAlreadyExist e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/user/addProfileImage/{profileImageId}")
    public ResponseEntity<?> addProfileImage(@PathVariable("profileImageId") Long profileImageId,
                                             @RequestHeader("x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }
            return ResponseEntity.ok(myUserService.addProfileImage(SecurityContextHolder.getContext().getAuthentication(),profileImageId));
        }catch (UsernameNotFoundException | InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));
        }catch (BadCredentialsException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/user/addComment/{commentId}")
    public ResponseEntity<?> addCommentToUser(@PathVariable("commentId") Long commentId,
                                              @RequestHeader("x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }
            myUserService.addCommentToUser(SecurityContextHolder.getContext().getAuthentication(),commentId);
            return ResponseEntity.ok("Comment added successfully");
        }catch (UsernameNotFoundException | InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));
        }catch (BadCredentialsException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/user/comment/del/{commentId}/{authorId}")
    public ResponseEntity<?> unlinkCommentAndUser(@PathVariable("commentId") Long commentId,
                                                  @PathVariable("authorId") Long authorId,
                                                  @RequestHeader("x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }
            myUserService.unlinkCommentAndUser(commentId,authorId);
            return ResponseEntity.ok("Comment deleted successfully");
        }catch (UsernameNotFoundException | InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));
        }catch (BadCredentialsException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/user/del")
    public ResponseEntity<?> delUser(HttpServletRequest request) throws BadCredentialsException{

        try {
            myUserService.delUser(SecurityContextHolder.getContext().getAuthentication(),request);
            return ResponseEntity.ok("User deleted successfully");
        } catch (BadCredentialsException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (CardNotDeletedException | ImageNotMovedException | ImageNotDeletedException e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Due to an internal error, your account was not deleted");
        }
    }

    @DeleteMapping("/user/card/del/{cardId}")
    public ResponseEntity<?> delUserCard(@PathVariable("cardId") Long cardId,
                                         @RequestHeader("x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }

            myUserService.delUserCard(SecurityContextHolder.getContext().getAuthentication(),cardId);
            return ResponseEntity.ok("Card deleted successfully");
        } catch (InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

}
