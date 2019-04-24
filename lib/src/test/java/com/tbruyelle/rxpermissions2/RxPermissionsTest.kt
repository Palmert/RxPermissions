/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tbruyelle.rxpermissions2

import android.Manifest
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.FragmentActivity
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyVararg
import com.nhaarman.mockitokotlin2.given
import com.nhaarman.mockitokotlin2.spy

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import io.reactivex.Observable
import io.reactivex.observers.TestObserver

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = [Build.VERSION_CODES.M])
class RxPermissionsTest {

  private var mActivity: FragmentActivity? = null

  private var mRxPermissions: RxPermissions? = null

  @Before
  fun setup() {
    val activityController = Robolectric.buildActivity(FragmentActivity::class.java)
    mActivity = spy(activityController.setup().get())
    mRxPermissions = spy(RxPermissions(mActivity!!))
    mRxPermissions!!.mRxPermissionsFragment = spy(mRxPermissions!!.mRxPermissionsFragment)
    val rxPermissionsFragment = spy(mRxPermissions!!.mRxPermissionsFragment.get())
    given(rxPermissionsFragment.activity).willReturn(mActivity)
    given(mRxPermissions!!.mRxPermissionsFragment.get()).willReturn(rxPermissionsFragment)
    given { rxPermissionsFragment.isGranted(any()) }.willReturn(false)
//    // Default deny all permissions
//    given(mRxPermissions!!.isGranted(any())).willReturn(false)
//    // Default no revoked permissions
//    given(mRxPermissions!!.isRevoked(any())).willReturn(false)
  }

  private fun trigger(): Observable<Any> {
    return Observable.just(RxPermissions.TRIGGER)
  }

  @Test
  @TargetApi(Build.VERSION_CODES.M)
  fun subscription_preM() {
    val sub = TestObserver<Boolean>()
    val permission = Permission(Manifest.permission.READ_PHONE_STATE)
    given(mRxPermissions!!.isGranted(permission)).willReturn(true)

    trigger().compose(mRxPermissions!!.ensure(permission)).subscribe(sub)

    sub.assertNoErrors()
    sub.assertTerminated()
    sub.assertValue(true)
  }

  @Test
  @TargetApi(Build.VERSION_CODES.M)
  fun subscription_granted() {
    val sub = TestObserver<Boolean>()
    val permission = Permission(Manifest.permission.READ_PHONE_STATE)
    given(mRxPermissions!!.isGranted(permission)).willReturn(false)
    val result = intArrayOf(PackageManager.PERMISSION_GRANTED)

    trigger().compose(mRxPermissions!!.ensure(permission)).subscribe(sub)
    mRxPermissions!!.onRequestPermissionsResult(arrayOf(permission.name), result)

    sub.assertNoErrors()
    sub.assertTerminated()
    sub.assertValue(true)
  }

  @Test
  @TargetApi(Build.VERSION_CODES.M)
  fun eachSubscription_granted() {
    val sub = TestObserver<Permission>()
    val permission = Permission(Manifest.permission.READ_PHONE_STATE)
    given(mRxPermissions!!.isGranted(permission)).willReturn(false)
    val result = intArrayOf(PackageManager.PERMISSION_GRANTED)

    trigger().compose(mRxPermissions!!.ensureEach(permission)).subscribe(sub)
    mRxPermissions!!.onRequestPermissionsResult(arrayOf(permission.name), result)

    sub.assertNoErrors()
    sub.assertTerminated()
    sub.assertValue(Permission(permission.name, granted = true))
  }

  @Test
  @TargetApi(Build.VERSION_CODES.M)
  fun eachSubscriptionCombined_granted() {
    val sub = TestObserver<Permission>()
    val permission = Permission(Manifest.permission.READ_PHONE_STATE)
    given(mRxPermissions!!.isGranted(permission)).willReturn(false)
    val result = intArrayOf(PackageManager.PERMISSION_GRANTED)

    trigger().compose(mRxPermissions!!.ensureEachCombined(permission)).subscribe(sub)
    mRxPermissions!!.onRequestPermissionsResult(arrayOf(permission.name), result)

    sub.assertNoErrors()
    sub.assertTerminated()
    sub.assertValue(Permission(permission.name, granted = true))
  }

