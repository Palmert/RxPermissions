package com.tbruyelle.rxpermissions2

import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import io.reactivex.disposables.CompositeDisposable

import java.util.HashMap

import io.reactivex.subjects.PublishSubject

class RxPermissionsFragment : Fragment() {

  // Contains all the current permission requests.
  // Once granted or denied, they are removed from it.
  private val mSubjects = HashMap<String, PublishSubject<Permission>>()
  private val disposables = CompositeDisposable()
  private var mLogging: Boolean = false
  private lateinit var currentPermission: Permission

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    retainInstance = true
  }

  override fun onDestroy() {
    disposables.clear()
    super.onDestroy()
  }

  @TargetApi(Build.VERSION_CODES.M)
  internal fun requestPermissions(permissions: Array<Permission>) {
    requestPermission(permissions.iterator())
  }

  private fun requestPermission(permissionsIterator: Iterator<Permission>) {
    if (permissionsIterator.hasNext()) {
      val permission = permissionsIterator.next()
      currentPermission = permission

      if (permission.permissionRequest != null) {
        permission.permissionRequest.requestPermission(this)
      } else {
        requestPermissions(arrayOf(permission.name), PERMISSIONS_REQUEST_CODE)
      }

      val subject = mSubjects[permission.name]
      if (subject != null) {
        disposables.add(subject.subscribe { requestPermission(permissionsIterator) })
      }
    }
  }

  @TargetApi(Build.VERSION_CODES.M)
  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    if (requestCode != PERMISSIONS_REQUEST_CODE) return

    val shouldShowRequestPermissionRationale = BooleanArray(permissions.size)

    for (i in permissions.indices) {
      shouldShowRequestPermissionRationale[i] = shouldShowRequestPermissionRationale(permissions[i])
    }

    onRequestPermissionsResult(permissions, grantResults, shouldShowRequestPermissionRationale)
  }

  internal fun onRequestPermissionsResult(permissions: Array<String>, grantResults: IntArray,
      shouldShowRequestPermissionRationale: BooleanArray) {
    var i = 0
    val size = permissions.size
    while (i < size) {
      log("onRequestPermissionsResult  " + permissions[i])
      // Find the corresponding subject
      val subject = mSubjects[permissions[i]]
      if (subject == null) {
        // No subject found
        Log.e(RxPermissions.TAG,
            "RxPermissions.onRequestPermissionsResult invoked but didn't find the corresponding permission request.")
        return
      }
      mSubjects.remove(permissions[i])
      val granted = grantResults[i] == PackageManager.PERMISSION_GRANTED
      subject.onNext(Permission(name = permissions[i], granted = granted, shouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale[i]))
      subject.onComplete()
      i++
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode != PERMISSIONS_REQUEST_CODE) return

    val subject = mSubjects[currentPermission.name]
    if (subject == null) {
      // No subject found
      Log.e(RxPermissions.TAG,
          "RxPermissions.onRequestPermissionsResult invoked but didn't find the corresponding permission request.")
      return
    }
    mSubjects.remove(currentPermission.name)
    val granted = currentPermission.isGranted(activity!!)
    subject.onNext(currentPermission.copy(granted = granted))
    subject.onComplete()

  }

  @TargetApi(Build.VERSION_CODES.M)
  internal fun isGranted(permission: Permission): Boolean {
    val fragmentActivity = activity ?: throw IllegalStateException("This fragment must be attached to an activity.")
    return permission.isGranted(fragmentActivity)
  }

  @TargetApi(Build.VERSION_CODES.M)
  internal fun isRevoked(permission: Permission): Boolean {
    val fragmentActivity = activity ?: throw IllegalStateException("This fragment must be attached to an activity.")
    return permission.isRevoked(fragmentActivity)
  }

  fun setLogging(logging: Boolean) {
    mLogging = logging
  }

  fun getSubjectByPermission(permission: Permission): PublishSubject<Permission>? {
    return mSubjects[permission.name]
  }

  fun containsByPermission(permission: Permission): Boolean {
    return mSubjects.containsKey(permission.name)
  }

  fun setSubjectForPermission(permission: String, subject: PublishSubject<Permission>) {
    mSubjects[permission] = subject
  }

  internal fun log(message: String) {
    if (mLogging) {
      Log.d(RxPermissions.TAG, message)
    }
  }

  companion object {
    const val PERMISSIONS_REQUEST_CODE = 42
  }

}
