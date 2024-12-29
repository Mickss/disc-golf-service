package org.micks.DiscGolfApplication.events;

import lombok.extern.slf4j.Slf4j;
import org.micks.DiscGolfApplication.user.UserDTO;
import org.micks.DiscGolfApplication.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    @Autowired
    private EventRegistrationService eventRegistrationService;

    @Autowired
    private UserService userService;

    @GetMapping
    public List<DiscGolfEventDTO> getEvents(@RequestParam(required = false) String valueToOrderBy,
                                            @RequestParam(required = false) OrderDirection orderDirection) {
        return discGolfEventService.getEvents(valueToOrderBy, orderDirection);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createEvent(@RequestBody DiscGolfEventDTO discGolfEventDTO,
                                            @RequestHeader(value = "Authorization") String token) {
        log.info("Received request for creating new event: {}", discGolfEventDTO);
        if (!userService.isUserAdmin(token)) {
            log.warn("User with token {} is not an admin", token);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        discGolfEventService.createEvent(discGolfEventDTO);
        return ResponseEntity.ok().build();
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

    @PostMapping("/{eventId}/register")
    public void registerUserForEvent(@RequestHeader(value = "Authorization") String authorizationHeader,
                                     @PathVariable String eventId) {
        log.info("Starting registration on event {}", eventId);
        UserDTO userDTO = userService.getLoggedInUser(authorizationHeader);
        eventRegistrationService.registerUserForEvent(userDTO.getUserId(), eventId);
    }
}
