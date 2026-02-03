package org.micks.DiscGolfApplication.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Date;

@Getter
@AllArgsConstructor
@ToString
public class DiscGolfEventDTO {
    private String id;
    private Date tournamentDateStart;
    private Date tournamentDateEnd;
    private Date registrationStart;
    private Date registrationEnd;
    private String pdga;
    private String tournamentTitle;
    private String region;
    private String externalLink;
    private String tournamentDirector;
    private Integer capacity;
}
