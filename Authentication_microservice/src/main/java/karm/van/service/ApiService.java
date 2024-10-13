package karm.van.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class ApiService {
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

    private HttpStatusCode sendPostRequest(String url, String token, String apiKey) {
        return Objects.requireNonNull(
                webClient
                        .post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(headers -> {
                            headers.setBearerAuth(token);
                            headers.set("x-api-key", apiKey);
                        })
                        .retrieve()
                        .toBodilessEntity()
                        .block()
        ).getStatusCode();
    }

    private String getIds(List<Long> imagesId){
        return imagesId.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public void moveImagesToProfileImagePackage(String url, String token, String apiKey) {
        sendMoveRequest(url,token,apiKey);
    }

    public HttpStatusCode moveImagesToTrashPackage(String url, List<Long> imagesId, String token, String apiKey) {
        String ids = getIds(imagesId);

        String fullUrl = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("ids", ids)
                .queryParam("toTrash",true)
                .toUriString();

        return sendMoveRequest(fullUrl,token,apiKey);
    }

    public HttpStatusCode sendDeleteImagesFromMinioRequest(String url, List<Long> imagesId, String token, String apiKey) {
        String ids = getIds(imagesId);

        String fullUrl = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("ids", ids)
                .toUriString();

        return sendDeleteRequest(fullUrl,token,apiKey);
    }

    private HttpStatusCode sendDeleteRequest(String url, String token, String apiKey) {
        return sendDeleteRequest(url, token, apiKey != null ? Optional.of(apiKey) : Optional.empty());
    }

    private HttpStatusCode sendDeleteRequest(String url, String token) {
        return sendDeleteRequest(url, token, Optional.empty());
    }

    private HttpStatusCode sendDeleteRequest(String url, String token, Optional<String> apiKey) {
        return Objects.requireNonNull(
                        webClient
                                .delete()
                                .uri(url)
                                .headers(headers -> {
                                    headers.setBearerAuth(token);
                                    apiKey.ifPresent(key -> headers.set("x-api-key", key));
                                })
                                .retrieve()
                                .toBodilessEntity()
                                .block())
                .getStatusCode();
    }

    public HttpStatusCode requestToDelCard(String url, String token) {
        return sendDeleteRequest(url,token);
    }

    private HttpStatusCode sendMoveRequest(String url,String token,String apiKey){
        return sendPostRequest(url,token,apiKey);
    }

}
