// Copyright (C) 2020-2025 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model.validation;

import com.nephest.battlenet.sc2.model.BaseTeam;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BaseTeamGamesValidator
implements ConstraintValidator<Games, BaseTeam>
{

    private long maxGames;

    @Override
    public void initialize(Games constraintAnnotation)
    {
        this.maxGames = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(BaseTeam team, ConstraintValidatorContext constraintValidatorContext)
    {
        return (team.getWins() == null ? 0 : team.getWins())
            + (team.getLosses() == null ? 0 : team.getLosses())
            + (team.getTies() == null ? 0 : team.getTies())
            <= maxGames;
    }

}
