package com.htdz.gateway.controllers;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import com.htdz.common.LogManager;


@RestController
public class FileUpDownloadController {
	public static final String fileRoot = "E:\\JavaProgramme\\HTDZProject\\htdz-services\\htdz-gateway\\";
	
	@RequestMapping("/html")
	public String html() {
		return "<html>    \r\n" + 
				"<body>\r\n" + 
				"  <form action=\"/upload\" method=\"POST\" enctype=\"multipart/form-data\">    \r\n" + 
				"  	<input type=\"text\" name=\"text\" value=\"text default\">\r\n" + 
				"    <input type=\"file\" name=\"file1\"/>  \r\n" + 
				"    <input type=\"file\" name=\"file2\"/>  \r\n" + 
				"    <input type=\"submit\" value=\"Upload\"/>     \r\n" + 
				"  </form>\r\n" + 
				"</body>    \r\n" + 
				"</html>";
	}
	
	@RequestMapping("/upload")      
    @ResponseBody      
    public String handleFileUpload(@RequestParam("file1") MultipartFile file1, 
    								@RequestParam("file2") MultipartFile file2,
    								@RequestParam("text") String text) {
		LogManager.info("------------------------------------------------------");
        if (!file1.isEmpty()) {      
            try {    
            	LogManager.info("file1={}", file1.getName());
            	LogManager.info("file1 name={}", file1.getOriginalFilename());
            	LogManager.info("file1 size={}", file1.getSize());
            	LogManager.info("file1 ContentType={}", file1.getContentType());
            	
                file1.transferTo(new File(fileRoot+file1.getOriginalFilename()));
            } catch (FileNotFoundException e) {      
                e.printStackTrace();    
            } catch (IOException e) {      
                e.printStackTrace();       
            }      
        }
        
        LogManager.info("------------------------------------------------------");
        if (!file2.isEmpty()) {      
            try {    
            	LogManager.info("file2={}", file2.getName());
            	LogManager.info("file2 name={}", file2.getOriginalFilename());
            	LogManager.info("file2 size={}", file2.getSize());
            	LogManager.info("file2 ContentType={}", file2.getContentType());
    
                file2.transferTo(new File(fileRoot+file2.getOriginalFilename()));
            } catch (FileNotFoundException e) {      
                e.printStackTrace();    
            } catch (IOException e) {      
                e.printStackTrace();       
            }      
        }
        
        LogManager.info("------------------------------------------------------");
        LogManager.info("text={}", text);
        
        return "上传成功";   
    }  
	
	
	@RequestMapping("/uploads")      
    @ResponseBody      
    public String handleFileUpload(HttpServletRequest request) {
		MultipartHttpServletRequest mrequest = (MultipartHttpServletRequest)request;
		try {
			MultiValueMap<String, MultipartFile> mvm = mrequest.getMultiFileMap();
			if (mvm.size() > 0) {
				Iterator<Entry<String, List<MultipartFile>>> iterator = mvm.entrySet().iterator();
				while (iterator.hasNext()) {
					LogManager.info("------------------------------------------------------");
					Entry<String, List<MultipartFile>> entry = iterator.next();
					for (MultipartFile mf : entry.getValue()) {
						LogManager.info("file={}", mf.getName());
		            	LogManager.info("file name={}", mf.getOriginalFilename());
		            	LogManager.info("fil size={}", mf.getSize());
		            	LogManager.info("file ContentType={}", mf.getContentType());
		            	
		            	mf.transferTo(new File(fileRoot+mf.getOriginalFilename()));
					}
				}
			}
			
			LogManager.info("------------------------------------------------------");
			Map<String, String[]> params = request.getParameterMap();
			Iterator<Entry<String, String[]>> iterator = params.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, String[]> entry = iterator.next();
				LogManager.info("test={} value={}", entry.getKey(), entry.getValue()[0]);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "上传成功";
	}
}

/*
<html>    
<body>
  <form action="/upload" method="POST" enctype="multipart/form-data">    
  	<input type="text" name="text" value="text default">
    <input type="file" name="file1"/>  
    <input type="file" name="file2"/>  
    <input type="submit" value="Upload"/>     
  </form>
</body>    
</html>

*/


