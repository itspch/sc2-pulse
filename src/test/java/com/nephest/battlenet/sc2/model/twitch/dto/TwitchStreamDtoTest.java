// Copyright (C) 2020-2025 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model.twitch.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TwitchStreamDtoTest
{

    private static final TwitchStreamDto DTO = new TwitchStreamDto
    (
        "1",
        "2", "login", "name",
        "gameId", "gameName",
        "live",
        "",
        3,
        "en",
        "https://static-cdn.jtvnw.net/previews-ttv/live_user_afro-{width}x{height}.jpg"
    );

    @Test
    public void testGetThumbnailUrl()
    {
        assertEquals
        (
            "https://static-cdn.jtvnw.net/previews-ttv/live_user_afro-20x30.jpg",
            DTO.getThumbnailUrl(20, 30)
        );
    }

    public static Stream<Arguments> invalidDimensions()
    {
        return Stream.of(0, -1).map(Arguments::of);
    }

    @MethodSource("invalidDimensions")
    @ParameterizedTest
    public void whenNonPositiveWidth_thenThrowException(int width)
    {
        assertThrows
        (
            IllegalArgumentException.class,
            ()->DTO.getThumbnailUrl(width, 10),
            "Positive width expected: " + width
        );
    }

    @MethodSource("invalidDimensions")
    @ParameterizedTest
    public void whenNonPositiveHeight_thenThrowException(int height)
    {
        assertThrows
        (
            IllegalArgumentException.class,
            ()->DTO.getThumbnailUrl(10, height),
            "Positive height expected: " + height
        );
    }

}
