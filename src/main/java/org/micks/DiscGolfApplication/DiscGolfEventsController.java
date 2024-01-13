package org.micks.DiscGolfApplication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/events")
public class DiscGolfEventsController {

    @Autowired
    private DiscGolfEventService discGolfEventService;

    @GetMapping("/test")
    public String test() {
        return "test application";
    }

    @GetMapping
    public List<DiscGolfEventDTO> getEvents() {
        return discGolfEventService.getEvents();
    }
}
