package com.benection.androidstudy;

public record SocialLoginResponse(ResponseStatus responseStatus, Account account, Token token) {
}
