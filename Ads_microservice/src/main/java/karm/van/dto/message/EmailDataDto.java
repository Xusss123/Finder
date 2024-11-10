package karm.van.dto.message;

import karm.van.dto.card.CardDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record EmailDataDto(String email, CardDto cardDto) {
}
