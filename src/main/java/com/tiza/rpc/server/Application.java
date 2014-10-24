package com.tiza.rpc.server;

import EnLonLat.EnLonLat;
import com.google.protobuf.Service;
import com.googlecode.protobuf.pro.duplex.CleanShutdownHandler;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.execute.RpcServerCallExecutor;
import com.googlecode.protobuf.pro.duplex.execute.ThreadPoolCallExecutor;
import com.googlecode.protobuf.pro.duplex.server.DuplexTcpServerPipelineFactory;
import com.googlecode.protobuf.pro.duplex.timeout.RpcTimeoutChecker;
import com.googlecode.protobuf.pro.duplex.timeout.RpcTimeoutExecutor;
import com.googlecode.protobuf.pro.duplex.timeout.TimeoutChecker;
import com.googlecode.protobuf.pro.duplex.timeout.TimeoutExecutor;
import com.tiza.rpc.bean.LocationBean;
import com.tiza.rpc.bean.LocationServiceImpl;
import com.tz.earth.GetLocationUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class descriptions : Application
 * Created by Chauncey on 2014/10/22 11:23.
 */
public class Application {
    private static Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        //初始化经纬度服务
        EnLonLat.FreeDll();
        EnLonLat.InitDll();
        double lon = 126.273591D;
        double lat = 39.532681D;
        EnLonLat.getEncLonLat(lon, lat);
        // 初始化省市区服务器
        GetLocationUtil.init();
        //开始配置RPC服务器相关信息
        String host = "192.168.1.51";
        int port = 8520;
        PeerInfo serverInfo = new PeerInfo(host, port);
        RpcServerCallExecutor executor = new ThreadPoolCallExecutor(3, 1000);
        DuplexTcpServerPipelineFactory serverFactory = new DuplexTcpServerPipelineFactory(serverInfo);
        serverFactory.setRpcServerCallExecutor(executor);

        RpcTimeoutExecutor timeoutExecutor = new TimeoutExecutor(1, 5);
        RpcTimeoutChecker timeoutChecker = new TimeoutChecker();
        timeoutChecker.setTimeoutExecutor(timeoutExecutor);
        timeoutChecker.startChecking(serverFactory.getRpcClientRegistry());

        Service service = LocationBean.LocationService.newReflectiveService(new LocationServiceImpl());
        serverFactory.getRpcServiceRegistry().registerService(service);

        ServerBootstrap b = new ServerBootstrap();
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup work = new NioEventLoopGroup();
        b.group(boss, work)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_SNDBUF, 1048576)
                .option(ChannelOption.SO_RCVBUF, 1048576)
                .childOption(ChannelOption.SO_SNDBUF, 1048576)
                .childOption(ChannelOption.SO_RCVBUF, 1048576)
                .option(ChannelOption.TCP_NODELAY, true)
                .childHandler(serverFactory)
                .localAddress(host, port);

        CleanShutdownHandler shutdownHandler = new CleanShutdownHandler();
        shutdownHandler.addResource(boss);
        shutdownHandler.addResource(work);
        shutdownHandler.addResource(executor);
        shutdownHandler.addResource(timeoutChecker);
        shutdownHandler.addResource(timeoutExecutor);
        try {
            b.bind().sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("服务器端口异常", e);
        } finally {
            boss.shutdownGracefully();
            work.shutdownGracefully();
        }
    }
}
