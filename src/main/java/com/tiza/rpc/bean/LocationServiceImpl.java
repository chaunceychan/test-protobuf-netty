package com.tiza.rpc.bean;

import EnLonLat.EnLonLat;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.tz.earth.GetLocationUtil;

/**
 * Class descriptions : LocationServiceImpl
 * Created by Chauncey on 2014/10/22 14:42.
 */
public class LocationServiceImpl implements LocationBean.LocationService.Interface {
    @Override
    public void getRealLocation(RpcController controller, LocationBean.Location request, RpcCallback<LocationBean.Location> done) {
        double d1 = request.getElng();
        double d2 = request.getElat();
        try {
            String encLonLat = EnLonLat.getEncLonLat(request.getElng(), request.getElat());
            int idx = encLonLat.indexOf(",");
            d1 = Double.parseDouble(encLonLat.substring(0, idx));
            d2 = Double.parseDouble(encLonLat.substring(idx + 1));
        } catch (Exception e) {
            e.printStackTrace();
        }
        String str = GetLocationUtil.getLocation(d1, d2);
        String[] arr = str.split(" ");
        LocationBean.Location response = LocationBean.Location.newBuilder().setElng(d1).setElat(d2).setProvince(arr[0]).setCity(arr[1]).setArea(arr[2]).build();
        done.run(response);
    }
}
