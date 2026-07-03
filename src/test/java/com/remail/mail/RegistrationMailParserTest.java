package com.remail.mail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegistrationMailParserTest {

    private final RegistrationMailParser parser = new RegistrationMailParser();

    @Test
    void parsesClayfinStyleRegistrationMail() {
        String body = """
                ## Clayfin Regular Internship Registration - 2027 Batch
                External

                Inbox

                ### 'No Reply CDC Info' via 2027 CDC <students.cdc2027@vitap.ac.in>

                **Regular  Internship Registration - 2027 Batch**

                Name of the Company
                CLAYFIN

                Category
                Regular Internship Registration - 2027 Batch

                Date of Visit:
                Will be announced later

                Eligible Branches
                M. Tech  5 year (CSE / IT ) related branches
                MCA

                CTC
                4 LPA

                Stipend
                20,000

                Last date for Registration
                24th June 2026 (2.00 pm)

                Website
                https://www.clayfin.com/

                Job location: Chennai

                Designation : Refer JD
                Job Description : Below attached

                No Manual Registration & extension will be entertained.
                """;

        RegistrationMailSnapshot snapshot = parser.parse(
            new MailIntakeDocument("message-1", "Clayfin Regular Internship Registration - 2027 Batch", "students.cdc2027@vitap.ac.in", body));

        assertThat(snapshot.title()).isEqualTo("Clayfin Regular Internship Registration - 2027 Batch");
        assertThat(snapshot.companyName()).isEqualTo("CLAYFIN");
        assertThat(snapshot.category()).isEqualTo("Regular Internship Registration - 2027 Batch");
        assertThat(snapshot.registrationDeadlineText()).isEqualTo("24th June 2026 (2.00 pm)");
        assertThat(snapshot.location()).isEqualTo("Chennai");
        assertThat(snapshot.website()).isEqualTo("https://www.clayfin.com/");
        assertThat(snapshot.designation()).isEqualTo("Refer JD");
        assertThat(snapshot.urgencyDetected()).isTrue();
    }
}