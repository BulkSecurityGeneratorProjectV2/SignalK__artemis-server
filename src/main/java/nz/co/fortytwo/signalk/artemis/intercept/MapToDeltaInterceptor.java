package nz.co.fortytwo.signalk.artemis.intercept;

import java.util.NavigableMap;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.core.protocol.core.Packet;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.SessionReceiveMessage;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.service.SignalkMapConvertor;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

/**
 * Convert a map of messages to delta format
 * 
 * @author robert
 *
 */
public class MapToDeltaInterceptor implements Interceptor {

	private static Logger logger = LogManager.getLogger(MapToDeltaInterceptor.class);

	@Override
	public boolean intercept(Packet packet, RemotingConnection connection) throws ActiveMQException {
		if(!packet.isResponse())return true;
		//we only want outgoing packets
		if (logger.isDebugEnabled())
			logger.debug("Outgoing Msg is:" + packet.getClass());
		//NullResponseMessage!
		if (packet instanceof SessionReceiveMessage) {
			SessionReceiveMessage realPacket = (SessionReceiveMessage) packet;
			
			ICoreMessage msg = realPacket.getMessage();
			String format = msg.getStringProperty(SignalKConstants.FORMAT);
			if(SignalKConstants.FORMAT_FULL.equals(format)) return true;
			
			if(StringUtils.isBlank(format))format=SignalKConstants.FORMAT_DELTA;
			
			//convert map to json and put in body
			@SuppressWarnings("unchecked")
			NavigableMap<String, Json> map = (NavigableMap<String, Json>) msg.getObjectProperty(Config.JSON_MAP);
			Json delta = SignalkMapConvertor.mapToDelta(map);
			msg.getBodyBuffer().clear();
			msg.getBodyBuffer().writeString(delta.toString());
			if (logger.isDebugEnabled())
				logger.debug("Msg body is:" + Util.readBodyBufferToString(msg));
		}
		return true;
	}

	

}
