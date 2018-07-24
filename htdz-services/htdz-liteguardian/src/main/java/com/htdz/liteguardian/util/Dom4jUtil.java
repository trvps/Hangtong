package com.htdz.liteguardian.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class Dom4jUtil {

	public Document getDocument(String inputXml) {
//		SAXReader saxReader = new SAXReader();
//		Document document = null;
//		try {
//			document = saxReader.read(inputXml);
//		} catch (DocumentException e) {
//			e.printStackTrace();
//		}
		 // 将字符串转为XML
		Document doc = null;
		try {
			doc = DocumentHelper.parseText(inputXml);
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
        // 获取根节点
//        Element rootElt = doc.getRootElement(); 
		return doc;
	}


	@SuppressWarnings("unchecked")
	public static SortedMap<String, String> iterator(String inputXml) {
		SortedMap<String, String> map = new TreeMap<>();
		Document doc = null;
		try {
			doc = DocumentHelper.parseText(inputXml);
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		if (doc == null)
			return map;
        // 获取根节点
		Element root = doc.getRootElement(); 
		for (Iterator<Element> ie = root.elementIterator(); ie.hasNext();) {
			Element element = ie.next();
			map.put(element.getName(), element.getText());
		}
		
		return map;
	}
	
	@SuppressWarnings("static-access")
	public static void main(String[] args) {
//		String inputXml = "<xml>"
//				+"<appid>wx2421b1c4370ec43b</appid>"
//				+"   <attach>支付测试</attach>"
//				+"<body>JSAPI支付测试</body>"
//				+"<mch_id>10000100</mch_id>"
//				+"<nonce_str>1add1a30ac87aa2db72f57a2375d8fec</nonce_str>"
//				+"<notify_url>http://wxpay.weixin.qq.com/pub_v2/pay/notify.v2.php</notify_url>"
//				+"<openid>oUpF8uMuAJO_M2pxb1Q9zNjWeS6o</openid>"
//				+"<out_trade_no>1415659990</out_trade_no>"
//				+"<spbill_create_ip>14.23.150.211</spbill_create_ip>"
//				+"<total_fee>1</total_fee>"
//				+"<trade_type>JSAPI</trade_type>"
//				+"<sign>0CB01533B8C1EF103065174F50BCA001</sign>"
//				+"</xml>";
//		
		String returnXml = "<xml>"
				+"<appid><![CDATA[wx4ecd29ff7829c799]]></appid>"
				+"<bank_type><![CDATA[ABC_DEBIT]]></bank_type>"
				+"<cash_fee><![CDATA[1]]></cash_fee>"
				+"<fee_type><![CDATA[CNY]]></fee_type>"
				+"<is_subscribe><![CDATA[N]]></is_subscribe>"
				+"<mch_id><![CDATA[1234976102]]></mch_id>"
				+"<nonce_str><![CDATA[e661551c8ec9308379cda7e2419348e5]]></nonce_str>"
				+"<openid><![CDATA[o5_gGuOnZA4PR5c4STBAiXwik4yE]]></openid>"
				+"<out_trade_no><![CDATA[e661551c8ec9308379cda7e2419348e5]]></out_trade_no>"
				+"<result_code><![CDATA[SUCCESS]]></result_code>"
				+"<return_code><![CDATA[SUCCESS]]></return_code>"
				+"<sign><![CDATA[C13AA4D95D121FAAA946614F68CCE368]]></sign>"
				+"<time_end><![CDATA[20150415135701]]></time_end>"
				+"<total_fee>1</total_fee>"
				+"<trade_type><![CDATA[APP]]></trade_type>"
				+"<transaction_id><![CDATA[1005840271201504150063947741]]></transaction_id>"
				
   +"</xml> ";
		
		Map<String, String> map = new Dom4jUtil().iterator(returnXml);
		for (Entry<String, String> entry : map.entrySet()) 
			System.out.println(entry.getKey()+"===="+entry.getValue());
	}
}
