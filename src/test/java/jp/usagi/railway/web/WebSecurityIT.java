package jp.usagi.railway.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** 画面 (フォーム POST) のロール制御. 画面上で非表示でもサーバ側で拒否されること. */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class WebSecurityIT {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @Before
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    public void anonymousIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
    }

    @Test
    public void maintainerCannotApproveOutageOrAckAlarm() throws Exception {
        mvc.perform(post("/power/outages/P-26-0041/approve").with(user("kenshu").roles("MAINTAINER")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/power/alarms/5/ack").with(user("kenshu").roles("MAINTAINER")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void dispatcherApprovesOutage() throws Exception {
        mvc.perform(post("/power/outages/P-26-0041/approve").with(user("shirei").roles("DISPATCHER")).with(csrf()))
                .andExpect(redirectedUrl("/power/outages/P-26-0041"));
        mvc.perform(get("/power/outages/new").with(user("shirei").roles("DISPATCHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void makerSeesRollingStockOnly() throws Exception {
        mvc.perform(get("/rolling/failures/F-26-0001").with(user("maker").roles("MAKER")))
                .andExpect(status().isOk()).andExpect(view().name("rolling/failure"));
        mvc.perform(get("/power/substations").with(user("maker").roles("MAKER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/rolling/failures/F-26-0004/repair").with(user("maker").roles("MAKER")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void onlyMakerUpdatesRepairProgress() throws Exception {
        mvc.perform(post("/rolling/repairs/R-26-0001/progress").param("status", "REPAIRED")
                .with(user("kenshu").roles("MAINTAINER")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/rolling/repairs/R-26-0001/progress").param("status", "RETURNED").param("note", "交換完了")
                .with(user("maker").roles("MAKER")).with(csrf()))
                .andExpect(redirectedUrl("/rolling/failures/F-26-0001"));
    }
}
