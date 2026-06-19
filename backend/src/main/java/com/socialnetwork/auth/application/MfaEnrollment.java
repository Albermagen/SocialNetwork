package com.socialnetwork.auth.application;

/** Datos de enrolamiento devueltos al iniciar el alta de MFA: secreto y URI para el QR. */
public record MfaEnrollment(String secret, String otpAuthUri) {}
