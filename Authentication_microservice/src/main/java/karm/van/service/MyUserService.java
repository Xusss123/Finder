package karm.van.service;

import karm.van.config.AdsMicroServiceProperties;
import karm.van.config.ImageMicroServiceProperties;
import karm.van.dto.request.UserDtoRequest;
import karm.van.dto.response.UserDtoResponse;
import karm.van.exception.*;
import karm.van.model.MyUser;
import karm.van.repo.MyUserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Log4j2
public class MyUserService {
    private final MyUserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AdsMicroServiceProperties adsProperties;
    private final ImageMicroServiceProperties imageProperties;
    private final ApiService apiService;

    @Value("${microservices.x-api-key}")
    private String apiKey;

    public UserDtoResponse getUser(String authorization, Optional<Long> userIdOpt) throws UsernameNotFoundException, BadCredentialsException {
        // Проверка на корректность переданного токена
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BadCredentialsException("Authorization header is not valid or missing.");
        }

        String token = authorization.substring(7);  // Извлекаем сам токен (после "Bearer ")
        UserDetails userDetails = jwtService.getUserDetailsFromToken(token);

        // Проверяем валидность токена
        if (!jwtService.validateAccessToken(token, userDetails)) {
            throw new BadCredentialsException("Incorrect access token");
        }

