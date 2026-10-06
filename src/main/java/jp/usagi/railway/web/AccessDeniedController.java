package jp.usagi.railway.web;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/** 権限不足 (403) 画面. Spring Security の accessDeniedPage からフォワードされる. */
@Controller
public class AccessDeniedController {

    private static final Logger log = LoggerFactory.getLogger(AccessDeniedController.class);

    @RequestMapping("/denied")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView denied(HttpServletRequest request) {
        log.warn("403 {} user={}", request.getAttribute("javax.servlet.forward.request_uri"), request.getRemoteUser());
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("status", 403);
        return mav;
    }
}
