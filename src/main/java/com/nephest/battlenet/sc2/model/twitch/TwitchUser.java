// Copyright (C) 2020-2025 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model.twitch;

import com.nephest.battlenet.sc2.model.twitch.dto.TwitchUserDto;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

public class TwitchUser
{

    @NotNull
    private Long id;

    @NotNull
    private String login;

    @NotNull
    private Boolean subOnlyVod;

    public TwitchUser(){}

    public TwitchUser(Long id, String login, Boolean subOnlyVod)
    {
        this.id = id;
        this.login = login;
        this.subOnlyVod = subOnlyVod;
    }

    public TwitchUser(Long id, String login)
    {
        this(id, login, false);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {return true;}
        if (!(o instanceof TwitchUser that)) {return false;}
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getId());
    }

    @Override
    public String toString()
    {
        return "TwitchUser{" + "id=" + id + '}';
    }

    public static TwitchUser of(TwitchUserDto user)
    {
        return new TwitchUser
        (
            Long.parseLong(user.id()),
            user.login()
        );
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getLogin()
    {
        return login;
    }

    public void setLogin(String login)
    {
        this.login = login;
    }

    public Boolean getSubOnlyVod()
    {
        return subOnlyVod;
    }

    public void setSubOnlyVod(Boolean subOnlyVod)
    {
        this.subOnlyVod = subOnlyVod;
    }

}
