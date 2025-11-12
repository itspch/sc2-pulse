// Copyright (C) 2020-2025 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.config.convert.min;

import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import org.springframework.core.convert.converter.Converter;

public class TimestampToMinimalObjectConverter
implements Converter<Timestamp, Object>
{

    @Override
    public Object convert(@NotNull Timestamp source)
    {
        return source.getTime() / 1000;
    }

}
