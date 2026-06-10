package com.personalblog.ragbackend.knowledge.handler;

import com.personalblog.ragbackend.knowledge.config.RagDocumentUploadProperties;
import com.personalblog.ragbackend.knowledge.service.document.KnowledgeFileStorageService;
import com.personalblog.ragbackend.rag.dto.StoredFileDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * 远程文件获取器
 */
@Component
public class RemoteFileFetcher {
    private static final long COPY_BUFFER_SIZE = 64 * 1024L;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final KnowledgeFileStorageService fileStorageService;
    private final RagDocumentUploadProperties uploadProperties;

    public RemoteFileFetcher(KnowledgeFileStorageService fileStorageService,
                             RagDocumentUploadProperties uploadProperties) {
        this.fileStorageService = fileStorageService;
        this.uploadProperties = uploadProperties;
    }

    public StoredFileDTO fetchAndStore(String bucketName, String url) {
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("url must not be blank");
        }

        Path tempFile = null;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url.trim()))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException("download remote file failed, status=" + response.statusCode());
            }

            String fileName = resolveFileName(url, response);
            String contentType = response.headers().firstValue("content-type").orElse("application/octet-stream");
            long maxBytes = Math.max(1, uploadProperties.getMaxRemoteDownloadSizeMb()) * 1024L * 1024L;
            long declaredSize = response.headers()
                    .firstValue("content-length")
                    .map(value -> parseLong(value, -1L))
                    .orElse(-1L);
            if (declaredSize > maxBytes) {
                throw new IllegalArgumentException("download remote file too large, maxSizeMb="
                        + uploadProperties.getMaxRemoteDownloadSizeMb());
            }

            tempFile = Files.createTempFile("knowledge-remote-", suffixOf(fileName));
            long bytesCopied;
            try (InputStream inputStream = response.body()) {
                bytesCopied = copyWithLimit(inputStream, tempFile, maxBytes);
            }

            String storedUrl = fileStorageService.store(
                    tempFile,
                    bytesCopied,
                    bucketName,
                    fileName,
                    fileName,
                    contentType
            );
            return StoredFileDTO.builder()
                    .url(storedUrl)
                    .detectedType(contentType)
                    .size(bytesCopied)
                    .originalFilename(fileName)
                    .build();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to fetch remote file", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to fetch remote file", exception);
        } finally {
            deleteQuietly(tempFile);
        }
    }

    private long copyWithLimit(InputStream inputStream, Path tempFile, long maxBytes) throws IOException {
        long copied = 0L;
        byte[] buffer = new byte[(int) COPY_BUFFER_SIZE];
        try (java.io.OutputStream outputStream = Files.newOutputStream(tempFile)) {
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                copied += read;
                if (copied > maxBytes) {
                    throw new IllegalArgumentException("download remote file too large, maxSizeMb="
                            + uploadProperties.getMaxRemoteDownloadSizeMb());
                }
                outputStream.write(buffer, 0, read);
            }
        }
        return copied;
    }

    private String resolveFileName(String url, HttpResponse<?> response) {
        String disposition = response.headers().firstValue("content-disposition").orElse(null);
        if (StringUtils.hasText(disposition)) {
            int index = disposition.indexOf("filename=");
            if (index >= 0) {
                String value = disposition.substring(index + "filename=".length()).replace("\"", "");
                if (StringUtils.hasText(value)) {
                    return value.trim();
                }
            }
        }
        try {
            Path path = Path.of(URI.create(url).getPath());
            Path fileName = path.getFileName();
            if (fileName != null && StringUtils.hasText(fileName.toString())) {
                return fileName.toString();
            }
        } catch (Exception ignored) {
        }
        return "remote-file";
    }

    private String suffixOf(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return ".tmp";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    private long parseLong(String value, long defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }
}