        // Лямбда-функция для создания UserDtoResponse
        Function<MyUser, UserDtoResponse> mapToDto = user -> new UserDtoResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRoles(),
                user.getFirstName(),
                user.getLastName(),
                user.getDescription(),
                user.getCountry(),
                user.getRoleInCommand(),
                user.getSkills()
        );

        // Если передан userId, ищем по ID, иначе по username из токена
        return userIdOpt.map(userId ->
                        userRepo.findById(userId)
                                .map(mapToDto)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId)))
                .orElseGet(() ->
                        userRepo.findByName(userDetails.getUsername())
                                .map(mapToDto)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + userDetails.getUsername())));
    }

    @Transactional
    public void addCardToUser(String authorization,Long cardId) throws UsernameNotFoundException, BadCredentialsException{
        MyUser user = getUserAndCheckToken(authorization);
        List<Long> cardsList = user.getCards();
        cardsList.add(cardId);
        userRepo.save(user);
    }

    @Transactional
    public void registerUser(UserDtoRequest userDtoRequest) throws UserAlreadyExist {
        String name = userDtoRequest.name();

        if (userRepo.existsByName(name)){
            throw new UserAlreadyExist("A user with this login already exists");
        }

        MyUser user = MyUser.builder()
                .name(name)
                .email(userDtoRequest.email())
                .country(userDtoRequest.country())
                .description(userDtoRequest.description())
                .roles(userDtoRequest.role())
                .firstName(userDtoRequest.firstName())
                .lastName(userDtoRequest.lastName())
                .skills(userDtoRequest.skills())
                .password(passwordEncoder.encode(userDtoRequest.password()))
                .roleInCommand(userDtoRequest.roleInCommand())
                .profileImage(0L)
                .unlockAt(LocalDateTime.now())
                .isEnable(true)
                .build();

        userRepo.save(user);

    }

    private void sendRequestToDelUserCard(String token, Long cardId) throws UserNotDeletedException {
        try {
            HttpStatusCode httpStatusCode = apiService.requestToDelCard(
                    apiService.buildUrl(adsProperties.getPrefix(),
                            adsProperties.getHost(),
                            adsProperties.getPort(),
                            adsProperties.getEndpoints().getDelCard(),
                            cardId),token);
            if (httpStatusCode != HttpStatus.OK){
                throw new UserNotDeletedException("Due to an error on the server, your this user has not been deleted");
            }
        } catch (UserNotDeletedException e){
            throw e;
        } catch (Exception e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw new UserNotDeletedException("Due to an error on the server, your this user has not been deleted");
        }
    }

    private void moveImagesToTrashBucket(List<Long> imagesId, String token) throws ImageNotMovedException {
        String imageUrl = apiService.buildUrl(
                imageProperties.getPrefix(),
                imageProperties.getHost(),
                imageProperties.getPort(),
                imageProperties.getEndpoints().getMoveImage()
        );
        try {
            HttpStatusCode httpStatusCode = apiService.moveImagesToTrashPackage(imageUrl, imagesId, token, apiKey);
            if (httpStatusCode != HttpStatus.OK) {
                throw new ImageNotMovedException();
            }
        }catch (Exception e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw new ImageNotMovedException("An error occurred on the server side during the image moving");
        }
    }

    private void requestToDeleteImagesFromMinio(List<Long> imageIds,String token) throws ImageNotDeletedException {
        String url = apiService.buildUrl(
                imageProperties.getPrefix(),
                imageProperties.getHost(),
                imageProperties.getPort(),
                imageProperties.getEndpoints().getDelImagesFromMinio()
        );

        try {
            HttpStatusCode httpStatusCode = apiService.sendDeleteImagesFromMinioRequest(url, imageIds, token, apiKey);
            if (httpStatusCode != HttpStatus.OK) {
                throw new ImageNotDeletedException();
            }
        }catch (Exception e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw new ImageNotDeletedException("An error occurred on the server side during the image deleting");
        }
    }

    @Async
    protected void rollBackImages(Long imageId, String token){
        String imageUrl = apiService.buildUrl(
                imageProperties.getPrefix(),
                imageProperties.getHost(),
                imageProperties.getPort(),
                imageProperties.getEndpoints().getMoveImageToProfile(),
                imageId
                );


        apiService.moveImagesToProfileImagePackage(imageUrl, token, apiKey);
    }

    private void deleteAllUserCards(MyUser user, String token) throws CardNotDeletedException {
        try {
            user.getCards().parallelStream()
                    .forEach(card-> {
                        try {
                            sendRequestToDelUserCard(token,card);
                        } catch (UserNotDeletedException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }catch (Exception e){
            throw new CardNotDeletedException();
        }
    }

    @Transactional
    public void delUser(String authorization) throws ImageNotMovedException, CardNotDeletedException, ImageNotDeletedException {
        String token = authorization.substring(7);
        MyUser user = getUserAndCheckToken(authorization);
        try {
            moveImagesToTrashBucket(List.of(user.getProfileImage()),token);
            if (!user.getCards().isEmpty()){
                deleteAllUserCards(user,token);
            }
            requestToDeleteImagesFromMinio(List.of(user.getProfileImage()),token);
            userRepo.delete(user);
        }catch (ImageNotMovedException | ImageNotDeletedException e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw e;
        }catch (CardNotDeletedException e){
            rollBackImages(user.getProfileImage(),token);
            throw e;
        }
    }

    @Transactional
    public void delUserCard(String authorizationHeader, Long cardId) throws NotEnoughPermissionsException {
        MyUser user = getUserAndCheckToken(authorizationHeader);
        List<Long> userCards = user.getCards();
        userCards.remove(cardId);
        userRepo.save(user);
    }

    public boolean checkApiKeyNotEquals(String key) {
        return !apiKey.equals(key);
    }


    private MyUser getUserAndCheckToken(String authorizationHeader){
        String token = authorizationHeader.substring(7);
        UserDetails userDetails = jwtService.getUserDetailsFromToken(token);
        if (jwtService.validateAccessToken(token,userDetails)){
            return userRepo.findByName(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User with this name doesn't exist"));
        }else {
            throw new BadCredentialsException("Incorrect access token");
        }
    }

    private Boolean checkToken(String authorizationHeader){
        String token = authorizationHeader.substring(7);
        UserDetails userDetails = jwtService.getUserDetailsFromToken(token);
        if (jwtService.validateAccessToken(token,userDetails)){
            return true;
        }else {
            throw new BadCredentialsException("Incorrect access token");
        }
    }

    @Transactional
    public void addCommentToUser(String authorizationHeader, Long commentId) {
        MyUser user = getUserAndCheckToken(authorizationHeader);
        List<Long> commentsList = user.getComments();
        commentsList.add(commentId);
        userRepo.save(user);
    }

    @Transactional
    public void unlinkCommentAndUser(String authorizationHeader, Long commentId, Long authorId) throws NotEnoughPermissionsException {
        if (checkToken(authorizationHeader)){
            MyUser user = userRepo.getReferenceById(authorId);
            List<Long> commentsList = user.getComments();
            commentsList.remove(commentId);
            userRepo.save(user);
        }

    }

    @Transactional
    public Long addProfileImage(String authorizationHeader, Long profileImageId) {
        MyUser user = getUserAndCheckToken(authorizationHeader);
        Long oldImageId = user.getProfileImage();
        user.setProfileImage(profileImageId);
        userRepo.save(user);

        return oldImageId;
    }
}
