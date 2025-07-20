package org.kiwi.console.upload;

import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
public class FilesController {

    private final UploadService uploadService;

    public FilesController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @SneakyThrows
    @PostMapping
    public UploadResult upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        var appIdHeader = request.getHeader("X-App-ID");
        if (appIdHeader == null)
            throw new IllegalArgumentException("X-App-ID header is required");
        var appId = Long.parseLong(appIdHeader);
        var fileName = file.getOriginalFilename();
        var input = file.getInputStream();
        return new UploadResult(uploadService.upload(appId, fileName, input));
    }

}
