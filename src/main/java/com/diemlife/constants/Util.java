package com.diemlife.constants;

import java.util.Collection;

public class Util {
	
	public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

	public static boolean isEmpty(String value) {
        return value == null || value.length()==0;
    }
	
	public static boolean isEmpty(String[] value) {
        return value == null || value.length==0;
    }
}
