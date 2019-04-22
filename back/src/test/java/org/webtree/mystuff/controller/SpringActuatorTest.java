package org.webtree.mystuff.controller;

import org.junit.Test;
import org.webtree.mystuff.security.WithMockCustomUser;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WithMockCustomUser
public class SpringActuatorTest extends AbstractControllerTest {


    @Test
    public void whenGetHealth_shouldReturnUP() throws Exception {
        mockMvc.perform(get("/health").contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.status").value("UP"));
    }
}
