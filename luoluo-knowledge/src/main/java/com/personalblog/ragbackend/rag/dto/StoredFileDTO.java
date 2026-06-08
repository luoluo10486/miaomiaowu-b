package com.personalblog.ragbackend.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stored文件数据传输对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoredFileDTO {

    private String url;

    private String detectedType;

    private Long size;

    private String originalFilename;
}