  @Test
  @TargetApi(Build.VERSION_CODES.M)
  fun eachSubscription_preM() {
    val sub = TestObserver<Permission>()
    val permission = Permission(Manifest.permission.READ_PHONE_STATE)
    given(mRxPermissions!!.isGranted(permission)).willReturn(true)

    trigger().compose(mRxPermissions!!.ensureEach(permission)).subscribe(sub)

    sub.assertNoErrors()
    sub.assertTerminated()
    sub.assertValue(Permission(permission.name, granted = true))
  }

  @Test
  @TargetApi(Build.VERSION_CODES.M)
  fun eachSubscriptionCombined_preM() {
    val sub = TestObserver<Permission>()
    val permission = Permission(Manifest.permission.READ_PHONE_STATE)
    given(mRxPermissions!!.isGranted(permission)).willReturn(true)

    trigger().compose(mRxPermissions!!.ensureEachCombined(permission)).subscribe(sub)

    sub.assertNoErrors()
    sub.assertTerminated()
    sub.assertValue(Permission(permission.name, granted = true))
  }

  @Test
  @TargetApi(Build.VERSION_CODES.M)
  fun subscription_alreadyGranted() {
    val sub = TestObserver<Boolean>()
    val permission = Permission(Manifest.permission.READ_PHONE_STATE)
    given(mRxPermissions!!.isGranted(permission)).willReturn(true)

    trigger().compose(mRxPermissions!!.ensure(permission)).subscribe(sub)

    sub.assertNoErrors()
    sub.assertTerminated()
    sub.assertValue(true)
  }

  @Test
  @TargetApi(Build.VERSION_CODES.M)
  fun subscription_denied() {
    val sub = TestObserver<Boolean>()
    val permission = Permission(Manifest.permission.READ_PHONE_STATE)
    given(mRxPermissions!!.isGranted(permission)).willReturn(false)
    val result = intArrayOf(PackageManager.PERMISSION_DENIED)

    trigger().compose(mRxPermissions!!.ensure(permission)).subscribe(sub)
    mRxPermissions!!.onRequestPermissionsResult(arrayOf(permission.name), result)

    sub.assertNoErrors()
    sub.assertTerminated()
    sub.assertValue(false)
  }

  @Test
  @TargetApi(Build.VERSION_CODES.M)
  fun eachSubscription_denied() {
    val sub = TestObserver<Permission>()
    val permission = Permission(Manifest.permission.READ_PHONE_STATE)
    given(mRxPermissions!!.isGranted(permission)).willReturn(false)
    val result = intArrayOf(PackageManager.PERMISSION_DENIED)

    trigger().compose(mRxPermissions!!.ensureEach(permission)).subscribe(sub)
    mRxPermissions!!.onRequestPermissionsResult(arrayOf(permission.name), result)

    sub.assertNoErrors()
    sub.assertTerminated()
    sub.assertValue(Permission(permission.name, granted = false))
  }

  @Test
  @TargetApi(Build.VERSION_CODES.M)
  fun eachSubscriptionCombined_denied() {
    val sub = TestObserver<Permission>()
    val permission = Permission(Manifest.permission.READ_PHONE_STATE)
    given(mRxPermissions!!.isGranted(permission)).willReturn(false)
    val result = intArrayOf(PackageManager.PERMISSION_DENIED)

    trigger().compose(mRxPermissions!!.ensureEachCombined(permission)).subscribe(sub)
    mRxPermissions!!.onRequestPermissionsResult(arrayOf(permission.name), result)

    sub.assertNoErrors()
    sub.assertTerminated()
    sub.assertValue(Permission(permission.name, granted = false))
  }

  @Test
  @TargetApi(Build.VERSION_CODES.M)
  fun subscription_revoked() {
    val sub = TestObserver<Boolean>()
    val permission = Permission(Manifest.permission.READ_PHONE_STATE)
    given(mRxPermissions!!.isRevoked(permission)).willReturn(true)

    trigger().compose(mRxPermissions!!.ensure(permission)).subscribe(sub)

    sub.assertNoErrors()
    sub.assertTerminated()
    sub.assertValue(false)
  }

  @Test
  @TargetApi(Build.VERSION_CODES.M)
  fun eachSubscription_revoked() {
    val sub = TestObserver<Permission>()
    val permission = Permission(Manifest.permission.READ_PHONE_STATE)
    given(mRxPermissions!!.isRevoked(permission)).willReturn(true)

    trigger().compose(mRxPermissions!!.ensureEach(permission)).subscribe(sub)

    sub.assertNoErrors()
    sub.assertTerminated()
    sub.assertValue(Permission(permission.name, granted = false))
  }

