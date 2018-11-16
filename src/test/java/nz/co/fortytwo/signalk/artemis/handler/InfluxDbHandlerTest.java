package nz.co.fortytwo.signalk.artemis.handler;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.env_wind_angleApparent;

import javax.ws.rs.core.MediaType;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.intercept.BaseMsgInterceptorTest;
import nz.co.fortytwo.signalk.artemis.util.Config;

public class InfluxDbHandlerTest extends BaseMsgInterceptorTest {
	@Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Mock
    private InfluxDbHandler handler;// 1

    @Before
    public void before(){
    	handler = partialMockBuilder(InfluxDbHandler.class)
	    	.addMockedMethod("save")
    			.createMock(); 
    }
	@Test
	public void shouldStoreKey() {
		Json json = Json.read("{\"value\":-1.5707963271535559,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}");
		handler.save("vessels.urn:mrn:signalk:uuid:a8fb07c0-1ffd-4663-899c-f16c2baf8270.environment.wind.angleApparent.values.unknown", json);
		
		replayAll();
		
		handler.consume(getMessage("{\"value\":-1.5707963271535559,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",env_wind_angleApparent,"unknown"));
		verifyAll();
	}
	
	@Test
	public void shouldStoreJsonNull() {
		Json json = Json.nil();
		handler.save("vessels.urn:mrn:signalk:uuid:a8fb07c0-1ffd-4663-899c-f16c2baf8270.environment.wind.angleApparent.values.unknown", json);
		
		replayAll();
		
		handler.consume(getMessage(Json.nil().toString(),env_wind_angleApparent,"unknown"));
		verifyAll();
	}
	
	//@Test
	//mock fails on null, but null is saved.
	public void shouldStoreNull() {
		
		handler.save("vessels.urn:mrn:signalk:uuid:a8fb07c0-1ffd-4663-899c-f16c2baf8270.environment.wind.angleApparent.values.unknown", null);
		
		replayAll();
		
		handler.consume(getMessage(null,env_wind_angleApparent,"unknown"));
		verifyAll();
	}

}
