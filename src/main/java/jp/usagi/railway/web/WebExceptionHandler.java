package jp.usagi.railway.web;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import jp.usagi.railway.service.NotFoundException;

@ControllerAdvice(basePackages = "jp.usagi.railway.web")
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView notFound(HttpServletRequest request, RuntimeException e) {
        log.warn("404 {} : {}", request.getRequestURI(), e.getMessage());
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("status", 404);
        mav.addObject("message", e.getMessage());
        return mav;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView badRequest(HttpServletRequest request, RuntimeException e) {
        log.warn("400 {} : {}", request.getRequestURI(), e.getMessage());
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("status", 400);
        mav.addObject("message", "入力内容に誤りがあります");
        return mav;
    }
}
