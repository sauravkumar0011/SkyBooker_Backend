package com.skybooker.seat.scheduler;

import com.skybooker.seat.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeatHoldScheduler {

    private final SeatService seatService;

    @Scheduled(fixedRate = 120000) // every 2 minutes
    public void releaseExpiredHolds() {
        seatService.autoReleaseExpiredHolds();
    }
}