  @Test
  @TargetApi(Build.VERSION_CODES.M)
  fun eachSubscriptionCombined_revoked() {
    val sub = TestObserver<Permission>()
    val permission = Permission(Manifest.permission.READ_PHONE_STATE)
    given(mRxPermissions!!.isRevoked(permission)).willReturn(true)

    trigger().compose(mRxPermissions!!.ensureEachCombined(permission)).subscribe(sub)

    sub.assertNoErrors()
    sub.assertTerminated()
    sub.assertValue(Permission(permission.name, granted = false))
  }

  @Test
  @TargetApi(Build.VERSION_CODES.M)
  fun subscription_severalPermissions_granted() {
    val sub = TestObserver<Boolean>()
    val permissions = arrayOf(Permission(Manifest.permission.READ_PHONE_STATE), Permission(Manifest.permission.CAMERA))
    given(mRxPermissions!!.isGranted(anyVararg())).willReturn(false)
    val result = intArrayOf(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED)

    trigger().compose(mRxPermissions!!.ensure(*permissions)).subscribe(sub)
    mRxPermissions!!.onRequestPermissionsResult(permissions.map { it.name }.toTypedArray(), result)

    sub.assertNoErrors()
    sub.assertTerminated()
    sub.assertValue(true)
  }

  @Test
  @TargetApi(Build.VERSION_CODES.M)
  fun eachSubscription_severalPermissions_granted() {
    val sub = TestObserver<Permission>()
    val permissions = arrayOf(Permission(Manifest.permission.READ_PHONE_STATE), Permission(Manifest.permission.CAMERA))
    given(mRxPermissions!!.isGranted(anyVararg())).willReturn(false)
    val result = intArrayOf(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED)

    trigger().compose(mRxPermissions!!.ensureEach(*permissions)).subscribe(sub)
    mRxPermissions!!.onRequestPermissionsResult(permissions.map { it.name }.toTypedArray(), result)

    sub.assertNoErrors()
    sub.assertTerminated()
    sub.assertValues(Permission(permissions.first().name, granted = true), Permission(permissions[1].name, granted = true))
  }

