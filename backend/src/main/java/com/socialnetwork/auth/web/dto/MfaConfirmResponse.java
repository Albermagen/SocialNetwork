package com.socialnetwork.auth.web.dto;

import java.util.List;

/** Códigos de recuperación, mostrados una única vez tras confirmar el alta de MFA. */
public record MfaConfirmResponse(List<String> recoveryCodes) {}
