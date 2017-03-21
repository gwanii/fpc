/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.impl.dpdkdpn;


import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.IPToDecimal;
import org.opendaylight.fpc.utils.zeromq.ZMQClientSocket;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.notify.value.DownlinkDataNotification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.notify.value.DownlinkDataNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcIdentity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.threegpp.rev160803.EbiType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.threegpp.rev160803.ImsiType;

/**
 * DPDK DPN API over ZeroMQ.
 */
public class DpnAPI2 {
    private static byte CREATE_SESSION_TYPE = 0b0000_0001;
    private static byte MODIFY_DL_BEARER_TYPE = 0b0000_0010;
    private static byte DELETE_SESSION_TYPE = 0b0000_0011;
    private static byte MODIFY_UL_BEARER_TYPE = 0b0000_0100;
    private static byte CREATE_UL_BEARER_TYPE = 0b0000_0101;
    private static byte CREATE_DL_BEARER_TYPE = 0b0000_0110;
    private static byte DELETE_BEARER_TYPE = 0b0000_0110;
    private static byte HELLO = 0b0000_1000;
    private static byte BYE = 0b0000_1001;

    public static String BROADCAST_TOPIC = "0";

    ByteBuffer cs_bb = ByteBuffer.allocate(24);

    ZMQClientSocket sock;

    /**
     * Constructor
     * @param sock - ZeroMQ Socket
     */
    public DpnAPI2(ZMQClientSocket sock) {
        this.sock = sock;
    }

    /**
     * Creates Mobility Session
     * @param dpn - DPN
     * @param imsi - IMSI
     * @param ue_ip - Session IP Address
     * @param default_ebi - Default EBI
     * @param s1u_sgw_gtpu_ipv4 - SGW GTP-U IPv4 Address
     * @param s1u_sgw_gtpu_teid - SGW GTP-U TEID
     */
    public void create_session(
            String dpn,
            BigInteger imsi,
            Ipv4Address ue_ip,
            Short default_ebi,
            Ipv4Address s1u_sgw_gtpu_ipv4,
            Long s1u_sgw_gtpu_teid  // Although this is intended to be a Uint32
            //UlTftTable ul_tft_table
            )
    {
        create_session(dpn, imsi, IPToDecimal.ipv4ToLong(ue_ip.getValue()),
                default_ebi, s1u_sgw_gtpu_ipv4, s1u_sgw_gtpu_teid);
    }

