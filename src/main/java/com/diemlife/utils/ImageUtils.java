package com.diemlife.utils;

import java.awt.image.BufferedImage;

public class ImageUtils {

    public static String getImageTypeString(BufferedImage bi) {
        switch (bi.getType()) {
            case BufferedImage.TYPE_3BYTE_BGR:
                return "TYPE_3BYTE_BGR";
            case BufferedImage.TYPE_4BYTE_ABGR:
                return "TYPE_4BYTE_ABGR";
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                return "TYPE_4BYTE_ABGR_PRE";
            case BufferedImage.TYPE_BYTE_BINARY:
                return "TYPE_BYTE_BINARY";
            case BufferedImage.TYPE_BYTE_GRAY:
                return "TYPE_BYTE_GRAY";
            case BufferedImage.TYPE_BYTE_INDEXED:
                return "TYPE_BYTE_INDEXED";
            case BufferedImage.TYPE_CUSTOM:
                return "TYPE_CUSTOM";
            case BufferedImage.TYPE_INT_ARGB:
                return "TYPE_INT_ARGB";
            case BufferedImage.TYPE_INT_ARGB_PRE:
                return "TYPE_INT_ARGB_PRE";
            case BufferedImage.TYPE_INT_BGR:
                return "TYPE_INT_BGR";
            case BufferedImage.TYPE_INT_RGB:
                return "TYPE_INT_RGB";
            case BufferedImage.TYPE_USHORT_555_RGB:
                return "TYPE_USHORT_555_RGB";
            case BufferedImage.TYPE_USHORT_565_RGB:
                return "TYPE_USHORT_565_RGB";
            case BufferedImage.TYPE_USHORT_GRAY:
                return "TYPE_USHORT_GRAY";
            default:
                return "Unknown";
        }
    }

    public static String contentTypeToImageIoType(String contentType) {
        if ((contentType == null) || contentType.isEmpty()) {
            return "jpeg";
        }

        int index = contentType.indexOf('/');
        contentType = ((index == -1) ? contentType.toLowerCase() : contentType.substring(index + 1).toLowerCase());

        switch (contentType) {
            case "gif":
                return "gif";
            case "png":
                return "png";
            case "jpg":
                return "jpeg";
            case "jpeg":
                return "jpeg";
            default:
                return "jpeg";
        }        
    }
}
