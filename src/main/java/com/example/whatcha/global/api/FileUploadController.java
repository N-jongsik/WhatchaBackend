package com.example.whatcha.global.api;

import com.example.whatcha.global.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequiredArgsConstructor
@RequestMapping("/upload")
@RestController
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping()
    public ResponseEntity<String> uploadFile(@RequestParam("images") MultipartFile multipartFile) throws IOException {

        long fileSize = multipartFile.getSize();

        String uploadedFileUrl = fileUploadService.upload(multipartFile.getInputStream(), multipartFile.getOriginalFilename(), fileSize);

        return new ResponseEntity<>(uploadedFileUrl, HttpStatus.CREATED);
    }
}

