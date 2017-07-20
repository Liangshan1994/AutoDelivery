package com.hr.utils;

public class StringUtils {

	public static boolean isNullOrEmpty(Object obj) {
		return ((obj == null) || (obj.equals("null")) || (obj.equals("")));
	}
}
