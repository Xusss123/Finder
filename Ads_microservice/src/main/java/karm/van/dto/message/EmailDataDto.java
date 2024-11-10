package karm.van.dto.message;

import karm.van.dto.card.CardDto;

public record EmailDataDto(String email, CardDto cardDto) {
}
