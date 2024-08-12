package com.benection.androidstudy;

public record UuidLoginResponse(ResponseStatus responseStatus, Integer deviceId, Account account) {
}
