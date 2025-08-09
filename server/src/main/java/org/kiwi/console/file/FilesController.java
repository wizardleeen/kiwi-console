package org.kiwi.console.file;

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

    private final FileService fileService;

    public FilesController(FileService fileService) {
        this.fileService = fileService;
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
        return new UploadResult(fileService.upload(appId, fileName, input));
    }

}
