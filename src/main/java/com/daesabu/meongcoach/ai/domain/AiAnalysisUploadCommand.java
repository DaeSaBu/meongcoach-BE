package com.daesabu.meongcoach.ai.domain;

public record AiAnalysisUploadCommand(Long userId, Long dogId, String videoUrl, Integer videoLengthSec,
                                      Long fileSizeBytes) {
}
