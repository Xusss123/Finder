package karm.van.dto;

import karm.van.model.CardModel;

import java.util.List;

public record CardPageResponseDto(List<CardModel> cards,
                                  boolean last,
                                  int totalPages,
                                  long totalElements,
                                  boolean first,
                                  int numberOfElements) {
}
