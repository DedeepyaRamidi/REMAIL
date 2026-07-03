package com.remail.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.remail.mail.MailIntakeDocument;
import com.remail.mail.MailProcessingService;
import java.time.LocalDateTime;
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

        @Autowired
        private TaskRepository taskRepository;

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

    @Test
    void listsTasksWithFilters() throws Exception {
        String body = """
                ## DataWorks Analytics Engineer Registration
                External

                Inbox

                Name of the Company
                DATAWORKS

                Category
                Analytics Engineer Hiring

                Last date for Registration
                30th June 2026 (5.00 pm)

                Website
                https://dataworks.example.com/

                Job location: Remote

                Designation : Analytics Engineer

                Job Description : Please review the opening

                No Manual Registration & extension will be entertained.
                """;

        mailProcessingService.process(
                new MailIntakeDocument("message-filter-1", "Analytics Engineer Registration", "hr@dataworks.example.com", body));

        mockMvc.perform(get("/api/tasks")
                        .param("company", "DataWorks")
                        .param("query", "Analytics"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].companyName").value("DATAWORKS"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].designation").value("Analytics Engineer"));
    }

    @Test
    void remindLaterBringsTaskBackWhenDue() throws Exception {
        String body = """
                ## Freshers Reminder Registration
                External

                Inbox

                Name of the Company
                REMINDER CO

                Category
                Internship Registration

                Last date for Registration
                15th July 2026 (5.00 pm)

                Website
                https://reminder.example.com/

                Job location: Remote

                Designation : Associate

                No Manual Registration & extension will be entertained.
                """;

        var created = mailProcessingService.process(
                new MailIntakeDocument("message-remind-1", "Freshers Reminder Registration", "hr@reminder.example.com", body));

        assertThat(created).isPresent();

        long taskId = created.orElseThrow().getId();

        mockMvc.perform(post("/api/tasks/{id}/action", taskId)
                        .param("actionType", "REMIND_LATER")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        TaskEntity task = taskRepository.findById(taskId).orElseThrow();
        task.snooze(LocalDateTime.now().minusMinutes(1));
        taskRepository.save(task);

        assertThat(taskService.reactivateDueReminders()).isEqualTo(1);
        assertThat(taskService.findActiveTask()).isPresent();
        }
}