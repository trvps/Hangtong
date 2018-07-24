package com.htdz.data.controllers;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.htdz.common.LogManager;


@RestController
public class APIController {
	
	@RequestMapping("/info")
	public String info() {
		return "Reg APIController";
	}
	
	@RequestMapping("/downloadOrOpen1")
	public String downloadOrOpen() {
		return "<html> \r\n" + 
				"<meta charset=\"UTF-8\">\r\n" + 
				"<meta name='apple-itunes-app' content='app-id=1133401990'> \r\n" + 
				"<title></title> \r\n" + 
				"<head></head> \r\n" + 
				"<body> \r\n" + 
				"<a href=\"https://itunes.apple.com/hk/app/%E8%88%AA%E9%80%9A%E5%AE%88%E6%8A%A4%E8%80%85-hk/id1133401990?mt=8\" id=\"openApp\">点击打开</a>\r\n" + 
				"<script type=\"text/javascript\"> \r\n" + 
				"var browser = { \r\n" + 
				"	versions: function () { \r\n" + 
				"		var u = navigator.userAgent, app = navigator.appVersion; \r\n" + 
				"		return { \r\n" + 
				"			//移动终端浏览器版本信息 \r\n" + 
				"			ios: !!u.match(/\\(i[^;]+;( U;)? CPU.+Mac OS X/), //ios终端 \r\n" + 
				"			android: u.indexOf('Android') > -1 || u.indexOf('Linux') > -1, //android终端或uc浏览器 \r\n" + 
				"			iPhone: u.indexOf('iPhone') > -1, //是否为iPhone或者QQHD浏览器 \r\n" + 
				"			iPad: u.indexOf('iPad') > -1, //是否iPad \r\n" + 
				"		}; \r\n" + 
				"	}(), \r\n" + 
				"} \r\n" + 
				"\r\n" + 
				"var url_ios_open = \"htdzrun://http://www.baidu.com\"\r\n" + 
				"var url_android_open = \"htdzrun://http://www.baidu.com\"\r\n" + 
				"var url_ios_download = \"https://itunes.apple.com/hk/app/%E8%88%AA%E9%80%9A%E5%AE%88%E6%8A%A4%E8%80%85-hk/id1133401990?mt=8\"\r\n" + 
				"//var url_android_download = \"http://54.179.149.239:10000/image/upload/app_apk/hangtong_HK.apk\"\r\n" + 
				"var url_android_download = \"https://play.google.com/store/apps/details?id=com.bluebud.hangtonggps_hk&rdid=com.bluebud.hangtonggps_hk&pli=1#details-reviews\"\r\n" + 
				"var url_common_download = \"http://app.livegpslocate.com/hk/d/\"\r\n" + 
				"\r\n" + 
				"document.getElementById('openApp').onclick = function(e) {  \r\n" + 
				"    // 通过iframe的方式试图打开APP，如果能正常打开，会直接切换到APP，并自动阻止a标签的默认行为  \r\n" + 
				"    // 否则打开a标签的href链接  \r\n" + 
				"    var ifr = document.createElement('iframe');  \r\n" + 
				"    ifr.src = url_ios_open;  \r\n" + 
				"    ifr.style.display = 'none';  \r\n" + 
				"    document.body.appendChild(ifr);  \r\n" + 
				"    window.setTimeout(function() {  \r\n" + 
				"        document.body.removeChild(ifr);  \r\n" + 
				"    }, 3000)  \r\n" + 
				"}; \r\n" + 
				"\r\n" + 
				"</script>\r\n" + 
				"</body> \r\n" + 
				"</html> \r\n" + 
				"";
	}
	
	@RequestMapping("/downloadOrOpen")
	public String downloadOrOpen1() {
		
		
		String str =  "<html> \r\n" + 
				"<meta name='apple-itunes-app' content='app-id=1133401990'> \r\n" + 
				"<title></title> \r\n" + 
				"<head></head> \r\n" + 
				"<body> \r\n" + 
				"<script type=\"text/javascript\"> \r\n" + 
				"var browser = { \r\n" + 
				"	versions: function () { \r\n" + 
				"		var u = navigator.userAgent, app = navigator.appVersion; \r\n" + 
				"		return { \r\n" + 
				"			//移动终端浏览器版本信息 \r\n" + 
				"			ios: !!u.match(/\\(i[^;]+;( U;)? CPU.+Mac OS X/), //ios终端 \r\n" + 
				"			android: u.indexOf('Android') > -1 || u.indexOf('Linux') > -1, //android终端或uc浏览器 \r\n" + 
				"			iPhone: u.indexOf('iPhone') > -1, //是否为iPhone或者QQHD浏览器 \r\n" + 
				"			iPad: u.indexOf('iPad') > -1, //是否iPad \r\n" + 
				"		}; \r\n" + 
				"	}(), \r\n" + 
				"} \r\n" + 
				"\r\n" + 
				"var url_ios_open = \"htdzrun://http://www.baidu.com\"\r\n" + 
				"var url_android_open = \"htdzrun://http://www.baidu.com\"\r\n" + 
				"var url_ios_download = \"https://itunes.apple.com/hk/app/%E8%88%AA%E9%80%9A%E5%AE%88%E6%8A%A4%E8%80%85-hk/id1133401990?mt=8\"\r\n" + 
				"//var url_android_download = \"http://54.179.149.239:10000/image/upload/app_apk/hangtong_HK.apk\"\r\n" + 
				"var url_android_download = \"https://play.google.com/store/apps/details?id=com.bluebud.hangtonggps_hk&rdid=com.bluebud.hangtonggps_hk&pli=1#details-reviews\"\r\n" + 
				"var url_common_download = \"http://app.livegpslocate.com/hk/d/\"\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"if (browser.versions.iPhone || browser.versions.iPad || browser.versions.ios) { \r\n" + 
				"    window.location = url_ios_open;\r\n" + 
				"    var loadDateTime = new Date();\r\n" + 
				"    window.setTimeout(function() {\r\n" + 
				"        var timeOutDateTime = new Date();\r\n" + 
				"        if (timeOutDateTime - loadDateTime > 800) {\r\n" + 
				"            window.location = url_ios_download;\r\n" + 
				"        } else {\r\n" + 
				"            window.close();\r\n" + 
				"        }\r\n" + 
				"    }, 1000);\r\n" + 
				"} else if (browser.versions.android) { \r\n" + 
				"    window.location = url_android_open;\r\n" + 
				"    var loadDateTime = new Date();\r\n" + 
				"    window.setTimeout(function() {\r\n" + 
				"        var timeOutDateTime = new Date();\r\n" + 
				"        if (timeOutDateTime - loadDateTime > 800) {\r\n" + 
				"            window.location = url_android_download;\r\n" + 
				"        } else {\r\n" + 
				"            window.close();\r\n" + 
				"        }\r\n" + 
				"    }, 1000);\r\n" + 
				"} else {\r\n" + 
				"	window.location.href = \"http://app.livegpslocate.com/hk/d/\"; \r\n" + 
				"}\r\n" + 
				"</script>\r\n" + 
				"</body> \r\n" + 
				"</html> \r\n" + 
				"";
		LogManager.info(str);
		return str;
	}
}
