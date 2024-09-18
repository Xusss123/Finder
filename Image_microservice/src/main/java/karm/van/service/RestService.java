package karm.van.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

@RequiredArgsConstructor
@Component
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

    public HttpStatusCode requestToDelOneImage(String url) {
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

}
