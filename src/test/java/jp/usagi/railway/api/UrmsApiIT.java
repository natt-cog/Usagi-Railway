package jp.usagi.railway.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UrmsApiIT {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @Before
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    public void dispatcherReadsSubstationWithBreakers() throws Exception {
        mvc.perform(get("/api/substations/SS04").with(basic("shirei", "shirei123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("卯月変電所"))
                .andExpect(jsonPath("$.breakers", hasSize(3)))
                .andExpect(jsonPath("$.breakers[1].state").value("TRIPPED"));
    }

    @Test
    public void makerCannotReadPowerData() throws Exception {
        mvc.perform(get("/api/substations").with(basic("maker", "maker123"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/formations/U5103").with(basic("maker", "maker123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inspection.judge").value("X"));
    }

    @Test
    public void rtuPostsTelemetryAndAlarmIsRaised() throws Exception {
        mvc.perform(post("/api/telemetry").with(basic("rtu", "rtu123")).contentType(MediaType.TEXT_PLAIN)
                .content("H20261006\nDSS0108000138004200000580001\nT000001\n"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.alarmsRaised").value(2));
        mvc.perform(post("/api/telemetry").with(basic("kenshu", "kenshu123")).contentType(MediaType.TEXT_PLAIN)
                .content("H20261006\nT000000\n"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void brokenTelemetryIsRejected() throws Exception {
        mvc.perform(post("/api/telemetry").with(basic("rtu", "rtu123")).contentType(MediaType.TEXT_PLAIN)
                .content("H20261006\nDSS0108000138004200000580001\nT000009\n"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void maintainerRegistersFailure() throws Exception {
        mvc.perform(post("/api/failures").with(basic("kenshu", "kenshu123")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"serialNo\":\"US08-0002\",\"symptom\":\"SIV 停止\",\"severity\":\"B\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.failureNo").value(startsWith("F-26-")))
                .andExpect(jsonPath("$.formationNo").value("U5102"));
        mvc.perform(post("/api/failures").with(basic("kenshu", "kenshu123")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"serialNo\":\"US08-0002\",\"symptom\":\"\",\"severity\":\"Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("UR-0001"));
    }

    @Test
    public void batchFilesRequireAdmin() throws Exception {
        mvc.perform(get("/api/batch/daily-report?date=20261005").with(basic("shirei", "shirei123")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/batch/inspection-due").with(basic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(content().string(startsWith("H20261005\nDU3101 ")));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor basic(String u, String p) {
        return SecurityMockMvcRequestPostProcessors.httpBasic(u, p);
    }
}
