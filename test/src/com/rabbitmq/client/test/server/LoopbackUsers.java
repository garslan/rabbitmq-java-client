package com.rabbitmq.client.test.server;

import com.rabbitmq.client.AuthenticationFailureException;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.tools.Host;
import junit.framework.TestCase;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class LoopbackUsers extends TestCase {
    @Override
    protected void setUp() throws IOException {
        Host.rabbitmqctl("add_user test test");
        Host.rabbitmqctl("set_permissions test '.*' '.*' '.*'");
    }

    @Override
    protected void tearDown() throws IOException {
        Host.rabbitmqctl("delete_user test");
    }

    public void testLoopback() throws IOException {
        String addr = findRealIPAddress().getHostAddress();
        assertGuestFail(addr);
        Host.rabbitmqctl("eval 'application:set_env(rabbit, loopback_users, []).'");
        assertGuestSucceed(addr);
        Host.rabbitmqctl("eval 'application:set_env(rabbit, loopback_users, [<<\"guest\">>]).'");
        assertGuestFail(addr);
    }

    private void assertGuestSucceed(String addr) throws IOException {
        succeedConnect("guest", addr);
        succeedConnect("guest", "localhost");
        succeedConnect("test", addr);
        succeedConnect("test", "localhost");
    }

    private void assertGuestFail(String addr) throws IOException {
        failConnect("guest", addr);
        succeedConnect("guest", "localhost");
        succeedConnect("test", addr);
        succeedConnect("test", "localhost");
    }

    private void succeedConnect(String name, String addr) throws IOException {
        getFactory(name, addr).newConnection().close();
    }

    private void failConnect(String name, String addr) throws IOException {
        try {
            getFactory(name, addr).newConnection();
            fail();
        }
        catch (AuthenticationFailureException e) {
            // success
        }
    }

    private ConnectionFactory getFactory(String name, String addr) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(name);
        factory.setPassword(name);
        factory.setHost(addr);
        return factory;
    }

    // Find the first IP address of a network interface that is up, not loopback, not point to point (e.g. VPN thing)
    private static InetAddress findRealIPAddress() throws SocketException {
        throw new RuntimeException("this test will be enabled once we stop supporting Java 1.5");
//        Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
//        while (ifs.hasMoreElements()) {
//            NetworkInterface nif = ifs.nextElement();
//            if (nif.isUp() && !nif.isPointToPoint() && !nif.isLoopback() && !nif.isVirtual()) {
//                Enumeration<InetAddress> addrs = nif.getInetAddresses();
//                while (addrs.hasMoreElements()) {
//                    InetAddress addr = addrs.nextElement();
//                    if (addr instanceof Inet4Address) {
//                        return addr;
//                    }
//                }
//            }
//        }
//        throw new RuntimeException("Could not determine real network address");
    }
}
