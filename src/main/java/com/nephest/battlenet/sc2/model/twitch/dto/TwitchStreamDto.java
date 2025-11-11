// Copyright (C) 2020-2025 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model.twitch.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TwitchStreamDto
(
    @NotNull String id,
    @NotNull String userId,
    @NotNull String userLogin,
    @NotNull String userName,
    @NotNull String gameId,
    @NotNull String gameName,
    @NotNull String type,
    @NotNull String title,
    @NotNull Integer viewerCount,
    @NotNull String language,
    @NotNull String thumbnailUrl
)
{

    public static final String DIMENSIONS_PLACEHOLDER_REGEXP = "\\{width}x\\{height}";

    public String getThumbnailUrl(int width, int height)
    {
        if(width < 1) throw new IllegalArgumentException("Positive width expected: " + width);
        if(height < 1) throw new IllegalArgumentException("Positive height expected: " + height);

        return thumbnailUrl.replaceAll(DIMENSIONS_PLACEHOLDER_REGEXP, width + "x" + height);
    }

}
