package org.micks.DiscGolfApplication.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/events")
@CrossOrigin
@Slf4j
public class DiscGolfEventsController {

    private final DiscGolfDataService discGolfDataService;
    private final DiscGolfEventService discGolfEventService;
    private final EventRegistrationService eventRegistrationService;

    public DiscGolfEventsController(DiscGolfDataService discGolfDataService,
                                    DiscGolfEventService discGolfEventService,
                                    EventRegistrationService eventRegistrationService
    ) {
        this.discGolfDataService = discGolfDataService;
        this.discGolfEventService = discGolfEventService;
        this.eventRegistrationService = eventRegistrationService;
    }

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

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@RequestHeader(value = "X-User-Role") String userRoleHeader,
                                            @PathVariable String eventId) {
        log.info("Received request to delete event: {} by user role: {}", eventId, userRoleHeader);

        if (userRoleHeader == null || userRoleHeader.isEmpty()) {
            log.warn("X-User-Role header is missing or empty in the request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Role header is required");
        }
        if (!userRoleHeader.equals("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }

        discGolfEventService.deleteEvent(eventId);
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

    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> exportEvents(
            @RequestHeader(value = "X-User-Role") String userRoleHeader) {

        log.info("Export events requested by user with role: {}", userRoleHeader);

        if (userRoleHeader == null || userRoleHeader.isEmpty()) {
            log.warn("X-User-Role header is missing or empty in the request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (!userRoleHeader.equals("ADMIN")) {
            log.warn("User with role {} is not authorized to export events", userRoleHeader);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        byte[] excelFile = discGolfDataService.generateEventsExcel();
        StreamingResponseBody stream = out -> {
            out.write(excelFile);
            out.flush();
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=events.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(stream);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> importEvents(
            @RequestHeader(value = "X-User-Role") String userRoleHeader,
            @RequestParam("file") MultipartFile file) {

        log.info("Import events requested by user with role: {}", userRoleHeader);

        if (userRoleHeader == null || userRoleHeader.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (!userRoleHeader.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try (InputStream is = file.getInputStream()) {
            discGolfDataService.importEventsExcel(is);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            log.error("Failed to import events", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
