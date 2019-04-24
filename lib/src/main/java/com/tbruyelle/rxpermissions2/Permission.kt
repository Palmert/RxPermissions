package com.tbruyelle.rxpermissions2

import android.annotation.TargetApi
import io.reactivex.Observable
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.FragmentActivity

data class Permission constructor(
  val name: String,
  val permissionRequest: PermissionRequest? = null,
  val granted: Boolean = false,
  val shouldShowRequestPermissionRationale: Boolean = false
){

  constructor(permissions: List<Permission>):
      this(Permission.combineName(permissions),
      null,
          Permission.combineGranted(permissions),
          Permission.combineShouldShowRequestPermissionRationale(permissions))


  @TargetApi(Build.VERSION_CODES.M)
  fun isGranted(fragmentActivity: FragmentActivity) =
      when {
        permissionRequest != null -> permissionRequest.isGranted(fragmentActivity)
        else -> fragmentActivity.checkSelfPermission(name) == PackageManager.PERMISSION_GRANTED
      }

  @TargetApi(Build.VERSION_CODES.M)
  fun isRevoked(fragmentActivity: FragmentActivity): Boolean =
      when {
        permissionRequest != null -> permissionRequest.isRevoked(fragmentActivity)
        else ->  fragmentActivity.packageManager.isPermissionRevokedByPolicy(name, fragmentActivity.packageName)
      }

  companion object {
    private  fun combineName(permissions: List<Permission>): String {
      return Observable.fromIterable(permissions)
          .map { permission -> permission.name }.collectInto(StringBuilder()) { s, s2 ->
            if (s.isEmpty()) {
              s.append(s2)
            } else {
              s.append(", ").append(s2)
            }
          }.blockingGet().toString()
    }

    private fun combineGranted(permissions: List<Permission>): Boolean {
      return Observable.fromIterable(permissions)
          .all { permission -> permission.granted }.blockingGet()
    }

    private fun combineShouldShowRequestPermissionRationale(permissions: List<Permission>): Boolean {
      return Observable.fromIterable(permissions)
          .any { permission -> permission.shouldShowRequestPermissionRationale }.blockingGet()
    }
  }

}

data class PermissionRequest(
    val requestPermission: ((RxPermissionsFragment) -> Unit),
    val isGranted: (FragmentActivity) -> Boolean,
    val isRevoked: (FragmentActivity) -> Boolean
)
