package karm.van.dto;

import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.util.List;

public record EmailDataDto(String email, CardDto cardDto) implements Serializable {
}
