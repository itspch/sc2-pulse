// Copyright (C) 2020-2025 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model.validation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nephest.battlenet.sc2.extension.ValidatorExtension;
import com.nephest.battlenet.sc2.model.BaseTeam;
import jakarta.validation.Validator;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(ValidatorExtension.class)
public class BaseTeamGamesValidatorTest
{

    private Validator validator;

    public static Stream<Arguments> testValidation()
    {
        return Stream.of
        (
            Arguments.of(32764, 2, 1, true),
            Arguments.of(null, null, null, true),
            Arguments.of(null, 32767, null, true),

            Arguments.of(32765, 2, 1, false),
            Arguments.of(null, 32767, 1, false)
        );
    }

    @MethodSource
    @ParameterizedTest
    public void testValidation(Integer wins, Integer losses, Integer ties, boolean valid)
    {
        long violationCount = validator.validate(new BaseTeam(1L, wins, losses, ties, 10)).stream()
            .filter
            (
                violation->violation.getConstraintDescriptor()
                    .getAnnotation()
                    .annotationType()
                    .isAssignableFrom(Games.class)
            )
            .count();
        assertTrue(valid ? violationCount == 0 : violationCount > 0);
    }

}
