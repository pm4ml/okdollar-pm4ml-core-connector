package com.modusbox.client.processor;

import com.google.gson.Gson;
import com.modusbox.client.customexception.CCCustomException;
import com.modusbox.client.enums.ErrorCode;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;

public class CheckCBSError implements Processor {

    public void process(Exchange exchange) throws Exception {
        Gson gson = new Gson();
        String s = gson.toJson(exchange.getIn().getBody(), LinkedHashMap.class);
        JSONObject respObject = new JSONObject(s);
        int errorCode = 0;
        String errorMessage = "";

        try {
            errorCode = respObject.getInt("code");
            errorMessage = respObject.getString("message");
//          respObject.getString("message");
            if (errorCode == 300) {
                if (errorMessage.equals("This Ip Address not allowed to access !")) {
                    throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, "IP has not been whitelisted to called the payment API."));
                }
                else if (errorMessage.equals("OK$ Payment Failure")) {
                    throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, "Payment failure"));
                }
                else if (errorMessage.equals("Client Not Found")) {
                    throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_ID_NOT_FOUND, "Client Not Found"));
                }
                else if (errorMessage.equals("Inactive Customer")) {
                    throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, "Inactive Customer"));
                }
                else if (errorMessage.equals("Invalid OK$ Customer")) {
                    throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, "Invalid OK$ Customer"));
                }
                else if (errorMessage.equals("Information is wrong")) {
                    throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, "Information is wrong"));
                }
                else if (errorMessage.equals("Invalid Secure Token")) {
                    throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, "Invalid Secure Token"));
                }
                else if (errorMessage.equals("Insufficient Wallet")) {
                    throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYER, "Insufficient Wallet"));
                }
                else if (errorMessage.equals("Subscriber/Agent Blocked")) {
                    throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, "Subscriber/Agent Blocked"));
                }
                else if (errorMessage.equals("Max Wallet Balance Reached")) {
                    throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, "Max Wallet Balance Reached"));
                }
                else if (errorMessage.equals("Inactive Destination Subscriber/Agent")) {
                    throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, "Inactive Destination Subscriber/Agent"));
                }
                else if (errorMessage.equals("Max Amount Limit Reached For Payment To Unregistered")) {
                    throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, "Max Amount Limit Reached For Payment To Unregistered"));
                }
                else if (errorMessage.equals("Total amount of Transactions per day Limit Reached")) {
                    throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, "Total amount of Transactions per day Limit Reached"));
                }
            }
            else if (errorCode == 301) {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, "Payee CBS failed due to Token Failure"));
            }
        } catch (JSONException e) {
            System.out.println("Problem extracting error code from CBS response occurred.");
        }

    }

}
