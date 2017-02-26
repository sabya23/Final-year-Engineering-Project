package tools;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by sanchita on 2/5/15.
 */
public class HardwareControl {
    private PackageManager packageManager = null;
    private Context context = null;
    private android.hardware.Camera camera = null;
    private boolean isFlashOn = false;

    public HardwareControl(Context context) {
        this.context = context;
        packageManager = context.getPackageManager();
    }

    public boolean turnFlashOn()
    {
        if(!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA))
        {
            Toast.makeText(context, "Your device does not support camera", Toast.LENGTH_SHORT).show();
            return false;
        }
        else
        {
            if(!isFlashOn) {
                try {
                    if(camera==null) {
                        camera = android.hardware.Camera.open();
                    }
                    android.hardware.Camera.Parameters parameters = camera.getParameters();
                    parameters.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(parameters);
                    camera.startPreview();
                    isFlashOn = true;
                    Toast.makeText(context, "FLASH TURNED ON!!", Toast.LENGTH_LONG).show();
                    return true;
                }
                catch (Exception e)
                {
                    Log.d("ERROR IN HW", e.getMessage());
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            else
            {
                return false;
            }
        }
    }

    public boolean turnFlashOff()
    {
        if(!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA))
        {
            Toast.makeText(context,"Your device does not support camera",Toast.LENGTH_LONG).show();
            return false;
        }
        else
        {
            if(isFlashOn) {

                try {
                    android.hardware.Camera.Parameters parameters = camera.getParameters();
                    parameters.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(parameters);
                    camera.stopPreview();
                    isFlashOn = false;
                    Toast.makeText(context, "FLASH TURNED OFF!!", Toast.LENGTH_LONG).show();
                    return true;
                }
                catch (Exception e)
                {
                    Log.d("ERROR IN HW",e.getMessage());
                    return false;
                }
            }
            else
            {
                return false;
            }
        }
    }

    public void destroy()
    {
        if(camera!=null)
        {
            camera.release();
        }
    }
}