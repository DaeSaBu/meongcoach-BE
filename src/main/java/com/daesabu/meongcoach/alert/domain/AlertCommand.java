package com.daesabu.meongcoach.alert.domain;

import com.daesabu.meongcoach.alert.domain.shared.AlertField;
import java.util.List;

public record AlertCommand(String title, List<AlertField> fields) {
}
