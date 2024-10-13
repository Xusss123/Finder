package karm.van.controller;

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
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authorizationHeader){
        String token = authorizationHeader.substring(7);
        try {
            return ResponseEntity.ok(Map.of("valid",
                    jwtService.validateAccessToken(token, jwtService.getUserDetailsFromToken(token))
            ));
        }catch (Exception e){
            log.error("message: "+e.getMessage()+" class: "+e.getClass());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid token"));
        }
    }

    @GetMapping("/get-user")
    public ResponseEntity<?> getUserDto(@RequestHeader("Authorization") String authorizationHeader,
                                            @RequestParam(name = "userId",required = false) Optional<Long> userId){
        try {
            return ResponseEntity.ok(myUserService.getUser(authorizationHeader,userId));
        }catch (UsernameNotFoundException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (BadCredentialsException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    //TODO добавить метод для получение полной информации о пользователе (данные о нем, все его объявления, где хранится изображение профиля)

    @PostMapping("/user/addCard/{cardId}")
    public ResponseEntity<?> addCardToUser(@RequestHeader("Authorization") String authorizationHeader,
                                           @PathVariable("cardId") Long cardId,
                                           @RequestHeader("x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }
            myUserService.addCardToUser(authorizationHeader,cardId);
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
    public ResponseEntity<?> addProfileImage(@RequestHeader("Authorization") String authorizationHeader,
                                             @PathVariable("profileImageId") Long profileImageId,
                                             @RequestHeader("x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }
            return ResponseEntity.ok(myUserService.addProfileImage(authorizationHeader,profileImageId));
        }catch (UsernameNotFoundException | InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));
        }catch (BadCredentialsException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/user/addComment/{commentId}")
    public ResponseEntity<?> addCommentToUser(@RequestHeader("Authorization") String authorizationHeader,
                                           @PathVariable("commentId") Long commentId,
                                           @RequestHeader("x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }
            myUserService.addCommentToUser(authorizationHeader,commentId);
            return ResponseEntity.ok("Comment added successfully");
        }catch (UsernameNotFoundException | InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));
        }catch (BadCredentialsException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/user/comment/del/{commentId}/{authorId}")
    public ResponseEntity<?> unlinkCommentAndUser(@RequestHeader("Authorization") String authorizationHeader,
                                              @PathVariable("commentId") Long commentId,
                                              @PathVariable("authorId") Long authorId,
                                              @RequestHeader("x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }
            myUserService.unlinkCommentAndUser(authorizationHeader,commentId,authorId);
            return ResponseEntity.ok("Comment deleted successfully");
        }catch (UsernameNotFoundException | InvalidApiKeyException | NotEnoughPermissionsException e){
            return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));
        }catch (BadCredentialsException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/user/del")
    public ResponseEntity<?> delUser(@RequestHeader("Authorization") String authorizationHeader) throws BadCredentialsException{
        try {
            myUserService.delUser(authorizationHeader);
            return ResponseEntity.ok("User deleted successfully");
        } catch (BadCredentialsException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (CardNotDeletedException | ImageNotMovedException | ImageNotDeletedException e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Due to an internal error, your account was not deleted");
        }
    }

    @DeleteMapping("/user/card/del/{cardId}")
    public ResponseEntity<?> delUserCard(@RequestHeader("Authorization") String authorizationHeader,
                                         @PathVariable("cardId") Long cardId,
                                         @RequestHeader("x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }

            myUserService.delUserCard(authorizationHeader,cardId);
            return ResponseEntity.ok("Card deleted successfully");
        } catch (NotEnoughPermissionsException | InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

}
