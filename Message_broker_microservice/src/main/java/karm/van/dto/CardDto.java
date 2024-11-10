package karm.van.dto;

import java.io.Serializable;

public record CardDto(String title, String text) implements Serializable {
}
