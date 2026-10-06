package jp.usagi.railway.api.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;

/** 故障登録 (API / 画面 共通フォーム) */
public class FailureRequest {

    @NotBlank
    @Size(max = 12)
    private String serialNo;

    @NotBlank
    @Size(max = 200)
    private String symptom;

    @Size(max = 8)
    private String failureCode;

    @NotNull
    @Pattern(regexp = "[ABC]")
    private String severity;

    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }
    public String getSymptom() { return symptom; }
    public void setSymptom(String symptom) { this.symptom = symptom; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
}
