package com.dongchedi.car.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.dongchedi.car.dto.MerchantCarView;
import com.dongchedi.car.entity.CarEntity;
import com.dongchedi.car.exception.BizException;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.repository.jpa.CarJpaRepository;

/**
 * 车源图片上传：商家给自己名下的车源上传实拍图，保存到本地 uploads 目录并通过 /uploads/** 访问。
 */
@Service
public class CarPhotoService {

    /** 允许的图片类型 */
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    /** 单个文件最大 5MB */
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    /** 上传文件对外访问前缀 */
    private static final String PUBLIC_PREFIX = "/uploads/cars/";

    private final CarJpaRepository carJpa;
    private final AuditService auditService;
    private final Path carsDir;

    public CarPhotoService(CarJpaRepository carJpa, AuditService auditService,
                           @Value("${dongchedi.upload.dir:./uploads}") String uploadDir) {
        this.carJpa = carJpa;
        this.auditService = auditService;
        this.carsDir = Path.of(uploadDir, "cars").toAbsolutePath().normalize();
    }

    @Transactional
    public MerchantCarView upload(UserAccount operator, Long carId, MultipartFile file) throws IOException {
        CarEntity entity = requireOwned(operator, carId);
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("请选择要上传的图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw BizException.badRequest("图片不能超过 5MB");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw BizException.badRequest("只支持 JPG / PNG / WebP 格式的图片");
        }

        Files.createDirectories(carsDir);
        String extension = extensionOf(contentType, file.getOriginalFilename());
        String filename = "car-" + carId + "-" + System.currentTimeMillis() + extension;
        Path target = carsDir.resolve(filename).normalize();
        if (!target.startsWith(carsDir)) {
            throw BizException.badRequest("非法的文件路径");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        deleteOldFile(entity.getPhotoUrl());
        entity.setPhotoUrl(PUBLIC_PREFIX + filename);
        entity.touch();
        CarEntity saved = carJpa.save(entity);
        auditService.record(operator, "上传车源图片", "CAR", carId, saved.getName() + " → " + filename);
        return MerchantCarView.from(saved);
    }

    @Transactional
    public MerchantCarView remove(UserAccount operator, Long carId) {
        CarEntity entity = requireOwned(operator, carId);
        deleteOldFile(entity.getPhotoUrl());
        entity.setPhotoUrl(null);
        entity.touch();
        CarEntity saved = carJpa.save(entity);
        auditService.record(operator, "删除车源图片", "CAR", carId, saved.getName());
        return MerchantCarView.from(saved);
    }

    private CarEntity requireOwned(UserAccount user, Long carId) {
        CarEntity entity = carJpa.findById(carId)
                .orElseThrow(() -> BizException.notFound("车源不存在：" + carId));
        if (!user.isAdmin() && !user.id().equals(entity.getDealerId())) {
            throw new BizException(HttpStatus.FORBIDDEN, "只能管理自己名下的车源");
        }
        return entity;
    }

    private void deleteOldFile(String photoUrl) {
        if (photoUrl == null || !photoUrl.startsWith(PUBLIC_PREFIX)) {
            return;
        }
        try {
            Path old = carsDir.resolve(photoUrl.substring(PUBLIC_PREFIX.length())).normalize();
            if (old.startsWith(carsDir)) {
                Files.deleteIfExists(old);
            }
        } catch (IOException ignored) {
            /* 旧文件删除失败不影响新图片保存 */
        }
    }

    private String extensionOf(String contentType, String originalName) {
        if (originalName != null && originalName.contains(".")) {
            String ext = originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (Set.of(".jpg", ".jpeg", ".png", ".webp").contains(ext)) {
                return ext;
            }
        }
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
