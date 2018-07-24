package com.htdz.device.data;


import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class WifiGroup {
	// WiFi信息列表
	List<WifiItem> bsItems = new ArrayList<WifiItem>();
}
