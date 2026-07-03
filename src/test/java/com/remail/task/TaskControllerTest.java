package com.remail.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.remail.mail.MailIntakeDocument;
import com.remail.mail.MailProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MailProcessingService mailProcessingService;

    @Autowired
    private TaskService taskService;

    @Test
    void ingestsUrgentMailAndExposesActiveTask() throws Exception {
        String body = """
                ## Clayfin Regular Internship Registration - 2027 Batch
                External

                Inbox

                Name of the Company
                CLAYFIN

                Category
                Regular Internship Registration - 2027 Batch

                Last date for Registration
                24th June 2026 (2.00 pm)

                Website
                https://www.clayfin.com/

                Job location: Chennai

                Designation : Refer JD

                No Manual Registration & extension will be entertained.
                """;

        var created = mailProcessingService.process(
                new MailIntakeDocument("message-1", "Clayfin Regular Internship Registration - 2027 Batch", "students.cdc2027@vitap.ac.in", body));

        assertThat(created).isPresent();

        mockMvc.perform(get("/api/tasks/active"))
                .andExpect(status().isOk());

        long taskId = created.orElseThrow().getId();

        mockMvc.perform(post("/api/tasks/{id}/action", taskId)
                        .param("actionType", "SNOOZE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assertThat(taskService.findActiveTask()).isPresent();
    }
}