package com.daesabu.meongcoach.support.application.provided;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

final class ChatMessageTimeComparator implements Comparator<LocalDateTime> {
    static final ChatMessageTimeComparator INSTANCE = new ChatMessageTimeComparator();

    @Override
    public int compare(LocalDateTime first, LocalDateTime second) {
        if (Math.abs(ChronoUnit.NANOS.between(first, second)) <= 1000) {
            return 0;
        }
        return first.compareTo(second);
    }
}
