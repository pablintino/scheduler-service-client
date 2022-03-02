package com.pablintino.schedulerservice.client.tests.utils;

import lombok.Data;

import java.time.Instant;

@Data
public class DummyPayload {

  public static DummyPayload buildDummy() {
    DummyPayload dummyPayload = new DummyPayload();
    dummyPayload.setTestInstant(Instant.now());
    dummyPayload.setTestInteger(4);
    dummyPayload.setTestString("test-string");
    return dummyPayload;
  }

  public String testString;
  public Instant testInstant;
  public Integer testInteger;
}
