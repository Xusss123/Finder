package karm.van.dto;

import java.io.Serializable;

public record EmailDataDto(String email, CardDto cardDto) implements Serializable {
}
