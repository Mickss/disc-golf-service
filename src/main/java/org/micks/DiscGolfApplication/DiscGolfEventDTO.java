package org.micks.DiscGolfApplication;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DiscGolfEventDTO {

    private String tournamentDate;
    private String league;
    private String pdga;
    private String tournamentTitle;
    private String registration;
    private String state;

}
