package org.micks.DiscGolfApplication;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DiscGolfEventService {

    public List<DiscGolfEventDTO> getEvents() {
        return List.of(
                new DiscGolfEventDTO("05/03/2024", "159", "-", "Fire Disk", "Phase 1", "22 / 22"),
                new DiscGolfEventDTO("15/06/2024", "237", "-", "Spin Around", "Phase 1", "0 / 30"),
                new DiscGolfEventDTO("04/08/2024", "262", "-", "Golf Basket", "Phase 1", "33 / 45"),
                new DiscGolfEventDTO("09/10/2024", "305", "-", "Flaying plates", "Phase 1", "20 / 25"),
                new DiscGolfEventDTO("11/11/2024", "356", "-", "Team Disk", "Phase 1", "22 / 26")
        );
    }

    public void createEvent(DiscGolfEventDTO discGolfEventDTO) {
        log.info("Creating new event: {}", discGolfEventDTO.getTournamentTitle());
    }
}
