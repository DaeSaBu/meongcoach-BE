package com.daesabu.meongcoach.alert.domain;

import com.daesabu.meongcoach.alert.domain.shared.AlertField;
import com.daesabu.meongcoach.alert.domain.shared.AlertLevel;
import java.util.List;

public record AlertCommand(AlertLevel level, String title, List<AlertField> fields) {
}
