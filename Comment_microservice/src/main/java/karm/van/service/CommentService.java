package karm.van.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import karm.van.exception.CardNotFoundException;
import karm.van.exception.CommentNotFoundException;
import karm.van.exception.InvalidDataException;
import karm.van.exception.SerializationException;
import karm.van.model.CardModel;
import karm.van.model.CommentModel;
import karm.van.repo.CardRepo;
import karm.van.repo.CommentRepo;
import karm.van.dto.CommentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${redis.host}")
    private String redisHost;
    private JedisPooled redis;

    @PostConstruct
    public void init(){
        redis = new JedisPooled(redisHost,6379);
    }

    @Transactional
    public void addComment(Long cardId, CommentDto commentDto) throws InvalidDataException, CardNotFoundException {
        try {
            String commentText = commentDto.text();

            if (commentText.trim().isEmpty()){
                throw new InvalidDataException("The text must be present");
            }

            CardModel cardModel = cardRepo.getCardModelById(cardId)
                    .orElseThrow(()->new CardNotFoundException("Card with this id doesn't exist"));

            CommentModel commentModel = CommentModel.builder()
                    .text(commentDto.text())
                    .card(cardModel)
                    .createdAt(LocalDateTime.now())
                    .build();

            commentRepo.save(commentModel);
        } catch (InvalidDataException | CardNotFoundException e){
            throw e;
        } catch (Exception e){
            log.debug("An unknown error occurred while deleting the card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    private List<CommentModel> cacheComments(Long id,int limit,int page,String commentsKeyForCache) throws SerializationException {
        List<CommentModel> comments = commentRepo.getCommentModelByCard_Id(id);// Идем в БД

        if (comments.isEmpty()){// Если комментариев вообще нет
            return List.of();//Возвращаем пустой список
        }

        int start = page * limit; // Это индекс первого элемента (Если page = 1 (вторая страница) и limit = 10, то start = 1 * 10 = 10 — это будет 11-й элемент)
        int end = Math.min(start + limit, comments.size());// Если на последней странице количество комментариев меньше, чем limit, то end будет равен количеству комментариев

        List<CommentModel> pageComments = comments.subList(start, end);// Выбираем только нужное нам количество комментариев

        for (CommentModel comment : pageComments) {// Проходимся по ним
            try {
                redis.rpush(commentsKeyForCache, objectMapper.writeValueAsString(comment));// Кешируем
            } catch (JsonProcessingException e) {
                throw new SerializationException("An error occurred during serialization");
            }
        }

        redis.expire(commentsKeyForCache, 60); // Устанавливаем время жизни равное минуте

        return pageComments;
    }


    public List<CommentModel> getComments(Long id,int limit,int page) throws CardNotFoundException, SerializationException {
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
    public void deleteAllCommentsByCard(Long cardId){
        try {
            List<CommentModel> comments = commentRepo.getCommentModelByCard_Id(cardId);
            String commentsKeyForCache = "comments:card:%d".formatted(cardId);

            if (redis.exists(commentsKeyForCache)){
                redis.del(commentsKeyForCache);
            }

            if (!comments.isEmpty()){
                commentRepo.deleteAllByCard_Id(cardId);
            }
        } catch (Exception e){
            log.debug("An unknown error occurred while deleting the card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }

    }

    @Transactional
    public void patchComment(Long commentId, CommentDto commentDto) throws InvalidDataException, CommentNotFoundException {
        try {
            String commentText = commentDto.text();

            if (commentText.trim().isEmpty()){
                throw new InvalidDataException("The text must be present");
            }

            CommentModel commentModel = commentRepo.getCommentModelById(commentId)
                    .orElseThrow(()->new CommentNotFoundException("Card with this id doesn't exist"));

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
    public void deleteOneComment(Long commentId) {
        commentRepo.deleteById(commentId);
    }
}
