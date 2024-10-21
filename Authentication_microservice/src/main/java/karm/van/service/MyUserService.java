package karm.van.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import karm.van.config.AdsMicroServiceProperties;
import karm.van.config.ImageMicroServiceProperties;
import karm.van.dto.request.UserDtoRequest;
import karm.van.dto.response.FullUserDtoResponse;
import karm.van.dto.response.ProfileImageDtoResponse;
import karm.van.dto.response.UserCardResponse;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.JedisPooled;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Log4j2
public class MyUserService {
    private final MyUserRepo userRepo;
    private JedisPooled redis;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdsMicroServiceProperties adsProperties;
    private final ImageMicroServiceProperties imageProperties;
    private final ApiService apiService;

    @Value("${microservices.x-api-key}")
    private String apiKey;

    @Value("${redis.host}")
    private String redisHost;
    @PostConstruct
    public void init(){
        redis = new JedisPooled(redisHost,6379);
    }

    public UserDtoResponse getUser(Authentication authentication, Optional<Long> userIdOpt) throws UsernameNotFoundException, BadCredentialsException {

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
                                .orElseThrow(() -> new UsernameNotFoundException("User not found with this ID")))
                .orElseGet(() ->
                        userRepo.findByName(authentication.getName())
                                .map(mapToDto)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found with this username")));
    }

    private List<UserCardResponse> sendRequestToGetUserCards(String token, Long userId) throws CardsNotGetedException {
        String uri = apiService.buildUrl(
                adsProperties.getPrefix(),
                adsProperties.getHost(),
                adsProperties.getPort(),
                adsProperties.getEndpoints().getGetUserCards(),
                userId
        );

        try {
            List<UserCardResponse> cards = apiService.getCardImagesRequest(uri,token,apiKey);
            if (cards==null){
                throw new CardsNotGetedException();
            }
            return cards;
        }catch (Exception e){
            throw new CardsNotGetedException("Due to an internal error, no results were received");
        }
    }

    private ProfileImageDtoResponse sendRequestToGetProfileImage(String token,Long imageId) throws ImageNotGetedException {
        String uri = apiService.buildUrl(
                imageProperties.getPrefix(),
                imageProperties.getHost(),
                imageProperties.getPort(),
                imageProperties.getEndpoints().getProfileImage(),
                imageId
        );

        try {
            ProfileImageDtoResponse profileImageDtoResponse = apiService.requestToGetProfileImage(uri,token,apiKey);
            if (profileImageDtoResponse==null){
                throw new ImageNotGetedException();
            }
            return profileImageDtoResponse;
        }catch (Exception e){
            throw new ImageNotGetedException("Due to an internal error, no results were received");
        }
    }

    public FullUserDtoResponse getFullUserData(HttpServletRequest request, String name) throws CardsNotGetedException, ImageNotGetedException, JsonProcessingException {
        String redisKey = "user_"+name;
        if (redis.exists(redisKey)){
            return objectMapper.readValue(redis.get(redisKey), FullUserDtoResponse.class);
        }else {
            return cacheUserInfo(request,name,redisKey);
        }
    }

