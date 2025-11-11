// Copyright (C) 2020-2025 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.web.service.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.nephest.battlenet.sc2.model.twitch.dto.TwitchStreamDto;
import com.nephest.battlenet.sc2.model.twitch.dto.TwitchUserDto;
import com.nephest.battlenet.sc2.web.service.TwitchAPI;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
public class TwitchVideoStreamSupplierTest
{

    @Mock
    private TwitchAPI api;

    private TwitchVideoStreamSupplier supplier;

    @BeforeEach
    public void beforeEach()
    {
        supplier = new TwitchVideoStreamSupplier(api);
    }

    @CsvSource
    ({
        "https://static-cdn.jtvnw.net/jtv_user_pictures/"
            + "ed0caac8-947b-412f-8a54-411588450540-profile_image-50x50.png, "
        + "https://static-cdn.jtvnw.net/jtv_user_pictures/"
            + "ed0caac8-947b-412f-8a54-411588450540-profile_image-50x50.png ",

        "https://static-cdn.jtvnw.net/jtv_user_pictures/"
            + "ed0caac8-947b-412f-8a54-411588450540-profile_image.png, "
        + "https://static-cdn.jtvnw.net/jtv_user_pictures/"
            + "ed0caac8-947b-412f-8a54-411588450540-profile_image.png ",

        "https://static-cdn.jtvnw.net/jtv_user_pictures/"
            + "ed0caac8-947b-412f-8a54-100x10050540-profile_image.png, "
        + "https://static-cdn.jtvnw.net/jtv_user_pictures/"
            + "ed0caac8-947b-412f-8a54-100x10050540-profile_image.png ",

        "https://static-cdn.jtvnw.net/jtv_user_pictures/"
            + "ed0caac8-947b-412f-8a54-100x10050540-profile_image-100x100.png, "
        + "https://static-cdn.jtvnw.net/jtv_user_pictures/"
            + "ed0caac8-947b-412f-8a54-100x10050540-profile_image-50x50.png ",
    })
    @ParameterizedTest
    public void normalizeStreamProfileImageUrlDimensions(String in, String out)
    {
        assertEquals(out, TwitchVideoStreamSupplier.normalizeStreamProfileImageUrlDimensions(in));
    }

    @Test
    public void whenThereIsNoCorrespondingStreamUser_thenSetUserValuesToNull()
    {
        TwitchStreamDto stream1 = new TwitchStreamDto
        (
            "1", "1", "login", "name",
            "gameId", "gameName",
            "type", "title", 1, "en", "http://127.0.0.1"
        );
        TwitchStreamDto stream2 = new TwitchStreamDto
        (
            "2", "2", "login", "name",
            "gameId", "gameName",
            "type", "title", 1, "en", "http://127.0.0.1"
        );

        when(api.getStreamsByGameId(anyString(), anyInt())).thenReturn(Flux.just(stream1, stream2));

        String profileImg1 = "https://static-cdn.jtvnw.net/jtv_user_pictures/"
            + "ed0caac8-947b-412f-8a54-100x10050540-profile_image-100x100.png";
        String normalizedImg1 = "https://static-cdn.jtvnw.net/jtv_user_pictures/"
            + "ed0caac8-947b-412f-8a54-100x10050540-profile_image-50x50.png";
        TwitchUserDto user1 = new TwitchUserDto
        (
            "1",
            "login",
            "name",
            profileImg1,
            "http://127.0.0.1"
        );
        when(api.getUsersByIds(any())).thenReturn(Flux.just(user1));

        List<VideoStream> streams = supplier.getStreams().collectList().block();
        streams.sort(Comparator.comparing(VideoStream::getId));
        assertEquals(normalizedImg1, streams.get(0).getProfileImageUrl());
        assertNull(streams.get(1).getProfileImageUrl());
    }

}
