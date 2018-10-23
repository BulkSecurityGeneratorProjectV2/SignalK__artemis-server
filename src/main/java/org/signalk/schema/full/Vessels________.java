
package org.signalk.schema.full;

import java.util.HashMap;
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
    "urn:mrn:imo:mmsi:366982330"
})
public class Vessels________ {

    @JsonProperty("urn:mrn:imo:mmsi:366982330")
    public UrnMrnImoMmsi366982330___ urnMrnImoMmsi366982330;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Vessels________ withUrnMrnImoMmsi366982330(UrnMrnImoMmsi366982330___ urnMrnImoMmsi366982330) {
        this.urnMrnImoMmsi366982330 = urnMrnImoMmsi366982330;
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

    public Vessels________ withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("urnMrnImoMmsi366982330", urnMrnImoMmsi366982330).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(urnMrnImoMmsi366982330).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Vessels________) == false) {
            return false;
        }
        Vessels________ rhs = ((Vessels________) other);
        return new EqualsBuilder().append(urnMrnImoMmsi366982330, rhs.urnMrnImoMmsi366982330).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}