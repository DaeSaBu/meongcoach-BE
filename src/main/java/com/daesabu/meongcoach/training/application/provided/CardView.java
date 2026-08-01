package com.daesabu.meongcoach.training.application.provided;

import java.util.List;

/**
 * 카드 조회 결과. 소속 미디어를 정렬 순서대로 담는다.
 */
public record CardView(Long id, String title, int sortOrder, String instruction, List<CardMediaView> cardMedia) {
}
