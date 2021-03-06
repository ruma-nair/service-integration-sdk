/*
 * Copyright 2017 AppDirect, Inc. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.appdirect.sdk.appmarket.usersync;

import static com.appdirect.sdk.support.ContentOf.resourceAsString;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.http.client.utils.URIBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.appdirect.sdk.exception.UserSyncException;
import com.appdirect.sdk.exception.UserSyncTooManyRequestsException;
import com.appdirect.sdk.web.oauth.RestTemplateFactory;
import com.appdirect.sdk.web.oauth.UserSyncRestTemplateFactoryImpl;
import com.github.tomakehurst.wiremock.junit.WireMockRule;


public class UserSyncApiClientTest {
	private static final String OAUTH_KEY = "testKey";
	private static final String OAUTH_SECRET = "testSecret";
	private static final String USER_SYNC_ENDPOINT = "/api/sync/v1/tasks";
	private static final String HOST = "localhost";
	private RestTemplateFactory restTemplateFactory;
	private UserSyncApiClient userSyncApiClient;
	private String hostUrl;
	private SyncedUser syncedUser;

	@Rule
	public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

	@Before
	public void setUp() throws Exception {
		restTemplateFactory = new UserSyncRestTemplateFactoryImpl();
		userSyncApiClient = new UserSyncApiClient(restTemplateFactory);
		syncedUser = createSyncedUser();
		hostUrl = new URIBuilder().setScheme("http").setHost(HOST).setPort(wireMockRule.port()).build().toString();
	}

	@Test
	public void postUserAssignment_returnsAccepted() throws Exception {
		//Given
		stubFor(post(urlEqualTo(USER_SYNC_ENDPOINT))
				.willReturn(aResponse()
						.withStatus(200)));

		//When
		userSyncApiClient.syncUserAssignment(hostUrl, OAUTH_KEY, OAUTH_SECRET, syncedUser);

		//Then
		verify(exactly(1), postRequestedFor(urlMatching(USER_SYNC_ENDPOINT))
				.withRequestBody(equalToJson(resourceAsString("usersync/usersync-assign.json")))
				.withHeader("Content-Type", matching("application/json;charset=UTF-8")));
	}

	@Test
	public void postUserAssignment_throwsUserApiException() throws Exception {
		//Given
		String responseBody = "{\"code\":\"SUBSCRIPTION_NOT_FOUND\", \"message\":\"Subscription is not found for the ISV\"}";
		stubFor(post(urlEqualTo(USER_SYNC_ENDPOINT))
				.willReturn(aResponse()
						.withStatus(404)
						.withHeader("Content-Type", "application/json")
						.withBody(responseBody)));
		//Then
		assertThatThrownBy(() -> userSyncApiClient.syncUserAssignment(hostUrl, OAUTH_KEY, OAUTH_SECRET, syncedUser))
				.isInstanceOf(UserSyncException.class)
				.hasMessage("Subscription is not found for the ISV")
				.hasFieldOrPropertyWithValue("code", "SUBSCRIPTION_NOT_FOUND");
	}

	@Test
	public void postUserAssignment_throwsUserSyncTooManyRequestsException() throws Exception {
		//Given
		stubFor(post(urlEqualTo(USER_SYNC_ENDPOINT))
				.willReturn(aResponse()
						.withStatus(429)));

		//Then
		assertThatThrownBy(() -> userSyncApiClient.syncUserUnAssignment(hostUrl, OAUTH_KEY, OAUTH_SECRET, syncedUser))
				.isInstanceOf(UserSyncTooManyRequestsException.class);
	}

	@Test
	public void postUserUnAssignment_returnsAccepted() throws Exception {
		//Given
		stubFor(post(urlEqualTo(USER_SYNC_ENDPOINT))
				.willReturn(aResponse()
						.withStatus(200)));


		//When
		userSyncApiClient.syncUserUnAssignment(hostUrl, OAUTH_KEY, OAUTH_SECRET, syncedUser);

		//Then
		verify(exactly(1), postRequestedFor(urlMatching(USER_SYNC_ENDPOINT))
				.withRequestBody(equalToJson(resourceAsString("usersync/usersync-unassign.json")))
				.withHeader("Content-Type", matching("application/json;charset=UTF-8")));
	}

	@Test
	public void postUserUnAssignment_throwsUserApiException() throws Exception {
		//Given
		String responseBody = "{\"code\":\"SUBSCRIPTION_NOT_FOUND\", \"message\":\"Subscription is not found for the ISV\"}";
		stubFor(post(urlEqualTo(USER_SYNC_ENDPOINT))
				.willReturn(aResponse()
						.withStatus(404)
						.withHeader("Content-Type", "application/json")
						.withBody(responseBody)));
		//Then
		assertThatThrownBy(() -> userSyncApiClient.syncUserUnAssignment(hostUrl, OAUTH_KEY, OAUTH_SECRET, syncedUser))
				.isInstanceOf(UserSyncException.class)
				.hasMessage("Subscription is not found for the ISV")
				.hasFieldOrPropertyWithValue("code", "SUBSCRIPTION_NOT_FOUND");
	}

	@Test
	public void postUserUnAssignment_throwsUserSyncTooManyRequestsException() throws Exception {
		//Given
		stubFor(post(urlEqualTo(USER_SYNC_ENDPOINT))
				.willReturn(aResponse()
						.withStatus(429)));
		//Then
		assertThatThrownBy(() -> userSyncApiClient.syncUserUnAssignment(hostUrl, OAUTH_KEY, OAUTH_SECRET, syncedUser))
				.isInstanceOf(UserSyncTooManyRequestsException.class);
	}

	@Test
	public void postUserUnAssignmentInvalidJson_throwsUseSyncException() throws Exception {
		//Given
		String responseBody = "{\"code\":\"SUBSCRIPTION_NOT_FOUND\", \"error\":\"Subscription is not found for the ISV\"}";
		stubFor(post(urlEqualTo(USER_SYNC_ENDPOINT))
				.willReturn(aResponse()
						.withStatus(404)
						.withHeader("Content-Type", "application/json")
						.withBody(responseBody)));
		//Then
		assertThatThrownBy(() -> userSyncApiClient.syncUserUnAssignment(hostUrl, OAUTH_KEY, OAUTH_SECRET, syncedUser))
				.isInstanceOf(UserSyncException.class)
				.hasFieldOrPropertyWithValue("code", "UNKNOWN_ERROR");
	}

	private SyncedUser createSyncedUser() {
		SyncedUser syncedUser = new SyncedUser();
		syncedUser.setDeveloperIdentifier("6b4bd452-895d-4098-aa56-e6046b238e0f");
		syncedUser.setAccountIdentifier("160744112");
		syncedUser.setEmail("tester1@goog-test.junittest.appdirect.co");
		syncedUser.setUserIdentifier("513");
		syncedUser.setFirstName("John");
		syncedUser.setLastName("Doe");
		syncedUser.setUserName("tester1");
		return syncedUser;
	}
}
