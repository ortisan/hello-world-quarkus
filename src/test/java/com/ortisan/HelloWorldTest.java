package com.ortisan;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class HelloWorldTest {

    @Test
    public void testHelloEndpoint() {


        given().when().get("/hello").then().statusCode(200)
                .body(
                        "data", Matchers.notNullValue(),
                        "data[0].id", Matchers.equalTo(1),
                        "data[0].country", Matchers.equalTo("United States"));
    }
}
