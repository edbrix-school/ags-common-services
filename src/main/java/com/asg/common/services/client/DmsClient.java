package com.asg.common.services.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@Component
public class DmsClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${dms.base-url}")
    private String dmsBaseUrl;

    /**
     * Uploads a file to DMS.
     * Returns stored key in format: DMS_{documentId}_{documentFileId}
     */
    public String uploadToDms(MultipartFile file, String docId, Long docKeyPoid, String authToken) {
        return uploadToDms(file, docId, docKeyPoid, authToken, null, null, null);
    }

    public String uploadToDms(MultipartFile file, String docId, Long docKeyPoid, String authToken, Long categoryId, String docShortName, String docRef) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set(HttpHeaders.AUTHORIZATION, authToken);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("title", file.getOriginalFilename());
            body.add("doc_id", docId);
            String description;
            if (docShortName != null && docRef != null) description = docShortName + "-" + docRef;
            else if (docShortName != null) description = docShortName;
            else description = docId + "_" + docKeyPoid;
            body.add("description", description);
            if (categoryId != null) body.add("category_id", categoryId.toString());
            body.add("tags", docShortName != null ? docShortName : docId);
            body.add("files", new MultipartFileResource(file));

            log.info("DMS upload payload => title: {}, doc_id: {}, description: {}, tags: {}, category_id: {}",
                    file.getOriginalFilename(), docId, description, body.getFirst("tags"), body.getFirst("category_id"));

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    dmsBaseUrl + "/api/documents", HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
                if (data != null && data.get("document_id") != null) {
                    Long documentId = Long.valueOf(data.get("document_id").toString());
                    return "DMS_" + documentId;
                }
            }
            throw new RuntimeException("DMS upload failed: unexpected response");
        } catch (Exception e) {
            log.error("DMS upload error for file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("DMS upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches document_file_id from DMS using document_id.
     * GET /api/documents/{documentId}
     */
    private Long getDocumentFileId(Long documentId, String authToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                dmsBaseUrl + "/api/documents/" + documentId, HttpMethod.GET, request, Map.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
            if (data != null) {
                java.util.List<?> documentFiles = (java.util.List<?>) data.get("documentFiles");
                if (documentFiles != null && !documentFiles.isEmpty()) {
                    Map<?, ?> firstFile = (Map<?, ?>) documentFiles.get(0);
                    return Long.valueOf(firstFile.get("document_file_id").toString());
                }
            }
        }
        throw new RuntimeException("DMS: could not fetch document_file_id for documentId=" + documentId);
    }

    /**
     * Deletes a document from DMS using document_id.
     * DELETE /api/documents/{documentId}
     */
    public void deleteFromDms(Long documentId, String authToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, authToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    dmsBaseUrl + "/api/documents/" + documentId, HttpMethod.DELETE, request, Map.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("DMS delete failed with status: " + response.getStatusCode());
            }
            log.info("DMS document deleted => documentId: {}", documentId);
        } catch (Exception e) {
            log.error("DMS delete error for documentId {}: {}", documentId, e.getMessage(), e);
            throw new RuntimeException("DMS delete failed: " + e.getMessage(), e);
        }
    }

    /**
     * Downloads file content from DMS using document_id.
     * GET /api/documents/{documentId}/files/{documentFileId}/content
     */
    public Resource downloadFromDms(Long documentId, String fileName, String authToken) {
        try {
            Long documentFileId = getDocumentFileId(documentId, authToken);
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, authToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    dmsBaseUrl + "/api/documents/" + documentId + "/files/" + documentFileId + "/content",
                    HttpMethod.GET, request, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return new ByteArrayResource(response.getBody()) {
                    @Override
                    public String getFilename() {
                        return fileName;
                    }
                };
            }
            throw new RuntimeException("DMS download failed: empty response");
        } catch (Exception e) {
            log.error("DMS download error for documentId {}: {}", documentId, e.getMessage(), e);
            throw new RuntimeException("DMS download failed: " + e.getMessage(), e);
        }
    }

    // Wraps MultipartFile into a Resource that Spring's RestTemplate can send
    private static class MultipartFileResource extends ByteArrayResource {
        private final String filename;

        public MultipartFileResource(MultipartFile file) {
            super(getBytes(file));
            this.filename = file.getOriginalFilename();
        }

        private static byte[] getBytes(MultipartFile file) {
            try {
                return file.getBytes();
            } catch (Exception e) {
                throw new RuntimeException("Failed to read file bytes", e);
            }
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