    private FullUserDtoResponse cacheUserInfo(HttpServletRequest request, String name, String redisKey) throws CardsNotGetedException, ImageNotGetedException, JsonProcessingException {
        MyUser user = userRepo.findByName(name)
                .orElseThrow(()->new UsernameNotFoundException("User with this name doesn't exist"));

        String token = (String) request.getAttribute("jwtToken");

        try {
            List<UserCardResponse> cards = sendRequestToGetUserCards(token,user.getId());
            Long userProfileImage = user.getProfileImage();


            ProfileImageDtoResponse imageDtoResponse;
            if (userProfileImage>0){
                imageDtoResponse = sendRequestToGetProfileImage(token,user.getProfileImage());
            }else {
                imageDtoResponse = new ProfileImageDtoResponse(null,null);
            }

            FullUserDtoResponse fullUserDtoResponse = new FullUserDtoResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRoles(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getDescription(),
                    user.getCountry(),
                    user.getRoleInCommand(),
                    user.getSkills(),
                    imageDtoResponse,
                    cards
            );

            String objectAsString = objectMapper.writeValueAsString(fullUserDtoResponse);
            redis.set(redisKey,objectAsString);
            redis.expire(redisKey,60);
            return fullUserDtoResponse;
        } catch (Exception e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void addCardToUser(Authentication authentication,Long cardId) throws UsernameNotFoundException, BadCredentialsException{
        MyUser user = userRepo.findByName(authentication.getName())
                .orElseThrow(()->new UsernameNotFoundException("User with this name doesn't exist"));
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
    public void delUser(Authentication authentication, HttpServletRequest request) throws ImageNotMovedException, CardNotDeletedException, ImageNotDeletedException {
        String token = (String) request.getAttribute("jwtToken");
        String redisKey = "user_"+authentication.getName();
        MyUser user = userRepo.findByName(authentication.getName())
                .orElseThrow(()->new UsernameNotFoundException("User with this name doesn't exist"));
        try {
            moveImagesToTrashBucket(List.of(user.getProfileImage()),token);
            if (!user.getCards().isEmpty()){
                deleteAllUserCards(user,token);
            }
            requestToDeleteImagesFromMinio(List.of(user.getProfileImage()),token);
            userRepo.delete(user);
            redis.del(redisKey);
        }catch (ImageNotMovedException | ImageNotDeletedException e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw e;
        }catch (CardNotDeletedException e){
            rollBackImages(user.getProfileImage(),token);
            throw e;
        }
    }

    @Transactional
    public void delUserCard(Authentication authentication, Long cardId){
        MyUser user = userRepo.findByName(authentication.getName())
                .orElseThrow(()->new UsernameNotFoundException("User with this name doesn't exist"));
        List<Long> userCards = user.getCards();
        userCards.remove(cardId);
        userRepo.save(user);
    }

    public boolean checkApiKeyNotEquals(String key) {
        return !apiKey.equals(key);
    }

    @Transactional
    public void addCommentToUser(Authentication authentication, Long commentId) {
        MyUser user = userRepo.findByName(authentication.getName())
                .orElseThrow(()->new UsernameNotFoundException("User with this name doesn't exist"));
        List<Long> commentsList = user.getComments();
        commentsList.add(commentId);
        userRepo.save(user);
    }

    @Transactional
    public void unlinkCommentAndUser(Long commentId, Long authorId){
        MyUser user = userRepo.getReferenceById(authorId);
        List<Long> commentsList = user.getComments();
        commentsList.remove(commentId);
        userRepo.save(user);
    }

    @Transactional
    public Long addProfileImage(Authentication authentication, Long profileImageId) {
        MyUser user = userRepo.findByName(authentication.getName())
                .orElseThrow(()->new UsernameNotFoundException("User with this name doesn't exist"));
        Long oldImageId = user.getProfileImage();
        user.setProfileImage(profileImageId);
        userRepo.save(user);

        return oldImageId;
    }


    @Transactional
    public void patchUser(Authentication authentication,
                          Optional<String> nameOpt,
                          Optional<String> emailOpt,
                          Optional<String> firstNameOpt,
                          Optional<String> lastNameOpt,
                          Optional<String> descriptionOpt,
                          Optional<String> countryOpt,
                          Optional<String> roleInCommandOpt,
                          Optional<String> skillsOpt) {

        MyUser user = userRepo.findByName(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User with this name doesn't exist"));

        String redisKey = "user_"+authentication.getName();


        nameOpt.ifPresent(name -> {
            if (name.trim().isEmpty()) {
                throw new IllegalArgumentException("Name cannot be blank");
            }
            user.setName(name);
        });

        emailOpt.ifPresent(email -> {
            if (!isValidEmail(email)) {
                throw new IllegalArgumentException("Invalid email format");
            }
            if (userRepo.existsByEmail(email)) {
                throw new IllegalArgumentException("Email is already in use");
            }
            user.setEmail(email);
        });

        firstNameOpt.ifPresent(firstName -> {
            if (firstName.trim().isEmpty()) {
                throw new IllegalArgumentException("First name cannot be blank");
            }
            user.setFirstName(firstName);
        });

        lastNameOpt.ifPresent(lastName -> {
            if (lastName.trim().isEmpty()) {
                throw new IllegalArgumentException("Last name cannot be blank");
            }
            user.setLastName(lastName);
        });

        descriptionOpt.ifPresent(description -> {
            if (description.trim().isEmpty()) {
                throw new IllegalArgumentException("Description cannot be blank");
            }
            user.setDescription(description);
        });

        countryOpt.ifPresent(country -> {
            if (country.trim().isEmpty()) {
                throw new IllegalArgumentException("Country cannot be blank");
            }
            user.setCountry(country);
        });

        roleInCommandOpt.ifPresent(roleInCommand -> {
            if (roleInCommand.trim().isEmpty()) {
                throw new IllegalArgumentException("Role in command cannot be blank");
            }
            user.setRoleInCommand(roleInCommand);
        });


        skillsOpt.ifPresent(skills -> {
            if (skills.length() > 255) {
                throw new IllegalArgumentException("Skills should not exceed 255 characters");
            }
            user.setSkills(skills);
        });

        userRepo.save(user);
        redis.del(redisKey);
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pat = Pattern.compile(emailRegex);
        return pat.matcher(email).matches();
    }

}
