package org.micks.DiscGolfApplication.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/public")
public class PublicEventsController {

    @Autowired
    private DiscGolfEventService discGolfEventService;

    @GetMapping("/events")
    public List<DiscGolfEventDTO> getEvents(@RequestParam(required = false) String valueToOrderBy,
                                            @RequestParam(required = false) OrderDirection orderDirection) {
        log.info("Public events: {}, {} ", valueToOrderBy, orderDirection);
        return discGolfEventService.getEvents(valueToOrderBy, orderDirection);
    }

    @GetMapping("/events/{eventId}")
    public DiscGolfEventDTO getEvent(@PathVariable String eventId) {
        return discGolfEventService.getEvent(eventId);
    }
}
