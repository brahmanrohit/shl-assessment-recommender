package com.teamtask.service;

import com.teamtask.dto.PageParams;
import com.teamtask.exception.AttachmentNotFoundException;
import com.teamtask.exception.InvalidFileException;
import com.teamtask.model.Attachment;
import com.teamtask.model.Task;
import com.teamtask.model.User;
import com.teamtask.repository.AttachmentRepository;
import com.teamtask.repository.UserRepository;
import com.teamtask.security.CurrentUser;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Files go to S3 (object storage), metadata goes to MySQL. Why the split?
 * Databases are built for small structured rows; blobs bloat backups,
 * replication and memory. Object storage is built for exactly this:
 * cheap, durable, effectively infinite.
 *
 * Downloads use PRESIGNED URLs: the server signs a short-lived link with
 * its credentials and the client fetches the bytes DIRECTLY from S3 -
 * the file never streams through our API (no memory/bandwidth cost here).
 */
@ApplicationScoped
public class AttachmentService {

    public static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    public static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png", "image/jpeg", "application/pdf", "text/plain");

    private final AttachmentRepository attachments;
    private final TaskService tasks;
    private final UserRepository users;
    private final S3Client s3;
    private final S3Presigner presigner;

    @ConfigProperty(name = "app.s3.bucket")
    String bucket;

    @ConfigProperty(name = "app.s3.presign-expiry-minutes")
    long presignExpiryMinutes;

    public AttachmentService(AttachmentRepository attachments, TaskService tasks,
                             UserRepository users, S3Client s3, S3Presigner presigner) {
        this.attachments = attachments;
        this.tasks = tasks;
        this.users = users;
        this.s3 = s3;
        this.presigner = presigner;
    }

    /** Create the bucket on startup if it doesn't exist (idempotent). */
    void onStart(@Observes StartupEvent event) {
        try {
            s3.headBucket(b -> b.bucket(bucket));
        } catch (NoSuchBucketException e) {
            s3.createBucket(b -> b.bucket(bucket));
            Log.infof("Created S3 bucket: %s", bucket);
        }
    }

    @Transactional
    public Attachment upload(Long taskId, FileUpload file, CurrentUser user) {
        Task task = tasks.findAccessible(taskId, user); // 404/403 gate

        // Trust-boundary validation: never store what we didn't agree to.
        if (file == null) {
            throw new InvalidFileException("A multipart field named 'file' is required");
        }
        if (file.size() == 0 || file.size() > MAX_SIZE_BYTES) {
            throw new InvalidFileException("File must be between 1 byte and 5 MB");
        }
        String contentType = file.contentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new InvalidFileException(
                    "Content type '" + contentType + "' not allowed. Allowed: " + String.join(", ", ALLOWED_TYPES));
        }

        // Sanitized original name for display; a UUID key in S3 so names
        // can never collide or traverse paths.
        String safeName = file.fileName() == null ? "file"
                : file.fileName().replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = "task-" + taskId + "/" + UUID.randomUUID() + "-" + safeName;

        s3.putObject(b -> b.bucket(bucket).key(key).contentType(contentType),
                file.uploadedFile()); // streams the temp file to S3

        Attachment attachment = new Attachment();
        attachment.setTask(task);
        attachment.setUploader(users.findById(user.id()));
        attachment.setFileName(safeName);
        attachment.setS3Key(key);
        attachment.setContentType(contentType);
        attachment.setSizeBytes(file.size());
        attachments.persist(attachment);
        return attachment;
    }

    @Transactional
    public PagedResult<Attachment> listForTask(Long taskId, CurrentUser user, PageParams pageParams) {
        tasks.findAccessible(taskId, user); // 404/403 gate
        List<Attachment> content = attachments.queryByTask(taskId)
                .page(Page.of(pageParams.page(), pageParams.size()))
                .list();
        return new PagedResult<>(content, attachments.countByTask(taskId));
    }

    /** Short-lived presigned URL - access is checked via the task's project. */
    @Transactional
    public String presignedUrl(Long attachmentId, CurrentUser user) {
        Attachment attachment = attachments.findById(attachmentId);
        if (attachment == null) {
            throw new AttachmentNotFoundException(attachmentId);
        }
        tasks.findAccessible(attachment.getTask().getId(), user); // 404/403 gate

        return presigner.presignGetObject(p -> p
                        .signatureDuration(Duration.ofMinutes(presignExpiryMinutes))
                        .getObjectRequest(g -> g.bucket(bucket).key(attachment.getS3Key())))
                .url().toString();
    }

    public long presignExpirySeconds() {
        return presignExpiryMinutes * 60;
    }
}
