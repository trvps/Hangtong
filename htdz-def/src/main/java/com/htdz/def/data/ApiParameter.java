package com.htdz.def.data;

public abstract class ApiParameter {
	public static enum UploadFileStatus {

		DEVICEPORTRAIT("1"), USERPORTRAIT("2"), DEVICEPHOTO("3"), ANDRIODAPK("4");

		private final String value;

		private UploadFileStatus(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	};

}