  @Test
  @TargetApi(Build.VERSION_CODES.M)
  fun eachSubscriptionCombined_severalPermissions_granted() {
    val sub = TestObserver<Permission>()
    val permissions = arrayOf(Permission(Manifest.permission.READ_PHONE_STATE), Permission(Manifest.permission.CAMERA))
    given(mRxPermissions!!.isGranted(anyVararg())).willReturn(false)
    val result = intArrayOf(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED)

    trigger().compose(mRxPermissions!!.ensureEachCombined(*permissions)).subscribe(sub)
    mRxPermissions!!.onRequestPermissionsResult(permissions.map { it.name }.toTypedArray(), result)

    sub.assertNoErrors()
    sub.assertTerminated()
    sub.assertValues(Permission(permissions[0].name, granted = true), Permission(permissions[1].name, granted = true))
  }

//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun subscription_severalPermissions_oneDenied() {
//    val sub = TestObserver<Boolean>()
//    val permissions = arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA)
//    given(mRxPermissions!!.isGranted(Matchers.anyVararg<String>())).willReturn(false)
//    val result = intArrayOf(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_DENIED)
//
//    trigger().compose(mRxPermissions!!.ensure(*permissions)).subscribe(sub)
//    mRxPermissions!!.onRequestPermissionsResult(permissions, result)
//
//    sub.assertNoErrors()
//    sub.assertTerminated()
//    sub.assertValue(false)
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun subscription_severalPermissions_oneRevoked() {
//    val sub = TestObserver<Boolean>()
//    val permissions = arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA)
//    given(mRxPermissions!!.isGranted(Matchers.anyVararg<String>())).willReturn(false)
//    given(mRxPermissions!!.isRevoked(Manifest.permission.CAMERA)).willReturn(true)
//
//    trigger().compose(mRxPermissions!!.ensure(*permissions)).subscribe(sub)
//    mRxPermissions!!.onRequestPermissionsResult(
//        arrayOf(Manifest.permission.READ_PHONE_STATE),
//        intArrayOf(PackageManager.PERMISSION_GRANTED))
//
//    sub.assertNoErrors()
//    sub.assertTerminated()
//    sub.assertValue(false)
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun eachSubscription_severalPermissions_oneAlreadyGranted() {
//    val sub = TestObserver<Permission>()
//    val permissions = arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA)
//    given(mRxPermissions!!.isGranted(Matchers.anyVararg<String>())).willReturn(false)
//    given(mRxPermissions!!.isGranted(Manifest.permission.CAMERA)).willReturn(true)
//
//    trigger().compose(mRxPermissions!!.ensureEach(*permissions)).subscribe(sub)
//    mRxPermissions!!.onRequestPermissionsResult(
//        arrayOf(Manifest.permission.READ_PHONE_STATE),
//        intArrayOf(PackageManager.PERMISSION_GRANTED))
//
//    sub.assertNoErrors()
//    sub.assertTerminated()
//    sub.assertValues(Permission(permission.names[0], true), Permission(permission.names[1], true))
//    val requestedPermissions = ArgumentCaptor.forClass(Array<String>::class.java)
//    verify<RxPermissions>(mRxPermissions).requestPermissionsFromFragment(requestedPermissions.capture())
//    assertEquals(1, requestedPermissions.value.size.toLong())
//    assertEquals(Manifest.permission.READ_PHONE_STATE, requestedPermissions.value[0])
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun eachSubscriptionCombined_severalPermissions_oneAlreadyGranted() {
//    val sub = TestObserver<Permission>()
//    val permissions = arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA)
//    given(mRxPermissions!!.isGranted(Matchers.anyVararg<String>())).willReturn(false)
//    given(mRxPermissions!!.isGranted(Manifest.permission.CAMERA)).willReturn(true)
//
//    trigger().compose(mRxPermissions!!.ensureEachCombined(*permissions)).subscribe(sub)
//    mRxPermissions!!.onRequestPermissionsResult(
//        arrayOf(Manifest.permission.READ_PHONE_STATE),
//        intArrayOf(PackageManager.PERMISSION_GRANTED))
//
//    sub.assertNoErrors()
//    sub.assertTerminated()
//    sub.assertValues(Permission(permission.names[0] + ", " + permissions[1], true))
//    val requestedPermissions = ArgumentCaptor.forClass(Array<String>::class.java)
//    verify<RxPermissions>(mRxPermissions).requestPermissionsFromFragment(requestedPermissions.capture())
//    assertEquals(1, requestedPermissions.value.size.toLong())
//    assertEquals(Manifest.permission.READ_PHONE_STATE, requestedPermissions.value[0])
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun eachSubscription_severalPermissions_oneDenied() {
//    val sub = TestObserver<Permission>()
//    val permissions = arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA)
//    given(mRxPermissions!!.isGranted(Matchers.anyVararg<String>())).willReturn(false)
//    val result = intArrayOf(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_DENIED)
//
//    trigger().compose(mRxPermissions!!.ensureEach(*permissions)).subscribe(sub)
//    mRxPermissions!!.onRequestPermissionsResult(permissions, result)
//
//    sub.assertNoErrors()
//    sub.assertTerminated()
//    sub.assertValues(Permission(permission.names[0], true), Permission(permission.names[1], false))
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun eachSubscriptionCombined_severalPermissions_oneDenied() {
//    val sub = TestObserver<Permission>()
//    val permissions = arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA)
//    given(mRxPermissions!!.isGranted(Matchers.anyVararg<String>())).willReturn(false)
//    val result = intArrayOf(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_DENIED)
//
//    trigger().compose(mRxPermissions!!.ensureEachCombined(*permissions)).subscribe(sub)
//    mRxPermissions!!.onRequestPermissionsResult(permissions, result)
//
//    sub.assertNoErrors()
//    sub.assertTerminated()
//    sub.assertValues(Permission(permission.names[0] + ", " + permissions[1], false))
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun eachSubscription_severalPermissions_oneRevoked() {
//    val sub = TestObserver<Permission>()
//    val permissions = arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA)
//    given(mRxPermissions!!.isGranted(Matchers.anyVararg<String>())).willReturn(false)
//    given(mRxPermissions!!.isRevoked(Manifest.permission.CAMERA)).willReturn(true)
//
//    trigger().compose(mRxPermissions!!.ensureEach(*permissions)).subscribe(sub)
//    mRxPermissions!!.onRequestPermissionsResult(
//        arrayOf(Manifest.permission.READ_PHONE_STATE),
//        intArrayOf(PackageManager.PERMISSION_GRANTED))
//
//    sub.assertNoErrors()
//    sub.assertTerminated()
//    sub.assertValues(Permission(permission.names[0], true), Permission(permission.names[1], false))
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun eachSubscriptionCombined_severalPermissions_oneRevoked() {
//    val sub = TestObserver<Permission>()
//    val permissions = arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA)
//    given(mRxPermissions!!.isGranted(Matchers.anyVararg<String>())).willReturn(false)
//    given(mRxPermissions!!.isRevoked(Manifest.permission.CAMERA)).willReturn(true)
//
//    trigger().compose(mRxPermissions!!.ensureEachCombined(*permissions)).subscribe(sub)
//    mRxPermissions!!.onRequestPermissionsResult(
//        arrayOf(Manifest.permission.READ_PHONE_STATE),
//        intArrayOf(PackageManager.PERMISSION_GRANTED))
//
//    sub.assertNoErrors()
//    sub.assertTerminated()
//    sub.assertValues(Permission(permission.names[0] + ", " + permissions[1], false))
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun subscription_trigger_granted() {
//    val sub = TestObserver<Boolean>()
//    val permission = Permission(Manifest.permission.READ_PHONE_STATE)
//    given(mRxPermissions!!.isGranted(permission)).willReturn(false)
//    val result = intArrayOf(PackageManager.PERMISSION_GRANTED)
//    val trigger = PublishSubject.create<Any>()
//
//    trigger.compose(mRxPermissions!!.ensure(permission)).subscribe(sub)
//    trigger.onNext(1)
//    mRxPermissions!!.onRequestPermissionsResult(arrayOf(permission.name), result)
//
//    sub.assertNoErrors()
//    sub.assertNotTerminated()
//    sub.assertValue(true)
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun eachSubscription_trigger_granted() {
//    val sub = TestObserver<Permission>()
//    val permission = Permission(Manifest.permission.READ_PHONE_STATE)
//    given(mRxPermissions!!.isGranted(permission)).willReturn(false)
//    val result = intArrayOf(PackageManager.PERMISSION_GRANTED)
//    val trigger = PublishSubject.create<Any>()
//
//    trigger.compose(mRxPermissions!!.ensureEach(permission)).subscribe(sub)
//    trigger.onNext(1)
//    mRxPermissions!!.onRequestPermissionsResult(arrayOf(permission.name), result)
//
//    sub.assertNoErrors()
//    sub.assertNotTerminated()
//    sub.assertValue(Permission(permission.name, true))
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun eachSubscriptionCombined_trigger_granted() {
//    val sub = TestObserver<Permission>()
//    val permission = Permission(Manifest.permission.READ_PHONE_STATE)
//    given(mRxPermissions!!.isGranted(permission)).willReturn(false)
//    val result = intArrayOf(PackageManager.PERMISSION_GRANTED)
//    val trigger = PublishSubject.create<Any>()
//
//    trigger.compose(mRxPermissions!!.ensureEachCombined(permission)).subscribe(sub)
//    trigger.onNext(1)
//    mRxPermissions!!.onRequestPermissionsResult(arrayOf(permission.name), result)
//
//    sub.assertNoErrors()
//    sub.assertNotTerminated()
//    sub.assertValue(Permission(permission.name, true))
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun shouldShowRequestPermissionRationale_allDenied_allRationale() {
//    given(mRxPermissions!!.isMarshmallow).willReturn(true)
//    val activity = mock(Activity::class.java)
//    given(activity.shouldShowRequestPermissionRationale(anyString())).willReturn(true)
//
//    val sub = TestObserver<Boolean>()
//    mRxPermissions!!.shouldShowRequestPermissionRationale(activity, "p1", "p2")
//        .subscribe(sub)
//
//    sub.assertComplete()
//    sub.assertNoErrors()
//    sub.assertValue(true)
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun shouldShowRequestPermissionRationale_allDenied_oneRationale() {
//    given(mRxPermissions!!.isMarshmallow).willReturn(true)
//    val activity = mock(Activity::class.java)
//    given(activity.shouldShowRequestPermissionRationale("p1")).willReturn(true)
//
//    val sub = TestObserver<Boolean>()
//    mRxPermissions!!.shouldShowRequestPermissionRationale(activity, "p1", "p2")
//        .subscribe(sub)
//
//    sub.assertComplete()
//    sub.assertNoErrors()
//    sub.assertValue(false)
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun shouldShowRequestPermissionRationale_allDenied_noRationale() {
//    given(mRxPermissions!!.isMarshmallow).willReturn(true)
//    val activity = mock(Activity::class.java)
//
//    val sub = TestObserver<Boolean>()
//    mRxPermissions!!.shouldShowRequestPermissionRationale(activity, "p1", "p2")
//        .subscribe(sub)
//
//    sub.assertComplete()
//    sub.assertNoErrors()
//    sub.assertValue(false)
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun shouldShowRequestPermissionRationale_oneDeniedRationale() {
//    given(mRxPermissions!!.isMarshmallow).willReturn(true)
//    val activity = mock(Activity::class.java)
//    given(mRxPermissions!!.isGranted("p1")).willReturn(true)
//    given(activity.shouldShowRequestPermissionRationale("p2")).willReturn(true)
//
//    val sub = TestObserver<Boolean>()
//    mRxPermissions!!.shouldShowRequestPermissionRationale(activity, "p1", "p2")
//        .subscribe(sub)
//
//    sub.assertComplete()
//    sub.assertNoErrors()
//    sub.assertValue(true)
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun shouldShowRequestPermissionRationale_oneDeniedNotRationale() {
//    given(mRxPermissions!!.isMarshmallow).willReturn(true)
//    val activity = mock(Activity::class.java)
//    given(mRxPermissions!!.isGranted("p2")).willReturn(true)
//
//    val sub = TestObserver<Boolean>()
//    mRxPermissions!!.shouldShowRequestPermissionRationale(activity, "p1", "p2")
//        .subscribe(sub)
//
//    sub.assertComplete()
//    sub.assertNoErrors()
//    sub.assertValue(false)
//  }
//
//  @Test
//  fun isGranted_preMarshmallow() {
//    // unmock isGranted
//    doCallRealMethod().given<RxPermissions>(mRxPermissions).isGranted(anyString())
//    doReturn(false).given<RxPermissions>(mRxPermissions).isMarshmallow
//
//    val granted = mRxPermissions!!.isGranted("p")
//
//    assertTrue(granted)
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun isGranted_granted() {
//    // unmock isGranted
//    doCallRealMethod().given<RxPermissions>(mRxPermissions).isGranted(anyString())
//    doReturn(true).given<RxPermissions>(mRxPermissions).isMarshmallow
//    given(mActivity!!.checkSelfPermission("p")).willReturn(PackageManager.PERMISSION_GRANTED)
//
//    val granted = mRxPermissions!!.isGranted("p")
//
//    assertTrue(granted)
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun isGranted_denied() {
//    // unmock isGranted
//    doCallRealMethod().given<RxPermissions>(mRxPermissions).isGranted(anyString())
//    doReturn(true).given<RxPermissions>(mRxPermissions).isMarshmallow
//    given(mActivity!!.checkSelfPermission("p")).willReturn(PackageManager.PERMISSION_DENIED)
//
//    val granted = mRxPermissions!!.isGranted("p")
//
//    assertFalse(granted)
//  }
//
//  @Test
//  fun isRevoked_preMarshmallow() {
//    // unmock isRevoked
//    doCallRealMethod().given<RxPermissions>(mRxPermissions).isRevoked(anyString())
//    doReturn(false).given<RxPermissions>(mRxPermissions).isMarshmallow
//
//    val revoked = mRxPermissions!!.isRevoked("p")
//
//    assertFalse(revoked)
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun isRevoked_true() {
//    // unmock isRevoked
//    doCallRealMethod().given<RxPermissions>(mRxPermissions).isRevoked(anyString())
//    doReturn(true).given<RxPermissions>(mRxPermissions).isMarshmallow
//    val pm = mock(PackageManager::class.java)
//    given(mActivity!!.packageManager).willReturn(pm)
//    given(pm.isPermissionRevokedByPolicy(eq("p"), anyString())).willReturn(true)
//
//    val revoked = mRxPermissions!!.isRevoked("p")
//
//    assertTrue(revoked)
//  }
//
//  @Test
//  @TargetApi(Build.VERSION_CODES.M)
//  fun isGranted_false() {
//    // unmock isRevoked
//    doCallRealMethod().given<RxPermissions>(mRxPermissions).isRevoked(anyString())
//    doReturn(true).given<RxPermissions>(mRxPermissions).isMarshmallow
//    val pm = mock(PackageManager::class.java)
//    given(mActivity!!.packageManager).willReturn(pm)
//    given(pm.isPermissionRevokedByPolicy(eq("p"), anyString())).willReturn(false)
//
//    val revoked = mRxPermissions!!.isRevoked("p")
//
//    assertFalse(revoked)
//  }
}
