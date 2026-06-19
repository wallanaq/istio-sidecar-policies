package com.example.tokenization.client;

import com.example.tokenization.service.exception.BinCheckerUnavailableException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Component
public class BinCheckerClient {

    private final RestClient restClient;

    public BinCheckerClient(BinCheckerProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.connectTimeout()));
        factory.setReadTimeout(Duration.ofMillis(properties.readTimeout()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.url())
                .requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attrs != null) {
                        String auth = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                        if (auth != null) {
                            request.getHeaders().set(HttpHeaders.AUTHORIZATION, auth);
                        }
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    public BinInfo findByBin(String bin) {
        try {
            return restClient.get()
                    .uri("/v1/bin/{bin}", bin)
                    .retrieve()
                    .body(BinInfo.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new BinNotFoundException(bin);
        } catch (RestClientException e) {
            throw new BinCheckerUnavailableException();
        }
    }
}
