package com.googlecode.utterlyidle;

import org.hamcrest.Matchers;
import org.junit.Test;

import static com.googlecode.utterlyidle.BasePath.basePath;
import static com.googlecode.utterlyidle.HttpHeaders.HOST;
import static com.googlecode.utterlyidle.HttpHeaders.LOCATION;
import static com.googlecode.utterlyidle.RequestBuilder.get;
import static com.googlecode.utterlyidle.Response.methods.header;
import static com.googlecode.utterlyidle.Responses.seeOther;
import static com.googlecode.utterlyidle.handlers.ReturnResponseHandler.returnsResponse;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BasePathHandlerTest {
    @Test
    public void removesBasePathFromRequestUri() throws Exception {
        HttpHandler handler = new BasePathHandler(returnsRequestUri(), basePath("basePath"));
        assertThat(handler.handle(get("/basePath/foo").build()).entity().toString(), is("/foo"));
    }

    private HttpHandler returnsRequestUri() {
        return request -> ResponseBuilder.response().entity(request.uri().toString()).build();
    }

    @Test
    public void shouldPrependPathWithBasePathForRedirectsWithRelativePaths() throws Exception {
        Response response = new BasePathHandler(returnsResponse(seeOther("bar")), basePath("/foo")).
                handle(get("").header(HOST, "mayhost:8080").build());
        assertThat(header(response, LOCATION), is("http://mayhost:8080/foo/bar"));
        assertThat(response.status(), Matchers.is(Status.SEE_OTHER));
    }

    @Test
    public void makesRelativeLocationsAbsolute() throws Exception {
        assertLocationIsCorrectlyModified("foo", "http://mayhost:8080/foo");
        assertLocationIsCorrectlyModified("foo/bar?a=b", "http://mayhost:8080/foo/bar?a=b");
        assertLocationIsCorrectlyModified("/bar/bob", "http://mayhost:8080/bar/bob");
    }

    @Test
    public void doesNotModifyAbsoluteLocations() throws Exception {
        assertLocationIsCorrectlyModified("http://mayhost:8080/bar/bob", "http://mayhost:8080/bar/bob");
    }

    private void assertLocationIsCorrectlyModified(final String originalLocation, final String finalLocation) throws Exception {
        assertLocationIsCorrectlyModified(originalLocation, "/", finalLocation);
    }

    private void assertLocationIsCorrectlyModified(String originalLocation, String basePath, String finalLocation) throws Exception {
        Response response = new BasePathHandler(returnsResponse(seeOther(originalLocation)), basePath(basePath)).
                handle(get("").header(HOST, "mayhost:8080").build());
        assertThat(header(response, LOCATION), is(finalLocation));
        assertThat(response.status(), Matchers.is(Status.SEE_OTHER));
    }
}
