package com.digitalhie.QRDAReportGenerator.service;

import com.digitalhie.QRDAReportGenerator.util.CCDGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class QrdaReportService {

    Logger logger = LoggerFactory.getLogger(QrdaReportService.class);

    @Autowired
    private CCDGenerator ccdGenerator;

    @GetMapping("/health-check")
    public String checkHealth() {
        return "OK";
    }

    @PostMapping(value = "/{qrdaType}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public void generateQrdaFile(
            @PathVariable("qrdaType") String qrdaType,
            @RequestBody JsonNode input,
            HttpServletResponse response) throws Exception {

        String templateFilePath = null;
        if(qrdaType.equalsIgnoreCase("qrda1")) {
            templateFilePath = "templates/2023-CMS-QRDA-I-v1.2-Sample-File.xml";
        }
        else if(qrdaType.equalsIgnoreCase("qrda3")) {
            templateFilePath = "templates/2023MIPSGroupSampleQRDA-III-v1.1.xml";
        }
        else {
            response.resetBuffer();
            response.setStatus(400);
            response.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            response.getOutputStream().print(new ObjectMapper()
                    .writeValueAsString("Invalid QRDA type. Only qrda1 or qrda3 are supported."));
            response.flushBuffer();
            return;
        }

        String fileName = null;
        try {
            fileName = ccdGenerator.createCCD(templateFilePath, input, qrdaType);
        } catch (Exception ex) {
            logger.error("Error occurred while creating CCD file: "+ex.getMessage(), ex);
            response.resetBuffer();
            response.setStatus(500);
            response.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            response.getOutputStream().print(new ObjectMapper()
                    .writeValueAsString("Error occurred while creating CCD file: "+ex.getMessage()));
            response.flushBuffer();
            return;
        }

        File file = new File(fileName);
        try (OutputStream out = response.getOutputStream(); FileInputStream in = new FileInputStream(file)) {
            response.setContentType("application/xml");
            response.setHeader("Content-disposition", "attachment; filename=" + file.getName());
            // copy from in to out
            IOUtils.copy(in, out);
            response.flushBuffer();
        }
        catch (Exception ex) {
            logger.error("Error occurred while formatting response file: "+ex.getMessage(), ex);
            response.resetBuffer();
            response.setStatus(500);
            response.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            response.getOutputStream().print(new ObjectMapper()
                    .writeValueAsString("Error occurred while formatting response file: "+ex.getMessage()));
        }
        finally {
            Files.deleteIfExists(Paths.get(fileName));
        }
        response.flushBuffer();
    }

}
