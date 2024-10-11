package karm.van.service;

import jakarta.annotation.PostConstruct;
import karm.van.dto.UserDtoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Objects;

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

    private <T> T sendGetResponse(String uri, String token, Class<T> responseType){
        try {
            return webClient
                    .get()
                    .uri(uri)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        throw new RuntimeException();
                    })
                    .bodyToMono(responseType)
                    .block();
        }catch (Exception e){
            return null;
        }
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

    private HttpStatusCode sendDeleteRequest(String url, String token, String apiKey) {
        return Objects.requireNonNull(
                        webClient
                                .delete()
                                .uri(url)
                                .headers(headers -> {
                                    headers.setBearerAuth(token);
                                    headers.set("x-api-key", apiKey);
                                })
                                .retrieve()
                                .toBodilessEntity()
                                .block())
                .getStatusCode();
    }

    public Boolean validateToken(String token, String url) {
        Map<?, ?> responseMap = sendGetResponse(url, token, Map.class);
        return responseMap != null && responseMap.containsKey("valid") && (Boolean) responseMap.get("valid");
    }

    private UserDtoRequest fetchUserData(String uri, String token) {
        return sendGetResponse(uri,token, UserDtoRequest.class);
    }

    public UserDtoRequest getUserByToken(String url, String token) {
        return fetchUserData(url, token);
    }

    public UserDtoRequest getUserById(String url, String token, Long userId) {
        String uri = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("userId", userId)
                .toUriString();
        return fetchUserData(uri, token);
    }

    public HttpStatusCode addCommentToUser(String url,String token,String apiKey) throws NullPointerException{
        return sendPostRequest(url,token,apiKey);
    }

    public HttpStatusCode requestToUnlinkCommentFromUser(String url, String token, String apiKey) {
        return sendDeleteRequest(url,token,apiKey);
    }
}
