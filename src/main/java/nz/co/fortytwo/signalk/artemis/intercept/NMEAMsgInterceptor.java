/*
 *
 * Copyright (C) 2012-2014 R T Huitema. All Rights Reserved.
 * Web: www.42.co.nz
 * Email: robert@42.co.nz
 * Author: R T Huitema
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
 * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package nz.co.fortytwo.signalk.artemis.intercept;

import static nz.co.fortytwo.signalk.util.SignalKConstants.UNKNOWN;
import static nz.co.fortytwo.signalk.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.util.SignalKConstants.env_depth_belowTransducer;
import static nz.co.fortytwo.signalk.util.SignalKConstants.env_wind_angleApparent;
import static nz.co.fortytwo.signalk.util.SignalKConstants.env_wind_speedApparent;
import static nz.co.fortytwo.signalk.util.SignalKConstants.label;
import static nz.co.fortytwo.signalk.util.SignalKConstants.nav_courseOverGroundMagnetic;
import static nz.co.fortytwo.signalk.util.SignalKConstants.nav_courseOverGroundTrue;
import static nz.co.fortytwo.signalk.util.SignalKConstants.nav_position;
import static nz.co.fortytwo.signalk.util.SignalKConstants.nav_speedOverGround;
import static nz.co.fortytwo.signalk.util.SignalKConstants.type;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels_dot_self_dot;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.protocol.core.Packet;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.SessionSendMessage;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.DepthSentence;
import net.sf.marineapi.nmea.sentence.HeadingSentence;
import net.sf.marineapi.nmea.sentence.MWVSentence;
import net.sf.marineapi.nmea.sentence.PositionSentence;
import net.sf.marineapi.nmea.sentence.RMCSentence;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.sentence.VHWSentence;
import nz.co.fortytwo.signalk.artemis.server.ArtemisServer;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;


/**
 * Processes NMEA sentences in the body of a message, firing events to
 * interested listeners Converts the NMEA messages to signalk
 * 
 * @author robert
 * 
 */
public class NMEAMsgInterceptor extends BaseInterceptor implements Interceptor {

	private static Logger logger = LogManager.getLogger(NMEAMsgInterceptor.class);
	
	
	SentenceListener listener;
	private boolean rmcClock = false;

	public NMEAMsgInterceptor() {
		super();
		
		setNmeaListeners();
	}

