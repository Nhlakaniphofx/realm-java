/*
 * Copyright 2016 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.realm.annotations.Beta;

/**
 * @Beta
 * Credentials represent a login with a 3rd party login provider in an OAuth2 login flow, and are used by the Realm
 * Object Server to verify the user and grant access.
 * <p>
 * Logging into the Realm Object Server consists of the following steps:
 * <ol>
 * <li>
 *     Log in to 3rd party provider (Facebook or Google). The result is usually an Authorization Grant that must be
 *     saved in a {@link SyncCredentials} object of the proper type e.g., {@link SyncCredentials#facebook(String)} for a
 *     Facebook login.
 * </li>
 * <li>
 *     Authenticate a {@link SyncUser} through the Object Server using these credentials. Once authenticated,
 *     an Object Server user is returned. Then this user can be attached to a {@link SyncConfiguration}, which
 *     will make it possible to synchronize data between the local and remote Realm.
 *     <p>
 *     It is possible to persist the user object e.g., using the {@link UserStore}. That means, logging
 *     into an OAuth2 provider is only required the first time the app is used.
 * </li>
 * </ol>
 *
 * <pre>
 * {@code
 * // Example
 *
 * Credentials credentials = Credentials.facebook(getFacebookToken());
 * User.login(credentials, "http://objectserver.realm.io/auth", new User.Callback() {
 *     \@Override
 *     public void onSuccess(User user) {
 *          // User is now authenticated and be be used to open Realms.
 *     }
 *
 *     \@Override
 *     public void onError(ObjectServerError error) {
 *
 *     }
 * });
 * }
 * </pre>
 */
@Beta
public class SyncCredentials {

    private final String userIdentifier;
    private final String identityProvider;
    private final Map<String, Object> userInfo;

    // Factory constructors

    /**
     * Creates credentials based on a Facebook login.
     *
     * @param facebookToken a facebook userIdentifier acquired by logging into Facebook.
     * @return a set of credentials that can be used to log into the Object Server using
     *         {@link SyncUser#loginAsync(SyncCredentials, String, SyncUser.Callback)}.
     * @throws IllegalArgumentException if user name is either {@code null} or empty.
     */
    public static SyncCredentials facebook(String facebookToken) {
        assertStringNotEmpty(facebookToken, "facebookToken");
        return new SyncCredentials(facebookToken, IdentityProvider.FACEBOOK, null);
    }

    /**
     * Creates credentials based on a Google login.
     *
     * @param googleToken a google userIdentifier acquired by logging into Google.
     * @return a set of credentials that can be used to log into the Object Server using
     *         {@link SyncUser#loginAsync(SyncCredentials, String, SyncUser.Callback)}.
     * @throws IllegalArgumentException if user name is either {@code null} or empty.
     */
    public static SyncCredentials google(String googleToken) {
        assertStringNotEmpty(googleToken, "googleToken");
        return new SyncCredentials(googleToken, IdentityProvider.GOOGLE, null);
    }

    /**
     * Creates credentials based on a login with username and password. These credentials will only be verified
     * by the Object Server.
     *
     * @param username username of the user.
     * @param password the users password.
     * @param createUser {@code true} if the user should be created, {@code false} otherwise. It is not possible to
     *                   create a user twice when logging in, so this flag should only be set to {@code true} the first
     *                   time a users log in.
     * @return a set of credentials that can be used to log into the Object Server using
     *         {@link SyncUser#loginAsync(SyncCredentials, String, SyncUser.Callback)}.
     * @throws IllegalArgumentException if user name is either {@code null} or empty.
     */
    public static SyncCredentials usernamePassword(String username, String password, boolean createUser) {
        assertStringNotEmpty(username, "username");
        Map<String, Object> userInfo = new HashMap<String, Object>();
        userInfo.put("register", createUser);
        userInfo.put("password", password);
        return new SyncCredentials(username, IdentityProvider.USERNAME_PASSWORD, userInfo);
    }

    /**
     * Creates credentials based on a login with username and password. These credentials will only be verified
     * by the Object Server.  The user is not created if she does not exist.
     *
     * @param username username of the user.
     * @param password the users password.
     * @return a set of credentials that can be used to log into the Object Server using
     *         {@link SyncUser#loginAsync(SyncCredentials, String, SyncUser.Callback)}.
     * @throws IllegalArgumentException if user name is either {@code null} or empty.
     */
    public static SyncCredentials usernamePassword(String username, String password) {
        return usernamePassword(username, password, false);
    }

    /**
     * Creates a custom set of credentials. The behaviour will depend on the type of {@code identityProvider} and
     * {@code userInfo} used.
     *
     * @param userIdentifier String identifying the user. Usually a username of userIdentifier.
     * @param identityProvider provider used to verify the credentials.
     * @param userInfo data describing the user further or {@code null} if the user does not have any extra data. The
     *              data will be serialized to JSON, so all values must be mappable to a valid JSON data type. Custom
     *              classes will be converted using {@code toString()}.
     * @return a set of credentials that can be used to log into the Object Server using
     *         {@link SyncUser#loginAsync(SyncCredentials, String, SyncUser.Callback)}.
     * @throws IllegalArgumentException if any parameter is either {@code null} or empty.
     */
    public static SyncCredentials custom(String userIdentifier, String identityProvider, Map<String, Object> userInfo) {
        assertStringNotEmpty(userIdentifier, "userIdentifier");
        assertStringNotEmpty(identityProvider, "identityProvider");
        if (userInfo == null) {
            userInfo = new HashMap<String, Object>();
        }
        return new SyncCredentials(userIdentifier, identityProvider, userInfo);
    }

    private static void assertStringNotEmpty(String string, String message) {
        if (string == null || "".equals(string)) {
            throw new IllegalArgumentException("Non-null '" + message + "' required.");
        }
    }

    private SyncCredentials(String token, String identityProvider, Map<String, Object> userInfo) {
        this.identityProvider = identityProvider;
        this.userIdentifier = token;
        this.userInfo = (userInfo == null) ? new HashMap<String, Object>() : userInfo;
    }

    /**
     * Returns the provider used by the Object Server to validate these credentials.
     *
     * @return the login type.
     */
    public String getIdentityProvider() {
        return identityProvider;
    }

    /**
     * Returns a String that identifies the user. The value will depend on the type of {@link IdentityProvider} used.
     *
     * @return a String identifying the user.
     */
    public String getUserIdentifier() {
        return userIdentifier;
    }

    /**
     * Returns any custom user information associated with this credential.
     * The type of information will depend on the type of {@link SyncCredentials.IdentityProvider}
     * used.
     *
     * @return a map of additional information about the user.
     */
    public Map<String, Object> getUserInfo() {
        return Collections.unmodifiableMap(userInfo);
    }

    /**
     * Enumeration of the different types of identity providers. An identity provider is the entity responsible for
     * verifying that a given credential is valid.
     */
    public static final class IdentityProvider {
        /**
         * Any credentials verified by the debug identity provider will always be considered valid.
         * It is only available if configured on the Object Server, and it is disabled by default.
         */
        public static final String DEBUG = "debug";

        /**
         * Credentials will be verified by Facebook.
         */
        public static final String FACEBOOK = "facebook";

        /**
         * Credentials will be verified by Google.
         */
        public static final String GOOGLE = "google";

        /**
         * Credentials will be verified by the Object Server.
         *
         * @see #usernamePassword(String, String, boolean)
         */
        public static final String USERNAME_PASSWORD = "password";
    }
}
