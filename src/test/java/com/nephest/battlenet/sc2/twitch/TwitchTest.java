// Copyright (C) 2020-2025 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.twitch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.junit.jupiter.DisabledIf;

@DisabledIf
(
    expression = "#{environment['spring.security.oauth2.client.registration.twitch-sys.client-secret'] == null}",
    reason = "Twitch key not found",
    loadContext = true
)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TwitchTest
{
}
