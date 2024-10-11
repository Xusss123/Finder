package karm.van.service;

import jakarta.annotation.PostConstruct;
import karm.van.dto.UserDtoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

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


    public Long requestToLinkImageAndUser(String uri, String token, String apiKey) {
        return Objects.requireNonNull(
                webClient
                        .patch()
                        .uri(uri)
                        .headers(headers->{
                            headers.setBearerAuth(token);
                            headers.set("x-api-key",apiKey);
                        })
                        .retrieve()
                        .bodyToMono(Long.class)
                        .onErrorResume(e -> {
                            log.error("Error: {}", e.getMessage());
                            return Mono.empty(); // Возвращаем пустой Mono при ошибке
                        })
                        .block());
    }
}
