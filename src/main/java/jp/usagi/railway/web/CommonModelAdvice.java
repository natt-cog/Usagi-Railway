package jp.usagi.railway.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jp.usagi.railway.service.OperationDateService;

/** 全画面共通のモデル属性 (運用日). */
@ControllerAdvice(basePackages = "jp.usagi.railway.web")
public class CommonModelAdvice {

    private final OperationDateService operationDate;

    public CommonModelAdvice(OperationDateService operationDate) {
        this.operationDate = operationDate;
    }

    @ModelAttribute("operationDate")
    public String operationDate() {
        return operationDate.today().toString("yyyy/MM/dd");
    }
}
