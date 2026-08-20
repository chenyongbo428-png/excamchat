package com.cheat.exam.service;

import com.cheat.exam.common.api.ApiException;
import com.cheat.exam.config.AppProperties;
import com.cheat.exam.domain.image.ImageResource;
import com.cheat.exam.domain.user.User;
import com.cheat.exam.repository.ImageResourceRepository;
import com.cheat.exam.repository.UserRepository;
import com.cheat.exam.security.AuthenticatedUser;
import com.cheat.exam.web.image.dto.ImageResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageService {

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/webp"
    );

    private final ImageResourceRepository imageResourceRepository;
    private final UserRepository userRepository;
    private final Path uploadDir;

    public ImageService(ImageResourceRepository imageResourceRepository, UserRepository userRepository, AppProperties appProperties)
        throws IOException {
        this.imageResourceRepository = imageResourceRepository;
        this.userRepository = userRepository;
        this.uploadDir = Path.of(appProperties.getStorage().getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
    }

    @Transactional
    public ImageResponse upload(AuthenticatedUser authenticatedUser, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException("BAD_REQUEST", "File must not be empty", HttpStatus.BAD_REQUEST);
        }
        String contentType = file.getContentType();
        if (!SUPPORTED_CONTENT_TYPES.contains(contentType)) {
            throw new ApiException("UNSUPPORTED_FILE_TYPE", "Unsupported image type", HttpStatus.BAD_REQUEST);
        }
        User user = userRepository.findById(authenticatedUser.userId())
            .orElseThrow(() -> new ApiException("UNAUTHORIZED", "User not found", HttpStatus.UNAUTHORIZED));

        String extension = resolveExtension(contentType);
        String storageName = UUID.randomUUID() + extension;
        Path target = uploadDir.resolve(storageName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ApiException("INTERNAL_ERROR", "Failed to store file", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ImageResource image = new ImageResource();
        image.setUser(user);
        image.setOriginFileName(file.getOriginalFilename());
        image.setStorageKey(target.toString());
        image.setAccessUrl("/api/images/%s/content".formatted("PENDING"));
        image.setMimeType(contentType);
        image.setFileSize(file.getSize());
        resolveImageDimension(target, image);
        image.setSha256(calculateSha256(target));
        image.setStorageType("LOCAL");
        image.setStatus("ACTIVE");
        ImageResource saved = imageResourceRepository.save(image);
        saved.setAccessUrl("/api/images/%d/content".formatted(saved.getId()));
        return new ImageResponse(
            saved.getId(),
            StringUtils.hasText(saved.getOriginFileName()) ? saved.getOriginFileName() : storageName,
            saved.getMimeType(),
            saved.getFileSize(),
            saved.getWidth(),
            saved.getHeight(),
            saved.getAccessUrl(),
            saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public ImageResponse getMetadata(AuthenticatedUser authenticatedUser, Long imageId) {
        ImageResource image = findOwnedImage(authenticatedUser, imageId);
        return new ImageResponse(
            image.getId(),
            image.getOriginFileName(),
            image.getMimeType(),
            image.getFileSize(),
            image.getWidth(),
            image.getHeight(),
            "/api/images/%d/content".formatted(image.getId()),
            image.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public Resource loadContent(AuthenticatedUser authenticatedUser, Long imageId) {
        ImageResource image = findOwnedImage(authenticatedUser, imageId);
        try {
            return new UrlResource(Path.of(image.getStorageKey()).toUri());
        } catch (IOException ex) {
            throw new ApiException("IMAGE_NOT_FOUND", "Image content not found", HttpStatus.NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> loadContentResponse(AuthenticatedUser authenticatedUser, Long imageId) {
        ImageResource image = findOwnedImage(authenticatedUser, imageId);
        Resource resource = loadContent(authenticatedUser, imageId);
        MediaType mediaType = resolveMediaType(image.getMimeType());
        String fileName = StringUtils.hasText(image.getOriginFileName()) ? image.getOriginFileName() : "image-" + image.getId();
        long contentLength = image.getFileSize();

        return ResponseEntity.ok()
            .contentType(mediaType)
            .contentLength(contentLength)
            .header("X-Content-Type-Options", "nosniff")
            .header("Content-Disposition", ContentDisposition.inline().filename(fileName).build().toString())
            .body(resource);
    }

    private MediaType resolveMediaType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (IllegalArgumentException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private ImageResource findOwnedImage(AuthenticatedUser authenticatedUser, Long imageId) {
        User user = userRepository.findById(authenticatedUser.userId())
            .orElseThrow(() -> new ApiException("UNAUTHORIZED", "User not found", HttpStatus.UNAUTHORIZED));
        return imageResourceRepository.findByIdAndUser(imageId, user)
            .orElseThrow(() -> new ApiException("IMAGE_NOT_FOUND", "Image not found", HttpStatus.NOT_FOUND));
    }

    private String calculateSha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(path);
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException | IOException ex) {
            throw new ApiException("INTERNAL_ERROR", "Failed to hash file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String resolveExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    /**
     * 从图片文件头解析实际像素尺寸，用于 AI 标注坐标的基准对齐。
     * PNG/JPEG 由 ImageIO 支持；WebP 等 ImageIO 不支持的格式返回 null，
     * 调用方回退到 null（前端会改用图片自然尺寸兜底）。
     */
    private void resolveImageDimension(Path target, ImageResource image) {
        try {
            BufferedImage bufferedImage = ImageIO.read(target.toFile());
            if (bufferedImage != null) {
                image.setWidth(bufferedImage.getWidth());
                image.setHeight(bufferedImage.getHeight());
                return;
            }
        } catch (IOException ex) {
            // 忽略，保持 null
        }
        image.setWidth(null);
        image.setHeight(null);
    }
}
