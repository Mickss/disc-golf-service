package org.micks.DiscGolfApplication;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/events")
@CrossOrigin
@Slf4j
public class DiscGolfEventsController {

    @Autowired
    private DiscGolfEventService discGolfEventService;

    @GetMapping
    public List<DiscGolfEventDTO> getEvents(@RequestParam(required = false) String valueToOrderBy,
                                            @RequestParam(required = false) OrderDirection orderDirection) {
        return discGolfEventService.getEvents(valueToOrderBy, orderDirection);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public void createEvent(@RequestBody DiscGolfEventDTO discGolfEventDTO) {
        log.info("Received request for creating new event: {}", discGolfEventDTO);
        discGolfEventService.createEvent(discGolfEventDTO);
    }

    @GetMapping("/{eventId}")
    public DiscGolfEventDTO getEvent(@PathVariable String eventId) {
        return discGolfEventService.getEvent(eventId);
    }

    @PutMapping(value = "/{eventId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void editEvent(@PathVariable String eventId, @RequestBody DiscGolfEventDTO discGolfEventDTO) {
        log.info("Received request for editing event for id: {}. Payload: {}", eventId, discGolfEventDTO);
        discGolfEventService.editEvents(eventId, discGolfEventDTO);
    }
}
