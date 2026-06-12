package com.socialnetwork.auth.application;

/** Datos de entrada del registro, ya validados sintácticamente en la capa web. */
public record RegisterCommand(String username, String email, String password) {}
