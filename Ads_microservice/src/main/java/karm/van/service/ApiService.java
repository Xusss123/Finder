package karm.van.service;

import jakarta.annotation.PostConstruct;
import karm.van.dto.ImageDto;
import karm.van.dto.UserDtoRequest;
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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class ApiService {
    private WebClient webClient;

    @PostConstruct
    public void init() {
        webClient = WebClient.create();
    }

    private <T> T sendPostRequest(String url, Object body, String token, String apiKey, ParameterizedTypeReference<T> responseType) {
        return webClient
                .post()
                .uri(url)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .headers(headers -> {
                    headers.setBearerAuth(token);
                    headers.set("x-api-key", apiKey);
                })
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType)
                .block();
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


    private <T> T sendGetResponse(String uri, String token, ParameterizedTypeReference<T> responseType){
        return webClient
                .get()
                .uri(uri)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .retrieve()
                .bodyToMono(responseType)
                .block();
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

    private MultiValueMap<String, HttpEntity<?>> buildMultipartBody(List<MultipartFile> files, int currentCardImagesCount) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        files.forEach(file -> builder.part("files", file.getResource()));
        builder.part("currentCardImagesCount", currentCardImagesCount);
        return builder.build();
    }


    public List<Long> postRequestToAddCardImage(List<MultipartFile> files, String url, int currentCardImagesCount, String token, String apiKey) {
        return sendPostRequest(
                url,
                buildMultipartBody(files, currentCardImagesCount),
                token,
                apiKey,
                new ParameterizedTypeReference<>() {}
        );
    }


    public List<ImageDto> getCardImagesRequest(List<Long> imagesId, String url, String token) {

        String uri = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("imagesId", imagesId)
                .toUriString();

        return sendGetResponse(
                uri,
                token,
                new ParameterizedTypeReference<>() {
                }
        );
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

    public Boolean validateToken(String token, String url) {
        Map<?, ?> responseMap = sendGetResponse(url, token, Map.class);
        return responseMap != null && responseMap.containsKey("valid") && (Boolean) responseMap.get("valid");
    }


    public HttpStatusCode requestToDelAllCommentsByCard(String url, String token, String apiKey) {
        return sendDeleteRequest(url,token,apiKey);
    }

    public void sendDeleteImagesFromMinioRequest(String url, List<Long> imagesId, String token, String apiKey) {
        String ids = getIds(imagesId);

        String fullUrl = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("ids", ids)
                .toUriString();

        sendDeleteRequest(fullUrl,token,apiKey);
    }

    public HttpStatusCode requestToDeleteOneImageFromDB(String url, String token, String apiKey) {
        return sendDeleteRequest(url,token,apiKey);
    }

    public HttpStatusCode requestToUnlinkCardFromUser(String url, String token, String apiKey) {
        return sendDeleteRequest(url,token,apiKey);
    }

    private HttpStatusCode sendMoveRequest(String url,String token,String apiKey){
        return sendPostRequest(url,token,apiKey);
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

    public HttpStatusCode addCardToUser(String url,String token,String apiKey) throws NullPointerException{
        return sendPostRequest(url,token,apiKey);
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

}
