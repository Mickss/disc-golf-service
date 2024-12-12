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
    private Date tournamentDate;
    private String pdga;
    private String tournamentTitle;
    private String region;
    private String registration;
    private String vacancies;
}
