package com.ibm.samplejavaapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@RestController
public class LogController {
    private static final Logger logger = LogManager.getLogger(LogController.class);

    @GetMapping(value = "/generatelogline")
    public String generateLog(@RequestParam(required = false) String level) {

        if (level == null) {
            logger.debug("[GET] /generatelogline endpoint called with log debug info");
            return "[GET] /generatelogline endpoint called with log debug info";
        } else if (level.equals("info")) {
            logger.info("[GET] /generatelogline endpoint called with log level info");
        } else if (level.equals("error")) {
            logger.error("[GET] /generatelogline endpoint called with log level error");
        } else if (level.equals("warn")) {
            logger.warn("[GET] /generatelogline endpoint called with log level warn");
        } else if (level.equals("fatal")) {
            logger.fatal("[GET] /generatelogline endpoint called with log level fatal");
        }

        return String.format("[GET] /generatelogline endpoint called with log level %s", level);
    }
}
