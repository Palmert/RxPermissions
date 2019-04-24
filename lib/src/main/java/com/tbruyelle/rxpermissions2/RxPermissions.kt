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

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build
import android.support.annotation.VisibleForTesting
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.text.TextUtils

import java.util.ArrayList

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.functions.Function
import io.reactivex.subjects.PublishSubject

class RxPermissions {

  @VisibleForTesting
  internal var mRxPermissionsFragment: Lazy<RxPermissionsFragment>

  private val isMarshmallow: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

  constructor(activity: FragmentActivity) {
    mRxPermissionsFragment = getLazySingleton(activity.supportFragmentManager)
  }

  constructor(fragment: Fragment) {
    mRxPermissionsFragment = getLazySingleton(fragment.childFragmentManager)
  }

  private fun getLazySingleton(fragmentManager: FragmentManager): Lazy<RxPermissionsFragment> {
    return object : Lazy<RxPermissionsFragment> {

      private var rxPermissionsFragment: RxPermissionsFragment? = null

      @Synchronized
      override fun get(): RxPermissionsFragment {
        if (rxPermissionsFragment == null) {
          rxPermissionsFragment = getRxPermissionsFragment(fragmentManager)
        }
        return rxPermissionsFragment!!
      }

    }
  }

  private fun getRxPermissionsFragment(fragmentManager: FragmentManager): RxPermissionsFragment {
    var rxPermissionsFragment: RxPermissionsFragment? = findRxPermissionsFragment(fragmentManager)
    val isNewInstance = rxPermissionsFragment == null
    if (isNewInstance) {
      rxPermissionsFragment = RxPermissionsFragment()
      fragmentManager
          .beginTransaction()
          .add(rxPermissionsFragment, TAG)
          .commitNow()
    }
    return rxPermissionsFragment!!
  }

  private fun findRxPermissionsFragment(fragmentManager: FragmentManager): RxPermissionsFragment? {
    return fragmentManager.findFragmentByTag(TAG) as? RxPermissionsFragment
  }

  fun setLogging(logging: Boolean) {
    mRxPermissionsFragment.get().setLogging(logging)
  }

  /**
   * Map emitted items from the source observable into `true` if permissions in parameters
   * are granted, or `false` if not.
   *
   *
   * If one or several permissions have never been requested, invoke the related framework method
   * to ask the user if he allows the permissions.
   */
  fun <T> ensure(vararg permissions: Permission): ObservableTransformer<T, Boolean> {
    return ObservableTransformer { o ->
      request(o, *permissions)
          // Transform Observable<Permission> to Observable<Boolean>
          .buffer(permissions.size)
          .flatMap(Function<List<Permission>, ObservableSource<Boolean>> { permissions ->
            if (permissions.isEmpty()) {
              // Occurs during orientation change, when the subject receives onComplete.
              // In that case we don't want to propagate that empty list to the
              // subscriber, only the onComplete.
              return@Function Observable.empty()
            }
            // Return true if all permissions are granted.
            for (p in permissions) {
              if (!p.granted) {
                return@Function Observable.just(false)
              }
            }
            Observable.just(true)
          })
    }
  }

  /**
   * Map emitted items from the source observable into [Permission] objects for each
   * permission in parameters.
   *
   *
   * If one or several permissions have never been requested, invoke the related framework method
   * to ask the user if he allows the permissions.
   */
  fun <T> ensureEach(vararg permissions: Permission): ObservableTransformer<T, Permission> {
    return ObservableTransformer { o -> request(o, *permissions) }
  }

  /**
   * Map emitted items from the source observable into one combined [Permission] object. Only if all permissions are granted,
   * permission also will be granted. If any permission has `shouldShowRationale` checked, than result also has it checked.
   *
   *
   * If one or several permissions have never been requested, invoke the related framework method
   * to ask the user if he allows the permissions.
   */
  fun <T> ensureEachCombined(vararg permissions: Permission): ObservableTransformer<T, Permission> {
    return ObservableTransformer { o ->
      request(o, *permissions)
          .buffer(permissions.size)
          .flatMap { permissions ->
            if (permissions.isEmpty()) {
              Observable.empty()
            } else Observable.just(Permission(permissions))
          }
    }
  }

  /**
   * Request permissions immediately, **must be invoked during initialization phase
   * of your application**.
   */
  fun request(vararg permissions: Permission): Observable<Boolean> {
    return Observable.just(TRIGGER).compose(ensure(*permissions))
  }

  /**
   * Request permissions immediately, **must be invoked during initialization phase
   * of your application**.
   */
  fun requestEach(vararg permissions: Permission): Observable<Permission> {
    return Observable.just(TRIGGER).compose(ensureEach(*permissions))
  }

  /**
   * Request permissions immediately, **must be invoked during initialization phase
   * of your application**.
   */
  fun requestEachCombined(vararg permissions: Permission): Observable<Permission> {
    return Observable.just(TRIGGER).compose(ensureEachCombined(*permissions))
  }

