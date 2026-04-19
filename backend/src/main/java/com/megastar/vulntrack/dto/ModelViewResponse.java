package com.megastar.vulntrack.dto;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

public record ModelViewResponse(
        String msg,
        String status,
        int code,
        String view) {

    public static ModelAndView res(
            String msg,
            HttpStatus status,
            String viewName) {

        HttpStatus resolvedStatus = (status != null) ? status : HttpStatus.BAD_REQUEST;

        ModelAndView mav = new ModelAndView(viewName != null ? viewName : "issues");

        mav.addObject("errorMessage", msg);
        mav.addObject("statusName", resolvedStatus.name());
        mav.addObject("statusCode", resolvedStatus.value());

        mav.setStatus(resolvedStatus);

        return mav;
    }
}
