package com.diemlife.utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class DBUtils {
    
    public static void printResultFields(ResultSet src, StringBuilder dst) throws SQLException {
        ResultSetMetaData metaData = src.getMetaData();
        int columnCount = metaData.getColumnCount();

        if (columnCount > 0) {
            dst.append(metaData.getColumnName(1));
            dst.append(", ");
            dst.append(metaData.getColumnTypeName(1));
 
            for (int i=2; i <= columnCount; i++) {
                dst.append('\n'); 
                dst.append(metaData.getColumnName(i));
                dst.append(", ");
                dst.append(metaData.getColumnTypeName(i));
            }
        }
    }

}