    /**
     * Create Mobility Session.
     * @param dpn - DPN
     * @param imsi - IMSI
     * @param ue_ip - Session IPv4 Address
     * @param lbi - Linked Bearer Identifier
     * @param s1u_sgw_gtpu_ipv4 - SGW GTP-U IPv4 Address
     * @param s1u_sgw_gtpu_teid - SGW GTP-U TEID
     */
    public void create_session(
            String dpn,
            BigInteger imsi,
            Long ue_ip,
            Short lbi,
            Ipv4Address s1u_sgw_gtpu_ipv4,
            Long s1u_sgw_gtpu_teid  // Although this is intended to be a Uint32
            //UlTftTable ul_tft_table
            )
    {
        //Create byte[] from arguments
        ByteBuffer bb = ByteBuffer.allocate(24);
        bb.put(dpn.getBytes())
            .put(CREATE_SESSION_TYPE)
            .put(toUint64(imsi))
            .put(toUint8(lbi))
            .put(toUint32(ue_ip))
            .put(toUint32(s1u_sgw_gtpu_teid))
            .put(toUint32(IPToDecimal.ipv4ToLong(s1u_sgw_gtpu_ipv4.getValue())));

        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Modify Downlink Bearer
     * @param dpn - DPN
     * @param s1u_sgw_gtpu_teid - SGW GTP-U TEID
     * @param s1u_enb_gtpu_ipv4 - ENodeB GTP-U IPv4 Address
     * @param s1u_enb_gtpu_teid - ENodeB GTP-U TEID
     */
    public void modify_bearer_dl(
            String dpn,
            Long s1u_sgw_gtpu_teid,
            Ipv4Address s1u_enb_gtpu_ipv4,
            Long s1u_enb_gtpu_teid)
    {
        ByteBuffer bb = ByteBuffer.allocate(15);
        bb.put(dpn.getBytes())
                .put(MODIFY_DL_BEARER_TYPE)
                .put(toUint32(IPToDecimal.ipv4ToLong(s1u_enb_gtpu_ipv4.getValue())))
                .put(toUint32(s1u_enb_gtpu_teid))
                .put(toUint32(s1u_sgw_gtpu_teid));
        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Delete Mobility Session.
     * @param dpn - DPN
     * @param del_default_ebi - Default EBI
     * @param s1u_sgw_gtpu_teid - SGW GTP-U TEID
     */
    public void delete_session(
            String dpn,
            Short del_default_ebi,
            Long s1u_sgw_gtpu_teid)
    {
        ByteBuffer bb = ByteBuffer.allocate(7);
        bb.put(dpn.getBytes())
            .put(DELETE_SESSION_TYPE)
            .put(toUint8(del_default_ebi))
            .put(toUint32(s1u_sgw_gtpu_teid));
        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Create Uplink Bearer.
     * @param dpn - DPN
     * @param imsi - IMSI
     * @param default_ebi - Default EBI
     * @param dedicated_ebi - Dedicated EBI
     * @param s1u_sgw_gtpu_ipv4 - SGW GTP-U IPv4 Address
     * @param s1u_sgw_gtpu_teid - SGW GTP-U TEID
     * @param ul_tft_table - Uplink TFT
     */
    public void create_bearer_ul(
            String dpn,
            BigInteger imsi,
            Short default_ebi,
            Short dedicated_ebi,
            Ipv4Address s1u_sgw_gtpu_ipv4,
            Long s1u_sgw_gtpu_teid,
            Object ul_tft_table)
    {
        ByteBuffer bb = ByteBuffer.allocate(21);
        bb.put(dpn.getBytes())
            .put(CREATE_UL_BEARER_TYPE)
            .put(toUint64(imsi))
            .put(toUint8(default_ebi))
            .put(toUint8(dedicated_ebi))
            .put(toUint32(IPToDecimal.ipv4ToLong(s1u_sgw_gtpu_ipv4.getValue())))
            .put(toUint32(s1u_sgw_gtpu_teid));

        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Create Downlink Bearer.
     * @param dpn - DPN
     * @param dedicated_ebi - Default EBI
     * @param s1u_sgw_gtpu_teid - SGW GTP-U TEID
     * @param s1u_enb_gtpu_ipv4 - ENodeB GTP-U IPv4 Address
     * @param s1u_enb_gtpu_teid - ENodeB GTP-U TEID
     */
    public void create_bearer_dl(
            String dpn,
            Short  dedicated_ebi,
            Long s1u_sgw_gtpu_teid,
            Ipv4Address s1u_enb_gtpu_ipv4,
            Long s1u_enb_gtpu_teid)
            //DlTft dl_tft_table)
    {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.put(dpn.getBytes())
            .put(CREATE_DL_BEARER_TYPE)
            .put(toUint8(dedicated_ebi))
            .put(toUint32(s1u_sgw_gtpu_teid))
            .put(toUint32(IPToDecimal.ipv4ToLong(s1u_enb_gtpu_ipv4.getValue())))
            .put(toUint32(s1u_enb_gtpu_teid));

        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Modify Downlink Bearer.
     * @param dpn - DPN
     * @param s1u_sgw_gtpu_ipv4 - SGW GTP-U IPv4 Address
     * @param s1u_enb_gtpu_teid - ENodeB TEID
     * @param s1u_sgw_gtpu_teid - SGW GTP-U TEID
     * @param dl_tft_table - Downlink TFT
     */
    public void modify_bearer_dl(
            String dpn,
            Ipv4Address s1u_sgw_gtpu_ipv4,
            Long s1u_enb_gtpu_teid,
            Long s1u_sgw_gtpu_teid,
            Object dl_tft_table)
    {
        ByteBuffer bb = ByteBuffer.allocate(15);
        bb.put(dpn.getBytes())
            .put(MODIFY_DL_BEARER_TYPE)
            .put(toUint32(IPToDecimal.ipv4ToLong(s1u_sgw_gtpu_ipv4.getValue())))
            .put(toUint32(s1u_enb_gtpu_teid))
            .put(toUint32(s1u_sgw_gtpu_teid));

        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Modify Uplink Bearer.
     * @param dpn - DPN
     * @param s1u_enb_gtpu_ipv4 - ENodeB GTP-U IPv4 Address
     * @param s1u_enb_gtpu_teid - ENodeB GTP-U TEID
     * @param s1u_sgw_gtpu_teid - SGW GTP-U TEID
     * @param dl_tft_table - Downlink TFT
     */
    public void modify_bearer_ul(
            String dpn,
            Ipv4Address s1u_enb_gtpu_ipv4,
            Long s1u_enb_gtpu_teid,
            Long s1u_sgw_gtpu_teid,
            Object dl_tft_table)
    {
        ByteBuffer bb = ByteBuffer.allocate(15);
        bb.put(dpn.getBytes())
            .put(MODIFY_UL_BEARER_TYPE)
            .put(toUint32(IPToDecimal.ipv4ToLong(s1u_enb_gtpu_ipv4.getValue())))
            .put(toUint32(s1u_enb_gtpu_teid))
            .put(toUint32(s1u_sgw_gtpu_teid));

        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Hello Message
     * @param dpn - DPN
     * @param controllerId - Controller Id
     */
    public void hello(
            String dpn,
            String controllerId)
    {
        ByteBuffer bb = ByteBuffer.allocate(3);
        bb.put(dpn.getBytes())
            .put(HELLO)
            .put(controllerId.getBytes());

        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Goodbye Message.
     * @param dpn - DPN
     * @param controllerId - Controller Id
     */
    public void bye(
            String dpn,
            String controllerId)
    {
        ByteBuffer bb = ByteBuffer.allocate(3);
        bb.put(dpn.getBytes())
            .put(BYE)
            .put(controllerId.getBytes());

        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Delete Bearer.
     * @param dpn - DPN
     * @param s1u_sgw_gtpu_teid - SGW GTP-U TEID
     */
    public void delete_bearer(
            String dpn,
            Long s1u_sgw_gtpu_teid)
    {
        ByteBuffer bb = ByteBuffer.allocate(7);
        bb.put(dpn.getBytes())
            .put(DELETE_BEARER_TYPE)
            .put(toUint32(s1u_sgw_gtpu_teid));

        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Short to Byte
     * @param value - Short
     * @return byte value
     */
    public byte toUint8(Short value) {
        return value.byteValue();
    }

    /**
     * Short to byte array
     * @param value - Short
     * @return byte array
     */
    public byte[] toUint16(Short value) {
        return new byte[]{(byte)(value>>>8),(byte)(value&0xFF)};
    }

    /**
     * Long to byte array.
     * @param value - long
     * @return byte array
     */
    public byte[] toUint32(long value) {
        return new byte[]{(byte)(value>>>24),(byte)(value>>>16),(byte)(value>>>8),(byte)(value&0xFF)};
    }

    /**
     * BigInteger to byte array.
     * @param value - BigInteger
     * @return byte array
     */
    public byte[] toUint64(BigInteger value) {
        return new byte[]{value.shiftRight(56).byteValue(),value.shiftRight(48).byteValue(),value.shiftRight(40).byteValue(),
                value.shiftRight(32).byteValue(),value.shiftRight(24).byteValue(),value.shiftRight(16).byteValue(),
                value.shiftRight(8).byteValue(),value.and(BigInteger.valueOf(0xFF)).byteValue()};
    }
}