	@Override
	public boolean intercept(Packet packet, RemotingConnection connection) throws ActiveMQException {
		if(packet.isResponse())return true;
		if (packet instanceof SessionSendMessage) {
			SessionSendMessage realPacket = (SessionSendMessage) packet;

			ICoreMessage message = realPacket.getMessage();
			if(!Config._0183.equals(message.getStringProperty(Config.AMQ_CONTENT_TYPE)))return true;
			String sessionId = message.getStringProperty(Config.AMQ_SESSION_ID);
			ServerSession sess = ArtemisServer.getActiveMQServer().getSessionByID(sessionId);
			String bodyStr = Util.readBodyBufferToString(message);
			if(logger.isDebugEnabled())logger.debug("Message: " +bodyStr);
			
			if (StringUtils.isNotBlank(bodyStr) && bodyStr.startsWith("$")) {
				try {
					if (logger.isDebugEnabled())
						logger.debug("Processing NMEA:[" + bodyStr + "]");
					Sentence sentence = SentenceFactory.getInstance().createParser(bodyStr);
					String src = sess.getRemotingConnection().getRemoteAddress();
					src=src.replace("/", "");
					src=src.replace(".", "-");
					fireSentenceEvent(sess, sentence, src);
					
				} catch (IllegalArgumentException e) {
					logger.debug(e.getMessage(), e);
					logger.info(e.getMessage() + ":" + bodyStr);
					logger.info("   in hexidecimal : " + Hex.encodeHexString(bodyStr.getBytes()));
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
		return true;
	}

	/**
	 * Dispatch data to all listeners. Puts the nmea string into
	 * sources.0183.device.[talkerid].[sentenceid] Processes the nmea
	 * into signalk position, heading, etc.
	 * 
	 * @param map
	 * 
	 * @param sentence
	 *            sentence string.
	 * @throws Exception 
	 */
	private void fireSentenceEvent(ServerSession sess, Sentence sentence, String device) throws Exception {
		if (!sentence.isValid()) {
			logger.warn("NMEA Sentence is invalid:" + sentence.toSentence());
			return;
		}
		String now = nz.co.fortytwo.signalk.util.Util.getIsoTimeString();
		if (StringUtils.isBlank(device))
			device = UNKNOWN;
		// A general rule of sources.protocol.bus.device.data
		String srcRef = "0183." + device + dot + sentence.getTalkerId() + dot + sentence.getSentenceId();
		
		sendSourceMsg(srcRef, getSrcJson(sentence,srcRef),now, sess);
		
		try {
			SentenceEventSource src = new SentenceEventSource(srcRef, now, sess);
			SentenceEvent se = new SentenceEvent(src, sentence);
			listener.sentenceRead(se);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	
	private Json getSrcJson(Sentence sentence, String srcLabel) {
		Json srcJson = Json.object();
		srcJson.set(label,srcLabel);
		srcJson.set(type,"NMEA0183");
		srcJson.set("talker",sentence.getTalkerId().name());
		srcJson.set("sentences",sentence.getSentenceId());
		return srcJson;
	}

	/**
	 * Adds NMEA sentence listeners to process NMEA to simple output
	 * 
	 * @param processor
	 */
	private void setNmeaListeners() {

		listener = new SentenceListener() {

			
			public void sentenceRead(SentenceEvent evt) {
				SentenceEventSource src = (SentenceEventSource) evt.getSource();
			
				try{
				
					if (evt.getSentence() instanceof PositionSentence) {
						PositionSentence sen = (PositionSentence) evt.getSentence();
	
						if(logger.isDebugEnabled())logger.debug("lat position:" + sen.getPosition().getLatitude() + ", hemi=" + sen.getPosition().getLatitudeHemisphere());
						Json position = Json.object();
						position.set("latitude", sen.getPosition().getLatitude());
						position.set("longitude", sen.getPosition().getLongitude());
						position.set("altitude", 0.0);
						sendMsg(vessels_dot_self_dot + nav_position, position, src.getNow(), src.getSourceRef(), src.getSession());
					}
	
					if (evt.getSentence() instanceof HeadingSentence) {
						
						if (!(evt.getSentence() instanceof VHWSentence)) {
							
							HeadingSentence sen = (HeadingSentence) evt.getSentence();

							if (sen.isTrue()) {
								try {
									sendDoubleAsMsg(vessels_dot_self_dot + nav_courseOverGroundTrue, Math.toRadians(sen.getHeading()), src.getNow(), src.getSourceRef(), src.getSession());
								} catch (Exception e) {
									logger.error(e.getMessage());
								}
							} else {
								sendDoubleAsMsg(vessels_dot_self_dot + nav_courseOverGroundMagnetic, Math.toRadians(sen.getHeading()), src.getNow(), src.getSourceRef(), src.getSession());
							}
						}
					}
					
					if (evt.getSentence() instanceof RMCSentence) {
						RMCSentence sen = (RMCSentence) evt.getSentence();
						if(rmcClock)Util.checkTime(sen);
					
						sendDoubleAsMsg(vessels_dot_self_dot + nav_speedOverGround, Util.kntToMs(sen.getSpeed()), src.getNow(), src.getSourceRef(), src.getSession());
					}
					if (evt.getSentence() instanceof VHWSentence) {
						VHWSentence sen = (VHWSentence) evt.getSentence();
						//VHW sentence types have both, but true can be empty
						try {
							
							sendDoubleAsMsg(vessels_dot_self_dot + nav_courseOverGroundMagnetic, Math.toRadians(sen.getMagneticHeading()), src.getNow(), src.getSourceRef(), src.getSession());
							
							sendDoubleAsMsg(vessels_dot_self_dot + nav_courseOverGroundTrue, Math.toRadians(sen.getHeading()), src.getNow(), src.getSourceRef(), src.getSession());
							
						} catch (DataNotAvailableException e) {
							logger.error(e.getMessage());
						}
						
						sendDoubleAsMsg(vessels_dot_self_dot + nav_speedOverGround, Util.kntToMs(sen.getSpeedKnots()), src.getNow(), src.getSourceRef(), src.getSession());
					}
	
					// MWV wind
					// Mega sends $IIMVW with 0-360d clockwise from bow, (relative to bow)
					// Mega value is int+'.0'
					if (evt.getSentence() instanceof MWVSentence) {
						MWVSentence sen = (MWVSentence) evt.getSentence();
						//TODO: check relative to bow or compass + sen.getSpeedUnit()
						// relative to bow
						double angle = sen.getAngle();
						if(angle>180d)angle=angle-360d;
						// signalk is -180 to 180 (in radians), negative to port, 0 is bow. 
						double aws = Math.toRadians(angle);
						
						sendDoubleAsMsg(vessels_dot_self_dot + env_wind_angleApparent, aws, src.getNow(), src.getSourceRef(), src.getSession());
						
						sendDoubleAsMsg(vessels_dot_self_dot + env_wind_speedApparent, Util.kntToMs(sen.getSpeed()), src.getNow(), src.getSourceRef(), src.getSession());
					}
					
					if (evt.getSentence() instanceof DepthSentence) {
						DepthSentence sen = (DepthSentence) evt.getSentence();
						// in meters
						sendDoubleAsMsg(vessels_dot_self_dot + env_depth_belowTransducer, sen.getDepth(), src.getNow(), src.getSourceRef(), src.getSession());
					}
				}catch (DataNotAvailableException e){
					logger.error(e.getMessage()+":"+evt.getSentence().toSentence());
					//logger.debug(e.getMessage(),e);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}


			public void readingStopped() {
			}

			public void readingStarted() {
			}

			public void readingPaused() {
			}
		};
	}

	

}
