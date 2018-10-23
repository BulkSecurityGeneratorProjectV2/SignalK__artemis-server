
package org.signalk.schema.subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "context",
    "subscribe"
})
public class DocsSubscriptionProtocol2 {

    @JsonProperty("context")
    public String context;
    @JsonProperty("subscribe")
    public List<Subscribe_> subscribe = new ArrayList<Subscribe_>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public DocsSubscriptionProtocol2 withContext(String context) {
        this.context = context;
        return this;
    }

    public DocsSubscriptionProtocol2 withSubscribe(List<Subscribe_> subscribe) {
        this.subscribe = subscribe;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public DocsSubscriptionProtocol2 withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("context", context).append("subscribe", subscribe).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(context).append(additionalProperties).append(subscribe).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof DocsSubscriptionProtocol2) == false) {
            return false;
        }
        DocsSubscriptionProtocol2 rhs = ((DocsSubscriptionProtocol2) other);
        return new EqualsBuilder().append(context, rhs.context).append(additionalProperties, rhs.additionalProperties).append(subscribe, rhs.subscribe).isEquals();
    }

}