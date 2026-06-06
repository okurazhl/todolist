package com.smartmemo.memo.application;

import com.smartmemo.memo.domain.MemoAttachment;
import com.smartmemo.memo.infrastructure.persistence.MemoAttachmentRepository;
import com.smartmemo.memo.infrastructure.storage.MinioStorageAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AttachmentApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentApplicationService.class);

    private final MemoAttachmentRepository attachmentRepository;
    private final MinioStorageAdapter storageAdapter;

    public AttachmentApplicationService(MemoAttachmentRepository attachmentRepository, MinioStorageAdapter storageAdapter) {
        this.attachmentRepository = attachmentRepository;
        this.storageAdapter = storageAdapter;
    }

    @Transactional
    public AttachmentResult upload(UUID memoId, UUID userId, String fileName, long fileSize,
                                    String contentType, InputStream data) {
        String objectKey = storageAdapter.upload(userId, memoId, fileName, data, fileSize, contentType);

        MemoAttachment att = new MemoAttachment();
        att.setMemoId(memoId);
        att.setUserId(userId);
        att.setFileName(fileName);
        att.setFileSize(fileSize);
        att.setContentType(contentType);
        att.setObjectKey(objectKey);

        MemoAttachment saved = attachmentRepository.save(att);
        log.info("Attachment uploaded: id={}, memoId={}, fileName={}", saved.getId(), memoId, fileName);
        return AttachmentResult.from(saved);
    }

    public List<AttachmentResult> listByMemo(UUID memoId) {
        return attachmentRepository.findByMemoId(memoId).stream()
                .map(AttachmentResult::from)
                .toList();
    }

    public Optional<String> getDownloadUrl(UUID attachmentId, UUID userId) {
        return attachmentRepository.findByIdAndUserId(attachmentId, userId)
                .map(att -> storageAdapter.presignedDownloadUrl(att.getObjectKey()));
    }

    @Transactional
    public boolean delete(UUID attachmentId, UUID userId) {
        Optional<MemoAttachment> opt = attachmentRepository.findByIdAndUserId(attachmentId, userId);
        if (opt.isEmpty()) return false;

        MemoAttachment att = opt.get();
        storageAdapter.delete(att.getObjectKey());
        att.setDeletedAt(Instant.now());
        attachmentRepository.save(att);
        return true;
    }

    public record AttachmentResult(UUID id, UUID memoId, String fileName, long fileSize,
                                    String contentType, Instant createdAt) {
        public static AttachmentResult from(MemoAttachment a) {
            return new AttachmentResult(a.getId(), a.getMemoId(), a.getFileName(),
                    a.getFileSize(), a.getContentType(), a.getCreatedAt());
        }
    }
}
