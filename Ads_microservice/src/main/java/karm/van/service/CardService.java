package karm.van.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import karm.van.config.CommentMicroServiceProperties;
import karm.van.config.ImageMicroServiceProperties;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import redis.clients.jedis.JedisPooled;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class CardService {
    private final CardRepo cardRepo;
    private WebClient webClient;
    private JedisPooled redis;
    private final ObjectMapper objectMapper;
    private final CommentMicroServiceProperties commentProperties;
    private final ImageMicroServiceProperties imageProperties;

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
        }catch (Exception e){
            log.error(e.getMessage());
            throw new CardNotSavedException(e.getMessage());
        }
    }

    private MultiValueMap<String, HttpEntity<?>> buildMultipartBody(List<MultipartFile> files, int currentCardImagesCount) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        files.forEach(file -> builder.part("files", file.getResource()));
        builder.part("currentCardImagesCount", currentCardImagesCount);
        return builder.build();
    }

    private List<Long> postRequestToAddCardImage(List<MultipartFile> files,String url,int currentCardImagesCount){
        // Выполняем запрос и получаем статус и тело
        return webClient.post()
                .uri(url)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(buildMultipartBody(files, currentCardImagesCount)))
                .exchangeToMono(response -> {
                    // Проверяем статус ответа
                    if (response.statusCode().is2xxSuccessful()) {
                        // Если статус успешный, возвращаем тело ответа как List<Long>
                        return response.bodyToMono(new ParameterizedTypeReference<List<Long>>() {});
                    } else {
                        // Если статус не 2xx, выбрасываем исключение с сообщением об ошибке
                        return response.createException().flatMap(Mono::error);
                    }
                })
                .block();  // Блокируем для синхронного выполнения
    }

    @Transactional
    public void addCard(List<MultipartFile> files, CardDto cardDto) throws ImageNotSavedException, CardNotSavedException, ImageLimitException {
        try {
            if (files.size() > allowedImagesCount) {
                throw new ImageLimitException("You have provided more than " + allowedImagesCount + " images");
            }

            CardModel cardModel = addCardText(cardDto);

            String url = buildUrl(imageProperties.getPrefix(),
                                    imageProperties.getHost(),
                                    imageProperties.getPort(),
                                    imageProperties.getEndpoints().getAddCardImages());

            List<Long> imageIds = postRequestToAddCardImage(files, url,0);

            if (imageIds == null || imageIds.isEmpty()) {
                throw new ImageNotSavedException("Image IDs not returned");
            }

            cardModel.setImgIds(imageIds);
            cardRepo.save(cardModel);  // Сохраняем один раз в конце

        } catch (ImageNotSavedException | ImageLimitException | CardNotSavedException e) {
            throw e;
        }
    }

    public CardModel getCard(Long id) throws CardNotFoundException, SerializationException {
        //TODO Возвращать не CardModel, а CardDTO, в котором будут не id на изображения, а список самих изображений
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

    private HttpStatusCode requestToDelAllCommentsByCard(String url) {
        return Objects.requireNonNull(
                        webClient
                                .delete()
                                .uri(url)
                                .retrieve()
                                .toBodilessEntity()
                                .block()
                )
                .getStatusCode();
    }

    private HttpStatusCode sendDeleteImagesFromMinioRequest(String url, List<Long> imagesId) {
        String ids = imagesId.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        String fullUrl = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("ids", ids)
                .toUriString();

        return Objects.requireNonNull(
                        webClient
                                .delete()
                                .uri(fullUrl)
                                .retrieve()
                                .toBodilessEntity()
                                .block()
                )
                .getStatusCode();
    }

    private String buildUrl(String prefix, String host, String port, String endpoint, Long id) {
        return UriComponentsBuilder.fromHttpUrl(prefix + host + ":" + port + endpoint + id)
                .toUriString();
    }

    private String buildUrl(String prefix, String host, String port, String endpoint) {
        return UriComponentsBuilder.fromHttpUrl(prefix + host + ":" + port + endpoint)
                .toUriString();
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
            String commentUrl = buildUrl(
                    commentProperties.getPrefix(),
                    commentProperties.getHost(),
                    commentProperties.getPort(),
                    commentProperties.getEndpoints().getDellAllCommentsByCard(),
                    id
            );

            HttpStatusCode commentStatusCode = requestToDelAllCommentsByCard(commentUrl);

            if (commentStatusCode != HttpStatus.OK) {
                throw new CardNotDeletedException("An error occurred on the server side during the deletion of comments");
            }

            List<Long> imagesId = cardModel.getImgIds();
            cardRepo.deleteById(id);

            // URL для удаления изображений
            String imageUrl = buildUrl(
                    imageProperties.getPrefix(),
                    imageProperties.getHost(),
                    imageProperties.getPort(),
                    imageProperties.getEndpoints().getDelImagesFromMinio()
            );

            HttpStatusCode imageStatusCode = sendDeleteImagesFromMinioRequest(imageUrl, imagesId);

            if (imageStatusCode != HttpStatus.OK) {
                throw new CardNotDeletedException("An error occurred on the server side during the image deletion");
            }
        } catch (CardNotFoundException | CardNotDeletedException e) {
            throw e; // Пробрасываем исключение дальше
        } catch (Exception e) {
            // Логирование исключения
            throw new RuntimeException("Unexpected error occurred", e);
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
            String url = buildUrl(imageProperties.getPrefix(),
                    imageProperties.getHost(),
                    imageProperties.getPort(),
                    imageProperties.getEndpoints().getAddCardImages());
            try {
                List<Long> imageIds = postRequestToAddCardImage(optFiles.get(), url, cardModel.getImgIds().size());

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

    }

    @Transactional
    public void delOneImageInCard(Long cardId, Long imageId) throws CardNotFoundException {

        CardModel card = cardRepo.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card with id doesn't exist"));

        card.getImgIds().remove(imageId);

        cardRepo.save(card);
    }
}
