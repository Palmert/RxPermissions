package com.tbruyelle.rxpermissions.sample

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.os.Build

import com.tbruyelle.rxpermissions2.Permission
import com.tbruyelle.rxpermissions2.RxPermissions

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import io.reactivex.Observable

import org.mockito.Mockito.`when`

/**
 * Sample tests for [RxPermissions].
 */
class RxPermissionsSampleTest {

  @Mock
  private val activity: Activity? = null
  @Mock
  private val rxPermissions: RxPermissions? = null

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
  }

  @Test
  @TargetApi(Build.VERSION_CODES.M)
  fun test_permission_denied_dont_ask_again() {
    // mocks
    val permissionString = Manifest.permission.READ_PHONE_STATE
    val granted = false
    val shouldShowRequestPermissionRationale = false
    val permission =
        Permission(name = permissionString, granted = granted , shouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale)
    `when`(rxPermissions!!.requestEach(permission)).thenReturn(Observable.just(permission))
    // test
    rxPermissions.requestEach(permission).test().assertNoErrors().assertValue(permission)
  }

}
