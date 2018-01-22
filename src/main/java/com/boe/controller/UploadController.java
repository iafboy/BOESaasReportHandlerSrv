package com.boe.controller;

import com.boe.service.ComprssServiceInf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class UploadController {
    private Logger logger= LoggerFactory.getLogger(UploadController.class);
    //Save the uploaded file to this local folder
    @Value("${uploaded.folder}")
    private String UPLOADED_FOLDER;

    @Value("${uploaded.succ.folder}")
    private String webSuccFolder;

    @Value("${uploaded.error.folder}")
    private String webErrorFolder;

    @GetMapping("/")
    public String index() {
        return "upload";
    }

    @Resource(name="SevenZipService")
    private ComprssServiceInf sevenZipService;

    @PostMapping("/upload")
    public String singleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
            return "redirect:uploadStatus";
        }

        try {
            logger.info("will save to "+UPLOADED_FOLDER+ file.getOriginalFilename());
            // Get the file and save it somewhere
            byte[] bytes = file.getBytes();
            Path path = Paths.get(UPLOADED_FOLDER + file.getOriginalFilename());
            Files.write(path, bytes);

            redirectAttributes.addFlashAttribute("message",
                    "You successfully uploaded '" + file.getOriginalFilename() + "'");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message",
                    " uploaded error: " + e.getMessage());
           logger.error(e.getMessage(),e);
        }
        return "redirect:/uploadStatus";
    }
}