package dev.taskflow.api;

import dev.taskflow.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
public class UploadController {
    private final Path uploadDir;

    public UploadController(@Value("${taskflow.upload.dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostMapping(value = "/api/chat/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestPart("file") MultipartFile file) {
        CurrentUser.require();
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        String original = file.getOriginalFilename() == null ? "file" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String stored = UUID.randomUUID() + extension.replaceAll("[^a-zA-Z0-9.]", "");
        try {
            Files.createDirectories(uploadDir);
            Files.copy(file.getInputStream(), uploadDir.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save upload");
        }
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        String type = contentType.startsWith("image/") ? "IMAGE"
                : contentType.startsWith("video/") ? "VIDEO"
                : contentType.startsWith("audio/") ? "AUDIO" : "FILE";
        return Map.of("url", "/uploads/" + stored, "name", original, "type", type);
    }

    @GetMapping("/uploads/{name:.+}")
    public ResponseEntity<Resource> download(@PathVariable String name) {
        try {
            Path file = uploadDir.resolve(name).normalize();
            if (!file.startsWith(uploadDir) || !Files.exists(file)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            String type = Files.probeContentType(file);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(type == null ? "application/octet-stream" : type))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
                    .body(new UrlResource(file.toUri()));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
