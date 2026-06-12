package com.socialnetwork.auth.application.port;

import com.socialnetwork.auth.application.UserSnapshot;

/** Puerto de emisión de access tokens. Adaptador JWT (Nimbus) en {@code infrastructure.security}. */
public interface AccessTokenIssuer {

    String issue(UserSnapshot user);
}
