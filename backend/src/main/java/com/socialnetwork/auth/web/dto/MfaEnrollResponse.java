package com.socialnetwork.auth.web.dto;

import com.socialnetwork.auth.application.MfaEnrollment;

/** Respuesta del alta de MFA: secreto Base32 y URI otpauth para generar el QR en el cliente. */
public record MfaEnrollResponse(String secret, String otpAuthUri) {

    public static MfaEnrollResponse of(MfaEnrollment enrollment) {
        return new MfaEnrollResponse(enrollment.secret(), enrollment.otpAuthUri());
    }
}
