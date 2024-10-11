package karm.van.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import karm.van.config.AuthenticationMicroServiceProperties;
import karm.van.dto.UserDtoRequest;
import karm.van.exception.card.CardNotFoundException;
import karm.van.exception.comment.CommentNotFoundException;
import karm.van.exception.comment.CommentNotSavedException;
import karm.van.exception.comment.CommentNotUnlinkException;
import karm.van.exception.other.InvalidDataException;
import karm.van.exception.other.SerializationException;
import karm.van.exception.token.TokenNotExistException;
import karm.van.exception.user.NotEnoughPermissionsException;
import karm.van.exception.user.UsernameNotFoundException;
import karm.van.model.CardModel;
import karm.van.model.CommentModel;
import karm.van.repo.CardRepo;
import karm.van.repo.CommentRepo;
import karm.van.dto.CommentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.JedisPooled;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class CommentService {
    private final CommentRepo commentRepo;
    private final CardRepo cardRepo;
    private final ObjectMapper objectMapper;
    private final AuthenticationMicroServiceProperties authProperties;
    private final ApiService apiService;

    @Value("${redis.host}")
    private String redisHost;

    @Value("${microservices.x-api-key}")
    private String apiKey;
    private JedisPooled redis;

    @PostConstruct
    public void init(){
        redis = new JedisPooled(redisHost,6379);
    }

    private void checkToken(String token) throws TokenNotExistException {
        if (!apiService.validateToken(token,
                apiService.buildUrl(authProperties.getPrefix(),
                        authProperties.getHost(),
                        authProperties.getPort(),
                        authProperties.getEndpoints().getValidateToken()
                )
        )){
            throw new TokenNotExistException("Invalid token or expired");
        }
    }

    public boolean checkNoneEqualsApiKey(String key) {
        return !key.equals(apiKey);
    }

    private UserDtoRequest requestToGetUserByToken(String token) throws UsernameNotFoundException {
        UserDtoRequest user = apiService.getUserByToken(apiService.buildUrl(
                authProperties.getPrefix(),
                authProperties.getHost(),
                authProperties.getPort(),
                authProperties.getEndpoints().getGetUserByToken()
        ), token);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return user;
    }

    private void requestToUnlinkCommentFromUser(String token, Long commentId, Long authorId) throws CommentNotUnlinkException {
        HttpStatusCode httpStatusCode = apiService.requestToUnlinkCommentFromUser(apiService.buildUrl(
                authProperties.getPrefix(),
                authProperties.getHost(),
                authProperties.getPort(),
                authProperties.getEndpoints().getUnlinkCommentAndUser(),
                commentId,
                authorId
        ),token,apiKey);

        if (httpStatusCode != HttpStatus.OK){
            throw new CommentNotUnlinkException("An error occurred while trying to delete the card");
        }
    }

    private void requestToLinkCommentAndUser(Long commentId, String token) throws CommentNotSavedException {
        String url = apiService.buildUrl(
                authProperties.getPrefix(),
                authProperties.getHost(),
                authProperties.getPort(),
                authProperties.getEndpoints().getLinkCommentAndUser(),
                commentId);

        try {
            if (apiService.addCommentToUser(url,token,apiKey) != HttpStatus.OK){
                log.error("The error occurred while the user was being assigned a card");
                throw new CommentNotSavedException("An error occurred with saving");
            }
        }catch (NullPointerException | CommentNotSavedException e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw e;
        }

    }

    @Transactional
    public void addComment(Long cardId, CommentDto commentDto, String authorization) throws InvalidDataException, CardNotFoundException, TokenNotExistException {
        String token = authorization.substring(7);
         checkToken(token);
        try {
            String commentText = commentDto.text();

            if (commentText.trim().isEmpty()){
                throw new InvalidDataException("The text must be present");
            }

            CardModel cardModel = cardRepo.getCardModelById(cardId)
                    .orElseThrow(()->new CardNotFoundException("Card with this id doesn't exist"));

            UserDtoRequest user = requestToGetUserByToken(token);

            CommentModel commentModel = CommentModel.builder()
                    .text(commentDto.text())
                    .card(cardModel)
                    .userId(user.id())
                    .createdAt(LocalDateTime.now())
                    .build();

            commentRepo.save(commentModel);

            requestToLinkCommentAndUser(commentModel.getId(),token);
        } catch (InvalidDataException | CardNotFoundException e){
            throw e;
        } catch (Exception e){
            log.error("An unknown error occurred while deleting the card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    private List<CommentModel> cacheComments(Long id,int limit,int page,String commentsKeyForCache) throws SerializationException {
        Page<CommentModel> comments = commentRepo.getCommentModelByCard_Id(id, PageRequest.of(page,limit));// Идем в БД

        if (comments.isEmpty()){// Если комментариев вообще нет
            return List.of();//Возвращаем пустой список
        }

        comments.stream().parallel()
                .forEach(comment->{
                    try {
                        redis.rpush(commentsKeyForCache, objectMapper.writeValueAsString(comment));// Кешируем
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(new SerializationException("An error occurred during serialization"));
                    }
                });

        redis.expire(commentsKeyForCache, 60); // Устанавливаем время жизни равное минуте

        return comments.getContent();
    }

//TODO вместо id авторов получать их имена и данные
    public List<CommentModel> getComments(Long id,int limit,int page, String authorization) throws CardNotFoundException, SerializationException, TokenNotExistException {
        checkToken(authorization.substring(7));
        try {
            if (cardRepo.existsById(id)){// Проверяем существует ли такая карточка
                String commentsKeyForCache = "comments:card:"+id; // Ключ в редисе от списка комментариев

                List<String> cachedComments = redis.lrange(commentsKeyForCache, 0, limit - 1);// Проверяем есть ли закешированные результаты

                if (cachedComments.isEmpty()){// Если кеша нет
                    return cacheComments(id,limit,page,commentsKeyForCache);

                }else {// Если кеш найден
                    return cachedComments.stream()
                            .map(comment-> {
                                try {
                                    return objectMapper.readValue(comment,CommentModel.class);// Десирализуем текст в классы и складываем в лист
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(new SerializationException("An error occurred during deserialization"));
                                }
                            }).toList();
                }
            }else {
                throw new CardNotFoundException("Card with this id doesn't exist");
            }
        } catch (CardNotFoundException | SerializationException e){
            throw e;
        } catch (Exception e){
            log.debug("An unknown error occurred while deleting the card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    @Transactional
    public void deleteAllCommentsByCard(Long cardId, String authorization) throws TokenNotExistException {
        String token = authorization.substring(7);
        checkToken(token);
        try {
            List<CommentModel> comments = commentRepo.getCommentModelByCard_Id(cardId);
            String commentsKeyForCache = "comments:card:%d".formatted(cardId);

            if (redis.exists(commentsKeyForCache)){
                redis.del(commentsKeyForCache);
            }

            if (!comments.isEmpty()){
                comments.parallelStream().forEach(comment->{
                    try {
                        requestToUnlinkCommentFromUser(token,comment.getId(),comment.getUserId());
                    } catch (CommentNotUnlinkException e){
                        throw new RuntimeException(e.getMessage());
                    }
                });
                commentRepo.deleteAllByCard_Id(cardId);
            }
        } catch (Exception e){
            log.debug("An unknown error occurred while deleting the card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }

    }

    private void checkPermissions(CommentModel commentModel, String token) throws UsernameNotFoundException, NotEnoughPermissionsException {
        Long commentAuthorId = commentModel.getUserId();
        UserDtoRequest user = requestToGetUserByToken(token);

        if (!commentAuthorId.equals(user.id()) && user.role().stream().noneMatch(role->role.equals("ADMIN"))){
            throw new NotEnoughPermissionsException("You don't have permission to do this");
        }
    }

    @Transactional
    public void patchComment(Long commentId, CommentDto commentDto, String authorization) throws InvalidDataException, CommentNotFoundException, TokenNotExistException {
        String token = authorization.substring(7);
        checkToken(token);
        try {
            String commentText = commentDto.text();

            if (commentText.trim().isEmpty()){
                throw new InvalidDataException("The text must be present");
            }

            CommentModel commentModel = commentRepo.getCommentModelById(commentId)
                    .orElseThrow(()->new CommentNotFoundException("Card with this id doesn't exist"));

            checkPermissions(commentModel,token);

            commentModel.setText(commentText);

            commentRepo.save(commentModel);
        } catch (InvalidDataException | CommentNotFoundException e){
            throw e;
        } catch (Exception e){
            log.debug("An unknown error occurred while deleting the card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    @Transactional
    public void deleteOneComment(Long commentId, String authorization) throws TokenNotExistException, CommentNotFoundException, UsernameNotFoundException, NotEnoughPermissionsException, CommentNotUnlinkException {
        String token = authorization.substring(7);
        checkToken(token);
        CommentModel commentModel = commentRepo.getCommentModelById(commentId)
                .orElseThrow(()->new CommentNotFoundException("Card with this id doesn't exist"));
        checkPermissions(commentModel,token);
        requestToUnlinkCommentFromUser(token,commentId,commentModel.getUserId());
        commentRepo.deleteById(commentId);
    }
}
