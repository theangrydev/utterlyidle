package com.googlecode.utterlyidle;

import com.googlecode.totallylazy.io.Uri;
import com.googlecode.utterlyidle.cookies.Cookie;
import com.googlecode.utterlyidle.cookies.CookieParameters;
import org.junit.Test;


import static com.googlecode.totallylazy.Pair.pair;
import static com.googlecode.totallylazy.Sequences.sequence;
import static com.googlecode.totallylazy.Strings.bytes;
import static com.googlecode.utterlyidle.Entities.inputStreamOf;
import static com.googlecode.utterlyidle.HeaderParameters.headerParameters;
import static com.googlecode.utterlyidle.HttpHeaders.CONTENT_LENGTH;
import static com.googlecode.utterlyidle.HttpHeaders.COOKIE;
import static com.googlecode.utterlyidle.RequestBuilder.get;
import static com.googlecode.utterlyidle.RequestBuilder.modify;
import static com.googlecode.utterlyidle.RequestBuilder.post;
import static com.googlecode.utterlyidle.RequestBuilder.put;
import static com.googlecode.utterlyidle.Requests.form;
import static com.googlecode.utterlyidle.Requests.query;
import static com.googlecode.utterlyidle.cookies.Cookie.cookie;
import static java.lang.String.valueOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class RequestBuilderTest {
    @Test
    public void setsContentLengthWithFormParameters() throws Exception {
        assertThat(post("/home").form("name", "value").build().headers().getValue(CONTENT_LENGTH), equalTo(valueOf(10)));
    }

    @Test
    public void alwaysSetsContentLengthForNonStreamingEntity() throws Exception {
        assertThat(put("/home").entity("").build().headers().getValue(CONTENT_LENGTH), equalTo("0"));
        assertThat(put("/home").entity("Hello").build().headers().getValue(CONTENT_LENGTH), equalTo(valueOf(bytes("Hello").length)));
        assertThat(put("/home").entity(bytes("Hello")).build().headers().getValue(CONTENT_LENGTH), equalTo(valueOf(bytes("Hello").length)));
        assertThat(put("/home").entity(inputStreamOf("Hello")).build().headers().contains(CONTENT_LENGTH), equalTo(false));
    }

    @Test
    public void canCreateRequestBuilderFromRequest() {
        Request originalRequest = get("/home").
                cookie(cookie("fred", "blogs")).
                form("going", "well").
                header("some", "header").
                accepting("accept header").
                query("a query", "a question").
                build();

        Request clonedRequest = modify(originalRequest).build();

        assertThat(clonedRequest.method(), is(equalTo(originalRequest.method())));
        assertThat(clonedRequest.uri(), is(equalTo(originalRequest.uri())));
        assertThat(clonedRequest.headers().toString(), is(equalTo(originalRequest.headers().toString())));
        assertThat(clonedRequest.entity().toString(), is(equalTo(originalRequest.entity().toString())));
    }

    @Test
    public void shouldRemoveQueryParamsFromEncodedUri() throws Exception {
        RequestBuilder requestBuilder = get("/home").query("^&%$^%", "foo").query("removeme", "");
        assertThat(requestBuilder.removeQuery("removeme").build().uri(), equalTo(Uri.uri("/home?%5E%26%25%24%5E%25=foo")));
    }

    @Test
    public void shouldBeAbleToReplaceACookie() throws Exception {
        Request request = get("/").cookie("cookie1", "value1").cookie("cookie2", "value2").replaceCookie("cookie1", "timtam").build();
        assertThat(CookieParameters.cookies(request).getValue("cookie1"), is("timtam"));
    }

    @Test
    public void shouldBeAbleToReplaceACookieEvenIfHeaderParameterCaseDiffers() throws Exception {
        Request request = get("/").
                header(COOKIE.toLowerCase(), Cookie.cookie("cookie1", "McVitees Digestive with caramel").toString()).
                cookie("cookie2", "value2").
                replaceCookie("cookie1", "timtam").build();

        assertThat(CookieParameters.cookies(request).getValue("cookie1"), is("timtam"));
    }

    @Test
    public void shouldBeAbleToRemoveACookie() throws Exception {
        Request request = get("/").cookie("cookie1", "value1").cookie("cookie2", "value2").removeCookie("cookie1").build();
        assertThat(CookieParameters.cookies(request).contains("cookie1"), is(false));
        assertThat(CookieParameters.cookies(request).getValue("cookie2"), is("value2"));
    }

    @Test
    public void shouldBeAbleToRemoveAllEvidenceOfCookie() throws Exception {
        Request request = get("/").cookie("cookie1", "value1").removeCookie("cookie1").build();
        assertThat(CookieParameters.cookies(request).contains("cookie1"), is(false));
        assertThat(request.headers().contains(COOKIE), is(false));
    }

    @Test
    public void replacingACookiePreservesHeaderOrder() throws Exception {
        Request request = put("/").header("path", "/").cookie("cookie1", "value1").cookie("cookie2", "value2").replaceCookie("cookie2", "penguin").build();
        assertThat(request.headers(), is(headerParameters(sequence(pair("path", "/"), pair(COOKIE, "cookie1=\"value1\""), pair(COOKIE, "cookie2=\"penguin\""), pair(CONTENT_LENGTH, "0")))));
    }

    @Test
    public void canReplaceCookieWhenListedInMiddleOfMultiCookie() throws Exception {
        Request request = get("/").header(COOKIE, "cookie1=\"value1\"; cookie2=\"value2\"").replaceCookie("cookie2", "hobnob").build();
        assertThat(request.headers().toMap().get(COOKIE).get(0), is("cookie1=\"value1\"; cookie2=\"hobnob\""));
    }

    @Test
    public void canCopyFormParamsIntoQueryParams() {
        Request postForm = post("/?three=3").form("one", "1").form("two", 2).build();

        Request modified = modify(postForm).copyFormParamsToQuery().build();

        QueryParameters queryParameters = query(modified);
        assertThat(queryParameters.getValue("one"), is(equalTo("1")));
        assertThat(queryParameters.getValue("two"), is(equalTo("2")));
        assertThat(queryParameters.getValue("three"), is(equalTo("3")));
        FormParameters formParameters = form(modified);
        assertThat(formParameters.size(), is(2));
    }
}
