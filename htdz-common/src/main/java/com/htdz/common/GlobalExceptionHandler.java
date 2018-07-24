package com.htdz.common;


import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import net.sf.json.JSONObject;


@ControllerAdvice
public class GlobalExceptionHandler {
	public static final String DEFAULT_ERROR_VIEW = "error";
    
	
//	@ExceptionHandler(value = Exception.class)
//    public ModelAndView defaultErrorHandler(HttpServletRequest req, Exception e) throws Exception {
//        ModelAndView mav = new ModelAndView();
//        mav.addObject("exception", e);
//        mav.addObject("url", req.getRequestURL());
//        mav.setViewName(DEFAULT_ERROR_VIEW);
//        return mav;
//    }
	
	@ExceptionHandler(value = Exception.class)
	@ResponseBody
    public String defaultErrorHandler(HttpServletRequest req, Exception e) throws Exception {
		String exception = "[My Exception]\n";
		exception += req.getRequestURL() + "\n";
		exception += e.getClass().getName() + "\n";
		exception += e.getMessage();
		
		JSONObject jsonobject = new JSONObject();
		
		try {
			jsonobject.put("code", 1);
			jsonobject.put("msg", "failed");
			jsonobject.put("exception", exception);
		} catch (Exception ee) {
			LogManager.exception(ee.getMessage(), ee);
		}
		
		return jsonobject.toString();
    }
}
