package com.googlecode.utterlyidle.html;

import com.googlecode.totallylazy.functions.Function1;
import com.googlecode.totallylazy.functions.Function2;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.Sequences;
import com.googlecode.totallylazy.xml.Xml;
import com.googlecode.utterlyidle.Request;
import com.googlecode.utterlyidle.RequestBuilder;
import com.googlecode.utterlyidle.annotations.HttpMethod;
import org.w3c.dom.Element;

import static com.googlecode.totallylazy.Sequences.sequence;
import static java.lang.String.format;

public class Form extends BlockLevelElement {
    public static final String DESCENDANT = "descendant::";

    public Form(Element form) {
        super(form);
    }

    public Request submit(String submitXpath) throws IllegalStateException {
        if (new Input(expectElement(submitXpath)).disabled()){
            throw new IllegalStateException(format("Attempt to invoke disabled input for [%s]", submitXpath));
        }
        return submitXpath(fieldExpressions().append(sanitise(submitXpath)));
    }

    public Request submit() throws IllegalStateException {
        return submitXpath(fieldExpressions());
    }

    public String action() {
        return attribute("action");
    }

    public String method() {
        return attribute("method");
    }

    private Request submitXpath(Sequence<String> fieldExpressions) {
        String action = selectContent("@action");
        String method = selectContent("@method");

        Sequence<NameValue> inputs = nameValuePairs(fieldExpressions);
        return inputs.fold(new RequestBuilder(method, action),
                method.equalsIgnoreCase(HttpMethod.POST) ? addFormParams() : addQueryParams()).
                build();
    }

    private Sequence<String> fieldExpressions() {
        return sequence("input[not(@type='submit')]", "textarea", "select");
    }

    private String sanitise(String submitXpath) {
        return submitXpath.startsWith(DESCENDANT) ? submitXpath.substring(DESCENDANT.length()) : submitXpath;
    }

    private Sequence<NameValue> nameValuePairs(Sequence<String> xpath) {
        return selectElements(xpath.toString(DESCENDANT, "|" + DESCENDANT, "")).flatMap(toNameAndValue());
    }

    private Function1<? super Element, Sequence<NameValue>> toNameAndValue() {
        return element -> {
            String type = type(element);
            if (type.equals("select")) {
                return Sequences.<NameValue>sequence(new Select(element));
            }
            if (type.equals("checkbox")) {
                Checkbox checkbox = new Checkbox(element);
                if (checkbox.checked()) {
                    return Sequences.<NameValue>sequence(checkbox);
                }
                return Sequences.empty();
            }
            if(type.equals("textarea")) {
                return Sequences.<NameValue>sequence(new TextArea(element));
            }
            return Sequences.<NameValue>sequence(new Input(element));
        };
    }

    private String type(Element element) {
        String tagName = element.getTagName();
        if (tagName.equals("input")) {
            return Xml.selectContents(element, "@type");
        }
        return tagName;
    }

    private Function2<RequestBuilder, NameValue, RequestBuilder> addQueryParams() {
        return (requestBuilder, nameValue) -> requestBuilder.query(nameValue.name(), nameValue.value());
    }

    private Function2<RequestBuilder, NameValue, RequestBuilder> addFormParams() {
        return (requestBuilder, nameValue) -> requestBuilder.form(nameValue.name(), nameValue.value());
    }

    public static Function1<? super Element, ? extends Form> fromElement() {
        return element -> new Form(element);
    }
}