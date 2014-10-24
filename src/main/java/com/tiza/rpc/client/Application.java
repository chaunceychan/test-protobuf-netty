package com.tiza.rpc.client;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.Service;
import com.google.protobuf.ServiceException;
import com.googlecode.protobuf.pro.duplex.CleanShutdownHandler;
import com.googlecode.protobuf.pro.duplex.ClientRpcController;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.RpcClientChannel;
import com.googlecode.protobuf.pro.duplex.client.DuplexTcpClientPipelineFactory;
import com.googlecode.protobuf.pro.duplex.execute.RpcServerCallExecutor;
import com.googlecode.protobuf.pro.duplex.execute.ThreadPoolCallExecutor;
import com.googlecode.protobuf.pro.duplex.timeout.RpcTimeoutChecker;
import com.googlecode.protobuf.pro.duplex.timeout.RpcTimeoutExecutor;
import com.googlecode.protobuf.pro.duplex.timeout.TimeoutChecker;
import com.googlecode.protobuf.pro.duplex.timeout.TimeoutExecutor;
import com.tiza.rpc.bean.LocationBean;
import com.tiza.rpc.bean.LocationServiceImpl;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.util.List;

/**
 * Class descriptions : Application
 * Created by Chauncey on 2014/10/22 16:25.
 */
public class Application {
    public static void main(String[] args) {
        PeerInfo client = new PeerInfo("192.168.1.24", 7530);
        String host = "192.168.1.51";
        int port = 8520;
        PeerInfo server = new PeerInfo(host, port);

        RpcServerCallExecutor executor = new ThreadPoolCallExecutor(3, 100);
        DuplexTcpClientPipelineFactory clientFactory = new DuplexTcpClientPipelineFactory();
        clientFactory.setClientInfo(client);
        clientFactory.setConnectResponseTimeoutMillis(10000);
        clientFactory.setRpcServerCallExecutor(executor);
        clientFactory.setCompression(true);

        RpcTimeoutExecutor timeoutExecutor = new TimeoutExecutor(1, 5);
        RpcTimeoutChecker checker = new TimeoutChecker();
        checker.setTimeoutExecutor(timeoutExecutor);
        checker.startChecking(clientFactory.getRpcClientRegistry());


        Service service = LocationBean.LocationService.newReflectiveService(new LocationServiceImpl());
        clientFactory.getRpcServiceRegistry().registerService(service);

        Bootstrap b = new Bootstrap();
        NioEventLoopGroup work = new NioEventLoopGroup();
        b.group(work)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_SNDBUF, 1048576)
                .option(ChannelOption.SO_RCVBUF, 1048576)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(clientFactory);


        CleanShutdownHandler shutdownHandler = new CleanShutdownHandler();
        shutdownHandler.addResource(executor);
        shutdownHandler.addResource(checker);
        shutdownHandler.addResource(timeoutExecutor);
        shutdownHandler.addResource(work);

        try {
            clientFactory.peerWith(server, b);
            LocationBean.Location request = LocationBean.Location.newBuilder().setElng(117.1234).setElat(34.5678).build();
            long start = System.currentTimeMillis();
            int i = 0;
            while (i < 100) {
                List<RpcClientChannel> channels = clientFactory.getRpcClientRegistry().getAllClients();
                if (channels.size() > 0) {
                    for (final RpcClientChannel channel : channels) {
                        LocationBean.LocationService.BlockingInterface anInterface = LocationBean.LocationService.newBlockingStub(channel);
                        final ClientRpcController controller = channel.newRpcController();
                        try {
                            LocationBean.Location response = anInterface.getRealLocation(controller, request);
//                            System.out.println("location{elng:" + response.getElng() + ",elat:" + response.getElat() + ",province:" + response.getProvince() + ",city:" + response.getCity() + ",area:" + response.getArea() + "}");
                        } catch (ServiceException e) {
                            e.printStackTrace();
                        }
                    }
                }
                i++;
            }
            System.out.println("消耗了:" + (System.currentTimeMillis() - start) + "ms");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
