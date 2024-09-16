package karm.van.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import karm.van.config.CommentMicroServiceProperties;
import karm.van.dto.CardDto;
import karm.van.exception.card.CardNotDeletedException;
import karm.van.exception.card.CardNotFoundException;
import karm.van.exception.card.CardNotSavedException;
import karm.van.exception.image.ImageNotDeletedException;
import karm.van.exception.image.ImageLimitException;
import karm.van.exception.image.ImageNotSavedException;
import karm.van.exception.other.SerializationException;
import karm.van.model.CardModel;
import karm.van.repository.CardRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import redis.clients.jedis.JedisPooled;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class CardService {
    private final CardRepo cardRepo;
    private final ImageService imageService;
    private WebClient webClient;
    private JedisPooled redis;
    private final ObjectMapper objectMapper;
    private final CommentMicroServiceProperties properties;

    @Value("${redis.host}")
    private String redisHost;

    @Value("${card.images.count}")
    private int allowedImagesCount;

    @PostConstruct
    public void init(){
        redis = new JedisPooled(redisHost,6379);
        webClient = WebClient.create();
    }

    private CardModel addCardText(CardDto cardDto) throws CardNotSavedException {
        try {

            String title = cardDto.title();
            String text = cardDto.text();

            if (title.trim().isEmpty() || text.trim().isEmpty()){
                throw new CardNotSavedException("The title and text should not be empty");
            }

            CardModel cardModel = CardModel.builder()
                    .title(cardDto.title())
                    .text(cardDto.text())
                    .createTime(LocalDateTime.now())
                    .build();

           return cardRepo.save(cardModel);
        }catch (Exception e){
            log.error(e.getMessage());
            throw new CardNotSavedException(e.getMessage());
        }
    }

    private void addCardText(CardDto cardDto, CardModel cardModel) throws CardNotSavedException {
        try {

            String title = cardDto.title();
            String text = cardDto.text();

            if (title.trim().isEmpty() || text.trim().isEmpty()){
                throw new CardNotSavedException("The title and text should not be empty");
            }

            cardModel.setTitle(title);
            cardModel.setText(text);

            cardRepo.save(cardModel);
        }catch (Exception e){
            log.error(e.getMessage());
            throw new CardNotSavedException(e.getMessage());
        }
    }

    @Transactional
    public void addCard(List<MultipartFile> files, CardDto cardDto) throws ImageNotSavedException, CardNotSavedException, ImageLimitException {
        try {

            if (files.size()>allowedImagesCount){
                throw new ImageLimitException("You have provided more than" + allowedImagesCount + "images");
            }

            imageService.addCardImages(files,addCardText(cardDto),0);
        }catch (ImageNotSavedException e){
            throw new ImageNotSavedException(e.getMessage());
        }catch (CardNotSavedException e){
            throw new CardNotSavedException(e.getMessage());
        }catch (ImageLimitException e){
            throw new ImageLimitException(e.getMessage());
        }

    }

    public CardModel getCard(Long id) throws CardNotFoundException, SerializationException {
        String key = "card%d".formatted(id);
        if (!redis.exists(key)) {//Если кеш отсутствует
            return cacheCard(id,key);
        } else {//Если кеш найден
            try {
                return objectMapper.readValue(redis.get(key), CardModel.class);//Десериализуем строку в объект и возвращаем
            } catch (JsonProcessingException e) {
                throw new SerializationException("an error occurred during serialization");
            }
        }
    }

    private CardModel cacheCard(Long id, String key) throws CardNotFoundException, SerializationException {
        Optional<CardModel> cardModelOptional = cardRepo.getCardModelById(id);//Ищем запись в БД

        if (cardModelOptional.isEmpty()) {//Если записи в БД нет
            throw new CardNotFoundException("card with this id doesn't exist");//Ошибка о том, что карточки не существует
        }

        CardModel card = cardModelOptional.get();
        String objectAsString;

        try {
            objectAsString = objectMapper.writeValueAsString(card);//Сериализуем объект в строку
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new SerializationException("an error occurred during serialization");
        }

        redis.set(key, objectAsString);//Кешируем объект
        redis.expire(key, 60);//Устанавливаем время жизни
        return card;
    }

    @Transactional
    public void deleteCard(Long id) throws CardNotFoundException, CardNotDeletedException {
        String key = "card%d".formatted(id);
        try {

            CardModel cardModel = cardRepo.getCardModelById(id)
                    .orElseThrow(()->new CardNotFoundException("Card with this id doesn't exist"));

            if (redis.exists(key)) {
                redis.del(key);
            }

            String url = properties.getPrefix()+properties.getHost()+":"+properties.getPort()+"/card/"+id+properties.getEndpoints().getDellAllCommentsByCard();

            HttpStatusCode statusCode = Objects.requireNonNull(webClient
                            .delete()
                            .uri(url)
                            .retrieve()
                            .toBodilessEntity()
                            .block())
                    .getStatusCode();

            if (statusCode == HttpStatus.OK) {
                cardRepo.deleteById(id);
                imageService.deleteImagesFromMinio(cardModel);
            } else {
                throw new CardNotDeletedException("An error occurred on the server side during the deletion");
            }
        } catch (CardNotFoundException e) {
            throw new CardNotFoundException(e.getMessage());
        } catch (CardNotDeletedException e){
            throw new CardNotDeletedException(e.getMessage());
        }
    }

    public List<CardModel> getAllCards(){
        return cardRepo.findAll();
    }

    @Transactional
    public void patchCard(Long id, Optional<CardDto> cardDtoOptional, Optional<List<MultipartFile>> optFiles) throws CardNotFoundException, CardNotSavedException, ImageNotSavedException, ImageNotDeletedException, ImageLimitException {
        CardModel cardModel = cardRepo.getCardModelById(id)
                .orElseThrow(() -> new CardNotFoundException("Card with this id doesn't exist"));

        String key = "card%d".formatted(id);

        if (redis.exists(key)){
            redis.del(key);
        }

        if (cardDtoOptional.isPresent()) {
            try {
                addCardText(cardDtoOptional.get(), cardModel);
            } catch (CardNotSavedException e) {
                throw new CardNotSavedException(e.getMessage());
            }
        }

        if (optFiles.isPresent()) {
            try {
                imageService.addCardImages(optFiles.get(), cardModel, cardModel.getImages().size());
            } catch (ImageNotSavedException e) {
                throw new ImageNotSavedException(e.getMessage());
            } catch (ImageLimitException e){
                throw new ImageLimitException(e.getMessage());
            }
        }
    }
}
