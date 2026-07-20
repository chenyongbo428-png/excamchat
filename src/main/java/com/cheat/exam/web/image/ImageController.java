package com.cheat.exam.web.image;

import com.cheat.exam.common.api.ApiResponse;
import com.cheat.exam.security.AuthenticatedUser;
import com.cheat.exam.security.SecurityUtils;
import com.cheat.exam.service.ImageService;
import com.cheat.exam.web.image.dto.ImageResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImageResponse> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(imageService.upload(SecurityUtils.currentUser(), file));
    }

    @GetMapping("/{imageId}")
    public ApiResponse<ImageResponse> metadata(@PathVariable Long imageId) {
        return ApiResponse.ok(imageService.getMetadata(SecurityUtils.currentUser(), imageId));
    }

    @GetMapping("/{imageId}/content")
    public ResponseEntity<Resource> content(@PathVariable Long imageId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return imageService.loadContentResponse(user, imageId);
    }

    @DeleteMapping("/{imageId}")
    public ApiResponse<Boolean> delete(@PathVariable Long imageId) {
        return ApiResponse.ok(Boolean.FALSE);
    }
}
