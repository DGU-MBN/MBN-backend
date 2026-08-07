package com.hackathon.MBN.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AiLocalizerTest {

    @Test
    void parseLocalizedContentMapsTitleAndBody() {
        String json = """
                {
                  "title": "Fire breaks out at officetel in Gangnam",
                  "body": "A fire broke out on the third floor of an officetel in Yeoksam-dong, Gangnam. Firefighters are working to put it out. No injuries have been reported so far."
                }
                """;

        LocalizedContent content = AiLocalizer.parseLocalizedContent(json);

        assertThat(content.title()).isEqualTo("Fire breaks out at officetel in Gangnam");
        assertThat(content.body()).contains("Yeoksam-dong");
    }

    @Test
    void parseLocalizedContentThrowsWhenTitleMissing() {
        assertThatThrownBy(() -> AiLocalizer.parseLocalizedContent("{\"body\": \"no title\"}"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseLocalizedContentThrowsWhenNotJson() {
        assertThatThrownBy(() -> AiLocalizer.parseLocalizedContent("not json"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