  private fun request(trigger: Observable<*>, vararg permissions: Permission): Observable<Permission> {
    if (permissions.isEmpty()) {
      throw IllegalArgumentException("RxPermissions.request/requestEach requires at least one input permission")
    }
    return oneOf(trigger, pending(*permissions))
        .flatMap { requestImplementation(*permissions) }
  }

  private fun pending(vararg permissions: Permission): Observable<*> {
    for (p in permissions) {
      if (!mRxPermissionsFragment.get().containsByPermission(p)) {
        return Observable.empty<Any>()
      }
    }
    return Observable.just(TRIGGER)
  }

  private fun oneOf(trigger: Observable<*>?, pending: Observable<*>): Observable<*> {
    return if (trigger == null) {
      Observable.just(TRIGGER)
    } else Observable.merge<Any>(trigger, pending)
  }

  @TargetApi(Build.VERSION_CODES.M)
  private fun requestImplementation(vararg permissions: Permission): Observable<Permission> {
    val list = ArrayList<Observable<Permission>>(permissions.size)
    val unrequestedPermissions = ArrayList<Permission>()

    // In case of multiple permissions, we create an Observable for each of them.
    // At the end, the observables are combined to have a unique response.
    for (permission in permissions) {
      mRxPermissionsFragment.get().log("Requesting permission $permission")
      if (isGranted(permission)) {
        // Already granted, or not Android M
        // Return a granted Permission object.

        list.add(Observable.just(permission.copy(granted = true, shouldShowRequestPermissionRationale = false)))
        continue
      }

      if (isRevoked(permission)) {
        // Revoked by a policy, return a denied Permission object.
        list.add(Observable.just(permission.copy(granted = false, shouldShowRequestPermissionRationale = false)))
        continue
      }

      var subject: PublishSubject<Permission>? = mRxPermissionsFragment.get().getSubjectByPermission(permission)
      // Create a new subject if not exists
      if (subject == null) {
        unrequestedPermissions.add(permission)
        subject = PublishSubject.create()
        mRxPermissionsFragment.get().setSubjectForPermission(permission.name, subject)
      }

      list.add(subject)
    }

    if (!unrequestedPermissions.isEmpty()) {
      val unrequestedPermissionsArray = unrequestedPermissions.toTypedArray()
      requestPermissionsFromFragment(unrequestedPermissionsArray)
    }
    return Observable.concat(Observable.fromIterable(list))
  }

  /**
   * Invokes Activity.shouldShowRequestPermissionRationale and wraps
   * the returned value in an observable.
   *
   *
   * In case of multiple permissions, only emits true if
   * Activity.shouldShowRequestPermissionRationale returned true for
   * all revoked permissions.
   *
   *
   * You shouldn't call this method if all permissions have been granted.
   *
   *
   * For SDK &lt; 23, the observable will always emit false.
   */
  fun shouldShowRequestPermissionRationale(activity: Activity, permissions: List<Permission>): Observable<Boolean> {
    return if (!isMarshmallow) {
      Observable.just(false)
    } else Observable.just(
        shouldShowRequestPermissionRationaleImplementation(activity, permissions))
  }

  @TargetApi(Build.VERSION_CODES.M)
  private fun shouldShowRequestPermissionRationaleImplementation(activity: Activity, permissions: List<Permission>): Boolean {
    for (p in permissions) {
      if (!isGranted(p) && !activity.shouldShowRequestPermissionRationale(p.name)) {
        return false
      }
    }
    return true
  }

  @TargetApi(Build.VERSION_CODES.M)
  internal fun requestPermissionsFromFragment(permissions: Array<Permission>) {
    mRxPermissionsFragment.get().log("requestPermissionsFromFragment " + TextUtils.join(", ", permissions))
    mRxPermissionsFragment.get().requestPermissions(permissions)
  }

  /**
   * Returns true if the permission is already granted.
   *
   *
   * Always true if SDK &lt; 23.
   */
  fun isGranted(permission: Permission): Boolean {
    return !isMarshmallow || mRxPermissionsFragment.get().isGranted(permission)
  }

  /**
   * Returns true if the permission has been revoked by a policy.
   *
   *
   * Always false if SDK &lt; 23.
   */
  fun isRevoked(permission: Permission): Boolean {
    return isMarshmallow && mRxPermissionsFragment.get().isRevoked(permission)
  }

  internal fun onRequestPermissionsResult(permissions: Array<String>, grantResults: IntArray) {
    mRxPermissionsFragment.get().onRequestPermissionsResult(permissions, grantResults, BooleanArray(permissions.size))
  }

  @FunctionalInterface
  interface Lazy<V> {
    fun get(): V
  }

  companion object {

    internal val TAG = RxPermissions::class.java.simpleName
    internal val TRIGGER = Any()
  }

}
