package com.htdz.def.view;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResourceInfo implements Serializable {
	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	private String orgfilename;
	private String newfilepath;
	private String url;
	private String urlThumbnail;

}
