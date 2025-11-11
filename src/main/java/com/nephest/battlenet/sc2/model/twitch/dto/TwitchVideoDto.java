// Copyright (C) 2020-2025 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model.twitch.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TwitchVideoDto
(
    @NotNull String id,
    @NotNull String userId,
    @NotNull String userLogin,
    @NotNull String userName,
    @NotNull String title,
    @NotNull String description,
    @NotNull OffsetDateTime createdAt,
    @NotNull OffsetDateTime publishedAt,
    @NotNull String url,
    @NotNull String thumbnailUrl,
    @NotNull String viewable,
    @NotNull String language,
    @NotNull Type type,
    @NotNull String duration
)
{

    public enum Type
    {

        ARCHIVE,
        HIGHLIGHT,
        UPLOAD;

        @JsonValue
        public String toTwitchString()
        {
            return this.name().toLowerCase();
        }

    }

}
