package com.tbruyelle.rxpermissions2.sample

import android.Manifest.permission
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.SurfaceView

import com.jakewharton.rxbinding2.view.RxView
import com.tbruyelle.rxpermissions2.Permission
import com.tbruyelle.rxpermissions2.PermissionRequest
import com.tbruyelle.rxpermissions2.RxPermissions

import io.reactivex.disposables.Disposable
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import com.tbruyelle.rxpermissions2.RxPermissionsFragment


class MainActivity : AppCompatActivity() {

  private var camera: Camera? = null
  private var surfaceView: SurfaceView? = null
  private var disposable: Disposable? = null

  @SuppressLint("BatteryLife")
  @TargetApi(Build.VERSION_CODES.M)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val rxPermissions = RxPermissions(this)
    rxPermissions.setLogging(true)

    setContentView(R.layout.act_main)
    surfaceView = findViewById(R.id.surfaceView)




    val permissionRequest = PermissionRequest(
        requestPermission = { rxPermissionsFragment ->
          rxPermissionsFragment.startActivityForResult(
              Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")),
                      RxPermissionsFragment.PERMISSIONS_REQUEST_CODE)
        },
        isGranted = { activity ->
          val packageName = activity.application.applicationContext.packageName
          val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
          pm.isIgnoringBatteryOptimizations(packageName)
        },
        isRevoked = { false }
    )
    disposable = RxView.clicks(findViewById(R.id.enableCamera))
        // Ask for permissions when button is clicked
        .compose(rxPermissions.ensureEach(Permission(permission.CAMERA), Permission(permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, permissionRequest)))
        .subscribe({ permission ->
          Log.i(TAG, "Permission result $permission")
        },
        { t -> Log.e(TAG, "onError", t) },
        { Log.i(TAG, "OnComplete") })
  }

  override fun onDestroy() {
    if (disposable != null && !disposable!!.isDisposed) {
      disposable!!.dispose()
    }
    super.onDestroy()
  }

  override fun onStop() {
    super.onStop()
    releaseCamera()
  }

  private fun releaseCamera() {
    if (camera != null) {
      camera!!.release()
      camera = null
    }
  }

  companion object {

    private val TAG = "RxPermissionsSample"
  }

}