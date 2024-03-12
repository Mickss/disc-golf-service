package org.micks.DiscGolfApplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class DiscGolfEventDTO {
    private String id;
    private String tournamentDate;
    private String pdga;
    private String tournamentTitle;
    private String region;
    private String registration;
    private String vacancies;
}
