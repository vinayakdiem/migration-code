/*package com.diemlife.utils;

import com.diemlife.models.Brand;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;

public class ResponseUtility {

    private static ObjectNode wrapper = Json.newObject();
    private static ObjectNode message = Json.newObject();
    private static final String MESSAGE_TYPE = "message";
    private static final String ERROR = "ERROR";
    private static final String SUCCESS = "SUCCESS";

    public static ObjectNode getErrorMessageForResponse(String errorMsg) {
        message.put(MESSAGE_TYPE, errorMsg);
        wrapper.set(ERROR, message);
        return wrapper;
    }

    public static ObjectNode getSuccessMessageForResponse(String successMsg) {
        message.put(MESSAGE_TYPE, successMsg);
        wrapper.set(SUCCESS, message);
        return wrapper;
    }

    public static ObjectNode getBrandForUser(ObjectNode response, Brand company){
        if (company != null) {
            response.putPOJO("brand", company);
        }
        return response;
    }
}
*/