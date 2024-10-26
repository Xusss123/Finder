package karm.van.service;

import jakarta.annotation.PostConstruct;
import karm.van.dto.image.ImageDto;
import karm.van.dto.user.UserDtoRequest;
import karm.van.exception.other.ServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
@Deprecated
public class RestService {
    private WebClient webClient;

    @PostConstruct
    public void init(){
        webClient = WebClient.create();
    }

    private MultiValueMap<String, HttpEntity<?>> buildMultipartBody(List<MultipartFile> files, int currentCardImagesCount) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        files.forEach(file -> builder.part("files", file.getResource()));
        builder.part("currentCardImagesCount", currentCardImagesCount);
        return builder.build();
    }

    public List<Long> postRequestToAddCardImage(List<MultipartFile> files,
                                                String url,
                                                int currentCardImagesCount,
                                                String token,
                                                String apiKey){
        // Выполняем запрос и получаем статус и тело
        return webClient.post()
                .uri(url)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .headers(header->{
                    header.setBearerAuth(token);
                    header.set("x-api-key",apiKey);
                })
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

    public List<ImageDto> getCardImagesRequest(List<Long> imagesId, String url, String token) {

        String uri = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("imagesId", imagesId)
                .toUriString();

        return webClient
                .get()
                .uri(uri)
                .headers(headers->headers.setBearerAuth(token))
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                    // Обработка 5xx ошибок (например, ошибка 500)
                    return Mono.error(new ServerException("An error occurred on the server"));
                })
                .bodyToFlux(ImageDto.class)
                .collectList()
                .block();
    }

    public HttpStatusCode requestToDelAllCommentsByCard(String url, String token, String apiKey) {
        return Objects.requireNonNull(
                        webClient
                                .delete()
                                .uri(url)
                                .headers(headers->{
                                    headers.setBearerAuth(token);
                                    headers.set("x-api-key",apiKey);
                                })
                                .retrieve()
                                .toBodilessEntity()
                                .block()
                )
                .getStatusCode();
    }

    public String buildUrl(String prefix, String host, String port, String endpoint, Long... ids) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(prefix + host + ":" + port + endpoint);

        if (ids.length > 0) {
            uriBuilder.pathSegment(ids[0].toString());
            for (int i = 1; i < ids.length; i++) {
                uriBuilder.pathSegment(ids[i].toString());
            }
        }

        return uriBuilder.toUriString();
    }

    public void sendDeleteImagesFromMinioRequest(String url, List<Long> imagesId, String token, String apiKey) {
        String ids = imagesId.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        String fullUrl = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("ids", ids)
                .toUriString();

        webClient
                .delete()
                .uri(fullUrl)
                .headers(header->{
                    header.setBearerAuth(token);
                    header.set("x-api-key",apiKey);
                })
                .retrieve()
                .toBodilessEntity()
                .block();

    }

    private HttpStatusCode sendMoveRequest(String fullUrl,String token,String apiKey){
        return Objects.requireNonNull(webClient
                        .post()
                        .uri(fullUrl)
                        .headers(headers -> {
                            headers.setBearerAuth(token);
                            headers.set("X-Api-Key",apiKey);
                        })
                        .retrieve()
                        .toBodilessEntity()
                        .block())
                .getStatusCode();
    }

    private String getIds(List<Long> imagesId){
        return imagesId.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public void moveImagesToImagePackage(String url, List<Long> imagesId, String token, String apiKey) {
        String ids = getIds(imagesId);

        String fullUrl = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("ids", ids)
                .toUriString();

        sendMoveRequest(fullUrl,token,apiKey);
    }

    public HttpStatusCode moveImagesToTrashPackage(String url, List<Long> imagesId, String token, String apiKey) {
        String ids = getIds(imagesId);

        String fullUrl = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("ids", ids)
                .queryParam("toTrash",true)
                .toUriString();

        return sendMoveRequest(fullUrl,token,apiKey);
    }

    public Boolean validateToken(String token, String url) {
        // Используем block() для синхронного получения результата
        return webClient
                .get()
                .uri(url) // Указываем URL для валидации
                .headers(headers->headers.setBearerAuth(token)) // Добавляем заголовок Authorization
                .retrieve()
                .bodyToMono(Map.class) // Преобразуем ответ в Map
                .map(response -> (Boolean) response.get("valid")) // Извлекаем значение "valid"
                .onErrorReturn(false) // Возвращаем false в случае ошибки
                .block(); // Блокируем выполнение до получения результата
    }

    public HttpStatusCode addCardToUser(String url,String token,String apiKey) throws NullPointerException{
        // Выполняем запрос и получаем статус и тело
        return Objects.requireNonNull(webClient.post()
                .uri(url)
                .headers(header->{
                    header.setBearerAuth(token);
                    header.set("x-api-key",apiKey);
                })
                .retrieve()
                .toBodilessEntity()
                .block()).getStatusCode();  // Блокируем для синхронного выполнения
    }

    private UserDtoRequest fetchUserData(String uri, String token) {
        return webClient.get()
                .uri(uri)
                .headers(header -> header.setBearerAuth(token))
                .retrieve()
                .bodyToMono(UserDtoRequest.class)
                .doOnNext(response -> log.info("Successfully retrieved user data: {}", response))
                .onErrorResume(e -> {
                    log.error("Error retrieving user data: {}", e.getMessage());
                    return Mono.empty(); // Возвращаем пустой Mono при ошибке
                })
                .block();
    }

    // Метод для получения пользователя по токену
    public UserDtoRequest getUserByToken(String url, String token) {
        return fetchUserData(url, token); // Используем универсальный метод
    }

    // Метод для получения пользователя по ID
    public UserDtoRequest getUserById(String url, String token, Long userId) {
        String uri = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("userId", userId)
                .toUriString();
        return fetchUserData(uri, token); // Используем универсальный метод
    }

    public HttpStatusCode requestToDeleteOneImageFromDB(String url, String token, String apiKey) {
        return Objects.requireNonNull(webClient
                .delete()
                .uri(url)
                .headers(header->{
                    header.setBearerAuth(token);
                    header.set("x-api-key",apiKey);
                })
                .retrieve()
                .toBodilessEntity()
                .block()).getStatusCode();

    }

    public HttpStatusCode requestToUnlinkCardFromUser(String url, String token, String apiKey) {
        return Objects.requireNonNull(
                        webClient
                                .delete()
                                .uri(url)
                                .headers(header->{
                                    header.setBearerAuth(token);
                                    header.set("x-api-key",apiKey);
                                })
                                .retrieve()
                                .toBodilessEntity()
                                .block()
                )
                .getStatusCode();
    }

}
