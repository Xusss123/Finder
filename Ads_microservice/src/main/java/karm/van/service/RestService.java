package karm.van.service;

import jakarta.annotation.PostConstruct;
import karm.van.dto.ImageDto;
import karm.van.exception.other.ServerException;
import lombok.RequiredArgsConstructor;
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
import redis.clients.jedis.JedisPooled;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
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

    public List<Long> postRequestToAddCardImage(List<MultipartFile> files,String url,int currentCardImagesCount){
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

    public List<ImageDto> getCardImagesRequest(List<Long> imagesId, String url) {

        String uri = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("imagesId", imagesId)
                .toUriString();

        return webClient
                .get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                    // Обработка 5xx ошибок (например, ошибка 500)
                    return Mono.error(new ServerException("An error occurred on the server"));
                })
                .bodyToFlux(ImageDto.class)
                .collectList()
                .block();
    }

    public HttpStatusCode requestToDelAllCommentsByCard(String url) {
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

    public HttpStatusCode sendDeleteImagesFromMinioRequest(String url, List<Long> imagesId) {
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

}
