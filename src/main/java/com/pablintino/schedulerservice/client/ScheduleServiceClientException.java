package com.pablintino.schedulerservice.client;

public class ScheduleServiceClientException extends RuntimeException{
    private final Integer httpCode;
    public ScheduleServiceClientException(String message){
        super(message);
        httpCode=null;
    }

    public ScheduleServiceClientException(String message, int httpCode){
        super(message);
        this.httpCode=httpCode;
    }

    public Integer getHttpCode(){
        return httpCode;
    }
}
