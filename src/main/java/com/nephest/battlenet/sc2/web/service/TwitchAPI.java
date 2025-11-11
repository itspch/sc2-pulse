// Copyright (C) 2020-2025 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.web.service;

import static com.nephest.battlenet.sc2.web.service.WebServiceUtil.getHttpClient;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nephest.battlenet.sc2.model.twitch.dto.TwitchDataDto;
import com.nephest.battlenet.sc2.model.twitch.dto.TwitchStreamDto;
import com.nephest.battlenet.sc2.model.twitch.dto.TwitchUserDto;
import com.nephest.battlenet.sc2.model.twitch.dto.TwitchVideoDto;
import com.nephest.battlenet.sc2.twitch.Twitch;
import com.nephest.battlenet.sc2.web.util.ReactorRateLimiter;
import java.time.Duration;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.RemoveAuthorizedClientOAuth2AuthorizationFailureHandler;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Service
@Twitch
public class TwitchAPI
extends BaseAPI
{

    public static final String BASE_URL = "https://api.twitch.tv/helix";
    public static final int REQUESTS_PER_PERIOD = 400;
    public static final Duration REQUEST_SLOT_REFRESH_DURATION = Duration.ofSeconds(60);

    public static final int USER_BATCH_SIZE = 100;
    public static final int STREAM_BATCH_SIZE = 100;

    private final ReactorRateLimiter rateLimiter = new ReactorRateLimiter();

    @Autowired
    public TwitchAPI
    (
        ObjectMapper objectMapper,
        OAuth2AuthorizedClientManager auth2AuthorizedClientManager,
        RemoveAuthorizedClientOAuth2AuthorizationFailureHandler failureHandler,
        @Value("${spring.security.oauth2.client.registration.twitch-sys.client-id}") String clientId
    )
    {
        setWebClient(createWebClient(objectMapper, auth2AuthorizedClientManager, failureHandler, clientId));
        Flux.interval(Duration.ofSeconds(0), REQUEST_SLOT_REFRESH_DURATION)
            .doOnNext(i->rateLimiter
                .refreshUndeterminedSlots(REQUEST_SLOT_REFRESH_DURATION, REQUESTS_PER_PERIOD))
            .subscribe();
    }

    private static WebClient createWebClient
    (
        ObjectMapper objectMapper,
        OAuth2AuthorizedClientManager auth2AuthorizedClientManager,
        RemoveAuthorizedClientOAuth2AuthorizationFailureHandler failureHandler,
        String clientId
    )
    {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
            new ServletOAuth2AuthorizedClientExchangeFilterFunction(auth2AuthorizedClientManager);
        oauth2Client.setDefaultClientRegistrationId("twitch-sys");
        oauth2Client.setAuthorizationFailureHandler(failureHandler);
        HttpClient httpClient = getHttpClient
        (
            WebServiceUtil.CONNECT_TIMEOUT,
            WebServiceUtil.IO_TIMEOUT
        );
        return WebServiceUtil
            .getWebClientBuilder(WebServiceUtil.CONNECTION_PROVIDER_MEDIUM, objectMapper)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl(BASE_URL)
            .defaultHeader("Client-Id", clientId)
            .apply(oauth2Client.oauth2Configuration())
            .build();
    }

    public Flux<TwitchUserDto> getUsersByIds(Set<String> ids)
    {
        return Flux.fromIterable(ids)
            .buffer(USER_BATCH_SIZE)
            .flatMap(idBatch->getWebClient().get()
                .uri(b->b.path("/users").queryParam("id", idBatch).build())
                .accept(APPLICATION_JSON)
                .exchangeToMono
                (
                    resp->WebServiceUtil
                        .updateRequestRateAndHandleMonoResponse
                        (
                            rateLimiter,
                            resp,
                            new ParameterizedTypeReference<TwitchDataDto<TwitchUserDto>>(){}
                        )
                )
                .retryWhen(rateLimiter.retryWhen(getRetry(WebServiceUtil.RETRY)))
                .delaySubscription(Mono.defer(rateLimiter::requestSlot))
            )
            .flatMapIterable(TwitchDataDto::data);
    }

    public Flux<TwitchUserDto> getUsersByLogins(Set<String> logins)
    {
        return Flux.fromIterable(logins)
            .buffer(USER_BATCH_SIZE)
            .flatMap(loginBatch->getWebClient().get()
                .uri(b->b.path("/users").queryParam("login", loginBatch).build())
                .accept(APPLICATION_JSON)
                .exchangeToMono
                (
                    resp->WebServiceUtil
                        .updateRequestRateAndHandleMonoResponse
                        (
                            rateLimiter,
                            resp,
                            new ParameterizedTypeReference<TwitchDataDto<TwitchUserDto>>(){}
                        )
                )
                .retryWhen(rateLimiter.retryWhen(getRetry(WebServiceUtil.RETRY)))
                .delaySubscription(Mono.defer(rateLimiter::requestSlot))
            )
            .flatMapIterable(TwitchDataDto::data);
    }

    public Flux<TwitchStreamDto> getStreamsByGameId(String gameId, int first)
    {
        if(first < 1) return Flux.empty();
        if(first > STREAM_BATCH_SIZE)
            return Flux.error(new UnsupportedOperationException("Pagination is not supported"));

        return getWebClient().get()
            .uri
            (
                b->b.path("/streams")
                    .queryParam("game_id", gameId)
                    .queryParam("first", first)
                    .build()
            )
            .accept(APPLICATION_JSON)
            .exchangeToMono
            (
                resp->WebServiceUtil
                    .updateRequestRateAndHandleMonoResponse
                    (
                        rateLimiter,
                        resp,
                        new ParameterizedTypeReference<TwitchDataDto<TwitchStreamDto>>(){}
                    )
            )
            .flatMapIterable(TwitchDataDto::data)
            .retryWhen(rateLimiter.retryWhen(getRetry(WebServiceUtil.RETRY)))
            .delaySubscription(Mono.defer(rateLimiter::requestSlot));
    }

    public Flux<TwitchVideoDto> getVideosByUserId
    (
        String userId,
        TwitchVideoDto.Type type,
        int first
    )
    {
        return getWebClient().get()
            .uri
            (
                b->b.path("/videos")
                    .queryParam("user_id", userId)
                    .queryParam("type", type.toTwitchString())
                    .queryParam("first", first)
                    .build()
            )
            .accept(APPLICATION_JSON)
            .exchangeToMono
            (
                resp->WebServiceUtil
                    .updateRequestRateAndHandleMonoResponse
                    (
                        rateLimiter,
                        resp,
                        new ParameterizedTypeReference<TwitchDataDto<TwitchVideoDto>>(){}
                    )
            )
            .flatMapIterable(TwitchDataDto::data)
            .retryWhen(rateLimiter.retryWhen(getRetry(WebServiceUtil.RETRY)))
            .delaySubscription(Mono.defer(rateLimiter::requestSlot));
    }

}
