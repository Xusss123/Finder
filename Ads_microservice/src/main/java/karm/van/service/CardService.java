package karm.van.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import karm.van.config.CommentMicroServiceProperties;
import karm.van.config.ImageMicroServiceProperties;
import karm.van.dto.*;
import karm.van.exception.card.CardNotDeletedException;
import karm.van.exception.card.CardNotFoundException;
import karm.van.exception.card.CardNotSavedException;
import karm.van.exception.image.ImageLimitException;
import karm.van.exception.image.ImageNotSavedException;
import karm.van.exception.other.SerializationException;
import karm.van.model.CardModel;
import karm.van.repository.CardRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import redis.clients.jedis.JedisPooled;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class CardService {
    private final CardRepo cardRepo;
    private JedisPooled redis;
    private final ObjectMapper objectMapper;
    private final CommentMicroServiceProperties commentProperties;
    private final ImageMicroServiceProperties imageProperties;
    private final RestService restService;

    @Value("${redis.host}")
    private String redisHost;

    @Value("${card.images.count}")
    private int allowedImagesCount;

    @PostConstruct
    public void init(){
        redis = new JedisPooled(redisHost,6379);
    }

    private CardModel addCardText(CardDto cardDto) throws CardNotSavedException {

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
        }catch (CardNotSavedException e){
            log.debug("Card has not been saved: "+e.getMessage());
            throw new CardNotSavedException(e.getMessage());
        }
    }

    @Transactional
    public void addCard(List<MultipartFile> files, CardDto cardDto) throws ImageNotSavedException, CardNotSavedException, ImageLimitException {
        try {
            if (files.size() > allowedImagesCount) {
                throw new ImageLimitException("You have provided more than " + allowedImagesCount + " images");
            }

            CardModel cardModel = addCardText(cardDto);

            String url = restService.buildUrl(imageProperties.getPrefix(),
                                    imageProperties.getHost(),
                                    imageProperties.getPort(),
                                    imageProperties.getEndpoints().getAddCardImages());

            List<Long> imageIds = restService.postRequestToAddCardImage(files, url,0);

            if (imageIds == null || imageIds.isEmpty()) {
                throw new ImageNotSavedException("Image IDs not returned");
            }

            cardModel.setImgIds(imageIds);
            cardRepo.save(cardModel);  // Сохраняем один раз в конце

        } catch (ImageNotSavedException | ImageLimitException | CardNotSavedException e) {
            log.debug("in class - "+e.getClass()+"an error has occurred: "+e.getMessage());
            throw e;
        } catch (Exception e){
            log.debug("An unknown error occurred while deleting the card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    public FullCardDtoForOutput getCard(Long id) throws CardNotFoundException, SerializationException {
        String key = "card%d".formatted(id);
        if (!redis.exists(key)) {//Если кеш отсутствует
            return cacheCard(id,key);
        } else {//Если кеш найден
            try {
                return objectMapper.readValue(redis.get(key), FullCardDtoForOutput.class);//Десериализуем строку в объект и возвращаем
            } catch (JsonProcessingException e) {
                throw new SerializationException("an error occurred during serialization");
            }
        }
    }

    private FullCardDtoForOutput cacheCard(Long id, String key) throws CardNotFoundException, SerializationException {
        Optional<CardModel> cardModelOptional = cardRepo.getCardModelById(id);//Ищем запись в БД

        if (cardModelOptional.isEmpty()) {//Если записи в БД нет
            throw new CardNotFoundException("card with this id doesn't exist");//Ошибка о том, что карточки не существует
        }

        CardModel card = cardModelOptional.get();
        String objectAsString;

        String url = restService.buildUrl(
                imageProperties.getPrefix(),
                imageProperties.getHost(),
                imageProperties.getPort(),
                imageProperties.getEndpoints().getGetImages()
        );

        List<ImageDto> images = restService.getCardImagesRequest(card.getImgIds(),url);

        FullCardDtoForOutput fullCardDtoForOutput = new FullCardDtoForOutput(card.getId(),card.getTitle(),card.getText(),card.getCreateTime(),images);

        try {
            objectAsString = objectMapper.writeValueAsString(fullCardDtoForOutput);//Сериализуем объект в строку
        } catch (Exception e) {
            log.error("An error occurred during serialization for redis: "+e.getMessage());
            throw new SerializationException("an error occurred during serialization");
        }

        redis.set(key, objectAsString);//Кешируем объект
        redis.expire(key, 60);//Устанавливаем время жизни
        return fullCardDtoForOutput;
    }

    @Transactional
    public void deleteCard(Long id) throws CardNotDeletedException, CardNotFoundException {
        String key = String.format("card%d", id);
        try {
            CardModel cardModel = cardRepo.getCardModelById(id)
                    .orElseThrow(() -> new CardNotFoundException("Card with this id doesn't exist"));

            if (redis.exists(key)) {
                redis.del(key);
            }

            // URL для удаления комментариев
            String commentUrl = restService.buildUrl(
                    commentProperties.getPrefix(),
                    commentProperties.getHost(),
                    commentProperties.getPort(),
                    commentProperties.getEndpoints().getDellAllCommentsByCard(),
                    id
            );

            HttpStatusCode commentStatusCode = restService.requestToDelAllCommentsByCard(commentUrl);

            if (commentStatusCode != HttpStatus.OK) {
                throw new CardNotDeletedException("An error occurred on the server side during the deletion of comments");
            }

            List<Long> imagesId = cardModel.getImgIds();
            cardRepo.deleteById(id);

            // URL для удаления изображений
            String imageUrl = restService.buildUrl(
                    imageProperties.getPrefix(),
                    imageProperties.getHost(),
                    imageProperties.getPort(),
                    imageProperties.getEndpoints().getDelImagesFromMinio()
            );

            HttpStatusCode imageStatusCode = restService.sendDeleteImagesFromMinioRequest(imageUrl, imagesId);

            if (imageStatusCode != HttpStatus.OK) {
                throw new CardNotDeletedException("An error occurred on the server side during the image deletion");
            }
        } catch (CardNotFoundException | CardNotDeletedException e) {
            throw e;
        } catch (Exception e) {
            log.debug("An unknown error occurred while deleting the card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    public CardPageResponseDto getAllCards(int pageNumber, int limit){
        Page<CardModel> page = cardRepo.findAll(PageRequest.of(pageNumber,limit));
        return new CardPageResponseDto(
                page.getContent(),
                page.isLast(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isFirst(),
                page.getNumberOfElements());
    }

    @Transactional
    public void patchCard(Long id, Optional<CardDto> cardDtoOptional, Optional<List<MultipartFile>> optFiles) throws CardNotFoundException, CardNotSavedException, ImageNotSavedException, ImageLimitException {
       try {
           CardModel cardModel = cardRepo.getCardModelById(id)
                   .orElseThrow(() -> new CardNotFoundException("Card with this id doesn't exist"));

           String key = "card%d".formatted(id);
           boolean cardChange = false;

           if (cardDtoOptional.isPresent()) {
               try {
                   addCardText(cardDtoOptional.get(), cardModel);
                   cardChange = true;
               } catch (CardNotSavedException e) {
                   throw new CardNotSavedException(e.getMessage());
               }
           }

           if (optFiles.isPresent()) {
               String url = restService.buildUrl(imageProperties.getPrefix(),
                       imageProperties.getHost(),
                       imageProperties.getPort(),
                       imageProperties.getEndpoints().getAddCardImages());
               try {
                   List<Long> imageIds = restService.postRequestToAddCardImage(optFiles.get(), url, cardModel.getImgIds().size());

                   if (imageIds == null || imageIds.isEmpty()) {
                       throw new ImageNotSavedException("An error occurred and the images were not saved");
                   }else {
                       List<Long> currentImagesId = cardModel.getImgIds();
                       imageIds.parallelStream().forEach(currentImagesId::add);
                       cardModel.setImgIds(currentImagesId);
                       cardRepo.save(cardModel);
                       cardChange = true;
                   }
               }catch (WebClientResponseException.BadRequest e){
                   throw new ImageLimitException("There is a maximum number of images in this ad");
               }
           }

           if (cardChange && redis.exists(key)){
               redis.del(key);
           }
       }catch (CardNotFoundException | CardNotSavedException | ImageNotSavedException | ImageLimitException e){
           throw e;
       }catch (Exception e){
           log.debug("An unknown error occurred while deleting the card: "+e.getMessage()+" - "+e.getClass());
           throw new RuntimeException("Unexpected error occurred", e);
       }
    }

    @Transactional
    public void delOneImageInCard(Long cardId, Long imageId) throws CardNotFoundException {

        CardModel card = cardRepo.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card with id doesn't exist"));

        card.getImgIds().remove(imageId);

        cardRepo.save(card);
    }
}
