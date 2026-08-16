package com.pathsathi.app.map
import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.*
object GeoUtils{private const val R=6371.0;fun distanceKm(a:GeoPoint,b:GeoPoint):Double{val dl=Math.toRadians(b.lat-a.lat);val dn=Math.toRadians(b.lng-a.lng);val x=Math.toRadians(a.lat);val y=Math.toRadians(b.lat);val h=sin(dl/2).let{it*it}+cos(x)*cos(y)*sin(dn/2).let{it*it};return R*2*atan2(sqrt(h),sqrt(1-h))};fun estimateMinutes(d:Double,m:TravelMode)=((d/when(m){TravelMode.WALKING->4.5;TravelMode.LOCAL_TRANSIT->15.0;TravelMode.DRIVING->30.0})*60).toInt().coerceAtLeast(1);fun estimate(a:GeoPoint,b:GeoPoint,m:TravelMode)=DistanceEstimate(distanceKm(a,b),estimateMinutes(distanceKm(a,b),m),m,true);suspend fun geocode(c:Context,q:String):GeoPoint?=withContext(Dispatchers.IO){if(q.isBlank()||!Geocoder.isPresent())return@withContext null;try{@Suppress("DEPRECATION") val r=Geocoder(c,Locale.getDefault()).getFromLocationName(q,1);r?.firstOrNull()?.let{GeoPoint(it.latitude,it.longitude)}}catch(_:Exception){null}}}
