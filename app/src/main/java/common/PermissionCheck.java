package common;

import android.Manifest;
import android.app.Activity;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.List;

public class PermissionCheck{
    Activity mActivity;
    String permissions[] = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    public  PermissionCheck(Activity act){
        this.mActivity=act;
    }

    public void setPermission(){
        TedPermission.with(mActivity)
                .setPermissionListener(permissionListener)
                .setRationaleMessage("To use the weather widget, you need to set up location permissions.")
                .setDeniedMessage("Please set permissions.")
                .setPermissions(permissions)
                .check();
    }
    PermissionListener permissionListener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            try{
                LocationPosition.act= mActivity;
                LocationPosition.setPosition(mActivity);
                if(LocationPosition.lng==0.0){
                    LocationPosition.setPosition(mActivity);
                }
            }catch(Exception e){

            }

        }
        @Override
        public void onPermissionDenied(List<String> deniedPermissions) {

        }
    };
}
