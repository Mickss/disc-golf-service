package org.micks.DiscGolfApplication.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;

@RestController
@RequestMapping("/data")
@CrossOrigin
@Slf4j
public class DataController {

    private final DiscGolfDataService discGolfDataService;

    public DataController(DiscGolfDataService discGolfDataService) {
        this.discGolfDataService = discGolfDataService;
    }

    @GetMapping("/export/events")
    public ResponseEntity<StreamingResponseBody> exportEvents(
            @RequestHeader(value = "X-User-Role") String userRoleHeader) throws IOException {
        {

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

            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=events.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(stream);
        }
    }
}
