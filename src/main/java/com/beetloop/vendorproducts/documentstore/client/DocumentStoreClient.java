package com.beetloop.vendorproducts.documentstore.client;

import com.beetloop.vendorproducts.documentstore.dto.DocumentStoreDocumentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@FeignClient(name = "document-store", url = "${app.document-store.base-url}")
public interface DocumentStoreClient {

    @PostMapping(value = "/document/api/documents/store/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    DocumentStoreDocumentDto upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("moduleReference") String moduleReference,
            @RequestParam("referenceId") String referenceId,
            @RequestParam("documentType") String documentType);

    @GetMapping("/document/api/documents/store/by-reference/{referenceId}")
    List<DocumentStoreDocumentDto> listByReference(@PathVariable("referenceId") String referenceId);
}
