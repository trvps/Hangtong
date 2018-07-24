package com.htdz.def.view;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * 语音消息。
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoiceMsg implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String deviceSn;
	// 获取表示语音内容，格式为 AMR，以 Base64 进行 Encode 后需要将所有 \r\n 和 \r 和 \n 替换成空，大小不超过
	private String content;
	// 60k，duration 表示语音长度，最长为 60 秒。
	private Long duration;

}