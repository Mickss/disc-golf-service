package org.micks.DiscGolfApplication.events;

import lombok.extern.slf4j.Slf4j;
import org.micks.DiscGolfApplication.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
                                            @RequestHeader(value = "X-User-Role") String userRoleHeader) {
        log.info("Received request for creating new event: {}", discGolfEventDTO);

        if (userRoleHeader == null || userRoleHeader.isEmpty()) {
            log.warn("X-User-Role header is missing or empty in the request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (!userRoleHeader.equals("ADMIN")) {
            log.warn("User with role {} is not authorized to create an event", userRoleHeader);
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
    public void editEvent(@RequestHeader(value = "X-User-Role") String userRoleHeader,
                          @PathVariable() String eventId,
                          @RequestBody DiscGolfEventDTO discGolfEventDTO) {
        log.info("Received request for editing event for id: {} by user {}. Payload: {}", eventId, userRoleHeader, discGolfEventDTO);

        if (userRoleHeader == null || userRoleHeader.isEmpty()) {
            log.warn("X-User-Role header is missing or empty in the request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Role header is required");
        }
        if (!userRoleHeader.equals("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid role: " + userRoleHeader);
        }
        discGolfEventService.editEvents(eventId, discGolfEventDTO);
    }

    @PostMapping("/{eventId}/register")
    public void registerUserForEvent(@RequestHeader(value = "X-User-Id") String userIdHeader,
                                     @PathVariable String eventId) {
        log.info("Starting registration on event {} for user {}", eventId, userIdHeader);
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            log.warn("X-User-Id header is missing or empty in the request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id header is required");
        }
        eventRegistrationService.registerUserForEvent(userIdHeader, eventId);
    }

    @DeleteMapping("/{eventId}/unregister")
    public void unregisterUserFromEvent(@RequestHeader(value = "X-User-Id") String userIdHeader,
                                        @PathVariable String eventId) {
        log.info("Starting unregistration from event {} for user {}", eventId, userIdHeader);
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            log.warn("X-User-Id header is missing or empty in the request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id header is required");
        }
        eventRegistrationService.unregisterUserFromEvent(userIdHeader, eventId);
    }

    @GetMapping("/my-events")
    public List<DiscGolfEventDTO> getMyEvents(@RequestHeader(value = "X-User-Id") String userIdHeader) {
        log.info("Received X-User-Id header: {}", userIdHeader);
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            log.warn("X-User-Id header is missing or empty in the request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id header is required");
        }
        return eventRegistrationService.getMyEventsWithDetails(userIdHeader);
    }
}
