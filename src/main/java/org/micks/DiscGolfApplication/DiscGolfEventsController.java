package org.micks.DiscGolfApplication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/events")
@CrossOrigin
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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public void createEvent(@RequestBody DiscGolfEventDTO discGolfEventDTO) {
        discGolfEventService.createEvent(discGolfEventDTO);
    }

    @GetMapping("/{eventId}")
    public DiscGolfEventDTO getEvent(@PathVariable String eventId) {
        return discGolfEventService.getEvent(eventId);
    }

    @PutMapping(value = "/{eventId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void editEvent(@PathVariable String eventId, @RequestBody DiscGolfEventDTO discGolfEventDTO) {
        discGolfEventService.editEvents(eventId, discGolfEventDTO);
    }
}
