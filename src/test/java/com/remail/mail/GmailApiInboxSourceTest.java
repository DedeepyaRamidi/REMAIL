package com.remail.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class GmailApiInboxSourceTest {

    @Test
    void pollsRecentMessagesFromGmailApi() {
        GmailProperties properties = enabledProperties();
        RestTemplate restTemplate = new RestTemplateBuilder().build();
      MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

      server.expect(once(), requestTo("https://gmail.googleapis.com/gmail/v1/users/me/messages?labelIds=INBOX&q=in:inbox%20newer_than:7d&maxResults=25"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(request -> assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-token"))
        .andRespond(withSuccess("""
                {
                  "messages": [{"id": "msg-1", "threadId": "thread-1"}]
                }
                """, APPLICATION_JSON));

      server.expect(once(), requestTo("https://gmail.googleapis.com/gmail/v1/users/me/messages/msg-1?format=full"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(request -> assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-token"))
        .andRespond(withSuccess("""
                {
                  "id": "msg-1",
                  "snippet": "Action required",
                  "payload": {
                    "mimeType": "multipart/alternative",
                    "headers": [
                      {"name": "Subject", "value": "Action required"},
                      {"name": "From", "value": "jobs@example.com"}
                    ],
                    "parts": [
                      {"mimeType": "text/plain", "body": {"data": "QWN0aW9uIHJlcXVpcmVk"}},
                      {"mimeType": "text/html", "body": {"data": "PGgxPkFjdGlvbiByZXF1aXJlZDwvaDE+"}}
                    ]
                  }
                }
                """, APPLICATION_JSON));

        GmailApiInboxSource source = new GmailApiInboxSource(properties, restTemplate);

        assertThat(source.pollRecentMessages())
                .hasSize(1)
                .first()
                .satisfies(document -> {
                    assertThat(document.messageId()).isEqualTo("msg-1");
                    assertThat(document.subject()).isEqualTo("Action required");
                    assertThat(document.from()).isEqualTo("jobs@example.com");
                    assertThat(document.body()).isEqualTo("Action required");
                });

        server.verify();
    }

    @Test
    void fetchChangedMessagesUsesHistoryIdAndMailboxAddress() {
        GmailProperties properties = enabledProperties();
        RestTemplate restTemplate = new RestTemplateBuilder().build();
      MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

      server.expect(once(), requestTo("https://gmail.googleapis.com/gmail/v1/users/student@example.com/history?startHistoryId=12345&historyTypes=messageAdded&labelId=INBOX&maxResults=100"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(request -> assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-token"))
        .andRespond(withSuccess("""
                {
                  "history": [{
                    "messagesAdded": [{"message": {"id": "msg-2", "threadId": "thread-2"}}]
                  }]
                }
                """, APPLICATION_JSON));

      server.expect(once(), requestTo("https://gmail.googleapis.com/gmail/v1/users/student@example.com/messages/msg-2?format=full"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(request -> assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-token"))
        .andRespond(withSuccess("""
                {
                  "id": "msg-2",
                  "payload": {
                    "headers": [
                      {"name": "Subject", "value": "Refresh needed"},
                      {"name": "From", "value": "alerts@example.com"}
                    ],
                    "body": {"data": "UmVmcmVzaCBuZWVkZWQ="}
                  }
                }
                """, APPLICATION_JSON));

        GmailApiInboxSource source = new GmailApiInboxSource(properties, restTemplate);

        assertThat(source.fetchChangedMessages("student@example.com", "12345"))
                .hasSize(1)
                .first()
                .satisfies(document -> {
                    assertThat(document.messageId()).isEqualTo("msg-2");
                    assertThat(document.subject()).isEqualTo("Refresh needed");
                    assertThat(document.from()).isEqualTo("alerts@example.com");
                    assertThat(document.body()).isEqualTo("Refresh needed");
                });

        server.verify();
    }

    private GmailProperties enabledProperties() {
        GmailProperties properties = new GmailProperties();
        properties.setEnabled(true);
        properties.setUserId("me");
        properties.setQuery("in:inbox newer_than:7d");
        properties.setAccessToken("test-token");
        return properties;
    }
}